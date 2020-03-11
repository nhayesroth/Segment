package resp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import cache.LruCache;
import io.lettuce.core.api.async.RedisAsyncCommands;
import server.Server;

/**
 * Server that handles Resp request/responses.
 */
public class RespServer extends Thread {

  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  private static final int PORT = 8124;
  private static final int MAX_CONCURRENT_HANDLERS = 3;
  private static int sessionId = 1;

  private final ExecutorService threadPool;
  private final ServerSocket serverSocket;
  private final LruCache cache;  

  public RespServer(LruCache cache) throws IOException {
    serverSocket = new ServerSocket(PORT);
    threadPool = Executors.newFixedThreadPool(MAX_CONCURRENT_HANDLERS);
    this.cache = cache;
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        RespServer.this.shutdown();
      }
    });
    logger.info("Server started with thread pool size [{}]...", MAX_CONCURRENT_HANDLERS);
  }

  @Override
  public void run() {
    while (!serverSocket.isClosed()) {
      try {
        Socket socket = waitForClientToConnect();
        spawnRequestHandler(socket);
      } catch (Exception e) {
        logger.info("Encountered an Exception: {}", e);
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private Socket waitForClientToConnect() throws IOException {
    logger.info("Waiting for client [{}] to connect...", sessionId);
    Socket socket = serverSocket.accept();
    logger.info("Client [{}] accepted: {}", sessionId, socket);
    return socket;
  }

  private void spawnRequestHandler(Socket socket) throws IOException {
    threadPool.execute(new RespRequestHandler(sessionId++, socket));
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
