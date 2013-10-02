package agent;

import java.util.ArrayList;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class Path {
	public ArrayList<Area> way = new ArrayList<Area>();
	public ArrayList<Building> buildings = new ArrayList<Building>();
	ArrayList<Road> extra = new ArrayList<Road>();
	public ArrayList<Path> neighbours = null;
	public int centralX = 0, centralY = 0;
	public int pathNum = -1;
	public boolean isChosen = false;
	public boolean chosenForBFS = false;

	public void setCenter() {
		int allX = 0, allY = 0;
		int tedad = 0;
		for (Area a : way) {
			allX += a.getX();
			allY += a.getY();
			tedad++;
		}
		centralX = allX / tedad;
		centralY = allY / tedad;
	}

	public void setNeighbours(StandardWorldModel model,
			utilities.Logger logger, PathArray hameyeRahHa) {
		neighbours = new ArrayList<Path>();
		ArrayList<Integer> addadash = new ArrayList<Integer>();
		for (Area khodesh : way)
			for (EntityID neighboureshID : khodesh.getNeighbours()) {
				Area neighbour = (Area) model.getEntity(neighboureshID);
				if (neighbour.pathNum != khodesh.pathNum
						&& !addadash.contains(neighbour.pathNum))
					addadash.add(neighbour.pathNum);
			}
		for (Building khodesh : buildings)
			for (EntityID neighboureshID : khodesh.getNeighbours()) {
				Area neighbour = (Area) model.getEntity(neighboureshID);
				if (neighbour.pathNum != khodesh.pathNum
						&& !addadash.contains(neighbour.pathNum))
					addadash.add(neighbour.pathNum);
			}
		for (Road khodesh : extra)
			for (EntityID neighboureshID : khodesh.getNeighbours()) {
				Area neighbour = (Area) model.getEntity(neighboureshID);
				if (neighbour.pathNum != khodesh.pathNum
						&& !addadash.contains(neighbour.pathNum))
					addadash.add(neighbour.pathNum);
			}

		for (Path haPath : hameyeRahHa.pathes)
			// if (haPath.buildings)
			if (addadash.contains(haPath.pathNum))
				neighbours.add(haPath);
	}

	public String toString() {
		String s = new String();
		for (Area a : way)
			s += a.getID().getValue() + " ";
		for (Building b : buildings)
			s += b.getID().getValue() + " ";
		for (Road r : extra)
			s += r.getID().getValue() + " ";
		return s;
	}
}
