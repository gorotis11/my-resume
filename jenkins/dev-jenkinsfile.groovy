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
                    command: ["sleep"]
                    args: ["9999999"]
                    volumeMounts:
                    - name: registry-auth
                      mountPath: /kaniko/.docker
                  - name: kubectl
                    image: bitnami/kubectl:latest
                    command: ["sleep"]
                    args: ["9999999"]
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
                    --skip-tls-verify \
                    --insecure \
                    --cache=true
                    """
                }
            }
        }

        stage('Deploy to K8s') {
            steps {
                container('kubectl') {
                    script {
                        // 1. 배포용 YAML 파일 적용 (미리 작성된 manifest가 프로젝트에 있어야 함)
                        sh "kubectl apply -f k8s/deployment.yaml"

                        // 2. 이미지가 새로 푸시되었으므로 강제 재시작 (Rolling Update 트리거)
                        sh "kubectl rollout restart deployment my-resume -n default"

                        // 3. 배포 상태 확인
                        sh "kubectl rollout status deployment my-resume -n default"
                    }
                }
            }
        }
    }
}