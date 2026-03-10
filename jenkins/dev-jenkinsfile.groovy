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
        MY_BUILD_NUM = "${env.BUILD_NUMBER}"
        MY_JOB_NAME  = "kaniko-build-${env.BUILD_NUMBER}"
        CURRENT_BRANCH = "dev"
    }

    stages {
        stage('Build & Push') {
            steps {
                container('kubectl') {
                    script {
                        echo "Target Branch: ${MY_BRANCH}"

                        // sed 명령어로 BUILD_NUMBER와 GIT_BRANCH를 모두 치환합니다.
                        sh """
                        sed -e "s/\\\\\\\${BUILD_NUMBER}/${MY_BUILD_NUM}/g" \
                            -e "s/\\\\\\\${GIT_BRANCH}/${CURRENT_BRANCH}/g" \
                            k8s/kaniko-job.yaml | kubectl apply -f -
                        """

                        // 로그 모니터링 및 완료 대기
                        sh "kubectl logs -f job/${MY_JOB_NAME} &"
                        sh "kubectl wait --for=condition=complete job/${MY_JOB_NAME} --timeout=900s"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Successfully pushed image: my-resume:${MY_BUILD_NUM}"
            // 배포 단계로 넘어가기 전, 성공한 Job은 깔끔하게 삭제
            container('kubectl') {
                sh "kubectl delete job ${MY_JOB_NAME}"
            }
        }
        failure {
            echo "Build failed. Check 'kubectl logs job/${MY_JOB_NAME}' for details."
        }
    }
}