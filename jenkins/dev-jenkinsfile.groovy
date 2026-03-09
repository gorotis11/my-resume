pipeline {
    agent {
        kubernetes {
            yaml """
                apiVersion: v1
                kind: Pod
                spec:
                  containers:
                  - name: kaniko
                    image: gcr.io/kaniko-project/executor:debug
                    command:
                    - sleep
                    args:
                    - 9999999
                    volumeMounts:
                    - name: registry-auth
                      mountPath: /kaniko/.docker
                  volumes:
                  - name: registry-auth
                    emptyDir: {}
                """
        }
    }

    environment {
        // 아까 설치한 로컬 레지스트리 주소
        DOCKER_FILE = "docker/Dockerfile"
        REGISTRY = "local-registry.registry.svc.cluster.local:443"
        IMAGE_NAME = "my-resume"
        TAG = "latest"
    }

    stages {
        stage('Checkout') {
            steps {
                // GitHub에서 소스 코드 가져오기
                checkout scm
            }
        }

        stage('Build and Push') {
            steps {
                container('kaniko') {
                    // Kaniko 실행: Docker 데몬 없이 이미지 빌드 및 푸시
                    sh """
                    /kaniko/executor \
                    --context=\${WORKSPACE} \
                    --dockerfile=\${DOCKER_FILE}\
                    --destination=\${REGISTRY}/\${IMAGE_NAME}:\${TAG} \
                    --cache=true
                    """
                }
            }
        }
    }
}