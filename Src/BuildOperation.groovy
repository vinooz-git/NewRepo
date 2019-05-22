package BuildLibrary
import groovy.json.JsonSlurperClassic
import groovy.json.JsonSlurper
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.NodeChild
import groovy.util.XmlParser
import groovy.util.slurpersupport.GPathResult

def BuildOperationCall(def FileLocation)
{
//InputFile Keywords and given variables should be matched
	def BuildDownload = "Download"
	def poweron = "VmPowerOn"    
	def revert = "VmRevert"
	def parallelExec = [:]         //Empty map for parallel execution
	def hostnames = []	      
    def readFileContents = new File(FileLocation).getText()
    def config = new XmlSlurper().parseText(readFileContents)
	config.'ServerSetup'.'Machine'.each {node -> hostnames.push(node.@'host')}
    hostnames = hostnames.toArray();
	println "Hostname "+hostnames.toString()
    for (host in hostnames) {
		def action = [];
		def BuildDesc = [];
        def nodeName = host.toString();
        parallelExec [nodeName] = {
        config.'VMSetup'.'**'.find { node ->
		if(node.@'host' == nodeName){node.'Operation'.each {node1 -> action.push(node1.@'action')}
		}}
        config.'VMSetup'.'**'.find { node -> 
		if(node.@'host' == nodeName){node.'Operation'.each {node1 -> BuildDesc.push(node1.@'BuildDesc')}
		}} 
        echo "Hostname: ${nodeName}; NetworkName: ${network}; Actions: ${action}; Snap: ${snapshot}"
		for(int i=0;i<action.size; i++)
		{
			def actions =action[i].toString();
			def builddesc = BuildDesc[i].toString();
			if(actions.contains(BuildDownload)){
				config.BuildAvailability.Builds.each { node ->
				def BuildUrl= config.BuildAvailability.'**'.find { Server -> Server['@Desc'] == builddesc}['@URL'].toString()
				 
				 //Download latest build 
				 DownloadByHttpReq()
			
				
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


def DownloadByHttpReq(outputfileLoc,BuildUrl)
{
 //Download latest build 
 httpRequest ignoreSslErrors: true, outputFile: outputfileLoc, responseHandle: 'NONE', url: BuildUrl		
}


























  def Buildoperation = "BuildDownload"
  def TriggerExecution = "ServerInstall"
  def ServersBuildDownload = [:]
  def ServerExecution =[:]
  def reader = new Utils.CsvReader()
  println "inputdata :"+inputdata.toString()
  fileContent = inputdata.toArray();
	for(content in fileContent)
	{
		def BuildUrl = [];
		def BuildOutputLoc = null;
		def Serv_ExecFileLoc = null;
		
		
		
		
		Map<String,List> argumentList = new HashMap<String,List>();
		argumentList= reader.argumentGetter(content);
		BuildOutputLoc = argumentList[Buildoperation][1].trim()
		println"BuildOutputLoc:"+BuildOutputLoc
		BuildUrl = getBuildUrl(BuildOutputLoc,argumentList[Buildoperation][2])
	        println"BuildUrl:"+BuildUrl.toString()
		if(argumentList.containsKey("BuildDownload"))
		{
		 def VmName = argumentList.values().toArray()[0][0]
		 ServersBuildDownload["node_" + VmName] = {
			node(VmName) 
			{		
			 //Download latest build 
			 httpRequest ignoreSslErrors: true, outputFile: BuildUrl.get(1), responseHandle: 'NONE', url: BuildUrl.get(0)
				
			 //Extract the Build
			 fileOperations([fileUnZipOperation(filePath: BuildUrl.get(1), targetLocation: BuildOutputLoc)])
			
		     /*For PACS Project */
			 String[] Tempfoldername  = BuildUrl.get(2).split("[.]");
			 String FolderName = Tempfoldername[0].replaceAll("%20"," ");
			 String CopyFromFolder = BuildOutputLoc +"\\"+FolderName;     
			 def deleteFile = BuildOutputLoc +"\\*.zip";
			 //Copy and paste the content outside the Directory Delete the File and other unwanted Folder - Only for Pacs Project
			 CopyAndDelete(CopyFromFolder,BuildOutputLoc,deleteFile)
			 
			}
		  }
		}
		if(argumentList.containsKey(TriggerExecution))
		{
		 def VmName = argumentList.values().toArray()[0][0]
		 println"Vm Name is "+VmName
		 Serv_ExecFileLoc = argumentList[TriggerExecution][1]
		 println "Serv_ExecFileLoc :"+Serv_ExecFileLoc
		 ServerExecution["Exec_" + VmName] = {
			node(VmName) 
			{		
			//Running Server Installation Script
			 bat label: '', script: "(cmd/c call \"${Serv_ExecFileLoc}\")"
			}
		  }
		}
	}
  parallel ServersBuildDownload;
  parallel ServerExecution;
}

def CopyAndDelete(CopyFromFolder,BuildOutputLoc,deleteFile)
{
	//Copy File and folder /* This step only for PACS Server Setup*
	bat label: '', script: "((robocopy \"${CopyFromFolder}\" ${BuildOutputLoc} /S /MT:100 > C:\\log.txt) ^& IF %ERRORLEVEL% LEQ 4 exit /B 0)"
	//Delete unwanted folders and files
	bat label: '', script: "(RD /S /Q \"${CopyFromFolder}\" > C:\\Deletelog1.txt)"
    bat label: '', script: "(del ${deleteFile} > C:\\Deletelog1.txt)"
}


def getBuildUrl(buildOutLoc,buildUrl)
{	
	def buildCmd = []; 
	buildCmd.add(buildUrl.trim());
	String[] filenametemp = buildUrl.split('/');
	def Filename = filenametemp[filenametemp.size()-1]
	def BuildCopyLoc = buildOutLoc+"\\"+Filename;  //Build download Location 
	buildCmd.add(BuildCopyLoc.trim());
	buildCmd.add(Filename.trim())
  return buildCmd
}
