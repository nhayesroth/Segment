package server;

import java.util.Arrays;
import java.util.StringJoiner;

/** Logging class. */
public class Logger {
  
  private static final String SERVER_ID = "Server";
  private static final String SESSION_ID_FORMAT = "Session-%d";
  
  private final String id;
  
  public static Logger getLogger() {
    return new Logger(SERVER_ID);
  }
  
  public Logger(String id) {
    this.id = id;
  }
  
  public Logger(int sessionId) {
    id = String.format(SESSION_ID_FORMAT, sessionId);
  }
  
  public void log(String str, Object... args) {
    logWithIdPrefix(false, id, str, args);
  }
  
  public void logWithLeadingNewline(String str, Object... args) {
    logWithIdPrefix(true, id, str, args);
  }
  
  private static void logWithIdPrefix(
      boolean withLeadingNewLine, String idPrefix, String str, Object...args) {
    if (withLeadingNewLine) {
      System.out.println();
    }
    System.out.println(
        new StringJoiner(" ")
        .add(String.format("[%s]", idPrefix))
        .add(
            String.format(
                "[%s]",
                Arrays.stream(Thread.currentThread().getStackTrace())
                      .map(Object::toString)
                      .filter(s -> !s.contains("log") && !s.contains("getStackTrace"))
                      .findFirst()
                      .get()))
          .add(String.format(str, args))
          .toString());
  }
}
