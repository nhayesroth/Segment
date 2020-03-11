############# Stage 1 - Use maven image to package the server code
FROM maven:3.6.3-jdk-8 AS MAVEN
COPY RedisProxy/src ./src
COPY RedisProxy/pom.xml ./
# TODO: change this to run the unit tests during this stage
RUN mvn clean package -Dmaven.test.skip=true

############# Stage 2 - Copy the artifact we need (w/dependencies), discard the rest, and set startup commands
FROM openjdk:8
COPY --from=MAVEN /target/server-0.0.1-SNAPSHOT-jar-with-dependencies.jar /server.jar
CMD ["java", "-jar", "/server.jar"]
