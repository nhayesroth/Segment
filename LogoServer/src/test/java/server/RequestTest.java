package server;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import java.io.IOException;
import org.junit.Test;
import server.Request.Type;

/** Tests for Request. */
public class RequestTest {
  
  @Test
  public void testBuilder_setNothing() {
    Request request = Request.newBuilder().build();
    assertThat(request.line).isEqualTo("");
    assertThat(request.type).isEqualTo(Type.INVALID);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testBuilder_setEverything() {
    String line = "  foo  ";
    Type type = Type.COORD;
    int steps = 12;
    Exception exception = new IOException("bar");
    Request request =
        Request.newBuilder()
          .setLine(line)
          .setType(type)
          .setSteps(steps)
          .setException(exception)
          .build();
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(type);
    assertThat(request.steps).hasValue(steps);
    assertThat(request.exception).hasValue(exception);
  }

  @Test
  public void testParse_steps() {
    String line = "steps 5";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.STEPS);
    assertThat(request.steps).hasValue(5);
    assertThat(request.exception).isEmpty();
  }

  @Test
  public void testParse_left() {
    String line = "left 0";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.LEFT);
    assertThat(request.steps).hasValue(0);
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_right() {
    String line = "right 12";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.RIGHT);
    assertThat(request.steps).hasValue(12);
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_hover() {
    String line = "hover";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.HOVER);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_eraser() {
    String line = "eraser";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.ERASER);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_coord() {
    String line = "coord";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.COORD);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_render() {
    String line = "render";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.RENDER);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_clear() {
    String line = "clear";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.CLEAR);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_quit() {
    String line = "quit";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.QUIT);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_draw() {
    String line = "draw";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.DRAW);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_stepsMissing_defaultsTo1() {
    String line1 = "steps";
    Request request1 = Request.parse(line1);
    assertThat(request1.line).isEqualTo(line1);
    assertThat(request1.type).isEqualTo(Type.STEPS);
    assertThat(request1.steps).hasValue(1);
    assertThat(request1.exception).isEmpty();
  }

  @Test
  public void testParse_stepsNotAppropriate_stepsIgnored() {
    String line = "draw 10";
    Request request = Request.parse(line);
    assertThat(request.line).isEqualTo(line);
    assertThat(request.type).isEqualTo(Type.DRAW);
    assertThat(request.steps).isEmpty();
    assertThat(request.exception).isEmpty();
  }
  
  @Test
  public void testParse_stepsNonInteger_requestInvalid() {
    String line1 = "steps 10.5";
    Request request1 = Request.parse(line1);
    assertThat(request1.line).isEqualTo(line1);
    assertThat(request1.type).isEqualTo(Type.INVALID);
    assertThat(request1.exception).isPresent();
    assertThat(request1.exception.get())
      .isInstanceOf(NumberFormatException.class);

    String line2 = "left foo";
    Request request2 = Request.parse(line2);
    assertThat(request2.line).isEqualTo(line2);
    assertThat(request2.type).isEqualTo(Type.INVALID);
    assertThat(request2.exception).isPresent();
    assertThat(request2.exception.get())
      .isInstanceOf(NumberFormatException.class);
  }
  
  @Test
  public void testParse_stepsNegative() {
    String line1 = "steps -10";
    Request request1 = Request.parse(line1);
    assertThat(request1.line).isEqualTo(line1);
    assertThat(request1.type).isEqualTo(Type.STEPS);
    assertThat(request1.steps).hasValue(-10);
    assertThat(request1.exception).isEmpty();
  }
  
  @Test
  public void testParse_invalidCommand() {
    String line1 = "foobar -10";
    Request request1 = Request.parse(line1);
    assertThat(request1.line).isEqualTo(line1);
    assertThat(request1.type).isEqualTo(Type.INVALID);
    assertThat(request1.exception).isEmpty();
  }
  
  @Test
  public void testToString() {
    String line = "foo";
    Type type = Type.STEPS;
    int steps = 12;
    
    Request request =
        Request.newBuilder()
          .setLine(line)
          .setType(type)
          .setSteps(steps)
          .build();
    assertThat(request.toString())
      .isEqualTo(
          String.format(
              "{line=\"foo\","
              + " type=STEPS,"
              + " steps=Optional[12],"
              + " exception=Optional.empty}"));
  }
  
  @Test
  public void testToString_withException() {
    String line = "foo";
    Type type = Type.STEPS;
    int steps = 12;
    Exception exception = new IOException("bar");
    StackTraceElement s1 =
        new StackTraceElement("Class1", "Method2", "File3", 4);
    StackTraceElement s2 =
        new StackTraceElement("Class11", "Method12", "File13", 14);
    StackTraceElement[] array = {s1, s2};
    exception.setStackTrace(array);
    
    Request request =
        Request.newBuilder()
          .setLine(line)
          .setType(type)
          .setSteps(steps)
          .setException(exception)
          .build();
    assertThat(request.toString())
      .isEqualTo(
          String.format(
              "{line=\"foo\","
              + " type=STEPS,"
              + " steps=Optional[12],"
              + " exception=Optional[java.io.IOException: bar"
              + "\n  Class1.Method2(File3:4)"
              + "\n  Class11.Method12(File13:14)]}"));
  }
}
