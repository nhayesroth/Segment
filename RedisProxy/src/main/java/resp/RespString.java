package resp;

import com.google.auto.value.AutoValue;
import resp.RespExceptions.RespSyntaxException;

/**
 * AutoValue representation of a RESP input string.
 * <p>
 * Splits the input into a two strings - "line" and an "end of line" to simplify
 * processing.
 */
@AutoValue
public abstract class RespString {

  /**
   * Empty RespString, used primarily when an input line is expected, but
   * missing.
   */
  static final RespString EMPTY =
      newBuilder().setEndOfLine("").setEndOfLine("").build();

  abstract String line();

  abstract String endOfLine();

  @Override
  public String toString() {
    return line() + RespProtocol.formatNewLineChars(endOfLine());
  }

  static RespString empty() {
    return new AutoValue_RespString.Builder().setLine("")
        .setEndOfLine("\r\n")
        .build();
  }

  static RespString.Builder newBuilder() {
    return new AutoValue_RespString.Builder().setLine("");
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract String line();

    abstract Builder setLine(String str);

    Builder addToLine(char c) {
      setLine(line() + c);
      return this;
    }

    abstract Builder setEndOfLine(String str);

    abstract RespString build();
  }

  RespString verifyNotEmpty() throws RespSyntaxException {
    if (this.equals(EMPTY)) {
      throw new RespSyntaxException(
          RespExceptions.UNNEXPECTED_EMPTY_LINE_ERROR);
    }
    return this;
  }
}
