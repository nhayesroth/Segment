package server;

import com.google.common.collect.ImmutableList;

/** Redis proxy server that adds additional features. */
public class Server {

  public static void main(String[] args) {
    ImmutableList<String> list = ImmutableList.of("Hello", " ", "world");
    System.out.println(list);
  }
}
