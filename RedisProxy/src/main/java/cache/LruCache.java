package cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * Least recently used (LRU) cache that stores values retrieved from a backing Redis instance.
 * 
 * <p>If a key is not present in the cache, its value is retrieved from the Redis instance, directly.
 * 
 * <p>If a key is not present in the redis instance, the cache stores an absent value.
 */
public class LruCache {
  
  private final LoadingCache<String, Optional<String>> cache;

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
      return Optional.ofNullable(commands.get(key).get());
    }
  }
  
  
  public LruCache(RedisAsyncCommands<String, String> commands) {
    // TODO: make configurable
    cache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(10))
        .maximumSize(2)
        .build(new Loader(commands));
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
