package configuration;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

/** Holds environment variables used to configure the server. */
@AutoValue
public abstract class Configuration {

  /** The keys that can be configured via environment variables. */
  public static enum Key {
    HTTP_PORT,
    RESP_PORT,
    REDIS_HOST,
    REDIS_PORT,
    CACHE_CAPACITY,
    MAX_CONCURRENT_HANDLERS,
    CACHE_EXPIRY,
    REDIS_TEST_HOST,
    MAX_PIPELINED_RESP_COMMANDS;
  }

  /** The default values for all configurable keys. */
  private static final ImmutableMap<Key, Object> DEFAULT_VALUES =
      ImmutableMap.<Key, Object>builder()
          .put(Key.REDIS_HOST, "localhost")
          .put(Key.REDIS_PORT, 6379)
          .put(Key.HTTP_PORT, 8080)
          .put(Key.RESP_PORT, 8124)
          .put(Key.CACHE_EXPIRY, Duration.ofMillis(5000))
          .put(Key.CACHE_CAPACITY, 10)
          .put(Key.MAX_CONCURRENT_HANDLERS, 10)
          .put(Key.MAX_PIPELINED_RESP_COMMANDS, 5)
          .build();

  private static final Logger logger =
      LoggerFactory.getLogger(Server.class.getName());

  public abstract String redisHost();

  public abstract int redisPort();

  public abstract int httpPort();

  public abstract int respPort();

  public abstract Duration cacheExpiry();

  public abstract int cacheCapacity();

  public abstract int maxConcurrentHandlers();

  public abstract int maxPipelinedRespCommands();

  /**
   * Gets a default Configuration instance with all values read from the system
   * environment.
   */
  public static Configuration getFromEnvironment() {
    Map<String, String> env = System.getenv();
    env.entrySet()
        .forEach(
            entry -> logger.info(
                "System.environment: {key={}, value={} }",
                entry.getKey(),
                entry.getValue()));
    return Configuration.newBuilder()
        .setRedisHost(getOrElse(Key.REDIS_HOST, s -> s))
        .setRedisPort(getOrElse(Key.REDIS_PORT, Integer::parseInt))
        .setHttpPort(getOrElse(Key.HTTP_PORT, Integer::parseInt))
        .setRespPort(getOrElse(Key.RESP_PORT, Integer::parseInt))
        .setCacheCapacity(getOrElse(Key.CACHE_CAPACITY, Integer::parseInt))
        .setCacheExpiry(
            getOrElse(
                Key.CACHE_EXPIRY,
                s -> Duration.ofMillis(Long.parseLong(s))))
        .setMaxConcurrentHandlers(
            getOrElse(Key.MAX_CONCURRENT_HANDLERS, Integer::parseInt))
        .setMaxPipelinedRespCommands(
            getOrElse(Key.MAX_PIPELINED_RESP_COMMANDS, Integer::parseInt))
        .build();
  }

  /**
   * Returns a new Builder that can be used to construct custom Configuration
   * objects.
   */
  public static Builder newBuilder() {
    return new AutoValue_Configuration.Builder()
        .setRedisHost((String) DEFAULT_VALUES.get(Key.REDIS_HOST))
        .setRedisPort((int) DEFAULT_VALUES.get(Key.REDIS_PORT))
        .setHttpPort((int) DEFAULT_VALUES.get(Key.HTTP_PORT))
        .setRespPort((int) DEFAULT_VALUES.get(Key.RESP_PORT))
        .setCacheCapacity((int) DEFAULT_VALUES.get(Key.CACHE_CAPACITY))
        .setCacheExpiry((Duration) DEFAULT_VALUES.get(Key.CACHE_EXPIRY))
        .setMaxConcurrentHandlers(
            (int) DEFAULT_VALUES.get(Key.MAX_CONCURRENT_HANDLERS))
        .setMaxPipelinedRespCommands(
            (int) DEFAULT_VALUES.get(Key.MAX_PIPELINED_RESP_COMMANDS));
  }

  /** Converts this Configuration object to a Builder that can be modified. */
  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setRedisHost(String redisHost);

    public abstract Builder setHttpPort(int httpPort);

    public abstract Builder setRespPort(int httpPort);

    public abstract Builder setRedisPort(int redisPort);

    public abstract Builder setCacheExpiry(Duration cacheExpiry);

    public abstract Builder setCacheCapacity(int cacheCapacity);

    public abstract Builder setMaxConcurrentHandlers(int maxConcurrentHandlers);

    public abstract Builder setMaxPipelinedRespCommands(
        int PipelinedRespCommands);

    public abstract Configuration build();
  }

  /**
   * Utility that gets a system variable and, if present, transforms it.
   * <p>
   * If the system variable is not present, the default value is returned
   * instead.
   */
  private static <T> T getOrElse(Key key, Function<String, T> transform) {
    return System.getenv().containsKey(key.name())
        ? transform.apply(System.getenv(key.name()))
        : (T) DEFAULT_VALUES.get(key);
  }
}
