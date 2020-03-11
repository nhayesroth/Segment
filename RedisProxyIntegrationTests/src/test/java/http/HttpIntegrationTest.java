package http;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.ClassRule;
import server.Server;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import com.google.common.collect.ImmutableMap;
import configuration.Configuration;
import http.HttpClient;
import http.HttpResponse;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;

/** Tests the HTTP portion of the proxy server. */
public class HttpIntegrationTest {
  
//  private static Server server;
  private static RedisClient redisClient;
  private static RedisCommands<String, String> commands;
  private static Configuration configuration;
  
  private static final ImmutableMap<String, String> KEY_VALUE_MAP =
      ImmutableMap.<String, String>builder()
      .put("key0", "val0")
      .put("key1", "val1")
      .put("key2", "val")
      .put("key3", "val3")
      .put("key4", "val4")
      .put("key5", "val5")
      .put("key6", "val6")
      .put("key7", "val7")
      .put("key8", "val8")
      .put("key9", "val9")
      .build();

  @BeforeClass
  public static void setUp() throws Exception {
    configuration =
        Configuration.getFromEnvironment()
        .toBuilder()
        .build();
    redisClient = RedisClient.create(
        RedisURI.builder()
          .withHost(configuration.redisHost())
          .withPort(configuration.redisPort())
          .build());
    commands = redisClient.connect().sync();
  }
  
  @AfterClass
  public static void tearDown() {
    commands.del("foo");
    KEY_VALUE_MAP.entrySet().forEach(entry -> commands.del(entry.getKey()));
    commands.getStatefulConnection().close();
    redisClient.shutdown();
  }

  @Test
  public void testGet() throws IOException {
    commands.set("foo", "bar");
    
    HttpResponse response1 = HttpClient.getFromSpecificHost(HttpClient.REDIS_PROXY, "foo");
    assertThat(response1.responseCode).isEqualTo(HttpURLConnection.HTTP_OK);
    assertThat(response1.output).isEqualTo("bar");
    
    HttpResponse response2 = HttpClient.getFromSpecificHost(HttpClient.REDIS_PROXY, "foo");
    assertThat(response2.responseCode).isEqualTo(HttpURLConnection.HTTP_OK);
    assertThat(response2.output).isEqualTo("bar"); 
  }
  
  @Test
  public void testGet_noAssociatedValue() throws IOException {
    HttpResponse response1 = HttpClient.getFromSpecificHost(HttpClient.REDIS_PROXY, "garbage");
    assertThat(response1.responseCode).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(response1.output).isEmpty();
    
    HttpResponse response2 = HttpClient.getFromSpecificHost(HttpClient.REDIS_PROXY, "swedish fish");
    assertThat(response2.responseCode).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(response2.output).isEmpty();
  }
  
  @Test
  public void testGet_parallelRequests() throws IOException, InterruptedException, ExecutionException {
//    ExecutorService threadPool = Executors.newFixedThreadPool(3);
//    try {
//      // Set 10 key value pairs.
//      KEY_VALUE_MAP.entrySet().forEach(entry -> commands.set(entry.getKey(), entry.getValue()));
//      
//      // Spawn 3 callables for each key value pair.
//      // Each callable will trigger the server to spawn a handler thread.
//      List<CallableHttpGet> callables = new LinkedList<>();
//      for (Map.Entry<String, String> entry : KEY_VALUE_MAP.entrySet()) {
//        callables.add(new CallableHttpGet(entry.getKey()));
//        callables.add(new CallableHttpGet(entry.getKey()));
//        callables.add(new CallableHttpGet(entry.getKey()));
//      }
//      
//      // Invoke all the requests in parallel.
//      List<Future<Entry<String, HttpResponse>>> futures =
//          threadPool.invokeAll(callables);
//      
//      // Verify that each future succeeds.
//      for (Future<Map.Entry<String, HttpResponse>> future : futures) {
//        Map.Entry<String, HttpResponse> result = future.get();
//        String key = result.getKey();
//        String value = result.getValue().output;
//        int code = result.getValue().responseCode;
//        assertThat(code).isEqualTo(HttpURLConnection.HTTP_OK);
//        assertThat(key).startsWith("key");
//        assertThat(value).startsWith("val");
//        assertThat(key.substring(key.length() - 1))
//            .isEqualTo(value.substring(value.length() - 1));
//      }
//    } finally {
//      threadPool.shutdown();
//    }
  }
  
  /**
   * Callable that sends an HTTP.GET request to the proxy server.
   *
   * <p>The result includes the key that was requested and the full HttpResponse.
   */
  private class CallableHttpGet implements Callable<Map.Entry<String, HttpResponse>> {
    String key;
    
    public CallableHttpGet(String key) {
      this.key = key;
    }
    @Override
    public Map.Entry<String, HttpResponse> call() throws Exception {
      return new AbstractMap.SimpleEntry<>(
          // TODO: change this to read from environment
          key, HttpClient.getFromSpecificHost(HttpClient.REDIS_PROXY, key));
    }
  }
}
