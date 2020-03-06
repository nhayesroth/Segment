package server;

/** Represents (X, Y) coordinates. */
public class Coordinates {

  public int x;
  public int y;
  public Coordinates(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  @Override
  public String toString() {
    return String.format("(%d,%d)", x, y);
  }
  
  /** Returns a new Coordinates object that results from moving the current coordinates in the specified direction. */
  public Coordinates move(Direction d) {
    return new Coordinates(x + d.x, y + d.y);
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Coordinates
        && x == ((Coordinates) obj).x
        && y == ((Coordinates) obj).y;
  }
}
