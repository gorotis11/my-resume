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
    command: ["/busybox/cat"]
    tty: true
    resources:
      requests:
        cpu: "500m"
        memory: "512Mi"
      limits:
        cpu: "1000m"
        memory: "2Gi"
    volumeMounts:
    - name: docker-config
      mountPath: /kaniko/.docker
  volumes:
  - name: docker-config
    secret:
      secretName: harbor-registry-secret
      items:
      - key: .dockerconfigjson
        path: config.json
"""
        }
    }

    environment {
        // Harbor 레지스트리 설정
        HARBOR_URL = "harbor-core.harbor.svc.cluster.local"
        HARBOR_PROJECT = "my-resume"
        IMAGE_NAME = "my-resume"
        DOCKERFILE = "docker/Dockerfile"
        REPO_TAG = "" // Git tag에서 동적으로 설정
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Get Version') {
            steps {
                script {
                    env.REPO_TAG = sh(
                        returnStdout: true,
                        script: "git describe --tags --abbrev=0 2>/dev/null || echo '0.0.1'"
                    ).trim()
                }
                echo "버전: ${env.REPO_TAG}"
            }
        }

        stage('Build and Push to Harbor') {
            steps {
                container('kaniko') {
                    sh """
                    /kaniko/executor \
                    --context=${WORKSPACE} \
                    --dockerfile=${DOCKERFILE} \
                    --destination=${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${REPO_TAG} \
                    --destination=${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:latest \
                    --skip-tls-verify \
                    --insecure \
                    --cache=true \
                    --cache-repo=${HARBOR_URL}/${HARBOR_PROJECT}/kaniko-cache
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Harbor 푸시 성공: ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${REPO_TAG}"
        }
        failure {
            echo "빌드 실패. Jenkins 콘솔 로그를 확인하세요."
        }
    }
}