package configuration;

import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

/** Holds environment variables used to configure the server. */
public class Configuration {
  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  // Note: when running within a container, this should be redis. localhost is appropriate for local development/testing.
  private static final String DEFAULT_REDIS_HOST = "localhost";
  private static final int DEFAULT_REDIS_PORT = 6379;
  private static final Duration DEFAULT_CACHE_EXPIRY = Duration.ofMillis(5000);
  private static final int DEFAULT_CACHE_CAPACITY = 10;
  
  private String redisHost;
  private int redisPort;
  private Duration cacheExpiry;
  private int cacheCapacity;
  
  /** Constructor. */
  Configuration(
      String redisHost,
      int redisPort,
      Duration cacheExpiry,
      int cacheCapacity) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
    this.cacheExpiry = cacheExpiry;
    this.cacheCapacity = cacheCapacity;
  }
  
  @Override
  public String toString() {
    return new StringJoiner(", ", "[", "]")
        .add(String.format("redisHost=%s", redisHost))
        .add(String.format("redisPort=%d", redisPort))
        .add(String.format("cacheExpiry=%s", cacheExpiry.toString()))
        .add(String.format("cacheCapacity=%d", cacheCapacity))
        .toString();
  }
  
  /** Gets a default Configuration instance with all values read from the system environment. */
  public static Configuration getFromEnvironment() {
    Map<String, String> env = System.getenv();
    env.entrySet()
      .forEach(
          entry ->
            logger.info(
                "System.environment: {key={}, value={} }",
                entry.getKey(), entry.getValue()));;
    return Configuration.newBuilder()
        .setRedisHost(
            env.getOrDefault(
                "REDIS_HOST", DEFAULT_REDIS_HOST))
        .setRedisPort(
            transformOrElse(
                "REDIS_PORT", Integer::parseInt, DEFAULT_REDIS_PORT))
        .setCacheCapacity(
            transformOrElse(
                "CACHE_CAPACITY", Integer::parseInt, DEFAULT_CACHE_CAPACITY))
        .setCacheExpiry(
            transformOrElse(
                "CACHE_EXPIRY",
                v -> Duration.ofMillis(Long.parseLong(v)),
                DEFAULT_CACHE_EXPIRY))
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
          .setCacheExpiry(cacheExpiry)
          .setCacheCapacity(cacheCapacity);
  }
  
  public String redisHost() {
    return redisHost;
  }
  
  public int redisPort() {
    return redisPort;
  }
  
  public Duration cacheExpiry() {
    return cacheExpiry;
  }
  
  public int cacheCapacity() {
    return cacheCapacity;
  }
  
  public static class Builder {
    private String redisHost;
    private int redisPort;
    private Duration cacheExpiry;
    private int cacheCapacity;
    
    public Builder setRedisHost(String redisHost) {
      this.redisHost = redisHost;
      return this;
    }
    public Builder setRedisPort(int redisPort) {
      this.redisPort = redisPort;
      return this;
    }
    
    public Builder setCacheExpiry(Duration cacheExpiry) {
      this.cacheExpiry = cacheExpiry;
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
          cacheExpiry,
          cacheCapacity);
    }
  }

  private static <T> T transformOrElse(String key, Function<String, T> transform, T defaultVal) {
    return System.getenv().containsKey(key)
        ? transform.apply(System.getenv(key))
        : defaultVal;
  }
}
