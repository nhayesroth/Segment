package server;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import server.Canvas.Mode;

/** Tests for Canvas. */
public class CanvasTest {

  private static final int SESSION_ID = 1;

  @Test
  public void testConstructor() {
    Canvas canvas = new Canvas(SESSION_ID);
    assertThat(canvas.canvas.length).isEqualTo(Canvas.CANVAS_SIZE);
    assertThat(canvas.canvas[0].length).isEqualTo(Canvas.CANVAS_SIZE);
    assertThat(canvas.direction).isEqualTo(Direction.TOP);
    assertThat(canvas.mode).isEqualTo(Mode.DRAW);
    assertThat(canvas.coordinates)
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD, Canvas.INITIAL_Y_COORD));
    // Verify that the canvas is empty.
    for (int x = 0; x < Canvas.CANVAS_SIZE; x++) {
      for (int y = 0; y < Canvas.CANVAS_SIZE; y++) {
        assertThat(canvas.get(new Coordinates(x, y))).isEqualTo(' ');
      }
    }
  }

  @Test
  public void testGetCurrentCoordinates() {
    Canvas canvas = new Canvas(SESSION_ID);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD, Canvas.INITIAL_Y_COORD));

    // Move up 5 and test.
    canvas.step(5);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD, Canvas.INITIAL_Y_COORD - 5));

    // Move down-left 2 and test.
    changeDirection(canvas, Direction.BOTTOM_LEFT);
    canvas.step(2);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD - 2, Canvas.INITIAL_Y_COORD - 3));
  }

  @Test
  public void testGetListOfStringsForRender() {
    Canvas canvas = new Canvas(SESSION_ID);
    ImmutableList<String> list1 = canvas.getListOfStringsForRender();
    for (int i = 0; i < list1.size(); i++) {
      System.out.println(String.format("%d-%d: [%s]", i, list1.get(i).length(), list1.get(i)));
    }
    // Canvas size + top border + bottom border
    assertThat(list1).hasSize(Canvas.CANVAS_SIZE + 2);
    assertThat(list1.get(Canvas.CANVAS_SIZE + 1)).isEqualTo(Canvas.BOTTOM_BORDER);
    assertThat(list1).containsExactly(
        Canvas.TOP_BORDER,
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        Canvas.BOTTOM_BORDER);

    // Draw a line in the top row and test.
    canvas.updateMode(Mode.HOVER);
    changeDirection(canvas, Direction.TOP_LEFT);
    canvas.step(Canvas.INITIAL_X_COORD);
    changeDirection(canvas, Direction.RIGHT);
    canvas.updateMode(Mode.DRAW);
    canvas.step(Canvas.CANVAS_SIZE);
    canvas.step(30);

    ImmutableList<String> list2 = canvas.getListOfStringsForRender();
    assertThat(list2).containsExactly(
        Canvas.TOP_BORDER,
        "║******************************║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        "║                              ║",
        Canvas.BOTTOM_BORDER);
  }

  @Test
  public void testStep() {
    Canvas canvas = new Canvas(SESSION_ID);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD, Canvas.INITIAL_Y_COORD));
    assertThat(canvas.mode).isEqualTo(Mode.DRAW);

    // Try (DRAW) stepping right, beyond the right border.
    changeDirection(canvas, Direction.RIGHT);
    canvas.step(100);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.CANVAS_SIZE - 1, Canvas.INITIAL_Y_COORD));

    // Try (DRAW) stepping top-left, beyond the top border.
    changeDirection(canvas, Direction.TOP_LEFT);
    canvas.step(100);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD - 1, 0));

    // Try (DRAW) stepping down-left, beyond the left border.
    changeDirection(canvas, Direction.BOTTOM_LEFT);
    canvas.step(100);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(0, Canvas.INITIAL_Y_COORD - 1));

    // Try (DRAW) stepping down-right, beyond the bottom border.
    changeDirection(canvas, Direction.BOTTOM_RIGHT);
    canvas.step(100);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD, Canvas.CANVAS_SIZE - 1));

    // Check that the canvas looks like we expect it to.
    assertThat(canvas.getListOfStringsForRender()).containsExactly(
        Canvas.TOP_BORDER,
        "║              *               ║",
        "║             * *              ║",
        "║            *   *             ║",
        "║           *     *            ║",
        "║          *       *           ║",
        "║         *         *          ║",
        "║        *           *         ║",
        "║       *             *        ║",
        "║      *               *       ║",
        "║     *                 *      ║",
        "║    *                   *     ║",
        "║   *                     *    ║",
        "║  *                       *   ║",
        "║ *                         *  ║",
        "║*                           * ║",
        "║ *             ***************║",
        "║  *                           ║",
        "║   *                          ║",
        "║    *                         ║",
        "║     *                        ║",
        "║      *                       ║",
        "║       *                      ║",
        "║        *                     ║",
        "║         *                    ║",
        "║          *                   ║",
        "║           *                  ║",
        "║            *                 ║",
        "║             *                ║",
        "║              *               ║",
        "║               *              ║",
        Canvas.BOTTOM_BORDER);

    // Try (HOVER) stepping up-left, beyond the left border.
    canvas.updateMode(Mode.HOVER);
    changeDirection(canvas, Direction.TOP_LEFT);
    canvas.step(100);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(0, Canvas.INITIAL_Y_COORD - 1));

    // Try (ERASER) stepping up-right, beyond the top border.
    canvas.updateMode(Mode.ERASER);
    changeDirection(canvas, Direction.TOP_RIGHT);
    canvas.step(100);
    assertThat(canvas.getCurrentCoordinates())
        .isEqualTo(new Coordinates(Canvas.INITIAL_X_COORD - 1, 0));

    // Check that the canvas looks like we expect it to.
    assertThat(canvas.getListOfStringsForRender()).containsExactly(Canvas.TOP_BORDER,
        "║                              ║",
        "║               *              ║",
        "║                *             ║",
        "║                 *            ║",
        "║                  *           ║",
        "║                   *          ║",
        "║                    *         ║",
        "║                     *        ║",
        "║                      *       ║",
        "║                       *      ║",
        "║                        *     ║",
        "║                         *    ║",
        "║                          *   ║",
        "║                           *  ║",
        "║                            * ║",
        "║ *             ***************║",
        "║  *                           ║",
        "║   *                          ║",
        "║    *                         ║",
        "║     *                        ║",
        "║      *                       ║",
        "║       *                      ║",
        "║        *                     ║",
        "║         *                    ║",
        "║          *                   ║",
        "║           *                  ║",
        "║            *                 ║",
        "║             *                ║",
        "║              *               ║",
        "║               *              ║",
        Canvas.BOTTOM_BORDER);
  }

  @Test
  public void testUpdateDirection() {
    // Starts in TOP.
    Canvas canvas = new Canvas(SESSION_ID);
    assertThat(canvas.direction).isEqualTo(Direction.TOP);

    // Changing by 0 steps in either direction does nothing.
    canvas.updateDirection(Request.newBuilder().setType(Request.Type.RIGHT).setSteps(0).build());
    assertThat(canvas.direction).isEqualTo(Direction.TOP);
    canvas.updateDirection(Request.newBuilder().setType(Request.Type.LEFT).setSteps(0).build());
    assertThat(canvas.direction).isEqualTo(Direction.TOP);

    // Otherwise, direction is updated as expected.
    canvas.updateDirection(Request.newBuilder().setType(Request.Type.RIGHT).setSteps(1).build());
    assertThat(canvas.direction).isEqualTo(Direction.TOP_RIGHT);
    canvas.updateDirection(Request.newBuilder().setType(Request.Type.LEFT).setSteps(3).build());
    assertThat(canvas.direction).isEqualTo(Direction.LEFT);
  }

  @Test
  public void testUpdateMode() {
    // Starts in draw.
    Canvas canvas = new Canvas(SESSION_ID);
    assertThat(canvas.mode).isEqualTo(Mode.DRAW);

    // Setting the mode to the current mode is allowed.
    canvas.updateMode(Mode.DRAW);
    assertThat(canvas.mode).isEqualTo(Mode.DRAW);

    // Changing the mode works as expected.
    canvas.updateMode(Mode.HOVER);
    assertThat(canvas.mode).isEqualTo(Mode.HOVER);

    canvas.updateMode(Mode.ERASER);
    assertThat(canvas.mode).isEqualTo(Mode.ERASER);
  }

  /**
   * Utility method that changes a canvas direction to the one that is specified.
   */
  private static void changeDirection(Canvas canvas, Direction direction) {
    while (canvas.direction != direction) {
      canvas.updateDirection(Request.newBuilder().setType(Request.Type.RIGHT).setSteps(1).build());
    }
  }
}
