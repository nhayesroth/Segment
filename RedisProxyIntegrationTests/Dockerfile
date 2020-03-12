FROM maven:3.6.3-jdk-8 AS MAVEN
COPY RedisProxyIntegrationTests/src ./src
COPY RedisProxyIntegrationTests/pom.xml ./
RUN mvn clean package -Dmaven.test.skip=true # this should run integration tests at compile time
CMD ["mvn", "test"]
