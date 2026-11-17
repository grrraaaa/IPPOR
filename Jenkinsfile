pipeline {
    agent any

    options {
        skipDefaultCheckout()
    }

    environment {
        JAVA_HOME = 'C:/jdk17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                bat 'git log -1 --pretty=format:"commit %%h | %%an | %%s"'
            }
        }
        stage('Build') {
            steps {
                bat 'mvn -B compile'
            }
        }
        stage('Static Analysis') {
            steps {
                bat 'mvn -B checkstyle:check'
            }
        }
        stage('Test') {
            steps {
                bat 'mvn -B test'
            }
        }
        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/delivery-*.jar', fingerprint: true
            }
        }
    }

    post {
        success { echo 'BUILD SUCCESS: Build -> Static Analysis -> Test -> Package passed' }
        failure { echo 'BUILD FAILURE: check Console Output (compilation, Checkstyle or tests)' }
    }
}
