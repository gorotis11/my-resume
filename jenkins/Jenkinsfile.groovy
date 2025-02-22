pipeline {
    agent any

    environment {
        SERVICE_NAME = 'my-resume'
        SERVICE_VERSION = 'latest'

        GIT_REPOSITORY_URL = "https://github.com/gorotis11/${SERVICE_NAME}"
        GIT_CREDENTIALS = 'github-token'

        DOCKER_OS = "linux/amd64"
        DOCKER_REGISTRY = '192.168.45.51:5000'
        DOCKER_IMAGE_NAME = "${DOCKER_REGISTRY}/${SERVICE_NAME}"
        DOCKER_HOST_NON_SECURE_PORT=18080

        JENKINS_SSH_PK = '~/.ssh/jenkins_rsa'
        JENKINS_SSH_CREDENTIALS = 'spring-thief-apps'

        DEPLOY_HOST = "wien@192.168.45.61"
        DEPLOY_UPLOAD_PATH = "/app/docker/${SERVICE_NAME}"
        DEPLOY_DOCKER_COMPOSE_FILE_NAME = "${SERVICE_NAME}.yml"
    }

    stages {
        stage('gradle version') {
            steps {
                script {
                    def output = sh(returnStdout: true, script: "./gradlew properties -q | grep \"version:\" | awk '{print \$2}'").trim()
                    SERVICE_VERSION = output
                }
            }
        }
        stage('gradle clean') {
            steps {
                sh './gradlew clean'
            }
        }
        stage('gradle build') {
            steps {
                sh './gradlew build'
            }
        }
        stage('build docker image') {
            steps {
                sh "docker build --platform ${DOCKER_OS} -t ${DOCKER_IMAGE_NAME}:${SERVICE_VERSION} -f docker/Dockerfile ."
            }
        }
        stage('tag docker image') {
            steps {
                sh "docker tag ${DOCKER_IMAGE_NAME}:${SERVICE_VERSION} ${DOCKER_IMAGE_NAME}:${SERVICE_VERSION}"
            }
        }
        stage('push docker image') {
            steps {
                sh "docker push ${DOCKER_IMAGE_NAME}:${SERVICE_VERSION}"
            }
        }
        stage('remove docker image') {
            steps {
                // sh "docker rmi -f \$(docker images -f \"dangling=true\" -q)"
                sh "docker system prune -f"
            }
        }
        stage('upload docker-compose file') {
            steps {
                sshagent (credentials: [JENKINS_SSH_CREDENTIALS]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                            rm -rf ${DEPLOY_UPLOAD_PATH}
                            mkdir -p ${DEPLOY_UPLOAD_PATH}
                        '
                    """
                    sh "scp -i ${JENKINS_SSH_PK} docker/${DEPLOY_DOCKER_COMPOSE_FILE_NAME} ${DEPLOY_HOST}:${DEPLOY_UPLOAD_PATH}"
                }
            }
        }
        stage('deploy docker container') {
            steps {
                sshagent (credentials: [JENKINS_SSH_CREDENTIALS]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                            docker pull ${DOCKER_IMAGE_NAME}:${SERVICE_VERSION}
                        '
                    """

                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                            SERVICE_NAME=${SERVICE_NAME} \\
                            SERVICE_VERSION=${SERVICE_VERSION} \\
                            DOCKER_IMAGE_NAME=${DOCKER_IMAGE_NAME} \\
                            DOCKER_HOST_NON_SECURE_PORT=${DOCKER_HOST_NON_SECURE_PORT} \\
                            docker compose -f ${DEPLOY_UPLOAD_PATH}/${DEPLOY_DOCKER_COMPOSE_FILE_NAME} -p ${SERVICE_NAME} down
                        '
                    """

                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                            SERVICE_NAME=${SERVICE_NAME} \\
                            SERVICE_VERSION=${SERVICE_VERSION} \\
                            DOCKER_IMAGE_NAME=${DOCKER_IMAGE_NAME} \\
                            DOCKER_HOST_NON_SECURE_PORT=${DOCKER_HOST_NON_SECURE_PORT} \\
                            docker compose -f ${DEPLOY_UPLOAD_PATH}/${DEPLOY_DOCKER_COMPOSE_FILE_NAME} -p ${SERVICE_NAME} up -d
                        '
                    """
                }
            }
        }
    }
}