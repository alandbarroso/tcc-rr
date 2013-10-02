package agent;

import geometry.Point;

import java.util.ArrayList;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;

public class PoliceZone {
	public int policeZoneNumber;
	public ArrayList<Area> areas = new ArrayList<Area>();
	public ArrayList<Building> buildings = new ArrayList<Building>();
	public ArrayList<Road> roads = new ArrayList<Road>();
	public ArrayList<Refuge> refuges = new ArrayList<Refuge>();
	public Point center = new Point();
	public SubZone subZones[];
	public ArrayList<Integer> policesDedicatedto = new ArrayList<Integer>();
	public int dedicatedTo = 0;

	public PoliceZone(int policeZoneNumber) {
		this.policeZoneNumber = policeZoneNumber;
	}

	public void setSubZonesForPolice(int subZoneCount) {
		subZones = new SubZone[subZoneCount];
		for (int i = 0; i < subZones.length; i++)
			subZones[i] = new SubZone(i);
	}

}
