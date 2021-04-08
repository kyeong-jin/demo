FROM openjdk:8-jdk-alpine
EXPOSE 8080
ARG JAR_FILE=target/rest-service-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} demo.jar
ENTRYPOINT ["java", "-jar", "/demo.jar"]