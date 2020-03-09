package server;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import configuration.Configuration;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisProxyEndToEndHttpTest {
  
  private static Server server;
  private static RedisClient client;
  private static RedisCommands<String, String> commands;

//  @BeforeClass
//  public static void setUp() throws Exception {
//    Server server = new Server()
//        .withConfiguration(
//            Configuration.getFromEnvironment()
//            .toBuilder()
//            .setRedisHost("redis")
//            .build())
//        .start();
//    client = RedisClient.create(
//        RedisURI.builder()
//          .withHost("redis")
//          .withPort(6379)
//          .build());
//    commands = client.connect().sync();
//  }
//  
//  @AfterClass
//  public static void tearDown() {
//    commands.getStatefulConnection().close();
//    client.shutdown();
//  }
//
//  @Test
//  public void testSet() {
//    commands.set("foo", "bar");
//  }
//  
//  @Test
//  public void testGet() {
//    System.out.println(commands.get("foo"));
//  }

}
