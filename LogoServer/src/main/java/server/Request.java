package server;

import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;

/** Represents a request sent by the client. */
public class Request {
  
  enum Type {
    INVALID,
    STEPS,
    LEFT,
    RIGHT,
    HOVER,
    DRAW,
    ERASER,
    COORD,
    RENDER,
    CLEAR,
    QUIT;
    
    private static Type fromString(String string) {
      try {
        return Type.valueOf(string.toUpperCase());
      } catch (IllegalArgumentException e) {
        return Type.INVALID;
      }
    }
  }
  
  String line;
  Type type;
  Optional<Integer> steps;
  Optional<Exception> exception;
  
  Request(
      String line,
      Type type,
      Optional<Integer> steps,
      Optional<Exception> exception) {
    this.line = line;
    this.type = type;
    this.steps = steps;
    this.exception = exception;
  }
  
  public static class Builder {
    Type type;
    String line;
    Optional<Integer> steps;
    Optional<Exception> exception;
    public Builder() {
      type = Type.INVALID;
      line = "";
      steps = Optional.empty();
      exception = Optional.empty();
    }
    public Builder setLine(String line) {
      this.line = line;
      return this;
    }
    Builder setType(Type type) {
      this.type = type;
      return this;
    }
    Builder setSteps(int steps) {
      this.steps = Optional.of(steps);
      return this;
    }
  
    public Builder setException(Exception e) {
      exception = Optional.of(e);
      return this;
    }
    Request build() {
      return new Request(line, type, steps, exception);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Request parse(String str) {
    // Trim any additional whitespace and split the request values.
    str = str.trim();
    String[] array = str.split("\\s+");
    // Parse the Type & Steps from the input array and build a Request.
    Type type = Type.fromString(array[0]);
    Request.Builder request =
        Request.newBuilder()
          .setType(type)
          .setLine(str);
    switch (type) {
      case STEPS:
      case LEFT:
      case RIGHT:
        // If step count isn't provided, default to 1.
        if (array.length <= 1) {
          return request.setSteps(1).build();
        } else {
          try {
            return request
              .setSteps(Integer.parseInt(array[1]))
              .build();
          } catch (NumberFormatException e) {
            // If step count is non-integer, reject the request.
            return request
                .setType(Type.INVALID)
                .setException(e)
                .build();
          }
        }
      default:
        return request.build();
    }
}  
  
  @Override
  public String toString() {
    return new StringJoiner(
        /** delimiter= */ ", ",
        /** prefix= */ "{",
        /** sufix= */ "}")
        .add(String.format("line=\"%s\"", line))
        .add(String.format("type=%s", type))
        .add(String.format("steps=%s", steps))
        .add(
            String.format(
                "exception=%s",
                exception.map(Request::getStackTraceString)))
        .toString();
  }
  
  private static String getStackTraceString(Exception e) {
    StringJoiner sj = new StringJoiner("\n  ").add(e.toString());
    Arrays.stream(e.getStackTrace())
      .forEach(element -> sj.add(element.toString()));
    return sj.toString();
  }
}
