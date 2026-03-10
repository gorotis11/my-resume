pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: default
  automountServiceAccountToken: true
  containers:
  - name: kubectl
    image: bitnami/kubectl:latest
    command: ["cat"]
    tty: true
"""
        }
    }

    environment {
        // Multibranch가 아니더라도 'dev'로 기본값 설정
        CURRENT_BRANCH = "${env.BRANCH_NAME ?: 'dev'}"
        MY_BUILD_NUM   = "${env.BUILD_NUMBER}"
        MY_JOB_NAME    = "kaniko-build-${env.BUILD_NUMBER}"
    }

    stages {
        stage('Deploy Kaniko Job') {
            steps {
                container('kubectl') {
                    script {
                        echo "Building Branch: ${CURRENT_BRANCH}"

                        // 1. YAML 파일 내 변수 치환 (${} 형태를 치환하기 위해 역슬래시 사용)
                        sh """
                        sed -e 's/\\\${BUILD_NUMBER}/${MY_BUILD_NUM}/g' \
                            -e 's/\\\${GIT_BRANCH}/${CURRENT_BRANCH}/g' \
                            k8s/kaniko-job.yaml > resolved-job.yaml
                        """

                        // 2. Job 생성
                        sh "kubectl apply -f resolved-job.yaml"

                        // 3. 빌드 로그 실시간 모니터링 (백그라운드 실행)
                        sh "kubectl logs -f job/${MY_JOB_NAME} &"

                        // 4. Job 완료 시까지 대기 (최대 15분)
                        echo "Waiting for Kaniko Job to complete..."
                        sh "kubectl wait --for=condition=complete job/${MY_JOB_NAME} --timeout=900s"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Build Success: Image pushed to registry."
            container('kubectl') {
                // 성공 시 Job 리소스 삭제 (클러스터 정리)
                sh "kubectl delete job ${MY_JOB_NAME}"
            }
        }
        failure {
            echo "Build Failed. Please check the logs above."
        }
    }
}