package worldGraph;

import geometry.Degree;
import geometry.Line;
import geometry.Point;
import graph.Graph;
import graph.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import agent.FireZone;

public class Area extends Shape {
	public ArrayList<Blockade> blockades = new ArrayList<Blockade>();
	public ArrayList<Enterance> enterances = new ArrayList<Enterance>();
	public rescuecore2.standard.entities.Area modelArea = null;
	public int distanceFromSelf = -1;
	public int distFromeSelfWOB = -1; // distance from self without blockade
	public boolean checkSituation4Fire = false;
	public boolean check = false;
	public ArrayList<FireBrigade> fB = new ArrayList<FireBrigade>();
	public FireZone fireZoneInArea = null;
	public ArrayList<Civilian> civilians = new ArrayList<Civilian>(); // for
	// ambulance
	public ArrayList<AmbulanceTeam> ambulances = new ArrayList<AmbulanceTeam>(); // for
	// ambulance
	public ArrayList<Human> agents = new ArrayList<Human>(); // for
	// ambulance
	public boolean mark = false; // for Ambulance
	public int ZoneNumberForPolice = -1;
	public int subZoneNumberForPolice = -1;
	public int policeZoneNumber = -1;

	public boolean isReachable = false;
	public boolean buildingReachability = false;
	public boolean buildingReachabilityCalculated = false;
	

	public Area() {
	}

	public Area(rescuecore2.standard.entities.Area modelArea) {
		this.modelArea = modelArea;
		modelArea.worldGraphArea = this;
	}

	public int POINTSCOUNT = 6;

	public void clearMarks() {
		for (Enterance enterance : enterances) {
			enterance.mark = false;
			enterance.lastEnterance = null;
			enterance.area.checkSituation4Fire = false;
		}
	}

	public void setStar() {// for ambulance search
		for (Enterance enterance : enterances)
			enterance.star = true;
	}

	public void clearStar() { // for ambulance search
		for (Enterance enterance : enterances)
			enterance.star = false;
	}

	public void clearCivilians() { // for ambulance
		for (Enterance enterance : enterances)
			enterance.civilians.clear();
	}

	public void addCivilian(Civilian civilian) { // for ambulance
		for (Enterance enterance : enterances)
			enterance.civilians.add(civilian);
	}

	public void setAreaAsTarget(boolean isTarget) {
		for (Enterance enterance : enterances)
			enterance.isTarget = isTarget;
	}

	public boolean isThereWay(Point firstPoint, Point secondPoint, int rad) {
		return isThereWay(firstPoint, secondPoint, rad, true);
	}

	public boolean isThereWay(Point firstPoint, Point secondPoint, int rad,
			boolean justBlockades) {
		Line[] lines = new Line[3];
		lines[0] = new Line(firstPoint, secondPoint);
		float theta = Degree.normalizeAngle(lines[0].getTheta()
				- (float) Math.PI / 2);
		Point diff = new Point(theta, rad, true);
		lines[1] = new Line(firstPoint.plus(diff), secondPoint.plus(diff));
		lines[2] = new Line(firstPoint.minus(diff), secondPoint.minus(diff));

		for (Line line : lines) {
			if (!justBlockades && hasIntersectWithShape(line))
				return false;
			for (Blockade blockade : blockades)
				if (blockade.hasIntersectWithShape(line))
					return false;
		}

		return true;
	}

	public ArrayList<Point> getSafeBlockadeEdgePoints(int rad) {
		ArrayList<Point> safePoints = new ArrayList<Point>();
		for (Blockade blockade : blockades) {
			for (int i = 0; i < blockade.points.size() - 1; i++) {
				Point point = blockade.getOuterEdgePoint(i, rad + 5);
				if (isItInAndSafeCircle(point, rad))
					safePoints.add(point);
				else {
					for (int diff = 20; diff != 10; diff -= 5) {
						point = blockade.getOuterEdgePoint(i, rad, rad + diff);
						if (isItInAndSafeCircle(point, rad)) {
							safePoints.add(point);
							break;
						}
					}
				}
			}
		}

		return safePoints;
	}

	public ArrayList<Point> getSafePoints(int index, int rad) {
		return getSafePoints(index, rad, true);
	}

	public ArrayList<Point> getSafePoints(int index, int rad,
			boolean checkBlockades) {
		ArrayList<Point> safePoints = new ArrayList<Point>();
		Line line = new Line(getInnerPoint(index, index + 1, rad + 5),
				getInnerPoint(index + 1, index, rad + 5));

		ArrayList<Point> pointsInLine = line.getPointsInLine(POINTSCOUNT);
		for (Point point : pointsInLine)
			if (isItInAndSafeCircle(point, rad, checkBlockades))
				safePoints.add(point);

		return safePoints;
	}

	public boolean isItInAndSafeCircle(Point point, int rad) {
		return isItInAndSafeCircle(point, rad, true);
	}

	public boolean isItInAndSafeCircle(Point point, int rad,
			boolean checkBlockades) {
		if (!isInShape(point))
			return false;

		if (hasIntersectWithShape(point, rad))
			return false;

		if (checkBlockades)
			return !hasConflictWithBlockades(point, rad);

		return true;
	}

	public boolean hasConflictWithBlockades(Point point, int rad) {
		for (Blockade blockade : blockades)
			if (blockade.hasItConflict(point, rad))
				return true;

		return false;
	}

	public void updateEnterancesConnectivity(int rad, ChangeSet changeSet,
			boolean shouldCheckChangeSet) {
		updateEnterancesConnectivity(null, rad, changeSet, shouldCheckChangeSet);
	}

	public void updateAvailablePoints() {
		for (Enterance enterance : enterances)
			enterance.updateAvaiablePoints();
	}

	public ArrayList<Enterance> updateEnterancesConnectivity(Point myPos,
			int rad, ChangeSet changeSet, boolean shouldCheckChangeSet) {
		for (Enterance enterance : enterances) {
			if (!shouldCheckChangeSet)
				enterance.updateConnectivity(true);
			else
				enterance
						.updateConnectivity(enterance.neighbourAreaInModel instanceof Building
								|| changeSet.getChangedEntities().contains(
										enterance.neighbourAreaInModel.getID()));
			enterance.internalEnterances = new ArrayList<Enterance>();
		}

		if (blockades.size() == 0) {
			for (int i = 0; i < enterances.size(); i++)
				for (int j = 0; j < i; j++) {
					enterances.get(i).internalEnterances.add(enterances.get(j));
					enterances.get(j).internalEnterances.add(enterances.get(i));
				}

			if (myPos != null)
				return enterances;
			return null;
		}

		Graph graph = new Graph();
		ArrayList<Node> nodes = graph.nodes;
		for (Enterance enterance : enterances) {
			enterance.nodes = new ArrayList<Node>();
			for (Point point : enterance.avaialablePoints) {
				Node node = new Node(nodes.size(), point);
				node.enterance = enterance;
				enterance.nodes.add(node);
				nodes.add(node);
			}
		}

		if (nodes.size() == 0)
			return null;

		// Inner Edge Points
		for (int i = 0; i < points.size() - 1; i++) {
			Point innerPoint = getInnerEdgePoint(i, rad);
			if (isItInAndSafeCircle(innerPoint, rad))
				nodes.add(new Node(nodes.size(), innerPoint));
		}
		// End

		ArrayList<Point> safePoints = getSafeBlockadeEdgePoints(rad);
		for (Point point : safePoints)
			nodes.add(new Node(nodes.size(), point));

		Node myPosNode = null;
		if (myPos != null) {
			myPosNode = new Node(nodes.size(), myPos);
			nodes.add(myPosNode);
		}

		for (int i = 1; i < nodes.size(); i++)
			for (int j = 0; j < i; j++) {
				Node node = nodes.get(i);
				Node other = nodes.get(j);

				if ((node.enterance == null || other.enterance == null || node.enterance != other.enterance)
						&& isThereWay(node.getPoint(), other.getPoint(), rad,
								true)) {
					node.neighbours.add(other);
					other.neighbours.add(node);
				}
			}

		if (myPosNode != null && myPosNode.neighbours.size() == 0) {
			for (int i = 0; i < nodes.size() - 1; i++) {
				Node node = nodes.get(i);

				if (isThereWay(node.getPoint(), myPosNode.getPoint(), rad - 50,
						true)) {
					node.neighbours.add(myPosNode);
					myPosNode.neighbours.add(node);
				}
			}
		}

		for (int i = 1; i < enterances.size(); i++)
			for (int j = 0; j < i; j++) {
				Enterance start = enterances.get(i);
				Enterance end = enterances.get(j);
				if (graph.isThereWay(start.nodes, end.nodes)
						|| start.points.size() == 0 || end.points.size() == 0) {
					start.internalEnterances.add(end);
					end.internalEnterances.add(start);
				}
			}

		if (myPosNode != null) {
			ArrayList<Enterance> myEnterances = new ArrayList<Enterance>();
			graph.clearNodesTarget();
			myPosNode.isTarget = true;

			for (Enterance enterance : enterances)
				if (graph.isThereWay(enterance.nodes))
					myEnterances.add(enterance);

			return myEnterances;
		}

		return null;
	}

	// Hey Calculations!
	public ArrayList<Point> getHeySafePoints(int index, int rad) {
		return getHeySafePoints(index, rad, true);
	}

	public ArrayList<Point> getHeySafePoints(int index, int rad,
			boolean checkBlockades) {
		ArrayList<Point> safePoints = new ArrayList<Point>();
		Line line = new Line(getInnerPoint(index, index + 1, rad + 5),
				getInnerPoint(index + 1, index, rad + 5));

		ArrayList<Point> pointsInLine = line.getPointsInLine(POINTSCOUNT);

		for (Point point : pointsInLine)
			if (isItInAndHeySafeCircle(point, rad, checkBlockades))
				safePoints.add(point);

		return safePoints;
	}

	public boolean isItInAndHeySafeCircle(Point point, int rad) {
		return isItInAndSafeCircle(point, rad, true);
	}

	public boolean isItInAndHeySafeCircle(Point point, int rad,
			boolean checkBlockades) {
		if (!isInShape(point))
			return false;

		if (checkBlockades)
			return !hasConflictWithBlockades(point, rad);

		return true;
	}

	// Blockade Calculations for Police
	public Collection<rescuecore2.standard.entities.Blockade> getBadBlockades(
			StandardWorldModel model, int x, int y, int rad,
			boolean isItNeedToChange) {
		if (isItNeedToChange) {
			TreeMap<Double, rescuecore2.standard.entities.Blockade> distTree = new TreeMap<Double, rescuecore2.standard.entities.Blockade>();
			if (modelArea.isBlockadesDefined())
				for (EntityID entityID : modelArea.getBlockades()) {
					rescuecore2.standard.entities.Blockade blockade = (rescuecore2.standard.entities.Blockade) model
							.getEntity(entityID);
					distTree.put(findDistanceTo(blockade, x, y), blockade);
				}
			return distTree.values();
		} else {
			HashSet<rescuecore2.standard.entities.Blockade> badBlockades = new HashSet<rescuecore2.standard.entities.Blockade>();
			for (Enterance enterance : enterances)
				if (enterance.avaialablePoints.size() == 0
						|| enterance.avaialablePoints.size() < enterance.points
								.size() / 2
						|| (!enterance.isItConnectedToNeighbour && enterance.neighbour.avaialablePoints
								.size() != 0)) {
					for (Point point : enterance.points)
						for (Blockade blockade : blockades)
							if (blockade.hasItConflict(point, rad))
								badBlockades.add(blockade.modelBlockade);
				}

			if (badBlockades.size() != blockades.size()) {
				done: for (int i = 1; i < enterances.size(); i++)
					for (int j = 0; j < i; j++) {
						Enterance start = enterances.get(i);
						Enterance end = enterances.get(j);

						if (!start.internalEnterances.contains(end)) {
							ArrayList<Point> startPoints = start.avaialablePoints
									.size() > 0 ? start.avaialablePoints
									: start.points;
							ArrayList<Point> endPoints = end.avaialablePoints
									.size() > 0 ? end.avaialablePoints
									: end.points;
							for (Point first : startPoints)
								for (Point second : endPoints) {
									Set<Blockade> blocks = getBadBlockades(
											first, second, rad);
									for (Blockade blockade : blocks)
										badBlockades
												.add(blockade.modelBlockade);
									if (badBlockades.size() == blockades.size())
										break done;
								}
						}
					}
			}

			TreeMap<Double, rescuecore2.standard.entities.Blockade> distTree = new TreeMap<Double, rescuecore2.standard.entities.Blockade>();
			for (rescuecore2.standard.entities.Blockade blockade : badBlockades) {
				distTree.put(findDistanceTo(blockade, x, y), blockade);
				// distTree.put(
				// Mathematic.getDistance(x, y, blockade.getX(),
				// blockade.getY()), blockade);
			}
			return distTree.values();
		}
	}

	private double findDistanceTo(rescuecore2.standard.entities.Blockade b,
			int x, int y) {
		List<Line2D> lines = GeometryTools2D.pointsToLines(
				GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D origin = new Point2D(x, y);
		for (Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next,
					origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			if (d < best)
				best = d;
		}
		return best;
	}

	public Set<Blockade> getBadBlockades(Point firstPoint, Point secondPoint,
			int rad) {
		HashSet<Blockade> badBlockades = new HashSet<Blockade>();
		Line[] lines = new Line[3];
		lines[0] = new Line(firstPoint, secondPoint);
		float theta = Degree.normalizeAngle(lines[0].getTheta()
				- (float) Math.PI / 2);
		Point diff = new Point(theta, rad, true);
		lines[1] = new Line(firstPoint.plus(diff), secondPoint.plus(diff));
		lines[2] = new Line(firstPoint.minus(diff), secondPoint.minus(diff));

		for (Line line : lines) {
			for (Blockade blockade : blockades)
				if (blockade.hasIntersectWithShape(line) || blockade.isInShape(firstPoint))
					badBlockades.add(blockade);
		}

		return badBlockades;
	}

	// ساختار جدید پلیس
	ArrayList<EntityID> nearAreas = null;

	public ArrayList<EntityID> getNearAreas() {
		if (nearAreas == null) {
			nearAreas = new ArrayList<EntityID>();
			for (Enterance e : enterances)
				if (!nearAreas.contains(e.neighbour.area.modelArea))
					nearAreas.add(e.neighbour.area.modelArea.getID());
		}
		return nearAreas;
	}
}
