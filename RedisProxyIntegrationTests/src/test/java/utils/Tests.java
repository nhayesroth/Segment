package utils;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

public class Tests {

  public static void main(String[] args) {
    System.out.println("Running tests!");
    JUnitCore engine = new JUnitCore();
    engine.run(AllTests.class);
    engine.addListener(new TextListener(System.out));
  }
}
