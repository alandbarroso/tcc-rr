package worldGraph;

import geometry.Point;
import graph.Node;
import java.util.ArrayList;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;

public class Enterance implements Comparable<Enterance> {
	public ArrayList<Point> points = new ArrayList<Point>();
	public ArrayList<Point> avaialablePoints = null;
	public Point center = null;
	public Point realCenter = null;

	public ArrayList<Node> nodes = null;
	public int index = -1;
	public int id = -1;
	public double distForDijk = 1000000000;
	public int rad = -1;
	public boolean isItConnectedToNeighbour = true;
	public boolean isCheck = false;
	public boolean star = false;// for Ambulance search
	public boolean mark = false;
	public boolean isTarget = false;
	public boolean isDijkTarget = false;
	public boolean isReachable = false;
	public Enterance lastEnterance = null;
	public Enterance prev = null;
	public ArrayList<Civilian> civilians = new ArrayList<Civilian>(); // for
	// ambulance
	public ArrayList<AmbulanceTeam> ambulances = new ArrayList<AmbulanceTeam>(); // for
	// ambulance
	public rescuecore2.standard.entities.Area neighbourAreaInModel = null;

	public Enterance() {
	}

	public Enterance(int id, int index, Area area, int rad) {
		this.id = id;
		this.index = index;
		this.area = area;
		this.rad = rad;
	}

	public Enterance(int id, int index, Area area, int rad,
			rescuecore2.standard.entities.Area neighbourAreaInModel) {
		this.id = id;
		this.index = index;
		this.area = area;
		this.rad = rad;
		this.neighbourAreaInModel = neighbourAreaInModel;
	}

	public void createPoints() {
		points = area.getSafePoints(index, rad, false);
		points.addAll(area.getSafePoints(index, rad / 4, false));
		int mult = 1;
		while (points.size() == 0 && mult < 40) {
			points = area.getHeySafePoints(index, rad / mult, false);
			mult *= 2;
		}
		Edge ed = area.modelArea.getEdgeTo(neighbourAreaInModel.getID());
		realCenter = new Point((ed.getStartX() + ed.getEndX()) / 2,
				(ed.getStartY() + ed.getEndY()) / 2);
		if (points.size() == 0) {
			center = new Point((ed.getStartX() + ed.getEndX()) / 2,
					(ed.getStartY() + ed.getEndY()) / 2);
			System.err.println("A: " + toString() + " " + area.points);
		} else
			center = points.get(points.size() / 2);
		avaialablePoints = points;
	}

	public void updateAvaiablePoints() {
		avaialablePoints = new ArrayList<Point>();
		for (Point point : points)
			if (!area.hasConflictWithBlockades(point, rad))
				avaialablePoints.add(point);
	}

	public Area area = null;
	public Enterance neighbour = null;

	public ArrayList<Enterance> internalEnterances = null;

	public void updateConnectivity(boolean isNeighbourInChangeSet) {
		boolean lastValue = isItConnectedToNeighbour;
		isItConnectedToNeighbour = false;

		if (isNeighbourInChangeSet || lastValue) {
			if (points.size() == 0 || neighbour.points.size() == 0) {
				if (points.size() == 0 && neighbour.points.size() > 0)
					isItConnectedToNeighbour = area.blockades.size() == 0
							&& neighbour.avaialablePoints.size() > 0;
				else if (neighbour.points.size() == 0 && points.size() > 0)
					isItConnectedToNeighbour = neighbour.area.blockades.size() == 0
							&& avaialablePoints.size() > 0;
				else
					isItConnectedToNeighbour = area.blockades.size() == 0
							&& neighbour.area.blockades.size() > 0;
			}
			else {
				done: for (Point point : avaialablePoints)
					for (Point other : neighbour.avaialablePoints)
						if (area.isThereWay(point, other, rad, true)
								&& neighbour.area.isThereWay(point, other, rad,
										true)) {
							isItConnectedToNeighbour = true;
							break done;
						}
			}
		}

		neighbour.isItConnectedToNeighbour = isItConnectedToNeighbour;
	}

	public String toString() {
		return ((area.modelArea != null) ? (area.modelArea.getID().getValue() + " ")
				: "")
				+ index
				+ " "
				+ neighbourAreaInModel.getID().getValue()
				+ " "
				+ isItConnectedToNeighbour;
	}

	public int compareTo(Enterance other) {
		if (distForDijk > other.distForDijk)
			return 1;
		if (distForDijk < other.distForDijk)
			return -1;
		if (area.modelArea.getID().getValue() > other.area.modelArea.getID()
				.getValue())
			return 1;
		if (area.modelArea.getID().getValue() < other.area.modelArea.getID()
				.getValue())
			return -1;
		if (id > other.id)
			return 1;
		if (id < other.id)
			return -1;
		return 0;
	}

}