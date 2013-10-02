package agent;

import geometry.Point;

import java.util.ArrayList;
import java.util.Collections;

import rescuecore2.standard.entities.Building;

public class LittleZone {
	public ArrayList<Building> searchBuildings = new ArrayList<Building>();
	public ArrayList<Building> buildings = new ArrayList<Building>();
	public int id = -1;
	public int minLTV = -1;
	public int owner = -1;
	public int ownerCycle = -1;
	public int ltv = -1;
	public int minLTS = -1;
	public int centreX = -1;
	public int centreY = -1;
	public boolean ischoose = false;
	public boolean isEnd = false;
	public boolean isChooseByFire = false;
	public double percent = 0;
	public int priority = 0;
	public ArrayList<Integer> owners = new ArrayList<Integer>();
	public ArrayList<Integer> ownersOfSearchZone = new ArrayList<Integer>();
	public boolean isRealLittleZone = true;
 void fixOwner() {
		if (ownersOfSearchZone.size() != 0) {
			Collections.sort(ownersOfSearchZone);
			owner = ownersOfSearchZone.get(0);
		}
	}
	public void centrePoint() {
		int allX = 0, allY = 0;
		for (Building building : buildings) {
			allX += building.getX();
			allY += building.getY();
		}
		centreX = allX / buildings.size();
		centreY = allY / buildings.size();
	}

	public void setminLTV() {
		for (Building build : buildings)
			if (build.lastTimeVisit < minLTV)
				minLTV = build.lastTimeVisit;
	}

	public Point setcenter() {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (Building build : buildings) {
			if (build.getX() > maxX)
				maxX = build.getX();
			if (build.getX() < minX)
				minX = build.getX();
			if (build.getY() > maxY)
				maxY = build.getY();
			if (build.getY() < minY)
				minY = build.getY();
		}
		return new Point((minX + maxX) / 2, (minY + maxY) / 2);
	}
}
