FROM openjdk:17-jdk-slim

ARG VERSAO="0.0.1-SNAPSHOT"

ENV APP_HOME=/app
ENV VERSION=$VERSAO

COPY jwarsv-sboot/build/libs/jwarsv-sboot-${VERSION}.jar app.jar
WORKDIR /app

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5083","-Djava.security.egd=file:/dev/./urandom","-Dinfo.project.version=${VERSION}","-Duser.timezone=\"-03:00\"","-jar","/app.jar"]
