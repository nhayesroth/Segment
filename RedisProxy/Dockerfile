############# Stage 1 - Use maven image to package the server code
FROM maven:3.6.3-jdk-8 AS MAVEN
COPY RedisProxy/src ./src
COPY RedisProxy/pom.xml ./
# COPY /src ./src
# COPY /pom.xml ./
# Package the server code
RUN mvn clean package

############# Stage 2 - Copy the artifact we need (w/dependencies), discard the rest, and set startup commands
FROM openjdk:8
# ENV REDIS_HOST localhost
# ENV REDIS_PORT 6379
# ENV HTTP_PORT 8080
# ENV RESP_PORT 8124
# ENV CACHE_EXPIRY 1
# ENV CACHE_CAPACITY 4
COPY --from=MAVEN /target/server-0.0.1-SNAPSHOT-jar-with-dependencies.jar /server.jar

# Set startup commands to execute the jar
CMD ["java", "-jar", "/server.jar"]
