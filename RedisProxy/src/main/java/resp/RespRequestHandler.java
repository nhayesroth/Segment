package resp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cache.LruCache;
import server.Server;

/** Handles an RESP get request. */
public class RespRequestHandler implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  private Socket socket;
  private final BufferedReader inputReader;
  private final OutputStream outputStream;
  private final LruCache cache;  

  public RespRequestHandler(Socket socket, LruCache cache) throws IOException {
    this.socket = socket;
    this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));;
    this.outputStream = socket.getOutputStream();
    this.cache = cache;
  }

  @Override
  public void run() {
    try {
      // TODO: read request in RESP, convert to String, and process.
      parseRequest();
      closeSocket();
    } catch (Exception e) {
      e.printStackTrace();
      closeSocket();
    }
  }
  
  private RespRequest parseRequest() {
    try {
      return RespRequest.parse(inputReader);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void closeSocket() {
    try {
      socket.close();
      logger.debug("Closed socket {}", socket);
    } catch (IOException e) {
      logger.warn("Encountered exception while closing socket.");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
