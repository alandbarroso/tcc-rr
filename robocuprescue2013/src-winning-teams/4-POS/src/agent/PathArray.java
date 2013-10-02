package agent;

import java.util.ArrayList;

import rescuecore2.standard.entities.Area;

public class PathArray {
	public ArrayList<Path> pathes = new ArrayList<Path>();
	// public Area centeralArea = null;
	public ArrayList<Integer> pathNums = new ArrayList<Integer>();
	public int centerX = -1, centerY = -1;

	public void setCenter() {
		int allX = 0, allY = 0;
		int tedad = 0;
		for (Path p : pathes) {
			for (Area a : p.way) {
				allX += a.getX();
				allY += a.getY();
				tedad++;
			}
		}
		centerX = allX / tedad;
		centerY = allY / tedad;
		// double minDist = Integer.MAX_VALUE;
		// for (Path p : pathes)
		// for (Area a : p.way) {
		// double x = Math.hypot(a.getX() - centreX, a.getY() - centreY);
		// if (x < minDist) {
		// centeralArea = a;
		// minDist = x;
		// }
		// }
	}

	public String toString() {
		String s = new String();
		for (Path p : pathes)
			s += p + "; ";
		return s;
	}
}
