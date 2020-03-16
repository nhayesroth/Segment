package resp;

import com.google.auto.value.AutoValue;
import io.lettuce.core.protocol.CommandType;

/**
 * Simple value class representing a request/command sent to Redis.
 * <p>
 * Note: this only supports GET requests as of now, but it could be easily
 * extended to support other command types and parameter(s).
 */
@AutoValue
public abstract class RespRequest {
  abstract CommandType commandType();

  abstract String key();

  /** Create a GET request targeting the specified key. */
  static RespRequest get(String key) {
    return new AutoValue_RespRequest(CommandType.GET, key);
  }
}
