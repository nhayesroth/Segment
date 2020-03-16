package cache;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.testing.FakeTicker;
import configuration.Configuration;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.time.Duration;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/** Tests for LruCache. */
public class LruCacheTest {

  private static final int DEFAULT_CACHE_CAPACITY = 1;
  private static final Duration DEFAULT_CACHE_EXPIRY = Duration.ofSeconds(10);
  private static final Configuration DEFAULT_CONFIGURATION =
      Configuration.newBuilder()
          .setCacheCapacity(DEFAULT_CACHE_CAPACITY)
          .setCacheExpiry(DEFAULT_CACHE_EXPIRY)
          .build();

  @Mock
  private RedisAsyncCommands<String, String> mockCommands;
  @Mock
  private RedisFuture<String> mockRedisResponse1;
  @Mock
  private RedisFuture<String> mockRedisResponse2;

  private FakeTicker fakeTicker;

  private LruCache cache;

  @Before
  public void setUp() throws Exception {
    mockCommands = Mockito.mock(RedisAsyncCommands.class);
    mockRedisResponse1 = Mockito.mock(RedisFuture.class);
    mockRedisResponse2 = Mockito.mock(RedisFuture.class);
    fakeTicker = new FakeTicker();
    cache = new LruCache(mockCommands, DEFAULT_CONFIGURATION, fakeTicker);

  }

  @Test
  public void testGet_keyNotInCache_loadsFromRedis() throws Exception {
    when(mockCommands.get("key")).thenReturn(mockRedisResponse1);
    when(mockRedisResponse1.get()).thenReturn("value");

    assertThat(cache.get("key")).isEqualTo(Optional.of("value"));
    verify(mockCommands).get("key");
  }

  @Test
  public void testGet_keyNotInCache_loadsFromRedis_null() throws Exception {
    when(mockCommands.get("key")).thenReturn(mockRedisResponse1);
    when(mockRedisResponse1.get()).thenReturn(null);

    assertThat(cache.get("key")).isEqualTo(Optional.empty());
    verify(mockCommands).get("key");
  }

  @Test
  public void testGet_keyInCache_doesNotLoadFromRedis() throws Exception {
    when(mockCommands.get("key")).thenReturn(mockRedisResponse1);
    when(mockRedisResponse1.get()).thenReturn("value");

    // Make one call to load the value from redis into the cache.
    assertThat(cache.get("key")).isEqualTo(Optional.of("value"));
    verify(mockCommands, times(1)).get("key");

    // Verify that the next call does not interact with redis.
    assertThat(cache.get("key")).isEqualTo(Optional.of("value"));
    verify(mockCommands, times(1)).get("key");
  }

  @Test
  public void testGet_keyExpired_loadsFromRedis() throws Exception {
    when(mockCommands.get("key")).thenReturn(mockRedisResponse1);
    when(mockRedisResponse1.get()).thenReturn("value");

    // Make one call to load the value from redis into the cache.
    assertThat(cache.get("key")).isEqualTo(Optional.of("value"));
    verify(mockCommands, times(1)).get("key");

    // Increment time beyond the cache expiry.
    fakeTicker.advance(DEFAULT_CACHE_EXPIRY);

    // Verify that the next call loads from redis.
    assertThat(cache.get("key")).isEqualTo(Optional.of("value"));
    verify(mockCommands, times(2)).get("key");
  }

  @Test
  public void testGet_capacityExhausted_loadsFromRedis() throws Exception {
    when(mockCommands.get("key1")).thenReturn(mockRedisResponse1);
    when(mockRedisResponse1.get()).thenReturn("value1");

    when(mockCommands.get("key2")).thenReturn(mockRedisResponse2);
    when(mockRedisResponse2.get()).thenReturn("value2");

    // Make call#1 to get the first key from redis and cache it.
    assertThat(cache.get("key1")).isEqualTo(Optional.of("value1"));

    // Make call#2 to get the second key from redis and cache it.
    assertThat(cache.get("key2")).isEqualTo(Optional.of("value2"));

    // Make call#3 to get the first key.
    assertThat(cache.get("key1")).isEqualTo(Optional.of("value1"));

    // Because the cache has a max capacity, call#2 and call#3 both must load
    // from redis.
    verify(mockCommands, times(2)).get("key1");
    verify(mockCommands, times(1)).get("key2");
  }

}
