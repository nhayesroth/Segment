package resp;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.lettuce.core.protocol.CommandType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resp.RespExceptions.RespException;
import resp.RespExceptions.RespSyntaxException;
import resp.RespExceptions.UnsupportedRespFeatureException;
import resp.RespExceptions.WrongNumberOfArgumentsException;
import server.Server;

/** Utilities related to converting to/from the RESP protocol. */
public class RespProtocol {

  public static final String NULL_BULK_STRING = "$-1\r\n";
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
    COMMAND, SIMPLE_STRING, ERROR, INTEGER, BULK_STRING, ARRAY;
  }

  private static final Logger logger =
      LoggerFactory.getLogger(Server.class.getName());

  static MessageType getMessageType(String str) {
    return TYPE_MAP.getOrDefault(str.charAt(0), MessageType.COMMAND);
  }

  /**
   * Parses a list of GET commands from the provided input.
   */
  public static ImmutableList<RespRequest> parse(InputStream inputStream)
      throws IOException, RespException {
    InputStreamReader inputReader =
        new InputStreamReader(inputStream, Charsets.UTF_8);
    ImmutableList.Builder<RespRequest> listBuilder = ImmutableList.builder();
    do {
      Optional<RespRequest> request = parseSingleRequest(inputReader);
      if (request.isPresent()) {
        logger.info("Parsed request: {}", request.get());
        listBuilder.add(request.get());
      } else {
        break;
      }
    } while (inputReader.ready());
    logger.info(
        "Parsed {} requests in total: {}",
        listBuilder.build().size(),
        listBuilder.build());
    return listBuilder.build();
  }

  public static Optional<RespRequest> parseSingleRequest(
      InputStreamReader inputReader) throws IOException, RespException {
    RespString firstLine = getFirstLine(inputReader);
    if (firstLine.line().isEmpty()) {
      logger.warn("Input is empty!");
      return Optional.empty();
    }
    RespProtocol.MessageType messageType =
        RespProtocol.getMessageType(firstLine.line());
    switch (messageType) {
      case ARRAY:
        return Optional.of(parseArrayCommand(firstLine, inputReader));
      case COMMAND:
        return Optional.of(parseSimpleCommand(firstLine));
      default:
        throw new UnsupportedRespFeatureException(messageType.name());
    }
  }

  /** Converts a string into the equivalent bulk string. */
  public static String toBulkString(Optional<String> result) {
    if (result.isPresent()) {
      String value = result.get();
      int length = value.getBytes().length;
      return String.format("$%d\r\n%s\r\n", length, value);
    } else {
      return NULL_BULK_STRING;
    }
  }

  /** Formats a RESP bulk string into a single line for human-reading. */
  public static String formatNewLineChars(String str) {
    return str.replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n");
  }

  private static RespRequest parseSimpleCommand(RespString command)
      throws UnsupportedRespFeatureException, WrongNumberOfArgumentsException {
    String[] array = command.line().split("\\s+");
    CommandType commandType = CommandType.valueOf(array[0]);
    if (commandType != CommandType.GET) {
      throw new UnsupportedRespFeatureException(commandType.name());
    }
    if (array.length != 2) {
      throw new WrongNumberOfArgumentsException(commandType);
    }
    String key = array[1];
    return RespRequest.get(key);
  }

  private static void checkExpectedBulkStringLength(RespString lengthLine,
      RespString bulkString) {
    // Warn if input has the wrong length, but handle it.
    if (bulkString.line().length() != Integer
        .parseInt(lengthLine.line().substring(1))) {
      logger.warn("Unnexpected length of bulk string.");
      logger.warn("lengthLins: {}", lengthLine);
      logger.warn("bulkString: {}", bulkString);
    }
  }

  private static RespRequest parseArrayCommand(RespString arrayLengthLine,
      InputStreamReader inputReader) throws IOException, RespException {
    validateArrayHasLength2(arrayLengthLine.line());

    RespString commandLengthLine = readLine(inputReader).verifyNotEmpty();
    RespString commandLine = readLine(inputReader).verifyNotEmpty();
    CommandType commandType = CommandType.valueOf(commandLine.line());
    checkExpectedBulkStringLength(commandLengthLine, commandLine);
    if (commandType != CommandType.GET) {
      throw new UnsupportedRespFeatureException(commandType.name());
    }

    RespString keyLengthLine = readLine(inputReader).verifyNotEmpty();
    RespString keyLine = readLine(inputReader).verifyNotEmpty();
    checkExpectedBulkStringLength(keyLengthLine, keyLine);

    return RespRequest.get(keyLine.line());
  }

  private static void validateArrayHasLength2(String arrayLengthLine)
      throws RespSyntaxException, UnsupportedRespFeatureException {
    if (getArrayLength(arrayLengthLine) != 2) {
      throw new UnsupportedRespFeatureException(
          RespExceptions.RESP_ARRAY_LENGTH_ERROR);
    }
  }

  private static int getArrayLength(String arrayFirstLine)
      throws RespSyntaxException {
    String possibleArrayLength = arrayFirstLine.substring(1);
    // Enforce digits only and that it can't start with 0.
    if (!possibleArrayLength.matches("[1-9]\\d*")) {
      throw new RespSyntaxException(RespExceptions.INVALID_MULTIBULK_LENGTH);
    }
    return Integer.parseInt(possibleArrayLength);
  }

  // Find the first real line of input, ignoring any leading "\r\n" or "\n".
  private static RespString getFirstLine(InputStreamReader inputReader)
      throws IOException, RespException {
    do {
      RespString firstLine = readLine(inputReader);
      if (!firstLine.line().isEmpty()) {
        logger.info("Found first line: {}", firstLine);
        return firstLine;
      }
      logger.info(
          "Skipped empty first line: {}",
          formatNewLineChars(firstLine.line()));
    } while (inputReader.ready());
    return RespString.EMPTY;
  }

  /**
   * Reads a line from input, enforcing end-of-line syntax before stripping
   * "\r\n" from the result.
   */
  private static RespString readLine(InputStreamReader inputReader)
      throws IOException, RespException {
    RespString.Builder respString = RespString.newBuilder();
    while (true) {
      int i = inputReader.read();
      if (i == -1) {
        logger.warn("Encountered end of stream in middle of request.");
        return RespString.EMPTY;
      }
      char c = (char) i;
      // Check if this is the end of line.
      if (c == '\r') {
        if ((char) inputReader.read() == '\n') {
          return respString.setEndOfLine("\r\n").build();
        }
        // Ending any line with only '\r' is never allowed.
        throw new RespSyntaxException(
            String.format(
                RespExceptions.BULK_STRING_END_OF_LINE_ERROR_FORMAT,
                respString.setEndOfLine("\r").build()));
      }
      // Some lines are allowed to end with only '\n'.
      if (c == '\n') {
        return respString.setEndOfLine("\n").build();
      }
      // Continue building the line.
      respString.addToLine(c);
    }
  }
}
