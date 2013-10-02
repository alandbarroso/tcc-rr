package geometry;

import java.io.Serializable;

import rescuecore2.standard.entities.Building;

public class Point implements Comparable<Point>, Serializable {
	private static final long serialVersionUID = -2853088059238776812L;

	private int x = 0;
	private int y = 0;
	public Building tmp=null;

	public int compareTo(Point other) {
		if (y < other.y)
			return -1;
		if (y > other.y)
			return 1;
		if (x < other.x)
			return -1;
		if (x > other.x)
			return 1;
		return 0;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Point) {
			Point point = (Point) obj;
			if (x == point.x && y == point.y)
				return true;
			return false;
		}

		return false;
	}


	public Point() {
	}

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Point(float direction, float magnitude, boolean isPolar) {
		x = (int) (magnitude * Math.cos(direction));
		y = (int) (magnitude * Math.sin(direction));
	}

	public float getDistance(Point other) {
		return (float) Math.hypot(x - other.x, y - other.y);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Point plus(Point point) {
		return new Point(x + point.x, y + point.y);
	}

	public Point minus(Point point) {
		return new Point(x - point.x, y - point.y);
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
