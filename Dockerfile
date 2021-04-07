FROM java:8
VOLUME /tmp
EXPOSE 8080
ARG JAR_FILE=target/gs-rest-service-master-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} demo.jar
ENTRYPOINT ["java", "-jar", "/demo.jar"]