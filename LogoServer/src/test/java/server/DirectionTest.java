package server;

import static com.google.common.truth.Truth.assertThat;
import static server.Direction.getNextDirection;
import static server.Direction.getPreviousDirection;
import org.junit.Test;

/** Tests for Direction. */
public class DirectionTest {

  @Test
  public void testGetNextDirection() {
    assertThat(getNextDirection(Direction.TOP, 1))
    .isEqualTo(Direction.TOP_RIGHT);
    assertThat(getNextDirection(Direction.TOP_RIGHT, 2))
    .isEqualTo(Direction.BOTTOM_RIGHT);
    assertThat(getNextDirection(Direction.RIGHT, 4))
    .isEqualTo(Direction.LEFT);
    assertThat(getNextDirection(Direction.BOTTOM_RIGHT, 8))
    .isEqualTo(Direction.BOTTOM_RIGHT);
    assertThat(getNextDirection(Direction.BOTTOM, -1))
    .isEqualTo(Direction.BOTTOM_RIGHT);
    assertThat(getNextDirection(Direction.BOTTOM_LEFT, -2))
    .isEqualTo(Direction.BOTTOM_RIGHT);
    assertThat(getNextDirection(Direction.LEFT, -3))
    .isEqualTo(Direction.BOTTOM_RIGHT);
    assertThat(getNextDirection(Direction.TOP_LEFT, -7))
    .isEqualTo(Direction.TOP);
  }
  
  @Test
  public void testPreviousNextDirection() {
    assertThat(getPreviousDirection(Direction.TOP, 1))
    .isEqualTo(Direction.TOP_LEFT);
    assertThat(getPreviousDirection(Direction.TOP_RIGHT, 2))
    .isEqualTo(Direction.TOP_LEFT);
    assertThat(getPreviousDirection(Direction.RIGHT, 4))
    .isEqualTo(Direction.LEFT);
    assertThat(getPreviousDirection(Direction.BOTTOM_RIGHT, 8))
    .isEqualTo(Direction.BOTTOM_RIGHT);
    assertThat(getPreviousDirection(Direction.BOTTOM, -1))
    .isEqualTo(Direction.BOTTOM_LEFT);
    assertThat(getPreviousDirection(Direction.BOTTOM_LEFT, -2))
    .isEqualTo(Direction.TOP_LEFT);
    assertThat(getPreviousDirection(Direction.LEFT, -3))
    .isEqualTo(Direction.TOP_RIGHT);
    assertThat(getPreviousDirection(Direction.TOP_LEFT, -7))
    .isEqualTo(Direction.LEFT);
  }

}
