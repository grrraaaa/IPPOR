pipeline {
    // Код приходит из Git автоматически (checkout scm) — customWorkspace из ЛР3 больше не нужен.
    agent any

    environment {
        JAVA_HOME = 'C:/jdk17'
    }

    stages {
        stage('Checkout info') {
            steps {
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
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success { echo 'BUILD SUCCESS: Build -> Static Analysis -> Test -> Package passed' }
        failure { echo 'BUILD FAILURE: check Console Output (compilation, Checkstyle or tests)' }
    }
}
