// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def hygieiaGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/Hygieia"

// Jobs
def environmentProvisioningPipelineView = buildPipelineView(projectFolderName + "/Hygieia_Provisioning")
def createHygieiaJob = freeStyleJob(projectFolderName + "/Create_Hygieia")
def destroyHygieiaJob = freeStyleJob(projectFolderName + "/Destroy_Hygieia")

// Create Hygieia
createHygieiaJob.with{
    description('''This job creates a Hygieia dashboard instance (see https://github.com/capitalone/Hygieia).''')
    label("docker")
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent("adop-jenkins-master")
    }
    steps {
        shell('''set +x
                |docker-compose up -d
                |docker exec mongodb mongo localhost/admin  --eval \'db.getSiblingDB("dashboard").createUser({user: "db", pwd: "dbpass", roles: [{role: "readWrite", db: "dashboard"}]})\' || true
                |set -x'''.stripMargin())
    }
    scm {
        git {
            remote {
                name("origin")
                url("${hygieiaGitUrl}")
                credentials("adop-jenkins-master")
            }
            branch("*/master")
        }
    }
    publishers {
        buildPipelineTrigger("${PROJECT_NAME}/Destroy_Hygieia") {
            parameters {
                currentBuild()
            }
        }
    }
}

// Destroy Hygieia
destroyHygieiaJob.with{
    description("This job deletes the environment.")
    label("docker")
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    scm {
        git {
            remote {
                name("origin")
                url("${hygieiaGitUrl}")
                credentials("adop-jenkins-master")
            }
            branch("*/master")
        }
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent("adop-jenkins-master")
    }
    steps {
        shell('''set +x
            |docker-compose kill 
            |docker-compose rm -f
            |set -x'''.stripMargin())
    }
}

// Pipeline
environmentProvisioningPipelineView.with{
    title('Hygieia Provisioning Pipeline')
    displayedBuilds(5)
    selectedJob("Create_Hygieia")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}



