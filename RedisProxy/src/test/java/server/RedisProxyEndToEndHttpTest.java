package server;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisProxyEndToEndHttpTest {
  
  private static RedisClient client;
  private static RedisCommands<String, String> commands;

  @BeforeClass
  public static void setUp() throws Exception {
    client = RedisClient.create(
        RedisURI.builder()
          .withHost("localhost")
          .withPort(6379)
          .build());
    commands = client.connect().sync();
  }
  
  @After
  public void tearDown() {
    commands.getStatefulConnection().close();
    client.shutdown();
  }

//  @Test
//  public void testSet() {
//    commands.set("foo", "bar");
//  }
  
  @Test
  public void testGet() {
    System.out.println(commands.get("foo"));
  }

}
