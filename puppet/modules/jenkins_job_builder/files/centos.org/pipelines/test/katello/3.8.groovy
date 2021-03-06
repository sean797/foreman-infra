pipeline {
    agent { label 'foreman' }

    stages {
        stage('Setup Environment') {
            steps {
                deleteDir()
                git url: 'https://github.com/theforeman/forklift.git'
            }
        }
        stage('Provision Node') {
            steps {
                provision()
            }
        }
        stage('Install Pipeline Requirements') {
            steps {
                runPlaybook(
                    playbook: 'playbooks/setup_forklift.yml',
                    inventory: cico_inventory('./'),
                    options: ['-b']
                )
            }
        }
        stage('Run Pipeline') {
            steps {
                duffy_ssh("cd forklift && ansible-playbook pipelines/pipeline_katello_38.yml -e forklift_state=up", 'duffy_box', './')
            }
        }
    }

    post {
        always {
            script {
                duffy_ssh("cd forklift && ansible-playbook playbooks/collect_debug.yml -l pipeline-katello-3.8-centos7", 'duffy_box', './')
                runPlaybook(
                    playbook: 'foreman-infra/ci/centos.org/ansible/fetch_debug_files.yml',
                    inventory: cico_inventory('./'),
                    extraVars: ["workspace=/home/foreman/workspace/${env.JOB_NAME}/debug"],
                    options: ['-b']
                )
            }

            archiveArtifacts artifacts: 'debug/**/*.tap'
            archiveArtifacts artifacts: 'debug/**/*.tar.xz'

            deprovision()
            deleteDir()
        }
    }
}
