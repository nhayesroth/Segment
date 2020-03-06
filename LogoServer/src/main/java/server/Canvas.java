package server;

import java.util.Arrays;
import com.google.common.collect.ImmutableList;

/** Represents a single Canvas object. */
public class Canvas {
  static final int CANVAS_SIZE = 30;
  static final int INITIAL_X_COORD = 15;
  static final int INITIAL_Y_COORD = 15;
  
  static final String TOP_BORDER = "╔══════════════════════════════╗";
  static final String BOTTOM_BORDER = "╚══════════════════════════════╝\r\n";

  /** Mode the cursor is currently in on this canvas. */  
  public enum Mode {
    HOVER, DRAW, ERASER;
  }

  private final Logger logger;
  final char[][] canvas;
  Direction direction;
  Mode mode;
  Coordinates coordinates;
  
  Canvas(int sessionId) {
    this.logger = new Logger(sessionId);
    this.canvas = new char[CANVAS_SIZE][CANVAS_SIZE];
    this.direction = Direction.TOP;
    this.mode = Mode.DRAW;
    this.coordinates = new Coordinates(INITIAL_X_COORD, INITIAL_Y_COORD);
    clear();
  }
  

  /** Writes empty space characters to each index of the canvas. */
  public Canvas clear() {
    for (int row = 0; row < 30; row++) {
      for (int col = 0; col < 30; col++) {
        canvas[row][col] = ' ';
      }
    }
    return this;
  }
  
  /** Returns the current coordinats of the cursor on the canvas. */
  public Coordinates getCurrentCoordinates() {
    return coordinates;
  }
  
  /** Returns a list of Strings representing the canvas. */
  public ImmutableList<String> getListOfStringsForRender() {
    return ImmutableList.<String>builder()
        .add(TOP_BORDER)
        .addAll(convertCanvasToListOfStrings())
        .add(BOTTOM_BORDER)
        .build();
  }
  
  /**
   * Moves the cursor on the canvas, updating fields based on the current
   * mode, direction, and coordinates.
   */
  public Canvas step(int steps) {
    if (steps < 0) {
      step(-steps);
    }
    if (steps == 0) {
      logger.log("Done stepping at %s", coordinates);
      return this;
    }
    // Update the current cursor location.
    switch (mode) {
      case DRAW:
        canvas[coordinates.y][coordinates.x] = '*';
        logger.log("Drew a '*' on %s", coordinates);
        break;
      case ERASER:
        canvas[coordinates.y][coordinates.x] = ' ';
        logger.log("Erased %s", coordinates);
        break;
      case HOVER:
        logger.log("Hovered over %s", coordinates);
        break;
    }
    // Move the cursor (if it is within the canvas boundaries).
    Coordinates newCoordinates = coordinates.move(direction);
    if (newCoordinates.y >= 0
        && newCoordinates.y < canvas.length
        && newCoordinates.x >= 0
        && newCoordinates.x < canvas[0].length) {
      coordinates = newCoordinates;
      return step(steps - 1);
    } else {
      logger.log(
          "Can't move %s any further from %s."
          + " Exiting with %d steps left.",
          direction.type.name(), coordinates, steps);
      return this;
    }
  }
  
  @Override
  public String toString() {
    return String.format(
        "Canvas status: coordinates=%s, mode=%s, direction=%s",
        coordinates, mode.name(), direction.type.name());
  }
  
  /** Updates the cursor's direction. */
  public Canvas updateDirection(Request request) {
    Direction initialDirection = direction;
    switch (request.type) {
      case LEFT:
        direction = Direction.getPreviousDirection(direction, request.steps.get());
        break;
      case RIGHT:
        direction = Direction.getNextDirection(direction, request.steps.get());
        break;
      default:
        throw new IllegalStateException(
            "Unexpected request type for switching directions: " + request);
    }
    logger.log("Updated direction from %s: (%s, %d) -> %s", initialDirection.type.name(),
        request.type.name(), request.steps.get(), direction.type.name());
    return this;
  }

  /** Updates the cursor's mode. */
  public Canvas updateMode(Mode mode) {
    this.mode = mode;
    logger.log("Set mode to: " + mode.name());
    return this;
  }
  
  /** Gets the character value of the canvas at the specified coordinates. */
  char get(Coordinates c) {
    return canvas[c.y][c.x];
  }
  
  private ImmutableList<String> convertCanvasToListOfStrings() {
    return Arrays.stream(canvas)
        .map(Canvas::getStringForRowOfCanvas)
        .collect(ImmutableList.toImmutableList());
  }
  
  private static String getStringForRowOfCanvas(char[] row) {
    return "║" + String.valueOf(row) + "║";
  }
}
