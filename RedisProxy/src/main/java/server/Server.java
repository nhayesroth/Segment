package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

/**
 * Redis proxy server that adds basic caching functionality.
 */
public class Server {

  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  private static final int PORT = 8124;
  private static final int MAX_CONCURRENT_HANDLERS = 3;
  private static int sessionId = 1;

  private final ExecutorService threadPool;
  private final ServerSocket serverSocket;

  public Server() throws IOException {
    serverSocket = new ServerSocket(PORT);
    threadPool = Executors.newFixedThreadPool(MAX_CONCURRENT_HANDLERS);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Server.this.shutdown();
      }
    });
    logger.info("Server started with thread pool size {}...\n", MAX_CONCURRENT_HANDLERS);
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
  public static void main(String[] args) throws IOException {
    Server server = new Server();
    logger.info("Test {}...\n", "foo");
    return;
//    while (true) {
//      try {
//        Socket socket = server.waitForClientToConnect();
//        server.spawnRequestHandler(socket);
//      } catch (Exception e) {
//        logger.info("Encountered an Exception: %s", e);
//        e.printStackTrace();
//        throw new RuntimeException(e);
//      }
//    }
  }

  private Socket waitForClientToConnect() throws IOException {
    logger.info("Waiting for client [%d] to connect...\n", sessionId);
    Socket socket = serverSocket.accept();
    logger.info("Client [%d] accepted: %s", sessionId, socket);
    return socket;
  }

  private void spawnRequestHandler(Socket socket) throws IOException {
//    threadPool.execute(new RequestHandler(sessionId++, socket));
  }

  private void shutdown() {
    logger.info("Shutting down the server...");
    if (serverSocket != null) {
      try {
        serverSocket.close();
        logger.info("Closed server socket: %s", serverSocket);
      } catch (IOException e) {
        logger.info("Failed to close serverSocket: %s", serverSocket);
        e.printStackTrace();
      }
    }
    threadPool.shutdownNow();
    logger.info("Shutdown threadpool: %s", threadPool); 
  }
}
