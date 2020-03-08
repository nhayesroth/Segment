package http;

import java.io.BufferedReader;
import java.io.IOException;
import com.google.common.base.Preconditions;

public class HttpRequest {
  enum Type {
    INVALID, GET, POST;
  }
  Type type;
  String key;
  
  public HttpRequest(Type type, String key) {
    this.type = type;
    this.key = key;
  }

  public static HttpRequest parse(String str) throws IOException {
    String[] array = str.split("\\s+");
    Preconditions.checkState(array[0].equals("GET"));
    Preconditions.checkState(array[1].startsWith("/"));
    return new HttpRequest(
        Type.valueOf(array[0]),
        array[1].substring(1));
  }
  
  @Override
  public String toString() {
    return String.format(
        "HttpRequest {"
        +"\n\tType=%s,"
        + "\n\tKey=%s"
        + "}",
        type,
        key);
  }
}
