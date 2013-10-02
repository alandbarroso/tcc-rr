package geometry;

import java.util.ArrayList;

public class Line {
	private Point firstPoint = null, secondPoint = null;

	public Line(Point firstPoint, Point secondPoint) {
		this.firstPoint = firstPoint;
		this.secondPoint = secondPoint;
	}

	public String draw() {
		return "line(" + getFirstPoint().getX() + ", " + getFirstPoint().getY()
				+ ")(" + getSecondPoint().getX() + ", "
				+ getSecondPoint().getY() + ") 4";
	}

	public static boolean isIntersectBetweenLines(Line l1, Line l2) {
		return isIntersectBetweenLines(l1.getFirstPoint(), l1.getSecondPoint(),
				l2.getFirstPoint(), l2.getSecondPoint());
	}

	public Point getIntersectPoint(Line other) {
		if (!isIntersectBetweenLines(this, other))
			return null;

		if (!hasItGradient() || !other.hasItGradient()) {
			int x = 0;
			float m = 0;
			int x0 = 0;
			int y0 = 0;
			if (!hasItGradient()) {
				x = firstPoint.getX();
				m = other.getGradient();
				x0 = other.firstPoint.getX();
				y0 = other.firstPoint.getY();
			} else {
				x = other.firstPoint.getX();
				m = getGradient();
				x0 = firstPoint.getX();
				y0 = firstPoint.getY();
			}

			int y = (int) (m * (x - x0) + y0);
			return new Point(x, y);
		}

		float m = getGradient(), mp = other.getGradient();
		int x0 = firstPoint.getX(), y0 = firstPoint.getY();
		int x1 = other.firstPoint.getX(), y1 = other.firstPoint.getY();
		int x = (int) ((m * x0 - mp * x1 + y1 - y0) / (m - mp));
		int y = (int) (m * (x - firstPoint.getX()) + firstPoint.getY());
		return new Point(x, y);
	}

	public Point getFirstPoint() {
		return firstPoint;
	}

	public void setFirstPoint(Point firstPoint) {
		this.firstPoint = firstPoint;
	}

	public Point getSecondPoint() {
		return secondPoint;
	}

	public void setSecondPoint(Point secondPoint) {
		this.secondPoint = secondPoint;
	}

	public Point getMiddlePoint() {
		return new Point((firstPoint.getX() + secondPoint.getX()) / 2,
				(firstPoint.getY() + secondPoint.getY()) / 2);
	}

	public float getDistance(Point point) {
		return getDistance(point, this);
	}

	public float getTheta() {
		return (float) Math.atan2(secondPoint.getY() - firstPoint.getY(),
				secondPoint.getX() - firstPoint.getX());
	}

	public static float getDistance(Point point, Line line) {
		if (line.hasItGradient()) {
			float m = line.getGradient();
			m = -1 / m;
			float y1 = m * (point.getX() - line.firstPoint.getX())
					+ line.firstPoint.getY();
			float y2 = m * (point.getX() - line.secondPoint.getX())
					+ line.secondPoint.getY();
			if (Mathematic.isInBetween(y1, point.getY(), y2)) {
				float a = line.firstPoint.getDistance(line.secondPoint);
				float b = point.getDistance(line.firstPoint);
				float c = point.getDistance(line.secondPoint);
				float p = (a + b + c) / 2;
				float s = (float) Math.sqrt(p * (p - a) * (p - b) * (p - c));
				return 2 * s / a;
			}
		} else if (Mathematic.isInBetween(line.firstPoint.getY(), point.getY(),
				line.secondPoint.getY()))
			return Math.abs(point.getX() - line.firstPoint.getX());

		return Math.min(point.getDistance(line.firstPoint),
				point.getDistance(line.secondPoint));
	}

	public boolean hasItGradient() {
		return (Math.abs(firstPoint.getX() - secondPoint.getX()) > 0.001);
	}

	public float getGradient() {
		return (float) (secondPoint.getY() - firstPoint.getY())
				/ (float) (secondPoint.getX() - firstPoint.getX());
	}

	public boolean hastIntersectWithCircle(Point point, float r) {
		return (getDistance(point) <= r);
	}

	public static boolean isIntersectBetweenLines(Point p1, Point q1, Point p2,
			Point q2) {
		int p1q1p2 = getDirection(p1, q1, p2);
		int p1q1q2 = getDirection(p1, q1, q2);
		int p2q2p1 = getDirection(p2, q2, p1);
		int p2q2q1 = getDirection(p2, q2, q1);
		if (p1q1p2 * p1q1q2 != 1 && p2q2p1 * p2q2q1 != 1)
			return true;

		if ((p1q1p2 == 0 && p1q1q2 == 0 && p2q2p1 == 0 && p2q2q1 == 0)
				&& (Mathematic.isInBetween(p1.getX(), p2.getX(), q1.getX())
						|| Mathematic.isInBetween(p1.getX(), q2.getX(),
								q1.getX()) || Mathematic.isInBetween(p2.getX(),
						p1.getX(), q2.getX()))
				&& (Mathematic.isInBetween(p1.getY(), p2.getY(), q1.getY())
						|| Mathematic.isInBetween(p1.getY(), q2.getY(),
								q1.getY()) || Mathematic.isInBetween(p2.getY(),
						p1.getY(), q2.getY())))
			return true;

		return false;
	}

	public static int getDirection(Point p1, Point p2, Point p3) {
		// int v1 = (p2.getY() - p1.getY()) * (p3.getX() - p2.getX());
		// int v2 = (p3.getY() - p2.getY()) * (p2.getX() - p1.getX());
		int p1X = p1.getX();
		int p1Y = p1.getY();
		int p2X = p2.getX();
		int p2Y = p2.getY();
		int p3X = p3.getX();
		int p3Y = p3.getY();

		int minX = Math.min(p1X, Math.min(p2X, p3X));
		int minY = Math.min(p1Y, Math.min(p2Y, p3Y));
		p1X -= minX;
		p2X -= minX;
		p3X -= minX;
		p1Y -= minY;
		p2Y -= minY;
		p3Y -= minY;

		long v1 = ((long) p2Y - (long) p1Y) * ((long) p3X - (long) p2X);
		long v2 = ((long) p3Y - (long) p2Y) * ((long) p2X - (long) p1X);
		if (v1 > v2)
			return 1;
		if (v1 < v2)
			return -1;
		return 0;
	}

	public ArrayList<Point> getPointsInLine(int pointsCount) {
		ArrayList<Point> points = new ArrayList<Point>();
		if (hasItGradient()) {
			float xDiff = Math.abs(firstPoint.getX() - secondPoint.getX())
					/ (float) (pointsCount + 1);
			float minX = Math.min(firstPoint.getX(), secondPoint.getX());
			float x = minX;
			float m = getGradient();
			float b = -m * firstPoint.getX() + firstPoint.getY();
			for (int i = 0; i < pointsCount; i++) {
				x += xDiff;
				points.add(new Point((int) x, (int) (m * x + b)));
			}
		} else {
			float yDiff = Math.abs(firstPoint.getY() - secondPoint.getY())
					/ (float) (pointsCount + 1);
			float minY = Math.min(firstPoint.getY(), secondPoint.getY());
			float y = minY;
			for (int i = 0; i < pointsCount; i++) {
				y += yDiff;
				points.add(new Point(firstPoint.getX(), (int) y));
			}
		}

		return points;
	}

	public String toString() {
		return "line" + firstPoint + secondPoint + " 4";
	}
}
