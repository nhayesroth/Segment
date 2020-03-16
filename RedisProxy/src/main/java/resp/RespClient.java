package resp;

import configuration.Configuration;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

/**
 * Simple client that can interact with the proxy server and its backing redis
 * instance via RESP.
 */
public class RespClient {
  private static final Logger logger =
      LoggerFactory.getLogger(Server.class.getName());

  private final String proxyHost;
  private final int proxyPort;

  private final String redisHost;
  private final int redisPort;

  public RespClient(String proxyHost, Configuration configuration) {
    this.proxyHost = proxyHost;
    this.proxyPort = configuration.respPort();
    this.redisHost = configuration.redisHost();
    this.redisPort = configuration.redisPort();
  }

  public String getFromProxyRawString(String rawString) throws Exception {
    return write(proxyHost, proxyPort, rawString);
  }

  public String getFromProxy(String key) throws Exception {
    String bulkStringArray = converToBulkStringArrayGetCommand(key);
    return write(proxyHost, proxyPort, bulkStringArray);
  }

  public String getFromRedisRawString(String rawString) throws Exception {
    return write(redisHost, redisPort, rawString);
  }

  public String getFromRedis(String key) throws Exception {
    String bulkStringArray = converToBulkStringArrayGetCommand(key);
    return write(redisHost, redisPort, bulkStringArray);
  }

  private static String converToBulkStringArrayGetCommand(String key) {
    return new StringJoiner(
        /* delimiter= */ "\r\n",
        /* prefix= */ "",
        /* suffix= */ "\r\n").add("*2")
            .add("$3")
            .add("GET")
            .add(String.format("$%d", key.length()))
            .add(key)
            .toString();
  }

  public static String write(String host, int port, String bulkStringArray)
      throws Exception {
    logger.info(
        "Sending RESP request to {}: {}",
        String.format("%s/%d", host, port),
        RespProtocol.formatNewLineChars(bulkStringArray));
    Socket clientSocket = new Socket(host, port);
    DataOutputStream outToServer =
        new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer =
        new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
    outToServer.writeBytes(bulkStringArray);
    Thread.sleep(500);
    String response = readResponse(inFromServer);
    clientSocket.close();
    logger.info(
        "Received RESP response: {}",
        RespProtocol.formatNewLineChars(response));
    return response;
  }

  private static String readResponse(BufferedReader responseReader)
      throws IOException, InterruptedException {
    String str = "" + (char) responseReader.read();
    while (responseReader.ready()) {
      str += (char) responseReader.read();
    }
    return str;
  }
}
