package agent;

import java.util.HashSet;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Road;

public class GroupForFire {
	public HashSet<FireBrigade> fireAgents = new HashSet<FireBrigade>();
	public HashSet<Building> buildings = new HashSet<Building>();
	public HashSet<Road> roads = new HashSet<Road>();
	public int num = -1;
	public HashSet<Building> visionBuildings = new HashSet<Building>();
}
