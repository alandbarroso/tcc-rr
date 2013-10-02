package geometry;

import java.util.ArrayList;

public class Circle {
	public static boolean hasIntersect(Point centerPoint, int rad,
			ArrayList<Point> points) {
		for (int i = 0; i < points.size() - 1; i++)
			if (new Line(points.get(i), points.get(i + 1))
					.getDistance(centerPoint) < rad)
				return true;
		if (new Line(points.get(0), points.get(points.size() - 1))
				.getDistance(centerPoint) < rad)
			return true;
		return false;
	}
}
