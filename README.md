# My Resume

Spring Boot + Thymeleaf 기반의 개인 이력서 웹 애플리케이션

## 기술 스택

- Java 21
- Spring Boot 3.4.3
- Thymeleaf
- Gradle 8.5
- Docker (멀티 스테이지 빌드)
- Kubernetes
- Jenkins (Kaniko + Harbor)

## 로컬 실행

```bash
./gradlew bootRun
```

http://localhost:8080 에서 확인

## 빌드

```bash
# JAR 빌드
./gradlew clean bootJar -x test

# Docker 이미지 빌드
docker build -t my-resume:latest -f docker/Dockerfile .
```

## 배포

### 버전 관리 (Git Tag)

```bash
git tag v1.1.0
git push origin --tags
```

Jenkins 파이프라인에서 Git tag를 자동으로 읽어 이미지 태그로 사용합니다.

### CI/CD 파이프라인

| 파이프라인 | 파일 | 설명 |
|---|---|---|
| Jenkins (K8s) | `jenkins/dev-jenkinsfile.groovy` | Kaniko 빌드 → Harbor 푸시 |
| Jenkins (OCI) | `jenkins/oci-jenkinsfile.groovy` | Docker 빌드 → Registry 푸시 → SSH 배포 |
| GitHub Actions | `.github/workflows/gradle.yml` | DockerHub 푸시 → Self-hosted 배포 |

### Kubernetes 배포

```bash
kubectl apply -f k8s/my-resume.yaml
```

## 모니터링

Spring Boot Actuator 엔드포인트:

- `/actuator/health` - 헬스 체크
- `/actuator/prometheus` - Prometheus 메트릭

## 프로젝트 구조

```
src/main/resources/templates/
├── resume.html              # 메인 레이아웃
├── fragments/nav.html       # 네비게이션 바
└── contents/                # 이력서 섹션
    ├── profile.html
    ├── introduce.html
    ├── skills.html
    ├── experience.html
    └── etc.html
```
