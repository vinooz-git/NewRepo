#!groovy
@Library('ProcessToolsLib@TestBranchV2.0') _
def fileOperation = new Utils.FileOperations()
def Email = new Utils.EmailUtils()
node('master')
{
	NodeName = "${params.ExecutionNode}"
	print "Execution Node :"+NodeName
}
pipeline {
  agent {
        label NodeName
		}	
    environment {
		def continuePipeline = true
        XML = fileOperation.FileUpload("Configuration_XML")
        InventYML = fileOperation.FileUpload("Inventory_YML")
        def Master_Workspace = "${env.WORKSPACE}"+"@libs//ProcessToolsLib"
		MasterNode = "${env.NODE_NAME}"
		MasterWorkspace = "${env.NODE_NAME == "master" ? "${Master_Workspace}" : "${env.WORKSPACE}"}"   
		def jenURL = "${env.JENKINS_URL}"
		def linuxPlaybook = "${MasterWorkspace}/ansibleActions/playbooks/slaveconfig-lnx.yml"
		def agentFilePath = "${MasterWorkspace}/ansibleActions/support/agent.jar"
		def linuxJavaPath = "${MasterWorkspace}/ansibleActions/support/jdk11_L.tar.gz"
		def wdwPlaybook = "${MasterWorkspace}/ansibleActions/playbooks/slaveconfig-wnd.yml"
		def windowsJavaPath = "${MasterWorkspace}/ansibleActions/support/jdk11_W.zip"
		def guestOPScript = "${MasterWorkspace}/Support/guestOpsManagement.pl"
		def secretIDPath = "${MasterWorkspace}/ansibleActions/playbooks/vars/secretID.yml"
		def WinrmWrkDir = "C:\\\\Winrm"
		def upgradePSPath = "${MasterWorkspace}/Support/Upgrade-PowerShell.ps1"
		def wmf3HotfixPSPath = "${MasterWorkspace}/Support/Install-WMF3Hotfix.ps1"
		def winrmPSPath = "${MasterWorkspace}/Support/winrm_enable.ps1"
		def winrmBatPath = "${MasterWorkspace}/Support/WinrmEnable.bat"
		def winexesvcPath = "${MasterWorkspace}/Support/winexesvc.exe"
		Vspherecre = credentials('VSPHERE')
        EncryptionKey = credentials('EncryptionAESKey')
		def stageChoice = "${params.StageSelection}"
		}
	options {
		timestamps()
		}
    stages {
	stage('CheckPatch Update') {
	 when { 
          expression {stageChoice == "SecurityBatch"}
        }
       steps {
          script {
			def continuePipeline = CheckPatchUpdate()
			echo "Check Patch Stage completed with return Status ${continuePipeline}"
           }
        }
      }
	stage('Build Availability') {
	 when { 
          expression {stageChoice == "All Stages"}
        }
       steps {
          script {
			def methodcall = new BuildLibrary.BuildAvailability()
			continuePipeline = methodcall.BuildAvailCall()
            echo "BuildAvailability stage completed with return Status ${continuePipeline}"
           }
        }
      }
	  stage('VM Setup') {
	   when { 
          expression {continuePipeline == true && stageChoice == "All Stages"}
        }
        steps {
          script {
            def methodcall = new VMLibrary.VmOperation()
			methodcall.VMOperationCall()
			echo "Vm operations completed"
			}
        }
      }
	  stage('Server Execution') {
	   when { 
          expression {continuePipeline == true && stageChoice == "All Stages"}
        }
	    steps {
          script {
            def methodcall = new ServerSetup.ServerSetup()
			methodcall.ServerOperations()
			echo "ServerSetup Stage Completed" 
           }
        }
      }
	stage('Script Execution') {
	     when { 
            expression {continuePipeline == true && stageChoice == "All Stages"}
          }
	     steps {
            script {
              def methodcall = new ClientSetup.ClientSetup()
			  methodcall.ClientOperations()
			  echo "ClientSetup stage"
             }
          }
        }
	stage('CheckPatch Update') {
	 when { 
          expression {stageChoice == "SecurityBatch"}
        }
       steps {
          script {
			ReportGeneration.groovy()
			echo "Check Patch Stage completed"
           }
        }
      }
	
  }
  post {
        success {
		 script {
             echo 'PipeLine Execution succeeeded!:)'
			 Email.EmailNotification(null,null,"JobSuccess","${env.JOB_NAME} #${env.BUILD_NUMBER}")
			}
        }
       failure {
	    script {
            echo 'PipeLine Execution Failed :('
			Email.EmailNotification(null,null,"JobFailure","${env.JOB_NAME} #${env.BUILD_NUMBER}")
			 }
        }
        
    }
}
      