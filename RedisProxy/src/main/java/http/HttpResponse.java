package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/** Simple value class representing an HTTP response returned by the server. */
public class HttpResponse {
  public final int responseCode;
  public final String output;

  public HttpResponse(int responseCode, String output) {
    this.responseCode = responseCode;
    this.output = output;
  }

  public static HttpResponse fromConnection(HttpURLConnection connection)
      throws IOException {
    int responseCode = connection.getResponseCode();
    BufferedReader in =
        new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line;
    String output = "";
    while ((line = in.readLine()) != null)
      output += line;
    in.close();
    return new HttpResponse(responseCode, output);
  }

  @Override
  public String toString() {
    return String
        .format("HttpResponse { Code=%s, Output=%s }", responseCode, output);
  }
}
