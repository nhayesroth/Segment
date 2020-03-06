package server;

import com.google.common.collect.ImmutableMap;

/** Represents a direction on the canvas. */
public final class Direction {
  enum Type {
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
    TOP_LEFT;
  }
  
  int x;
  int y;
  Type type;
  
  public Direction(Type type, int x, int y) {
    this.type = type;
    this.x = x;
    this.y = y;
  }
  
  @Override
  public String toString() {
    return String.format("%s (%d,%d)", type.name(), x, y);
  }
  

  public static final Direction TOP =
      new Direction(Type.TOP, 0, -1);
  public static final Direction TOP_RIGHT =
      new Direction(Type.TOP_RIGHT, 1, -1);
  public static final Direction RIGHT =
      new Direction(Type.RIGHT, 1, 0);
  public static final Direction BOTTOM_RIGHT =
      new Direction(Type.BOTTOM_RIGHT, 1, 1);
  public static final Direction BOTTOM =
      new Direction(Type.BOTTOM, 0, 1);
  public static final Direction BOTTOM_LEFT =
      new Direction(Type.BOTTOM_LEFT, -1, 1);
  public static final Direction LEFT =
      new Direction(Type.LEFT, -1, 0);
  public static final Direction TOP_LEFT =
      new Direction(Type.TOP_LEFT, -1, -1);
  
  private static final ImmutableMap<Direction, Direction> NEXT_DIRECTIONS =
      ImmutableMap.<Direction, Direction>builder()
      .put(TOP, TOP_RIGHT)
      .put(TOP_RIGHT, RIGHT)
      .put(RIGHT, BOTTOM_RIGHT)
      .put(BOTTOM_RIGHT, BOTTOM)
      .put(BOTTOM, BOTTOM_LEFT)
      .put(BOTTOM_LEFT, LEFT)
      .put(LEFT, TOP_LEFT)
      .put(TOP_LEFT,  TOP)
      .build();
  
  private static final ImmutableMap<Direction, Direction> PREVIOUS_DIRECTIONS =
      ImmutableMap.<Direction, Direction>builder()
      .put(TOP, TOP_LEFT)
      .put(TOP_RIGHT, TOP)
      .put(RIGHT, TOP_RIGHT)
      .put(BOTTOM_RIGHT, RIGHT)
      .put(BOTTOM, BOTTOM_RIGHT)
      .put(BOTTOM_LEFT, BOTTOM)
      .put(LEFT, BOTTOM_LEFT)
      .put(TOP_LEFT, LEFT)
      .build();
  
  public static Direction getNextDirection(Direction direction, int n) {
    if (n < 0) {
      return getPreviousDirection(direction, -n);
    }
    while (n > 0) {
      direction = NEXT_DIRECTIONS.get(direction);
      n--;
    }
    return direction;
  }
  
  public static Direction getPreviousDirection(Direction direction, int n) {
    if (n < 0) {
      return getNextDirection(direction, -n);
    }
    while (n > 0) {
      direction = PREVIOUS_DIRECTIONS.get(direction);
      n--;
    }
    return direction;
  }
}
