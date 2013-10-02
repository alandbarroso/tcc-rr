package agent;

import geometry.Point;

import java.util.ArrayList;
import rescuecore2.standard.entities.Civilian;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Area;

public class Zone implements Cloneable {
	public int zoneNumber;
	public ArrayList<Area> areas = new ArrayList<Area>();
	public ArrayList<Building> buildings = new ArrayList<Building>();
	public ArrayList<Road> roads = new ArrayList<Road>();
	public ArrayList<Refuge> refuges = new ArrayList<Refuge>();
	public SubZone subZones[];
	public PoliceZone policeZones[];
	public int agentID;
	public Point center = new Point();
	public int priority = 0;
	boolean isIgnited = false;
	// From 2009
	public double zonePriority = 1000;
	public int endZoneTime = -1;
	public Building markaz;
	ArrayList<Building> newBuried = new ArrayList<Building>();
	ArrayList<Building> unSeenBuilding = new ArrayList<Building>();
	ArrayList<Building> buriedBuilding = new ArrayList<Building>();
	ArrayList<Building> ezafeShand = new ArrayList<Building>();
	ArrayList<Civilian> CV = new ArrayList<Civilian>();
	double buriedAreaInWorld;
	double buriedArea;
	double unburnedArea;
	double Area;
	int maxX, minX, maxY, minY, midleX, midleY;
	boolean taghsimShode = false;

	public int dedicatedto = 0;

	// Till Here
	// Remember... you didn't add the noCommi part

	public Zone(int zoneNumber) {
		this.zoneNumber = zoneNumber;
	}

	public void setSubZones(int subZoneCount) {
		subZones = new SubZone[subZoneCount];
	}

	public void setPoliceZones(int policeZoneCount) {
		policeZones = new PoliceZone[policeZoneCount];
	}

	public int numberOfCVsInThisZone() {
		return CV.size();
	}

	public String toString() {
		return zoneNumber + " ";
	}
	
	public void setSubZonesForPolice(int subZoneCount) {
		subZones = new SubZone[subZoneCount];
		for (int i = 0; i < subZones.length; i++)
			subZones[i] = new SubZone(i);
	}

	protected Object clone() throws CloneNotSupportedException {
		Zone copyObj = new Zone(this.zoneNumber);
		copyObj.zoneNumber = this.zoneNumber;
		copyObj.Area = this.Area;
		copyObj.areas.addAll(this.areas);
		copyObj.buildings.addAll(this.buildings);
		copyObj.center = this.center;
		copyObj.CV = this.CV;
		copyObj.markaz = this.markaz;
		copyObj.refuges.addAll(this.refuges);
		copyObj.roads.addAll(this.roads);
		return copyObj;
		// return super.clone();
	}

}
