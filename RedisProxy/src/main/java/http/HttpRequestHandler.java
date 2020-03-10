package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cache.LruCache;
import server.Server;

/** Handles an HTTP get request. */
public class HttpRequestHandler implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
  private Socket socket;
  private final BufferedReader inputReader;
  private final OutputStream outputStream;
  private final LruCache cache;  

  public HttpRequestHandler(Socket socket, LruCache cache) throws IOException {
    this.socket = socket;
    this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));;
    this.outputStream = socket.getOutputStream();
    this.cache = cache;
  }

  @Override
  public void run() {
    try {
      HttpRequest request = parseRequest();
      String result = getResult(request);
      writeToOutput(result);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      closeSocket();
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
  
  public static final String FOUND_FORMAT_STRING = "HTTP/1.1 200 OK\r\n\r\n%s";
  public static final String NO_CONTENT_STRING = "HTTP/1.1 204 No Content\r\n\r\n";
  
  private String getResult(HttpRequest request) throws ExecutionException {
    return cache.get(request.key)
        .map(value -> String.format(FOUND_FORMAT_STRING, value))
        .orElse(NO_CONTENT_STRING);
  }

  private HttpRequest parseRequest() {
    try {
      return HttpRequest.parse(inputReader.readLine());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  private void writeToOutput(String httpResponse) {
    try {
      outputStream.write(httpResponse.getBytes("UTF-8"));
      logger.info("Wrote to outputStream: {}", httpResponse);
    } catch (IOException e) {
      logger.warn("Encountered exception writing to output: ", httpResponse);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
