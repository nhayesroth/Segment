package http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

/** Simple client that can make http requests to the proxy server. */
public class HttpClient {
  public static final String LOCALHOST = "localhost";
  public static final String REDIS_PROXY = "redis_proxy";
  private static final String URL_FORMAT = "http://%s:%d/%s";
  private static final Logger logger =
      LoggerFactory.getLogger(Server.class.getName());

  private final String host;
  private final int port;

  public HttpClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Calls HTTP.GET against the proxy server and returns the result.
   */
  public HttpResponse get(String key) throws IOException {
    String url = makeUrl(key);
    logger.info("Connecting to {}...", url);
    HttpURLConnection connection = getConnection(url);
    return HttpResponse.fromConnection(connection);
  }

  private static HttpURLConnection getConnection(String urlWithKey)
      throws MalformedURLException, IOException {
    return (HttpURLConnection) new URL(urlWithKey).openConnection();
  }

  private String makeUrl(String key) {
    return String.format(URL_FORMAT, host, port, key);
  }

//  /**
//   * Calls HTTP.GET against the proxy server and returns the result.
//   * 
//   * <p>This method targets localhost (appropriate for local/unit tests).
//   * To target a specific host (e.g. a different container), use {@link #getFromSpecificHost}. 
//   */
//  public static HttpResponse get(String key) throws IOException {
//    HttpURLConnection connection =
//        getConnection(makeUrl(LOCALHOST, 8080, key));
//    return HttpResponse.fromConnection(connection);
//  }
//  
//  /** Calls HTTP.GET against the proxy server and returns the result. */
//  public static HttpResponse getFromSpecificHost(String host, int port, String key) throws IOException {
//    HttpURLConnection connection = getConnection(makeUrl(host, port, key));
//    return HttpResponse.fromConnection(connection);
//  }
//  
//  private static HttpURLConnection getConnection(String urlWithKey) throws MalformedURLException, IOException {
//    logger.info("Connecting to {}...", urlWithKey);
//    return (HttpURLConnection) new URL(urlWithKey).openConnection();
//  }
//  
//  private static String makeUrl(String host, int port, String key) {
//    return String.format(URL_FORMAT, host, port, key);
//  }

}
