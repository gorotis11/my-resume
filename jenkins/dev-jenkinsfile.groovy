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
"""
        }
    }

    environment {
        REGISTRY = "local-registry.registry.svc.cluster.local:443"
        IMAGE_NAME = "my-resume"
        DOCKERFILE = "docker/Dockerfile"
        // [개선] 브랜치와 빌드 번호를 조합한 동적 태그
        REPO_TAG = "${env.BRANCH_NAME ?: 'dev'}-${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                // Jenkins 에이전트의 기본 컨테이너(jnlp)에서 소스를 땡겨옵니다.
                checkout scm
            }
        }

        stage('Build and Push') {
            steps {
                // [중요] Checkout 받은 소스가 있는 ${WORKSPACE}를 그대로 사용합니다.
                container('kaniko') {
                    sh """
                    /kaniko/executor \
                    --context=${WORKSPACE} \
                    --dockerfile=${DOCKERFILE} \
                    --destination=${REGISTRY}/${IMAGE_NAME}:${REPO_TAG} \
                    --destination=${REGISTRY}/${IMAGE_NAME}:latest \
                    --skip-tls-verify \
                    --insecure \
                    --cache=true \
                    --cache-repo=${REGISTRY}/kaniko-cache
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Successfully pushed: ${IMAGE_NAME}:${REPO_TAG}"
        }
        failure {
            echo "Build failed. Check Jenkins console logs."
        }
    }
}