pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kubectl
    image: bitnami/kubectl:latest
    command: ["cat"]
    tty: true
    resources:
      requests:
        cpu: "100m"
        memory: "128Mi"
      limits:
        cpu: "200m"
        memory: "256Mi"
"""
        }
    }

    environment {
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        JOB_NAME  = "kaniko-build-${env.BUILD_NUMBER}"
        CURRENT_BRANCH = "dev"
    }

    stages {
        stage('Build & Push') {
            steps {
                container('kubectl') {
                    script {
                        echo "Target Branch: ${CURRENT_BRANCH}"

                        sh "kubectl auth can-i create jobs"
                        sh "kubectl auth can-i apply -f k8s/kaniko-job.yaml"

                        // sed 명령어로 BUILD_NUMBER와 GIT_BRANCH를 모두 치환합니다.
                        sh """kubectl apply -f k8s/kaniko-job.yaml"""

                        // 로그 모니터링 및 완료 대기
                        sh "kubectl logs -f job/${JOB_NAME} &"
                        sh "kubectl wait --for=condition=complete job/${JOB_NAME} --timeout=900s"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Successfully pushed image: my-resume:${BUILD_NUMBER}"
            // 배포 단계로 넘어가기 전, 성공한 Job은 깔끔하게 삭제
            container('kubectl') {
                sh "kubectl delete job ${JOB_NAME}"
            }
        }
        failure {
            echo "Build failed. Check 'kubectl logs job/${JOB_NAME}' for details."
        }
    }
}