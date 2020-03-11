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
  private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());
    
  private static final String urlPrefix = String.format("http://redis_proxy:%d/", HttpServer.PORT);
  
  /** Calls HTTP.GET against the proxy server and returns the result. */
  public static HttpResponse get(String key) throws IOException {
    HttpURLConnection connection = getConnection(key);
    return HttpResponse.fromConnection(connection);
  }
  
  private static HttpURLConnection getConnection(String key) throws MalformedURLException, IOException {
    logger.info("Connecting to {}...", urlPrefix + key);
    return (HttpURLConnection) new URL(urlPrefix + key).openConnection();
  }
  
  
}
