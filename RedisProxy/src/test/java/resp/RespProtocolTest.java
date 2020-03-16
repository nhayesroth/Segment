package resp;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import io.lettuce.core.protocol.CommandType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import org.junit.Test;
import resp.RespExceptions.RespSyntaxException;
import resp.RespExceptions.UnsupportedRespFeatureException;
import resp.RespExceptions.WrongNumberOfArgumentsException;
import resp.RespProtocol.MessageType;

/** Tests for the RespProtocol class. */
public class RespProtocolTest {

  /**
   * Utility method for testing {@link RespProtocol#parse}
   * <p>
   * Does the following:
   * <ul>
   * <li>Writes the specified string to an output stream
   * <li>Copies the output stream to a byte array
   * <li>Creates an input stream from the byte array
   * <li>Passes that input stream to {@link RespProtocol#parse}
   * <li>Returns the result
   * </ul>
   */
  private static ImmutableList<RespRequest> writeAndParse(String string)
      throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.write(string.getBytes());
    return RespProtocol
        .parse(new ByteArrayInputStream(outputStream.toByteArray()));
  }

  @Test
  public void testGetMessgeType() {
    assertThat(RespProtocol.getMessageType("+PING"))
        .isEqualTo(MessageType.SIMPLE_STRING);
    assertThat(RespProtocol.getMessageType("-ERR unknown command"))
        .isEqualTo(MessageType.ERROR);
    assertThat(RespProtocol.getMessageType(":1"))
        .isEqualTo(MessageType.INTEGER);
    assertThat(RespProtocol.getMessageType("$3\r\nfoo\r\n"))
        .isEqualTo(MessageType.BULK_STRING);
    assertThat(RespProtocol.getMessageType("*2\r\n$3\r\nGET\r\n$3\r\nfoo\r\n"))
        .isEqualTo(MessageType.ARRAY);
    assertThat(RespProtocol.getMessageType("GET foo\r\n"))
        .isEqualTo(MessageType.COMMAND);
  }

  @Test
  public void testToBulkString() throws Exception {
    assertThat(RespProtocol.toBulkString(Optional.of("foo")))
        .isEqualTo("$3\r\nfoo\r\n");

    assertThat(RespProtocol.toBulkString(Optional.of("cheeseburger")))
        .isEqualTo("$12\r\ncheeseburger\r\n");
  }

  @Test
  public void testToBulkString_null() throws Exception {
    assertThat(RespProtocol.toBulkString(Optional.empty()))
        .isEqualTo(RespProtocol.NULL_BULK_STRING);
  }

  @Test
  public void testParse_simpleGetCommand() throws Exception {
    String request = "GET foo\r\n";
    assertThat(writeAndParse(request)).containsExactly(RespRequest.get("foo"));
  }

  @Test
  public void testParse_arrayGetCommand() throws Exception {
    String request =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .toString();
    assertThat(writeAndParse(request)).containsExactly(RespRequest.get("foo"));
  }

  @Test
  public void testParse_chained_simpleGetCommand() throws Exception {
    String request1 = "GET foo\r\n";
    String request2 = "GET bar\r\n";
    String request3 = "GET hello\r\n";
    assertThat(writeAndParse(request1 + request2 + request3)).containsExactly(
        RespRequest.get("foo"),
        RespRequest.get("bar"),
        RespRequest.get("hello"));
  }

  @Test
  public void testParse_chained_arrayGetCommand() throws Exception {
    String request1 =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .toString();
    String request2 =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$12\r\n")
            .append("cheeseburger\r\n")
            .toString();
    assertThat(writeAndParse(request1 + request2)).containsExactly(
        RespRequest.get("foo"),
        RespRequest.get("cheeseburger"));
  }

  @Test
  public void testParse_chained_mixed_simpleAndArrayGetCommands()
      throws Exception {
    String request1 = "GET foo\r\n";
    String request2 =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$12\r\n")
            .append("cheeseburger\r\n")
            .toString();
    assertThat(writeAndParse(request1 + request2)).containsExactly(
        RespRequest.get("foo"),
        RespRequest.get("cheeseburger"));
  }

  @Test
  public void testParse_extraNewLines() throws Exception {
    String request1 = "GET foo\r\n";
    String request2 =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$12\r\n")
            .append("cheeseburger\r\n")
            .toString();
    String request =
        new StringBuilder().append("\r\n\r\n")
            .append("\n\n\n\n\n\n\n")
            .append(request1)
            .append("\n")
            .append("\r\n\r\n\r\n\r\n\r\n")
            .append("\n")
            .append(request2)
            .append("\r\n")
            .append("\n")
            .toString();
    System.out
        .println(request.replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n"));
    assertThat(writeAndParse(request)).containsExactly(
        RespRequest.get("foo"),
        RespRequest.get("cheeseburger"));
  }

  @Test
  public void testParse_unsupportedCommandType_throws() throws Exception {
    String request1 = "INCR foo\r\n";
    String request2 =
        new StringBuilder().append("*2\r\n")
            .append("$2\r\n")
            .append("INCR\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .toString();

    UnsupportedRespFeatureException exception1 =
        assertThrows(
            UnsupportedRespFeatureException.class,
            () -> writeAndParse(request1));
    UnsupportedRespFeatureException exception2 =
        assertThrows(
            UnsupportedRespFeatureException.class,
            () -> writeAndParse(request2));

    UnsupportedRespFeatureException expectedException =
        new UnsupportedRespFeatureException("INCR");

    assertThat(exception1).isEqualTo(expectedException);
    assertThat(exception2).isEqualTo(expectedException);
  }

  @Test
  public void testParse_get_tooManyParams_throws() throws Exception {
    String request1 = "GET foo bar\r\n";
    WrongNumberOfArgumentsException exception1 =
        assertThrows(
            WrongNumberOfArgumentsException.class,
            () -> writeAndParse(request1));
    WrongNumberOfArgumentsException expectedException1 =
        new WrongNumberOfArgumentsException(CommandType.GET);
    assertThat(exception1).isEqualTo(expectedException1);

    String request2 =
        new StringBuilder().append("*3\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .append("$3\r\n")
            .append("bar\r\n")
            .toString();
    UnsupportedRespFeatureException exception2 =
        assertThrows(
            UnsupportedRespFeatureException.class,
            () -> writeAndParse(request2));
    UnsupportedRespFeatureException expectedException2 =
        new UnsupportedRespFeatureException(
            RespExceptions.RESP_ARRAY_LENGTH_ERROR);
    assertThat(exception2).isEqualTo(expectedException2);
  }

  @Test
  public void testParse_get_tooFewParams_throws() throws Exception {
    String request1 = "GET\r\n";
    WrongNumberOfArgumentsException exception1 =
        assertThrows(
            WrongNumberOfArgumentsException.class,
            () -> writeAndParse(request1));
    WrongNumberOfArgumentsException expectedException1 =
        new WrongNumberOfArgumentsException(CommandType.GET);
    assertThat(exception1).isEqualTo(expectedException1);

    String request2 =
        new StringBuilder().append("*2\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .toString();
    RespSyntaxException exception2 =
        assertThrows(RespSyntaxException.class, () -> writeAndParse(request2));
    RespSyntaxException expectedException2 =
        new RespSyntaxException(RespExceptions.UNNEXPECTED_EMPTY_LINE_ERROR);
    assertThat(exception2).isEqualTo(expectedException2);
  }

  @Test
  public void testParse_array_tooLong_throws() throws Exception {
    String request =
        new StringBuilder().append("*3\r\n")
            .append("$3\r\n")
            .append("SET\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .append("$3\r\n")
            .append("bar\r\n")
            .toString();

    UnsupportedRespFeatureException exception =
        assertThrows(
            UnsupportedRespFeatureException.class,
            () -> writeAndParse(request));

    UnsupportedRespFeatureException expectedException =
        new UnsupportedRespFeatureException(
            RespExceptions.RESP_ARRAY_LENGTH_ERROR);

    assertThat(exception).isEqualTo(expectedException);
  }

  @Test
  public void testParse_array_tooShort_throws() throws Exception {
    String request =
        new StringBuilder().append("*1\r\n")
            .append("$4\r\n")
            .append("PING\r\n")
            .toString();

    UnsupportedRespFeatureException exception =
        assertThrows(
            UnsupportedRespFeatureException.class,
            () -> writeAndParse(request));

    UnsupportedRespFeatureException expectedException =
        new UnsupportedRespFeatureException(
            RespExceptions.RESP_ARRAY_LENGTH_ERROR);

    assertThat(exception).isEqualTo(expectedException);
  }

  @Test
  public void testParse_array_lengthMissing_throws() throws Exception {
    String request1 =
        new StringBuilder().append("*\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .toString();

    RespSyntaxException exception1 =
        assertThrows(RespSyntaxException.class, () -> writeAndParse(request1));

    RespSyntaxException expectedException =
        new RespSyntaxException(RespExceptions.INVALID_MULTIBULK_LENGTH);

    assertThat(exception1).isEqualTo(expectedException);
  }

  @Test
  public void testParse_array_lengthNonNumeric_throws() throws Exception {
    String request1 =
        new StringBuilder().append("*bar\r\n")
            .append("$3\r\n")
            .append("GET\r\n")
            .append("$3\r\n")
            .append("foo\r\n")
            .toString();

    RespSyntaxException exception1 =
        assertThrows(RespSyntaxException.class, () -> writeAndParse(request1));

    RespSyntaxException expectedException =
        new RespSyntaxException(RespExceptions.INVALID_MULTIBULK_LENGTH);

    assertThat(exception1).isEqualTo(expectedException);
  }

}
