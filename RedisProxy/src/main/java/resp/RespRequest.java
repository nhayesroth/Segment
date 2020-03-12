package resp;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Simple value class used to parse RESP input.
 * 
 * <p>TODO: this class is not implemented
 */
public class RespRequest {
  
  protected String input;
  protected RespProtocol.MessageType messageType;
  protected String key;

  public static RespRequest parse(BufferedReader inputReader) throws IOException {
    return null;
  }

}
