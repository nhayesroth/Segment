FROM maven:3.6.3-jdk-8 AS MAVEN
COPY RedisProxyIntegrationTests/src ./src
COPY RedisProxyIntegrationTests/pom.xml ./
RUN mvn clean package -Dmaven.test.skip=true # skip tests at compile time since they rely on the other services
CMD ["mvn", "test"]
