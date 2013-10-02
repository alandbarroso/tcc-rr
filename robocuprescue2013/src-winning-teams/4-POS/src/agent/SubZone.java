package agent;

import geometry.Point;

import java.util.ArrayList;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;

public class SubZone {
	boolean finished = false;
	public int subZoneNumber;
	public int zoneNumber;
	public ArrayList<Area> areas = new ArrayList<Area>();
	public ArrayList<Building> buildings = new ArrayList<Building>();
	public ArrayList<Road> roads = new ArrayList<Road>();
	public ArrayList<Refuge> refuges = new ArrayList<Refuge>();
	public int agentID;
	public Point center = new Point();
	public int priority = 0;
	int subZoneID = -1;
	boolean isIgnited = false;

	public SubZone() {

	}

	public SubZone(int subZoneNumber) {
		this.subZoneNumber = subZoneNumber;
	}

	public void setSubZoneID(int ID) {
		this.subZoneID = ID;
	}
}
