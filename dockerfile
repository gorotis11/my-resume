FROM openjdk:17-jdk-slim
# or
# FROM openjdk:8-jdk-alpine
# FROM openjdk:11-jdk-alpine

CMD ["./gradlew", "clean", "build"]
# or Maven
# CMD ["./mvnw", "clean", "package"]

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
# or Maven
# ARG JAR_FILE_PATH=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 18080

ENTRYPOINT ["java", "-jar", "app.jar"]