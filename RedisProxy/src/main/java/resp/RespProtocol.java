package resp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** Utilities related to converting to/from the RESP protocol. */
public class RespProtocol {
  
  /** Maps the first char of a message to its MessageType. */
  private static final ImmutableMap<Character, MessageType> TYPE_MAP =
      ImmutableMap.<Character, MessageType>builder()
      .put('+', MessageType.SIMPLE_STRING)
      .put('-', MessageType.ERROR)
      .put(':', MessageType.INTEGER)
      .put('$', MessageType.BULK_STRING)
      .put('*', MessageType.ARRAY)
      .build();
  
  /** Different types of RESP messages. */ 
  enum MessageType {
    INVALID,
    SIMPLE_STRING,
    ERROR,
    INTEGER,
    BULK_STRING,
    ARRAY;
  }
  
  byte[] toByteArray(InputStream inputStream) throws IOException {
    return IOUtils.toByteArray(inputStream);
  }

  public static String[] convertToStringArray(InputStream inputStream) throws IOException {
    byte[] byteArray = IOUtils.toByteArray(inputStream);
    String command = new String(byteArray, StandardCharsets.UTF_8);
    Preconditions.checkState(
        getMessageType(command) == MessageType.ARRAY,
        "Command is not of type Array: %s", command);
    // Iterate across the command, parsing out individual bulk strings.
    String[] stringArray = command.substring(6).split("\\$\\d");
    for (int i = 0; i < stringArray.length; i++) {
      stringArray[i] = convertBulkStringToString(stringArray[i]);
    }
    return stringArray;
  }
  
  private static MessageType getMessageType(String str) {
    return TYPE_MAP.getOrDefault(str.charAt(0), MessageType.INVALID);
  }
  
  public static String convertBulkStringToString(String str) {
    Preconditions.checkState(getMessageType(str) == MessageType.BULK_STRING);
    int firstIndexOfN = str.indexOf('n');
    int lastIndexOfCRLF = str.lastIndexOf("\r\n");
    return str.substring(firstIndexOfN + 1, lastIndexOfCRLF);
  }
}
