package server;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import configuration.Configuration;
import http.HttpClient;
import http.HttpResponse;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resp.RespClient;
import resp.RespProtocol;

/**
 * Integration tests for the proxy server (both HTTP and RESP).
 * <p>
 * This test expects to be run inside a docker container that can communicate
 * with a RedisProxy in another container.
 */
public class IntegrationTest {
  private static final Logger logger =
      LoggerFactory.getLogger(IntegrationTest.class.getName());

  private static Configuration configuration;
  private static HttpClient httpClient;
  private static RespClient respClient;
  private static RedisClient redisClient;
  private static RedisCommands<String, String> commands;

  private static final ImmutableMap<String, String> KEY_VALUE_MAP =
      ImmutableMap.<String, String>builder()
          .put("key1", "val1")
          .put("key2", "val2")
          .put("key3", "val3")
          .put("key4", "val4")
          .put("key5", "val5")
          .build();

  @BeforeClass
  public static void setUp() throws Exception {
    configuration = Configuration.getFromEnvironment();
    httpClient = new HttpClient("redis_proxy", configuration.httpPort());
    respClient = new RespClient("redis_proxy", configuration);
    redisClient =
        RedisClient.create(
            RedisURI.builder()
                .withHost(configuration.redisHost())
                .withPort(configuration.redisPort())
                .build());
    commands = redisClient.connect().sync();
  }

  @AfterClass
  public static void tearDown() {
    KEY_VALUE_MAP.entrySet().forEach(entry -> commands.del(entry.getKey()));
    commands.getStatefulConnection().close();
    redisClient.shutdown();
  }

  @Test
  public void testGet_http() throws IOException {
    commands.del("foo-http");
    commands.set("foo-http", "bar");

    HttpResponse response1 = httpClient.get("foo-http");
    assertThat(response1.responseCode).isEqualTo(HttpURLConnection.HTTP_OK);
    assertThat(response1.output).isEqualTo("bar");

    HttpResponse response2 = httpClient.get("foo-http");
    assertThat(response2.responseCode).isEqualTo(HttpURLConnection.HTTP_OK);
    assertThat(response2.output).isEqualTo("bar");
  }

  @Test
  public void testGet_resp() throws Exception {
    commands.del("foo-resp");
    commands.set("foo-resp", "bar");

    logger.info("Sending to redis directly...");
    String redisResponse = respClient.getFromRedis("foo-resp");
    assertThat(redisResponse).isEqualTo("$3\r\nbar\r\n");

    logger.info("Sending to proxy...");
    String proxyResponse = respClient.getFromProxy("foo-resp");
    assertThat(proxyResponse).isEqualTo("$3\r\nbar\r\n");
  }

  @Test
  public void testGet_http_noAssociatedValue() throws IOException {
    commands.del("garbage");
    HttpResponse response1 = httpClient.get("garbage");
    assertThat(response1.responseCode)
        .isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(response1.output).isEmpty();

    HttpResponse response2 = httpClient.get("garbage");
    assertThat(response2.responseCode)
        .isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(response2.output).isEmpty();
  }

  @Test
  public void testGet_resp_noAssociatedValue() throws Exception {
    commands.del("garbage");

    logger.info("Sending to redis directly...");
    String redisResponse = respClient.getFromRedis("garbage");
    assertThat(redisResponse).isEqualTo(RespProtocol.NULL_BULK_STRING);

    logger.info("Sending to proxy...");
    String proxyResponse = respClient.getFromProxy("garbage");
    assertThat(proxyResponse).isEqualTo(redisResponse);
    assertThat(redisResponse).isEqualTo(RespProtocol.NULL_BULK_STRING);
  }

  @Test
  public void testGet_resp_pipelined() throws Exception {
    commands.del("resp-pipeline-1", "resp-pipeline-2");
    commands.set("resp-pipeline-1", "hello");
    commands.set("resp-pipeline-2", "goodbye");
    String request1 = "GET resp-pipeline-1\r\n";
    String request2 =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$15\r\n")
            .append("resp-pipeline-2\r\n")
            .toString();

    logger.info("Sending to redis directly...");
    String redisResponse =
        respClient.getFromRedisRawString(request1 + request2);
    assertThat(redisResponse).isEqualTo("$5\r\nhello\r\n$7\r\ngoodbye\r\n");

    logger.info("Sending to proxy...");
    String proxyResponse =
        respClient.getFromProxyRawString(request1 + request2);
    assertThat(proxyResponse).isEqualTo("$5\r\nhello\r\n$7\r\ngoodbye\r\n");
  }

  @Test
  public void testGet_resp_exceptionReportedAsProtocolError() throws Exception {
    String requestWithoutArrayLength = "*\r\n$3GET\r\n$3\r\nfoo\r\n";

    logger.info("Sending to proxy...");
    String proxyResponse =
        respClient.getFromProxyRawString(requestWithoutArrayLength);
    assertThat(proxyResponse)
        .isEqualTo("-ERR Protocol error: invalid multibulk length\r\n");
  }

  @Test
  public void testGet_http_lruCacheEviction()
      throws IOException, InterruptedException, ExecutionException {
    // Read the capacity of the cache (n).
    int capacity = configuration.cacheCapacity();

    // Clear (n) keys in Redis and retrieve from the proxy (should return
    // nothing).
    for (int i = 1; i <= capacity; i++) {
      String key = String.format("key-%d", i);
      commands.del(key);
      HttpResponse response = httpClient.get(key);
      assertThat(response.responseCode)
          .isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
      assertThat(response.output).isEmpty();
    }

    // Set the first key (LRU entry in the cache) to a different value in Redis.
    commands.set("key-1", "new-val-1");

    // Get all (n) keys from proxy (should all be cached as missing).
    for (int i = 1; i <= capacity; i++) {
      String key = String.format("key-%d", i);
      HttpResponse response = httpClient.get(key);
      assertThat(response.responseCode)
          .isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
      assertThat(response.output).isEmpty();
    }

    // If we try to get a new value, it should replace the LRU entry in the
    // cache.
    httpClient.get("another-key");

    // Calling the proxy for the first entry will then read it from the backing
    // Redis instance.
    HttpResponse loadedResponse = httpClient.get("key-1");
    // The previously cached, absent value, is replaced with the newly-loaded
    // value.
    assertThat(loadedResponse.responseCode)
        .isEqualTo(HttpURLConnection.HTTP_OK);
    assertThat(loadedResponse.output).isEqualTo("new-val-1");
  }

  @Test
  public void testGet_http_parallelRequests()
      throws IOException, InterruptedException, ExecutionException {
    ExecutorService threadPool = Executors.newFixedThreadPool(3);
    try {
      // Set 5 key value pairs.
      KEY_VALUE_MAP.entrySet()
          .forEach(entry -> commands.set(entry.getKey(), entry.getValue()));

      // Spawn a callable for each key value pair.
      // Each callable will trigger the server to spawn a handler thread.
      List<CallableHttpGet> callables = new LinkedList<>();
      for (Map.Entry<String, String> entry : KEY_VALUE_MAP.entrySet()) {
        callables.add(new CallableHttpGet(entry.getKey()));
      }

      // Invoke all the requests in parallel.
      List<Future<Entry<String, HttpResponse>>> futures =
          threadPool.invokeAll(callables);

      // Verify that each future succeeds.
      for (Future<Map.Entry<String, HttpResponse>> future : futures) {
        Map.Entry<String, HttpResponse> result = future.get();
        String key = result.getKey();
        String value = result.getValue().output;
        int code = result.getValue().responseCode;
        assertThat(code).isEqualTo(HttpURLConnection.HTTP_OK);
        assertThat(key).startsWith("key");
        assertThat(value).startsWith("val");
        assertThat(value).isEqualTo(KEY_VALUE_MAP.get(key));
      }
    } finally {
      commands.del(KEY_VALUE_MAP.keySet().toArray(new String[0]));
      threadPool.shutdown();
    }
  }

  @Test
  public void testGet_resp_parallelPipelinedRequests()
      throws IOException, InterruptedException, ExecutionException {
    ExecutorService threadPool = Executors.newFixedThreadPool(3);
    try {
      // Set 3 key value pairs.
      commands.set("resp-key-1", "1");
      commands.set("resp-key-2", "2");
      commands.set("resp-key-3", "3");

      // Spawn 3 callables that will call pipelined get commands for the 3 key
      // value pairs.
      // Each callable will trigger the server to spawn a handler thread, which
      // will handle the pipelined commands in parallel.
      String pipelinedCommand =
          "GET resp-key-1\r\n" + "GET resp-key-2\r\n"
              + "*2\r\n$3\r\nGET\r\n$10\r\nresp-key-3\r\n";
      List<CallablePipelinedRespGets> callables = new LinkedList<>();
      callables.add(new CallablePipelinedRespGets(pipelinedCommand));
      callables.add(new CallablePipelinedRespGets(pipelinedCommand));
      callables.add(new CallablePipelinedRespGets(pipelinedCommand));

      // Invoke all the requests in parallel.
      List<Future<String>> futures = threadPool.invokeAll(callables);

      // Verify that each future succeeds.
      for (Future<String> future : futures) {
        assertThat(future.get()).isEqualTo("$1\r\n1\r\n$1\r\n2\r\n$1\r\n3\r\n");
      }
    } finally {
      commands.del("resp-key-1", "resp-key-2", "resp-key-3");
      threadPool.shutdown();
    }
  }

  /**
   * Callable that sends an HTTP.GET request to the proxy server.
   * <p>
   * The result includes the key that was requested and the full HttpResponse.
   */
  private class CallableHttpGet
      implements
        Callable<Map.Entry<String, HttpResponse>> {
    String key;

    public CallableHttpGet(String key) {
      this.key = key;
    }

    @Override
    public Map.Entry<String, HttpResponse> call() throws Exception {
      return new AbstractMap.SimpleEntry<>(key, httpClient.get(key));
    }
  }

  /**
   * Callable that sends a sequence of RESP.GET requests to the proxy server.
   * <p>
   * The result includes the server's response.
   */
  private class CallablePipelinedRespGets implements Callable<String> {
    String command;

    public CallablePipelinedRespGets(String command) {
      this.command = command;
    }

    @Override
    public String call() throws Exception {
      return respClient.getFromProxyRawString(command);
    }
  }
}
