package configuration;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

/** Holds environment variables used to configure the server. */
public class Configuration {
  private static final String DEFAULT_REDIS_HOST = "redis";
  private static final int DEFAULT_REDIS_PORT = 6379;
  private static final int DEFAULT_CACHE_EXPIRY_TIME_IN_SECONDS = 5;
  private static final int DEFAULT_CACHE_CAPACITY = 5;
  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  
  private String redisHost;
  private int redisPort;
  private int cacheExpiryTimeInSeconds;
  private int cacheCapacity;
  
  Configuration(
      String redisHost,
      int redisPort,
      int cacheExpiryTimeInSeconds,
      int cacheCapacity) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
    this.cacheExpiryTimeInSeconds = cacheExpiryTimeInSeconds;
    this.cacheCapacity = cacheCapacity;
  }
  
  @Override
  public String toString() {
    return new StringJoiner(", ", "[", "]")
        .add(String.format("redisHost=%s", redisHost))
        .add(String.format("redisPort=%d", redisPort))
        .add(String.format("cacheExpiryTimeInSeconds=%d", cacheExpiryTimeInSeconds))
        .add(String.format("cacheCapacity=%d", cacheCapacity))
        .toString();
  }
  
  /** Gets a default Configuration instance with all values read from the system environment. */
  public static Configuration getFromEnvironment() {
    logger.warn("=======================");
    logger.warn("=======================");
    logger.warn("=======================");
    Map<String, String> env = System.getenv();
    env.entrySet()
      .forEach(
          entry -> logger.warn("Entry {key={}, value={} }", entry.getKey(), entry.getValue()));;
    logger.warn("=======================");
    logger.warn("=======================");
    logger.warn("=======================");
    return Configuration.newBuilder()
        .setRedisHost(
            env.getOrDefault("REDIS_HOST", DEFAULT_REDIS_HOST))
        .setRedisPort(
            env.containsKey("REDIS_PORT")
              ? Integer.parseInt(env.get("REDIS_PORT"))
              : DEFAULT_REDIS_PORT)
        .build();
  }
  
  /** Returns a new Builder that can be used to construct custom Configuration objects. */
  public static Builder newBuilder() {
      return new Configuration.Builder();
  }
  
  /** Converts this Configuration object to a Builder that can be modified. */
  public Builder toBuilder() {
      return new Configuration.Builder()
          .setRedisHost(redisHost)
          .setRedisPort(redisPort)
          .setCacheExpiryTimeInSeconds(cacheExpiryTimeInSeconds)
          .setCacheCapacity(cacheCapacity);
  }
  
  public String redisHost() {
    return redisHost;
  }
  
  public int redisPort() {
    return redisPort;
  }
  
  public int cacheExpiryTimeInSeconds() {
    return cacheExpiryTimeInSeconds;
  }
  
  public int cacheCapacity() {
    return cacheCapacity;
  }
  
  public static class Builder {
    private String redisHost;
    private int redisPort;
    private int cacheExpiryTimeInSeconds;
    private int cacheCapacity;
    
    public Builder setRedisHost(String redisHost) {
      this.redisHost = redisHost;
      return this;
    }
    public Builder setRedisPort(int redisPort) {
      this.redisPort = redisPort;
      return this;
    }
    public Builder setCacheExpiryTimeInSeconds(int cacheExpiryTimeInSeconds) {
      this.cacheExpiryTimeInSeconds = cacheExpiryTimeInSeconds;
      return this;
    }
    public Builder setCacheCapacity(int cacheCapacity) {
      this.cacheCapacity = cacheCapacity;
      return this;
    }
    
    public Configuration build() {
      return new Configuration(
          redisHost,
          redisPort,
          cacheExpiryTimeInSeconds,
          cacheCapacity);
    }
  }
}
