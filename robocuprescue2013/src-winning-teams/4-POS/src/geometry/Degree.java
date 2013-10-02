package geometry;

public class Degree {
	public static float RAD2DEG = (float) (180 / Math.PI);
	public static float DEG2RAD = (float) (Math.PI / 180);

	public static float normalizeAngle(float theta) {
		while (theta > Math.PI || theta < -Math.PI) {
			if (theta > Math.PI)
				theta -= 2 * Math.PI;
			else
				theta += 2 * Math.PI;
		}

		return theta;
	}

	public static float absoluteAngle(float theta) {
		while (theta > 2 * Math.PI || theta < 0) {
			if (theta > Math.PI)
				theta -= 2 * Math.PI;
			else
				theta += 2 * Math.PI;
		}

		return theta;
	}

	public static float getDeltaAngle(float angle1, float angle2) {
		angle1 = absoluteAngle(angle1);
		angle2 = absoluteAngle(angle2);
		return normalizeAngle(angle1 - angle2);
	}

	public static float getDegree(Point pos1, Point pos2, Point pos3) {
		Line v1 = new Line(new Point(0, 0), pos1.minus(pos2)), v2 = new Line(
				new Point(0, 0), pos3.minus(pos2));
		return normalizeAngle(v1.getTheta() - v2.getTheta());
	}

	public static boolean isClockWise(Point p1, Point p2, Point p3) {
		return getDegree(p1, p2, p3) > 0;
	}

	public static boolean isBetween(float angle1, float angle2, float checkAngle) {
		angle1 = absoluteAngle(angle1);
		angle2 = absoluteAngle(angle2);
		checkAngle = absoluteAngle(checkAngle);

		if (angle1 == angle2) {
			if (checkAngle == angle1)
				return true;
			else
				return false;
		} else if (angle1 < angle2) {
			if (checkAngle >= angle1 && checkAngle <= angle2)
				return true;
			else
				return false;
		} else {
			if (checkAngle <= angle2 || checkAngle >= angle1)
				return true;
			else
				return false;
		}
	}
}
