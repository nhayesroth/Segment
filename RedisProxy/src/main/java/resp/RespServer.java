package resp;

import cache.LruCache;
import configuration.Configuration;
import io.lettuce.core.RedisClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

/**
 * Server that handles Resp request/responses.
 */
public class RespServer extends Thread {

  private static final Logger logger =
      LoggerFactory.getLogger(Server.class.getName());

  private final ExecutorService threadPool;
  private final ServerSocket serverSocket;
  private final LruCache cache;
  private final Configuration configuration;

  public RespServer(
      LruCache cache,
      Configuration configuration,
      RedisClient redisClient)
      throws IOException {
    serverSocket = new ServerSocket(configuration.respPort());
    threadPool =
        Executors.newFixedThreadPool(configuration.maxConcurrentHandlers());
    this.cache = cache;
    this.configuration = configuration;
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        RespServer.this.shutdown();
      }
    });
    logger.info(
        "RESP server started with thread pool size [{}]...",
        configuration.maxConcurrentHandlers());
  }

  @Override
  public void run() {
    while (!serverSocket.isClosed()) {
      try {
        Socket socket = waitForClientToConnect();
        spawnRequestHandler(socket);
      } catch (Exception e) {
        logger.info("Encountered an Exception: {}", e.getMessage());
        shutdown();
        return;
      }
    }
  }

  private Socket waitForClientToConnect() throws IOException {
    logger.info("Waiting for client to connect...");
    Socket socket = serverSocket.accept();
    logger.info("Client accepted: {}", socket);
    return socket;
  }

  private void spawnRequestHandler(Socket socket) throws IOException {
    threadPool.execute(new RespRequestHandler(socket, cache, configuration));
  }

  public void shutdown() {
    logger.info("Shutting down the server...");
    if (serverSocket != null) {
      try {
        serverSocket.close();
        logger.info("Closed server socket: {}", serverSocket);
      } catch (IOException e) {
        logger.info("Failed to close serverSocket: {}", serverSocket);
        e.printStackTrace();
      }
    }
    threadPool.shutdownNow();
    logger.info("Shutdown threadpool: {}", threadPool);
  }
}
