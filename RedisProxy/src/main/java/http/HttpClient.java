package http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import configuration.Configuration;

/** Simple client that can make http requests to the proxy server. */
public class HttpClient {
    
  private String urlPrefix;
  
  public HttpClient(Configuration configuration) {
    urlPrefix = String.format("http://%s:%d/", configuration.redisHost(), HttpServer.PORT);
  }
  
  /** Calls HTTP.GET against the proxy server and returns the result. */
  public HttpResponse get(String key) throws IOException {
    HttpURLConnection connection = getConnection(key);
    return HttpResponse.fromConnection(connection);
  }
  
  private HttpURLConnection getConnection(String key) throws MalformedURLException, IOException {
    return (HttpURLConnection) new URL(urlPrefix + key).openConnection();
  }
  
  
}
