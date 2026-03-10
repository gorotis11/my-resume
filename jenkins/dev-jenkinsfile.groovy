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
    command: ["sleep"]
    args: ["9999999"]
"""
        }
    }

    environment {
        // 빌드 번호를 고유 식별자로 사용 (따옴표 필수)
        MY_BUILD_NUM = "${env.BUILD_NUMBER}"
        MY_JOB_NAME  = "kaniko-build-${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                // GitHub에서 최신 소스 가져오기
                checkout scm
            }
        }

        stage('Build with Kubernetes Job') {
            steps {
                container('kubectl') {
                    script {
                        // 1. YAML 파일의 ${BUILD_NUMBER}를 현재 빌드 번호로 치환하여 Job 생성
                        sh "sed \"s/\\\\\\\${BUILD_NUMBER}/${MY_BUILD_NUM}/g\" k8s/kaniko-job.yaml | kubectl apply -f -"

                        // [추가 조언 1] 빌드 로그 실시간 출력
                        // 백그라운드(&)로 실행하여 로그를 뿌리면서 동시에 다음 명령어(wait)를 수행합니다.
                        echo "Starting to stream logs from Kaniko Job..."
                        sh "kubectl logs -f job/${MY_JOB_NAME} &"

                        // 2. Job이 성공(complete)할 때까지 대기 (최대 10분)
                        echo "Waiting for Job: ${MY_JOB_NAME} to complete..."
                        sh "kubectl wait --for=condition=complete job/${MY_JOB_NAME} --timeout=600s"
                    }
                }
            }
        }

        stage('Deploy and Cleanup') {
            steps {
                container('kubectl') {
                    script {
                        // 3. 실제 앱 배포 (Deployment 업데이트)
                        sh "kubectl apply -f k8s/deployment.yaml"
                        sh "kubectl rollout restart deployment my-resume"
                        sh "kubectl rollout status deployment my-resume"

                        // [추가 조언 2] 성공 후 Job 리소스 삭제
                        // 클러스터에 불필요한 Job 객체가 쌓이지 않도록 청소합니다.
                        echo "Cleaning up completed Job: ${MY_JOB_NAME}"
                        sh "kubectl delete job ${MY_JOB_NAME}"
                    }
                }
            }
        }
    }

    // 빌드 실패 시에도 로그 확인을 위해 Job을 남겨두거나 정리하는 로직
    post {
        failure {
            echo "Build failed. Job '${MY_JOB_NAME}' will be kept for debugging."
        }
    }
}