/* vim:set ts=4:sw=4:et:sts=4:ai:tw=80
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.log4j.*
import groovy.util.logging.*

class PackageBuilder {
	def BDROOT
	def LOG
	def globalConfig
	def specfile

    // rpmbuild -ba -D'_topdir /home/jroman/javi' javi/SPECS/flume.spec
	def PackageBuilder(buildoop) {
		LOG = buildoop.log
		BDROOT = buildoop.ROOT
		globalConfig = buildoop.globalConfig
        LOG.info "[PackageBuilder] constructor"
	}

    def makeWorkingFolders(basefolders) {
        LOG.info "[PackageBuilder:makeWorkingFolders] making folders"
		new File(basefolders["dest"]).mkdir()
		new File(basefolders["dest"] + "/rpmbuild").mkdir()
		new File(basefolders["dest"] + "/rpmbuild/SPECS").mkdir()
		new File(basefolders["dest"] + "/rpmbuild/SOURCES").mkdir()
		new File(basefolders["dest"] + "/rpmbuild/BUILD").mkdir()
    }


	def copyFile(src, dest) {
 
		println "copying " + src
		println " to " + dest
		def input = src.newDataInputStream()
		def output = dest.newDataOutputStream()
 
		output << input 
 
		input.close()
		output.close()
	}

	def runCommand(strList)  { 
		assert (strList instanceof String ||
            (strList instanceof List && strList.each{ it instanceof String }))

  		def proc = strList.execute()
  			proc.in.eachLine { 
				line -> println line 
  		}

  		proc.out.close()
  		proc.waitFor()

  		print "[INFO] ( "
  		if(strList instanceof List) {
    		strList.each { print "${it} " }
  		} else {
    		print "command: " + strList
  		}
  		println " )"

  		if (proc.exitValue()) {
    	println "gave the following error: "
    	println "[ERROR] ${proc.getErrorStream()}"
  		}
  		assert !proc.exitValue()
	}

    def copyBuildFiles(basefolders) {
        println "copy source code to " + basefolders["dest"] + "/rpmbuild/SOURCES"
        println "copy spec file to " + basefolders["dest"] + "/rpmbuild/SPECS"
 
		// rpm/sources staff copy to work folder
		def folderIn = basefolders["src"] + "/rpm/sources/"
		def folderOut = basefolders["dest"] + "/rpmbuild/SOURCES/"

		new File(folderIn).eachFileRecurse { 
			copyFile(new File(folderIn + it.name), 
				 new File(folderOut + it.name))
		}

		// source code package from download area to work folder
		def srcFile  = new File(basefolders["srcpkg"])
		def destFile = new File(basefolders["dest"]  + 
							"/rpmbuild/SOURCES/" +  
							basefolders["srcpkg"].split('/')[-1])

		copyFile(srcFile, destFile)

		// copy SPEC file
		folderIn = basefolders["src"] + "/rpm/specs/"
		folderOut = basefolders["dest"] + "/rpmbuild/SPECS/"

		new File(folderIn).eachFileRecurse { 
			specfile = it.name
			copyFile(new File(folderIn + it.name), 
				 new File(folderOut + it.name))
		}
    }

    def execRpmBuild(basefolders, buildoop) {
        def command = "rpmbuild -ba -D'_topdir " + buildoop.ROOT + "/" +
            	basefolders["dest"] + "/rpmbuild" + "' " +
            	basefolders["dest"] + "/rpmbuild/SPECS/" + specfile.split('/')[-1]
                
		println "Executing: " +  command
		runCommand(["bash", "-c", command])
    }

	def moveToDeploy(basefolders, buildoop) {
		// RPMS folder
		def folderIn = buildoop.ROOT + "/" + basefolders["dest"] + 
				"/rpmbuild/RPMS/noarch/"

		def folderOut = buildoop.ROOT + "/" + 
					buildoop.globalConfig.buildoop.bomdeploybin + "/"

		new File(folderIn).eachFileRecurse { 
			new File(folderIn + "/" + it.name).
				renameTo(new File(folderOut + "/" + it.name))
		}

		// SRPMS folder
		folderIn = buildoop.ROOT + "/" + basefolders["dest"] + 
				"/rpmbuild/SRPMS/"

		folderOut = buildoop.ROOT + "/" + 
					buildoop.globalConfig.buildoop.bomdeploysrc + "/"

		new File(folderIn).eachFileRecurse { 
			new File(folderIn + "/" + it.name).
				renameTo(new File(folderOut + "/" + it.name))
		}
	}
}

















