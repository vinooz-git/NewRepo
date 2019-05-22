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
	def hostnames = []	      
    def readFileContents = new File(FileLocation).getText()
    def config = new XmlSlurper().parseText(readFileContents)
	config.'VMSetup'.'Machine'.each {node -> hostnames.push(node.@'host')}
    hostnames = hostnames.toArray();
	println "Hostname "+hostnames.toString()
    for (host in hostnames) {
		def network = [];
		def action = [];
		def snapshot = [];
        def nodeName = host.toString();
        parallelExec [nodeName] = {
        config.'VMSetup'.'**'.find { node -> 
		if(node.@'host' == nodeName){node.'Operation'.each {node1 -> network.push(node1.@'network')}
		}}
        config.'VMSetup'.'**'.find { node ->
		if(node.@'host' == nodeName){node.'Operation'.each {node1 -> action.push(node1.@'action')}
		}}
        config.'VMSetup'.'**'.find { node -> 
		if(node.@'host' == nodeName){node.'Operation'.each {node1 -> snapshot.push(node1.@'snap')}
		}} 
        echo "Hostname: ${nodeName}; NetworkName: ${network}; Actions: ${action}; Snap: ${snapshot}"
		for(int i=0;i<action.size; i++)
		{
			def actions =action[i].toString();
			if(actions.contains(poweroff)){
				VmPowerOff(nodeName,network[i].toString())
			}
			else if(actions.contains(revert)){	
				VmPowerOff(nodeName,network[i].toString())
				sleep 15;
				VmRevert(nodeName,network[i].toString(),snapshot[i].toString())
				sleep 15;
				VmPowerOn(nodeName,network[i].toString())
				sleep 20;
			}
			else if(actions.contains(poweron)){
				println"Vm power on loop"
				VmPowerOn(nodeName.toString(),network[i].toString())
				sleep 15;
			}	
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