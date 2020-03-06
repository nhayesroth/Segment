package server;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

/** Tests for Coordinates. */
public class CoordinatesTest {
  
  @Test
  public void testConstructor() {
    Coordinates c = new Coordinates(10,  20);
    assertThat(c.x).isEqualTo(10);
    assertThat(c.y).isEqualTo(20);
  }
  
  @Test
  public void testMove() {
    Coordinates c =
        new Coordinates(10,  20).move(Direction.BOTTOM);
    assertThat(c.x).isEqualTo(10);
    assertThat(c.y).isEqualTo(21);
  }
  
  @Test
  public void testEquals() {
    Coordinates c1 = new Coordinates(10,  20);
    Coordinates c2 = new Coordinates(10,  20);
    assertThat(c1.equals(c2)).isTrue();
    assertThat(c1).isEqualTo(c2);
  }
  
  @Test
  public void testToString() {
    assertThat(new Coordinates(10, 20).toString())
      .isEqualTo(String.format("(%d,%d)", 10, 20));
  }

}
