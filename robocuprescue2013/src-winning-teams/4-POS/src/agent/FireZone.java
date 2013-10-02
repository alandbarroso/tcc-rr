package agent;

import geometry.Degree;
import geometry.Line;
import geometry.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeSet;
import utilities.Logger;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;

public class FireZone {
	public Logger logger = null;
	public int time = -1;
	public int agentID = -1;
	public HashSet<GroupForFire> fireGroups = new HashSet<GroupForFire>();
	public boolean emergency = false;
	public int zoneNum = -1;
	public Point centerPoint = null;
	public int workersNum;
	public double distToFire = 0.00;
	public double value = 0;
	public int centerX = 0;
	public int centerY = 0;
	public int volume = 0;
	public int assignedWorkerNum = 0;
	public double valueForWorkwerNum = 0;
	public boolean isInhibitor = false;
	public boolean ExtinguishedByMe = false;
	public ArrayList<Building> ignitedBuilds = new ArrayList<Building>();
	public ArrayList<Building> buildings = new ArrayList<Building>();
	public HashSet<Building> aroundBuildings = new HashSet<Building>();
	public HashSet<Building> aroundBuildingsWithoutSim = new HashSet<Building>();
	public HashSet<FireBrigade> fireAgents = new HashSet<FireBrigade>();

	public FireZone(int num, int agentID) {
		zoneNum = num;
		this.agentID = agentID;
	}

	public void setAroundBuildings(boolean bigMap) {
		setAroundBuilding();
//		if (!bigMap) {
//			ArrayList<Building> save = new ArrayList<Building>();
//			for (Building building : aroundBuildings)
//				save.add(building);
//			ignitedBuilds.removeAll(aroundBuildings);
//			setAroundBuilding();
//			aroundBuildings.addAll(save);
//			ignitedBuilds.addAll(save);
//		}
	}

	public void setAroundBuilding() {
		setAroundBuildings(1, ignitedBuilds);
//		ArrayList<Building> aroundBuilding = new ArrayList<Building>();
//		aroundBuilding.addAll(aroundBuildings);
//		for (int i = 0; i < aroundBuilding.size(); i++) {
//			if (aroundBuilding.get(i).owners.size() == aroundBuilding.get(i).hamraheAval
//					&& !aroundBuilding.get(i).owners.contains(agentID)) {
//				aroundBuilding.remove(aroundBuilding.get(i));
//				i--;
//			}
//		}
//		aroundBuildings = new HashSet<Building>();
//		aroundBuildings.addAll(aroundBuilding);
//		if (aroundBuildings.size() == 0)
//			if (!setAroundBuildings(0, ignitedBuilds))
//				setAroundBuildings(1, ignitedBuilds);
	}

	public void setAroundBuildingWithoutSim() {
		setAroundBuildingsWithoutSim(1, ignitedBuilds);
	}

	public boolean setAroundBuildings(int hamrah,
			ArrayList<Building> ignitedbuildings) {
		float minTheta = Integer.MAX_VALUE, b = 0, a;
		Point minPoint = new Point();
		Point pointMoredeNazar = new Point();
		TreeSet<Point> pointsM = new TreeSet<Point>();

		for (Building building : ignitedbuildings) {
			if (!building.isNotIgnitable()
					&& (building.owners.size() < building.hamraheAval + hamrah || building.owners
							.contains(agentID)))
				for (int i = 0; i < building.worldGraphArea.points.size() - 1; i++) {
					building.worldGraphArea.points.get(i).tmp = building;
					pointsM.add(building.worldGraphArea.points.get(i));
				}
		}

		if (pointsM.size() == 0)
			return false;

		minPoint = Collections.min(pointsM);

		aroundBuildings = new HashSet<Building>();
		Line line = new Line(minPoint, null);
		while (true) {
			minTheta = Integer.MAX_VALUE;
			float minB = -1;
			for (Point secondPoint : pointsM)
				if (secondPoint != line.getFirstPoint()) {
					line.setSecondPoint(secondPoint);
					a = line.getTheta();
					if (Degree.absoluteAngle(a - b) < minTheta) {
						pointMoredeNazar = secondPoint;
						minTheta = Degree.absoluteAngle(a - b);
						minB = a;
					}
				}
			b = minB;
			if (pointMoredeNazar == minPoint) {
				aroundBuildings.add(minPoint.tmp);
				break;
			}

			line.setFirstPoint(pointMoredeNazar);
			aroundBuildings.add(pointMoredeNazar.tmp);
		}

		return true;
	}

	public boolean setAroundBuildingsWithoutSim(int hamrah,
			ArrayList<Building> ignitedbuildings) {
		float minTheta = Integer.MAX_VALUE, b = 0, a;
		Point minPoint = new Point();
		Point pointMoredeNazar = new Point();
		TreeSet<Point> pointsM = new TreeSet<Point>();
		for (Building building : ignitedbuildings) {
			if (!building.isNotIgnitable()
					&& (building.owners.size() < building.hamraheAval + hamrah || building.owners
							.contains(agentID)))
				for (int i = 0; i < building.worldGraphArea.points.size() - 1; i++) {
					building.worldGraphArea.points.get(i).tmp = building;
					pointsM.add(building.worldGraphArea.points.get(i));
				}
		}

		if (pointsM.size() == 0)
			return false;

		minPoint = Collections.min(pointsM);

		aroundBuildingsWithoutSim = new HashSet<Building>();
		Line line = new Line(minPoint, null);
		while (true) {
			minTheta = Integer.MAX_VALUE;
			float minB = -1;
			for (Point secondPoint : pointsM)
				if (secondPoint != line.getFirstPoint()) {
					line.setSecondPoint(secondPoint);
					a = line.getTheta();
					if (Degree.absoluteAngle(a - b) < minTheta) {
						pointMoredeNazar = secondPoint;
						minTheta = Degree.absoluteAngle(a - b);
						minB = a;
					}
				}
			b = minB;
			if (pointMoredeNazar == minPoint) {
				aroundBuildingsWithoutSim.add(minPoint.tmp);
				break;
			}

			line.setFirstPoint(pointMoredeNazar);
			aroundBuildingsWithoutSim.add(pointMoredeNazar.tmp);
		}
		return true;
	}
}
