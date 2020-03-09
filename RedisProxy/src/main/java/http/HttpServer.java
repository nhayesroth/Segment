package http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cache.LruCache;
import server.Server;

/**
 * Server that handles HTTP request/responses.
 */
public class HttpServer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  private static final int PORT = 8080;
  private static final int MAX_CONCURRENT_HANDLERS = 3;

  private final ExecutorService threadPool;
  private final ServerSocket serverSocket;
  private final LruCache cache;  

  public HttpServer(LruCache cache) throws IOException {
    serverSocket = new ServerSocket(PORT);
    threadPool = Executors.newFixedThreadPool(MAX_CONCURRENT_HANDLERS);
    this.cache = cache;
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        HttpServer.this.shutdown();
      }
    });
    logger.info("Server started with thread pool size [{}]...", MAX_CONCURRENT_HANDLERS);
  }

  @Override
  public void run() {
    while (true) {
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
    logger.debug("Waiting for client to connect...");
    Socket socket = serverSocket.accept();
    logger.debug("Client accepted: {}", socket);
    return socket;
  }

  private void spawnRequestHandler(Socket socket) throws IOException {
    threadPool.execute(new HttpRequestHandler(socket, cache));
  }

  private void shutdown() {
    logger.info("Shutting down the server...");
    if (serverSocket != null) {
      try {
        serverSocket.close();
        logger.warn("Closed server socket: {}", serverSocket);
      } catch (IOException e) {
        logger.warn("Failed to close serverSocket: {}", serverSocket);
        e.printStackTrace();
      }
    }
    threadPool.shutdownNow();
    logger.info("Shutdown threadpool: {}", threadPool); 
  }
}