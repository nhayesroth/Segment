package http;

import java.io.IOException;
import com.google.common.base.Preconditions;

/** Simple value class representing an HTTP request sent to the server. */
public class HttpRequest {
  String key;
  
  public HttpRequest(String key) {
    this.key = key;
  }

  public static HttpRequest parse(String str) throws IOException {
    String[] array = str.split("\\s+");
    Preconditions.checkState(array[0].equals("GET"));
    Preconditions.checkState(array[1].startsWith("/"));
    return new HttpRequest(array[1].substring(1));
  }
  
  @Override
  public String toString() {
    return String.format("HttpRequest { Key=%s }", key);
  }
}
