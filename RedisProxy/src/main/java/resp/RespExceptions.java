package resp;

import io.lettuce.core.protocol.CommandType;

/**
 * Exceptions thrown due to RESP protocol errors or unsupported RESP features.
 */
public final class RespExceptions {

  static final String UNNEXPECTED_EMPTY_LINE_ERROR =
      "unnexpected empty line while parsing command";
  static final String RESP_ARRAY_LENGTH_ERROR =
      "this proxy server only supports bulk string arrays of size 2";
  static final String BULK_STRING_END_OF_LINE_ERROR_FORMAT =
      "bulk string %s ended incorrectly";
  static final String INVALID_MULTIBULK_LENGTH = "invalid multibulk length";
  static final String INVALID_BULK_LENGTH = "invalid bulk length";

  /** Abstract exception class extended by all others. */
  abstract static class RespException extends Exception {

    private static final long serialVersionUID = 1L;

    String message;

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof RespException
          && this.getClass().equals(obj.getClass())
          && this.getMessage().equals(((RespException) obj).getMessage());
    }
  }

  /**
   * Exception thrown when a RESP feature is not supported by this application.
   */
  public static class UnsupportedRespFeatureException extends RespException {
    private static final long serialVersionUID = 1L;

    public UnsupportedRespFeatureException(String feature) {
      message =
          String.format(
              "-ERR proxy server does not support this resp feature: %s",
              feature);
    }
  }

  /**
   * Exception thrown when the wrong number of arguments are provided for a
   * command.
   */
  public static class WrongNumberOfArgumentsException extends RespException {
    private static final long serialVersionUID = 1L;

    public WrongNumberOfArgumentsException(CommandType commandType) {
      message =
          String.format(
              "-ERR wrong number of arguments for '%s' command",
              commandType.name().toLowerCase());
    }
  }

  /**
   * Exception thrown when we are unable to parse the RESP request.
   */
  public static class RespSyntaxException extends RespException {
    private static final long serialVersionUID = 1L;

    public RespSyntaxException(String str) {
      message = String.format("-ERR Protocol error: %s", str);
    }

  }
}
