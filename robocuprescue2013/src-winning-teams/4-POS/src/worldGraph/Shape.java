package worldGraph;

import geometry.Degree;
import geometry.Line;
import geometry.Point;

import java.util.ArrayList;

public class Shape {
	public ArrayList<Point> points = new ArrayList<Point>();

	private Point centerPoint = null;

	public Point getCenterPoint() {
		return centerPoint;
	}

	public void updateCenterPoint() {
		int x = 0, y = 0;
		for (int i = 1; i < points.size(); i++) {
			x += points.get(i).getX();
			y += points.get(i).getY();
		}

		centerPoint = new Point(x / (points.size() - 1), y
				/ (points.size() - 1));
	}

	public Point getPlusPoint(int i, int j, int d) {
		return getMPPPoint(i, j, d, true);
	}

	public Point getMinusPoint(int i, int j, int d) {
		return getMPPPoint(i, j, d, false);
	}

	public Point getMPPPoint(int i, int j, int d, boolean plus) {
		Line line = new Line(points.get(i), points.get(j));
		Point point = points.get(i);
		Point diff = null;

		if (line.hasItGradient()) {
			float m = line.getGradient();
			if (Math.abs(m) < 0.001)
				diff = new Point(0, d);
			else {
				m = -1 / m;
				float theta = (float) Math.atan(m);
				float alpha = d * (float) Math.cos(theta);
				float beta = d * (float) Math.sin(theta);
				diff = new Point((int) alpha, (int) beta);
			}
		} else
			diff = new Point(d, 0);

		if (plus)
			return point.plus(diff);
		else
			return point.minus(diff);
	}

	public static final int MINDIST = 20; // It was 5
	public Point getIOPoint(int i, int j, int d, boolean inner) {
		Line line = new Line(points.get(i), points.get(j));
		Point point = line.getMiddlePoint();
		Point diff = null;

		if (line.hasItGradient()) {
			float m = line.getGradient();
			if (Math.abs(m) < 0.001)
				diff = new Point(0, MINDIST);
			else {
				m = -1 / m;
				float theta = (float) Math.atan(m);
				float alpha = MINDIST * (float) Math.cos(theta);
				float beta = MINDIST * (float) Math.sin(theta);
				diff = new Point((int) alpha, (int) beta);
			}
		} else
			diff = new Point(MINDIST, 0);

		boolean isInShape = isInShape(point.minus(diff));
		if ((inner && isInShape) || (!inner && !isInShape))
			return getMinusPoint(i, j, d);
		else
			return getPlusPoint(i, j, d);
	}

	public Point getInnerPoint(int i, int j, int d) {
		return getIOPoint(i, j, d, true);
	}

	public Point getOuterPoint(int i, int j, int d) {
		return getIOPoint(i, j, d, false);
	}

	public Point getFarerEdgePoint(int i, int r) {
		Point prevPoint = null;
		if (i == 0)
			prevPoint = points.get(points.size() - 2);
		else
			prevPoint = points.get(i - 1);
		Point point = points.get(i);
		Point nextPoint = points.get(i + 1);
		float firstDirection = new Line(point, prevPoint).getTheta();
		float secondDirection = new Line(point, nextPoint).getTheta();
		float direction = Degree
				.normalizeAngle((firstDirection + secondDirection) / 2);

		float d = Math.abs(r
				/ (float) Math.sin(Degree.normalizeAngle(direction
						- firstDirection)));
		Point diff = new Point((int) (d * Math.cos(direction)),
				(int) (d * Math.sin(direction)));
		Point minus = point.minus(diff);
		Point plus = point.plus(diff);
		if (minus.getDistance(centerPoint) > plus.getDistance(centerPoint))
			return minus;
		else
			return plus;
	}

	public Point getFarerEdgePoint(int i, int r, int d) {
		Point prevPoint = null;
		if (i == 0)
			prevPoint = points.get(points.size() - 2);
		else
			prevPoint = points.get(i - 1);
		Point point = points.get(i);
		Point nextPoint = points.get(i + 1);
		float firstDirection = new Line(point, prevPoint).getTheta();
		float secondDirection = new Line(point, nextPoint).getTheta();
		float direction = Degree
				.normalizeAngle((firstDirection + secondDirection) / 2);

		Point diff = new Point((int) (d * Math.cos(direction)),
				(int) (d * Math.sin(direction)));
		Point minus = point.minus(diff);
		Point plus = point.plus(diff);
		if (minus.getDistance(centerPoint) > plus.getDistance(centerPoint))
			return minus;
		else
			return plus;
	}

	public Point getInnerEdgePoint(int i, int r) {
		return getEdgePoint(i, r, true);
	}

	// Added
	public Point getOuterEdgePoint(int i, int r) {
		return getEdgePoint(i, r, false);
	}

	// Added
	public Point getEdgePoint(int i, int r, boolean inner) {
		Point prevPoint = null;
		if (i == 0)
			prevPoint = points.get(points.size() - 2);
		else
			prevPoint = points.get(i - 1);
		Point point = points.get(i);
		Point nextPoint = points.get(i + 1);
		float firstDirection = new Line(point, prevPoint).getTheta();
		float secondDirection = new Line(point, nextPoint).getTheta();
		float direction = Degree
				.normalizeAngle((firstDirection + secondDirection) / 2);

		Point diff = new Point((int) (MINDIST * Math.cos(direction)),
				(int) (MINDIST * Math.sin(direction)));
		Point tmp = point.minus(diff);
		float d = Math.abs(r
				/ (float) Math.sin(Degree.normalizeAngle(direction
						- firstDirection)));
		diff = new Point((int) (d * Math.cos(direction)),
				(int) (d * Math.sin(direction)));
		if ((inner && isInShape(tmp)) || (!inner && !isInShape(tmp)))
			return point.minus(diff);
		else
			return point.plus(diff);
	}

	// Added
	public Point getInnerEdgePoint(int i, int r, int d) {
		return getEdgePoint(i, r, d, true);
	}

	// Added
	public Point getOuterEdgePoint(int i, int r, int d) {
		return getEdgePoint(i, r, d, false);
	}

	// Added
	public Point getEdgePoint(int i, int r, int d, boolean inner) {
		Point prevPoint = null;
		if (i == 0)
			prevPoint = points.get(points.size() - 2);
		else
			prevPoint = points.get(i - 1);
		Point point = points.get(i);
		Point nextPoint = points.get(i + 1);
		float firstDirection = new Line(point, prevPoint).getTheta();
		float secondDirection = new Line(point, nextPoint).getTheta();
		float direction = Degree
				.normalizeAngle((firstDirection + secondDirection) / 2);

		Point diff = new Point((int) (MINDIST * Math.cos(direction)),
				(int) (MINDIST * Math.sin(direction)));
		Point tmp = point.minus(diff);
		diff = new Point((int) (d * Math.cos(direction)),
				(int) (d * Math.sin(direction)));
		if ((inner && isInShape(tmp)) || (!inner && !isInShape(tmp)))
			return point.minus(diff);
		else
			return point.plus(diff);
	}

	// Added
	public Point getCloserEdgePoint(int i, int r) {
		Point prevPoint = null;
		if (i == 0)
			prevPoint = points.get(points.size() - 2);
		else
			prevPoint = points.get(i - 1);
		Point point = points.get(i);
		Point nextPoint = points.get(i + 1);
		float firstDirection = new Line(point, prevPoint).getTheta();
		float secondDirection = new Line(point, nextPoint).getTheta();
		float direction = Degree
				.normalizeAngle((firstDirection + secondDirection) / 2);

		float d = Math.abs(r
				/ (float) Math.sin(Degree.normalizeAngle(direction
						- firstDirection)));
		Point diff = new Point((int) (d * Math.cos(direction)),
				(int) (d * Math.sin(direction)));
		Point minus = point.minus(diff);
		Point plus = point.plus(diff);
		if (minus.getDistance(centerPoint) < plus.getDistance(centerPoint))
			return minus;
		else
			return plus;
	}

	// Added
	public Point getCloserEdgePoint(int i, int r, int d) {
		Point prevPoint = null;
		if (i == 0)
			prevPoint = points.get(points.size() - 2);
		else
			prevPoint = points.get(i - 1);
		Point point = points.get(i);
		Point nextPoint = points.get(i + 1);
		float firstDirection = new Line(point, prevPoint).getTheta();
		float secondDirection = new Line(point, nextPoint).getTheta();
		float direction = Degree
				.normalizeAngle((firstDirection + secondDirection) / 2);

		Point diff = new Point((int) (d * Math.cos(direction)),
				(int) (d * Math.sin(direction)));
		Point minus = point.minus(diff);
		Point plus = point.plus(diff);
		if (minus.getDistance(centerPoint) < plus.getDistance(centerPoint))
			return minus;
		else
			return plus;
	}

	public boolean isInShape(Point point) {
		Point center = new Point(-100, -100);
		int intersects = 0;
		for (int i = 0; i < points.size() - 1; i++)
			if (Line.isIntersectBetweenLines(center, point, points.get(i),
					points.get(i + 1)))
				intersects++;

		return (intersects % 2 == 1);
	}

	public boolean hasIntersectWithShape(Point point, int rad) {
		for (int i = 0; i < points.size() - 1; i++)
			if ((new Line(points.get(i), points.get(i + 1)))
					.hastIntersectWithCircle(point, rad))
				return true;

		return false;
	}

	public boolean hasIntersectWithShape(Line line) {
		for (int i = 0; i < points.size() - 1; i++)
			if (Line.isIntersectBetweenLines(points.get(i), points.get(i + 1),
					line.getFirstPoint(), line.getSecondPoint()))
				return true;

		return false;
	}

	public boolean hasItConflict(Point point, int rad) {
		if (isInShape(point))
			return true;

		return hasIntersectWithShape(point, rad);
	}
}
