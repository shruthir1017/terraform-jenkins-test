pipeline {
    agent { label 'slave2' }

    parameters {
        booleanParam(name: 'autoApprove', defaultValue: false, description: 'Automatically run apply after generating plan?')
        choice(name: 'action', choices: ['apply', 'destroy'], description: 'Select the action to perform')
    }

    environment {
        AWS_ACCESS_KEY_ID     = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        AWS_DEFAULT_REGION    = 'us-east-1'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/shruthir1017/terraform-jenkins-new.git'
            }
        }
        stage('Terraform init') {
            steps {
                sh 'terraform init'
            }
        }
        stage('Plan') {
            steps {
                sh 'terraform plan -out tfplan'
                sh 'terraform show -no-color tfplan > tfplan.txt'
            }
        }
        stage('Apply / Destroy') {
            steps {
                script {
                    // Function definition for apply or destroy logic
                    def applyOrDestroy(action, autoApprove) {
                        if (action == 'apply') {
                            if (!autoApprove) {
                                def plan = readFile 'tfplan.txt'
                                input message: "Do you want to apply the plan?", parameters: [text(name: 'Plan', description: 'Please review the plan', defaultValue: plan)]
                            }
                            sh "terraform apply -input=false tfplan"
                        } else if (action == 'destroy') {
                            if (!autoApprove) {
                                input message: "Are you sure you want to destroy all resources? This action is irreversible and may result in data loss. Type 'yes' to confirm destruction.", parameters: [string(defaultValue: 'no', description: 'Type "yes" to confirm destruction', name: 'Confirmation')]
                            }
                            // Remove --auto-approve to ensure confirmation step is respected
                            sh "terraform destroy -input=false"
                        }
                    }
                    
                    // Call the function with parameters from pipeline
                    applyOrDestroy(params.action, params.autoApprove)
                }
            }
        }
    }
}
