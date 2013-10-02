package geometry;

public class Mathematic {
	public static double getDistance(int x1, int y1, int x2, int y2) {
		return Math.hypot(x1 - x2, y1 - y2);
	}

	public static boolean isInBetween(float a, float b, float c) {
		float min = a, max = c;
		if (min > max) {
			min = c;
			max = a;
		}
		return (min < b && b < max);
	}
}
