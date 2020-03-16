package cache;

import java.util.Optional;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callable that can be used to return async results from the LruCache.
 * <p>
 * This is most useful when multiple requests are sent in parallel (e.g.
 * pipelined RESP requests).
 */
public class CallableGet implements Callable<Optional<String>> {
  private static final Logger logger =
      LoggerFactory.getLogger(CallableGet.class.getName());

  private final LruCache cache;
  private final String key;

  public CallableGet(LruCache cache, String key) {
    this.cache = cache;
    this.key = key;
  }

  @Override
  public Optional<String> call() throws Exception {
    Optional<String> value = cache.get(key);
    logger.info("cache.get({}) returned {}", key, value);
    return value;
  }
}