# 1단계: 빌드 스테이지 (Gradle 빌드 수행)
FROM gradle:8.5-jdk21 AS build
WORKDIR /home/wien/k8s/my-resume

# 소스 코드 복사
COPY . .

# 실행 권한 부여 및 빌드 (테스트 제외로 속도 향상)
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

# 2단계: 실행 스테이지 (최소한의 JRE만 사용)
FROM eclipse-temurin:21-jre
WORKDIR /home/wien/k8s/my-resume

# 1단계에서 빌드된 jar 파일만 가져오기
COPY --from=build /app/build/libs/*.jar app.jar

# 로그 디렉토리 생성
RUN mkdir /logs && chmod 777 /logs

EXPOSE 28080

# 절대 경로를 사용하여 실행
ENTRYPOINT ["java", "-Xmx512M", "-Xms256M", "-jar", "/app/app.jar"]