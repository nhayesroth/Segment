package server;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import configuration.Configuration;
import http.HttpClient;
import http.HttpResponse;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;
import resp.RespClient;
import resp.RespProtocol;

/**
 * End-to-end tests of the ProxyServer.
 * <p>
 * This test is mostly meant for local development, but can also be run in a
 * docker container.
 * <ul>
 * <li>First, the test attempts to connect to a redis instance based on the
 * environment variables recorded in {@link Configuration}.
 * <li>If the test is unable to connect to to that redis instance, a new
 * instance is spun up instead.
 * <li>Finally, a {@link Server ProxyServer} is created using test-specific
 * ports. So, it should not conflict with any that are already running.
 */
public class EndToEndServerTest {
  private static final Logger logger =
      LoggerFactory.getLogger(EndToEndServerTest.class.getName());

  private static final int HTTP_PORT = 8088;
  private static final int RESP_PORT = 9099;

  private static Configuration configuration;
  private static HttpClient httpClient;
  private static RespClient respClient;
  private static RedisClient redisClient;
  private static RedisCommands<String, String> commands;

  private static RedisServer redisServer;
  private static Server server;

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
    // Initialize configuration from environment variables and test-specific
    // proxy ports.
    configuration =
        Configuration.getFromEnvironment()
            .toBuilder()
            .setHttpPort(HTTP_PORT)
            .setRespPort(RESP_PORT)
            .build();
    // Use a backing redis instance if available, otherwise setup our own.
    try {
      redisClient =
          RedisClient.create(
              RedisURI.builder()
                  .withHost(configuration.redisHost())
                  .withPort(configuration.redisPort())
                  .build());
      commands = redisClient.connect().sync();
      logger.info(
          "Successfully connected to Redis instance at {}:{}.",
          configuration.redisHost(),
          configuration.redisPort());
    } catch (RedisConnectionException e) {
      logger.warn(
          "Unable to connect to Redis instance at {}:{}. Creating an embedded instance instead...",
          configuration.redisHost(),
          configuration.redisPort());
      redisServer = new RedisServer(configuration.redisPort());
      redisServer.start();
      configuration =
          configuration.toBuilder().setRedisHost("localhost").build();
      redisClient =
          RedisClient.create(
              RedisURI.builder()
                  .withHost(configuration.redisHost())
                  .withPort(configuration.redisPort())
                  .build());
      commands = redisClient.connect().sync();
    }
    // Configure the HTTP/RESP clients.
    httpClient = new HttpClient("localhost", HTTP_PORT);
    respClient = new RespClient("localhost", configuration);
    // Create the proxy server.
    server = new Server().withConfiguration(configuration).start();
  }

  @AfterClass
  public static void tearDown() {
    KEY_VALUE_MAP.entrySet().forEach(entry -> commands.del(entry.getKey()));
    server.shutdown();
    if (redisServer != null) {
      redisServer.stop();
    }
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
}
