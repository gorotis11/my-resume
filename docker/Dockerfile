FROM eclipse-temurin:21-jdk

CMD ["./gradlew", "clean", "build"]

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
# or Maven
# ARG JAR_FILE_PATH=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 28080

RUN mkdir /logs && chmod 777 /logs

ENTRYPOINT ["java", "-Xmx512M", "-Xms256M", "-jar", "app.jar"]