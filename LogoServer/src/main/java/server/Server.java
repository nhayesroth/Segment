package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server that allows clients to draw on and render a virtual canvas.
 */
public class Server {

  private static final Logger logger = Logger.getLogger();
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
    logger.log("Server started with thread pool size [%d]...\n", MAX_CONCURRENT_HANDLERS);
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
    while (true) {
      try {
        Socket socket = server.waitForClientToConnect();
        server.spawnRequestHandler(socket);
      } catch (Exception e) {
        logger.log("Encountered an Exception: %s", e);
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private Socket waitForClientToConnect() throws IOException {
    logger.logWithLeadingNewline("Waiting for client [%d] to connect...\n", sessionId);
    Socket socket = serverSocket.accept();
    logger.log("Client [%d] accepted: %s", sessionId, socket);
    return socket;
  }

  private void spawnRequestHandler(Socket socket) throws IOException {
    threadPool.execute(new RequestHandler(sessionId++, socket));
  }

  private void shutdown() {
    logger.log("Shutting down the server...");
    if (serverSocket != null) {
      try {
        serverSocket.close();
        logger.log("Closed server socket: %s", serverSocket);
      } catch (IOException e) {
        logger.log("Failed to close serverSocket: %s", serverSocket);
        e.printStackTrace();
      }
    }
    threadPool.shutdownNow();
    logger.log("Shutdown threadpool: %s", threadPool); 
  }
}
