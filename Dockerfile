FROM maven:3.8.3-openjdk-17 AS maven

WORKDIR /usr/src/app
COPY . /usr/src/app

RUN mvn package

FROM openjdk:17

ARG JAR_FILE=RESTyourSOAP.jar

WORKDIR /opt/app

# Copy the spring-boot-api-tutorial.jar from the maven stage to the /opt/app directory of the current stage.
COPY --from=maven /usr/src/app/target/${JAR_FILE} /opt/app/

ENTRYPOINT ["java","-jar","RESTyourSOAP.jar"]