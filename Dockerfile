FROM openjdk:8-jdk-alpine3.9
VOLUME /tmp
ADD smart-talk-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EXPOSE 8085 8088