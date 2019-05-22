/*
Scope : Perform VM operations - PowerOFF/ON, Revert based on Property file details
Parameters :
  propertyFileLoc - Project pipeline Property file 
*/
package VmSetup

import groovy.json.JsonSlurperClassic
import groovy.json.JsonSlurper
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.NodeChild
import groovy.util.XmlParser
import groovy.util.slurpersupport.GPathResult

def VMOperationCall(def FileLocation)
{
   //InputFile Keywords and given variables should be matched
	def poweroff = "VmPowerOff"
	def poweron = "VmPowerOn"    
	def revert = "VmRevert"
	def parallelExec = [:]         //Empty map for parallel execution
	def hostnames,Labelnames = []	      
	
    def readFileContents = new File(FileLocation).getText()
    def config = new XmlSlurper().parseText(readFileContents)
	config.VMSetup.Server.each {node -> Labelnames.push(node['@label'])}
	println "label value :"+Labelnames.toString()
    config.VMSetup.Server.each {node -> hostnames.push(if(node['@host'])!=null)}
    hostnames = hostnames.toArray();
    for (host in hostnames) {
        def nodeName = host.toString();
        parallelExec [nodeName] = {
        def network = config.VMSetup.'**'.find { Server -> Server['@host'] == nodeName}['@network'].toString()
        def action = config.VMSetup.'**'.find { Server -> Server['@host'] == nodeName}['@actions'].toString()
        def snapshot = config.VMSetup.'**'.find { Server -> Server['@host'] == nodeName}['@snap'].toString()
        echo "Hostname: ${nodeName}; NetworkName: ${network}; Actions: ${action}; Snap: ${snapshot}"
		if(action.contains(poweroff)){
				VmPowerOff(nodeName,network)
				sleep 15;
			}
		if(action.contains(revert)){	
				VmPowerOff(nodeName,network)
				sleep 15;
				VmRevert(nodeName,network,snapshot)
				sleep 15;
				VmPowerOn(nodeName,network)
				sleep 20;
			}
		if(action.contains(poweron)){
				VmPowerOn(nodeName,network)
				sleep 15;
			}	
        }
    }
	 parallel parallelExec
}

@NonCPS
def VmRevert(String VmName,String Network,String Snapshot){
	vSphere buildStep: [$class: 'RevertToSnapshot', snapshotName: Snapshot.trim(), vm: VmName.trim()], serverName: Network.trim()
	echo "${VmName} is Reverted to ${Snapshot} - Snapshot"
	}

@NonCPS
def VmPowerOn(String VmName,String Network){
	vSphere buildStep: [$class: 'PowerOn', timeoutInSeconds: 260, vm: VmName.trim()], serverName: Network.trim()
	echo "${VmName} is Switched ON"
	}

@NonCPS
def VmPowerOff(String VmName,String Network){
	    vSphere buildStep: [$class: 'PowerOff', evenIfSuspended: false, ignoreIfNotExists: false, shutdownGracefully: false, vm: VmName.trim()], serverName: Network.trim()
		echo "${VmName} is Switched Off"	
	}

@NonCPS
def nodeNames(label) {
  def nodes = []
  jenkins.model.Jenkins.instance.computers.each { c ->
    if (c.node.labelString.equals(label)) {
      nodes.add(c.node.selfLabel.name)
      //print "Node found under ${label}- ${c.node.selfLabel.name}"
    }
  }
  return nodes
}