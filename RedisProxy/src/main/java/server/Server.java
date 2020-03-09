package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cache.LruCache;
import configuration.Configuration;
import http.HttpServer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import resp.RespServer;

/**
 * Redis proxy server that adds basic caching functionality.
 * 
 * <p>
 * The server spawns two threads that listen for clients on different ports.
 * Each port allows clients to retrieve values from Redis, but they use
 * separate protocols:
 * <ul>
 * <li>8080: HTTP GET
 * <li>8124: Resp GET
 * </ul>
 * 
 * <p>
 * Each protocol manages its own threadpool of concurrent clients. Requests are
 * pipelined to Redis.
 */
public class Server {

  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  private final ExecutorService threadPool = Executors.newFixedThreadPool(2);
  private RedisClient redisClient;
  private RedisAsyncCommands<String, String> commands;
  private LruCache cache;
  private Configuration configuration;

  public Server() {
    configuration = Configuration.getFromEnvironment();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Server.this.shutdown();
      }
    });
  }
  
  public Server withConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }
  
  public Server start() throws IOException {
    logger.info("*****************************");
    logger.info("Starting server with configuration {}", configuration);
    logger.info("*****************************");
    redisClient =
        RedisClient.create(
            RedisURI.builder()
              .withHost(configuration.redisHost())
              .withPort(configuration.redisPort())
              .build());
    commands = redisClient.connect().async();
    cache = new LruCache(commands);
    startHttpServer();
    startRespServer();
    logger.info("*****************************");
    logger.info("Server started!");
    logger.info("*****************************");
    return this;
  }
  
  
  /**
   * Main server method, which initializes the cache and starts both listening for
   * HTTP and RESP connections.
   */
  public static void main(String[] args) throws Exception {
    Server server = new Server();
    server.start();
  }
  
  private void startRespServer() throws IOException {
    threadPool.execute(new RespServer(cache));    
  }
  
  private void startHttpServer() throws IOException {
    threadPool.execute(new HttpServer(cache));
  }

  private void shutdown() {
    logger.info("Shutting down the server...");
    commands.getStatefulConnection().close();
    redisClient.shutdown();
    logger.info("Redis client disconnected and shutdown.");
    threadPool.shutdownNow();
    logger.info("Shutdown threadpool: {}", threadPool);
  }
}
