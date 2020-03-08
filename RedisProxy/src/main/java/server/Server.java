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
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cache.LruCache;
import http.HttpServer;
import http.HttpServlet;
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
  private final RedisClient redisClient;
  private final RedisAsyncCommands<String, String> commands;
  private final LruCache cache;  

  public Server() {
    // TODO: make configurable
    redisClient =
        RedisClient.create(
            RedisURI.builder()
              .withHost("localhost")
              .withPort(6379)
              .build());
    commands = redisClient.connect().async();
    cache = new LruCache(commands);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Server.this.shutdown();
      }
    });
  }
  /**
   * Main server method, which continuously does the following:
   * 
   * <ul>
   * <li>Waits for a client to connect.
   * <li>Accepts a connection and spawns a RequestHandler to execute it.
   * </ul>
   * 
   * <p>
   * RequestHandlers are executed by the {@link #threadPool} at some point in the
   * future.
   */
  public static void main(String[] args) throws Exception {
    Server server = new Server();
    server.startHttpServer();
    server.startRespServer();
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
