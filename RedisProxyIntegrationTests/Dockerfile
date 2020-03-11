############# Stage 1 - Use maven image to package the test code
FROM maven:3.6.3-jdk-8 AS MAVEN
COPY RedisProxyIntegrationTests/src ./src
COPY RedisProxyIntegrationTests/pom.xml ./
RUN mvn clean package -Dmaven.test.skip=true # this should integration tests at compile time

############# Stage 2 - Copy the artifact we need (w/dependencies), discard the rest, and set startup commands
FROM openjdk:8
COPY --from=MAVEN /target/integration-tests-0.0.1-SNAPSHOT-jar-with-dependencies.jar /integration-tests.jar

# Set startup commands to execute the jar
CMD ["java", "-jar", "/integration-tests.jar"]
