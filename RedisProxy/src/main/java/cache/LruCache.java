package cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import configuration.Configuration;
import io.lettuce.core.api.async.RedisAsyncCommands;
import server.Server;

/**
 * Least recently used (LRU) cache that stores values retrieved from a backing Redis instance.
 * 
 * <ul>
 * <li>If a key is not present in the cache, its value is retrieved from the Redis instance, directly.
 * <li>If a key is not present in the Redis instance, the cache stores an absent value.
 * </ul>
 */
public class LruCache {
  
  private final LoadingCache<String, Optional<String>> cache;
  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());

  /**
   * Retrieves values for keys in the backing Redis instance.
   */
  private class Loader extends CacheLoader<String, Optional<String>> {
    
    RedisAsyncCommands<String, String> commands;
    
    Loader(RedisAsyncCommands<String, String> commands) {
      this.commands = commands;
    }
    
    @Override
    public Optional<String> load(String key) throws Exception {
      logger.info("Attempting to load value for key: {}", key);
      return Optional.ofNullable(commands.get(key).get());
    }
  }
  
  /** Constructor. */
  public LruCache(
      RedisAsyncCommands<String, String> commands,
      Configuration configuration) {
    cache = CacheBuilder.newBuilder()
        .expireAfterWrite(configuration.cacheExpiry())
        .maximumSize(configuration.cacheCapacity())
        .build(new Loader(commands));
  }
  
  /** Returns a new Builder */
  public static Builder newBuilder() {
    return new Builder();
  }
  
  /** Builder. */
  public static class Builder {

    private RedisAsyncCommands<String, String> commands;
    private Configuration configuration;
    
    public Builder setCommands(RedisAsyncCommands<String, String> commands) {
      this.commands = commands;
      return this;
    }
    
    public Builder setConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }
    
    public LruCache build() {
      Preconditions.checkState(commands != null);
      Preconditions.checkState(configuration != null);
      return new LruCache(commands, configuration);
    }
  }
  
  /**
   * Get the value associated with the specified key.
   * 
   * <p>An absent value indicates that the key does not have an associated value.
   */
  public Optional<String> get(String key) throws ExecutionException {
    return cache.get(key);
  }
}
