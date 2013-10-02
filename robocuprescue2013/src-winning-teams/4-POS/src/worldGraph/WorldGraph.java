package worldGraph;

import geometry.Mathematic;
import geometry.Point;

import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeSet;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import utilities.Logger;
import agent.Agent;

public class WorldGraph {
	public ArrayList<Area> areas = null;

	// This variable shows the Enterances which there is a way from our position
	// to that enterances
	public ArrayList<Enterance> myEnterances = null;
	public Area myArea = null;

	public int RAD = 500;
	private static final int MIN_EDGE_LENGTH = 500;

	private StandardWorldModel model = null;

	public WorldGraph(StandardWorldModel model,
			ArrayList<Building> modelBuildings, ArrayList<Road> modelRoads,
			ArrayList<Refuge> modelRefuges) {

		this.model = model;
		areas = new ArrayList<worldGraph.Area>();

		ArrayList<rescuecore2.standard.entities.Area> modelAreas = new ArrayList<rescuecore2.standard.entities.Area>();
		modelAreas.addAll(modelBuildings);
		modelAreas.addAll(modelRoads);
		// modelAreas.addAll(modelRefuges);

		for (rescuecore2.standard.entities.Area area : modelAreas) {
			int pCount = 6;
			for (EntityID e : area.getNeighbours())
				if (model.getEntity(e) instanceof Refuge) {
					pCount = 12;
					break;
				}

			Area newArea = new Area(area);
			newArea.POINTSCOUNT = pCount;
			areas.add(newArea);
		}

	
		for (Area area : areas) {
			rescuecore2.standard.entities.Area modelArea = area.modelArea;
			for (Edge edge : modelArea.getEdges()) {
				area.points.add(new Point(edge.getStartX(), edge.getStartY()));
				if (edge.getNeighbour() != null
						&& Mathematic.getDistance(edge.getStartX(),
								edge.getStartY(), edge.getEndX(),
								edge.getEndY()) > MIN_EDGE_LENGTH)
					area.enterances.add(new Enterance(area.enterances.size(),
							area.points.size() - 1, area, RAD,
							(rescuecore2.standard.entities.Area) model
									.getEntity(edge.getNeighbour())));
			}
			area.points.add(new Point(modelArea.getEdges().get(0).getStartX(),
					modelArea.getEdges().get(0).getStartY()));

			area.updateCenterPoint();
			if (area.enterances.size() == 0)
				System.out.println("Map BUG: Area: "
						+ area.modelArea.getID().getValue());
			for (Enterance enterance : area.enterances)
				enterance.createPoints();
		}

		for (Area area : areas) {
			int areaID = area.modelArea.getID().getValue();
			for (Enterance enterance : area.enterances) {
				Area otherArea = enterance.neighbourAreaInModel.worldGraphArea;
				for (Enterance neighbour : otherArea.enterances)
					if (neighbour.neighbourAreaInModel.getID().getValue() == areaID) {
						int x0 = enterance.area.points.get(enterance.index).getX();
						int x1 = enterance.area.points.get(enterance.index + 1).getX();
						int x2 = neighbour.area.points.get(neighbour.index).getX();
						int x3 = neighbour.area.points.get(neighbour.index + 1).getX();
						if ((x0 == x2 && x1 == x3) || (x0 == x3 && x1 == x2)) {
							enterance.neighbour = neighbour;
							break;
						}
						enterance.neighbour = neighbour;
					}
			}
		}

		for (Area area : areas)
			area.updateAvailablePoints();

		for (Area area : areas)
			area.updateEnterancesConnectivity(RAD, null, false);

	}

	public void clearAreas() {
		for (Area area : areas)
			area.clearMarks();
	}

	public boolean isThereWayFromHere(Area target) {
		if (myEnterances == null)
			return false;
		if (myArea != null && myArea == target)
			return true;
		setTarget(target);

		return isThereWay(myEnterances);
	}

	public boolean isThereWay(EntityID start, EntityID target) {
		return isThereWay(
				((rescuecore2.standard.entities.Area) model.getEntity(start)).worldGraphArea,
				((rescuecore2.standard.entities.Area) model.getEntity(target)).worldGraphArea);
	}

	public boolean isThereWay(Area start, Area target) {
		if (start == target)
			return true;
		setTarget(target);

		return isThereWay(start.enterances);
	}

	public void clearTargets() {
		for (Area area : areas)
			area.setAreaAsTarget(false);
	}

	public void setTarget(Area target) {
		clearTargets();
		target.setAreaAsTarget(true);
	}

	public boolean isThereWay(ArrayList<Enterance> start) {
		clearAreas();

		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		for (Enterance enterance : start)
			if (!enterance.mark) {
				enterance.mark = true;
				layer.add(enterance);
			}

		while (layer.size() > 0) {
			ArrayList<Enterance> newLayer = new ArrayList<Enterance>();
			for (Enterance enterance : layer) {
				if (enterance.isItConnectedToNeighbour
						&& !enterance.neighbour.mark) {
					Enterance neighbour = enterance.neighbour;
					if (neighbour.isTarget)
						return true;

					neighbour.mark = true;
					for (Enterance internal : neighbour.internalEnterances) {
						if (!internal.mark) {
							if (internal.isTarget)
								return true;

							internal.mark = true;
							newLayer.add(internal);
						}
					}
				}
			}

			layer = newLayer;
		}

		return false;
	}

	public ArrayList<EntityID> getMinPath(EntityID start, EntityID end,
			boolean checkBlockades) {
		return getMinPath(
				((rescuecore2.standard.entities.Area) model.getEntity(start)).worldGraphArea,
				((rescuecore2.standard.entities.Area) model.getEntity(end)).worldGraphArea,
				checkBlockades);
	}

	public ArrayList<EntityID> getMinPath(Area start, Area end,
			boolean checkBlockades) {
		return getMinPath(start.enterances, end, checkBlockades);
	}

	public ArrayList<EntityID> getMinPath(ArrayList<Enterance> start, Area end,
			boolean checkBlockades) {
		setTarget(end);
		return getMinPath(start, checkBlockades);
	}

	public ArrayList<EntityID> getMinPath(ArrayList<Enterance> start,
			boolean checkBlockades) {
		clearAreas();
		ArrayList<EntityID> path = new ArrayList<EntityID>();
		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		for (Enterance enterance : start)
			if (enterance.isTarget)
				return path;
			else if (!checkBlockades || !enterance.mark) {
				enterance.mark = true;
				layer.add(enterance);
			}

		Enterance lastEnterance = null;
		done: while (layer.size() > 0) {
			ArrayList<Enterance> newLayer = new ArrayList<Enterance>();
			for (Enterance enterance : layer)
				if (!checkBlockades
						|| (enterance.isItConnectedToNeighbour && !enterance.neighbour.mark)) {
					Enterance neighbour = enterance.neighbour;
					neighbour.lastEnterance = enterance;
					if (neighbour.isTarget) {
						lastEnterance = neighbour;
						break done;
					}

					neighbour.mark = true;
					if (checkBlockades) {
						for (Enterance internal : neighbour.internalEnterances)
							if (!internal.mark) {
								internal.lastEnterance = neighbour;
								if (internal.isTarget) {
									lastEnterance = internal;
									break done;
								}

								internal.mark = true;
								newLayer.add(internal);
							}
					} else {
						for (Enterance internal : neighbour.area.enterances)
							if (neighbour != internal && internal.mark == false) {
								internal.lastEnterance = neighbour;
								if (internal.isTarget) {
									lastEnterance = internal;
									break done;
								}

								internal.mark = true;
								newLayer.add(internal);
							}
					}
				}

			layer = newLayer;
		}

		Stack<EntityID> stack = new Stack<EntityID>();
		while (lastEnterance != null) {
			EntityID entityID = lastEnterance.area.modelArea.getID();
			if (stack.size() == 0 || stack.peek() != entityID)
				stack.add(entityID);
			lastEnterance = lastEnterance.lastEnterance;
		}

		while (stack.size() != 0)
			path.add(stack.pop());

		return path;
	}

	public boolean log = false;
	public int time = -1;

	public void update(ChangeSet changeSet) {
		for (EntityID entityID : changeSet.getChangedEntities()) {
			Entity e = model.getEntity(entityID);
			if (e instanceof rescuecore2.standard.entities.Road) {
				rescuecore2.standard.entities.Area modelArea = (rescuecore2.standard.entities.Area) e;
				Area area = modelArea.worldGraphArea;
				area.blockades.clear();
				if (modelArea.isBlockadesDefined())
					for (EntityID blockadeID : modelArea.getBlockades()) {
						rescuecore2.standard.entities.Blockade blockade = (rescuecore2.standard.entities.Blockade) model
								.getEntity(blockadeID);
						area.blockades.add(new Blockade(blockade));
					}
				area.updateAvailablePoints();
			}
		}

		int tofRAD = RAD;
//		if (agent instanceof PoliceForceAgent)
//			tofRAD *= 2;
		logger.log("after update availablePoints");
		for (EntityID entityID : changeSet.getChangedEntities()) {
			Entity e = model.getEntity(entityID);
			if (e instanceof rescuecore2.standard.entities.Area)
				((rescuecore2.standard.entities.Area) e).worldGraphArea
						.updateEnterancesConnectivity(tofRAD, changeSet, true);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private Agent agent = null;

	public Logger logger = null;

	@SuppressWarnings({ "rawtypes" })
	public void update(int time, Agent agent, EntityID myPosition, int myX,
			int myY) {
		this.time = time;
		this.agent = agent;

		if (myPosition != null && myX != -1 && myY != -1) {
			myArea = ((rescuecore2.standard.entities.Area) model
					.getEntity(myPosition)).worldGraphArea;
			myEnterances = myArea.updateEnterancesConnectivity(new Point(myX,
					myY), RAD, agent.changeSet, true);
			if (myEnterances == null || myEnterances.size() == 0)
				myEnterances = myArea.updateEnterancesConnectivity(new Point(
						myX, myY), (int) (RAD * 0.75), agent.changeSet, true);
		}
		if (myEnterances == null)
			myEnterances = new ArrayList<Enterance>();
	}

	@SuppressWarnings("unchecked")
	public void addReachableArea(Enterance enterance) {
		enterance.isReachable = true;
		boolean oldValue = enterance.area.isReachable;
		enterance.area.isReachable = true;
		rescuecore2.standard.entities.Area area = enterance.area.modelArea;
		// agent.reachableEnterances.put(enterance.index, enterance);

		// if (!agent.reachableAreas.contains(area)) {
		if (!oldValue) {
			// agent.reachableAreas.put(area.getID().getValue(), area);
			area.worldGraphArea.distanceFromSelf = distance;
			agent.reachableAreas.add(area);

			if (area instanceof Refuge)
				agent.reachableRefuges.add((Refuge) area);
			else if (area instanceof Building)
				agent.reachableBuildings.add((Building) area);
			else if (area instanceof Road)
				agent.reachableRoads.add((Road) area);
			if(area instanceof Hydrant)
				agent.reachableHydrants.add((Hydrant)area);
		}
	}

	private int distance = -1;

	@SuppressWarnings("unchecked")
	public void updateReachableAreas(ArrayList<Enterance> start) {
		agent.reachableBuildings = new ArrayList<Building>();
		agent.reachableRoads = new ArrayList<Road>();
		agent.reachableRefuges = new ArrayList<Refuge>();
		agent.reachableHydrants = new ArrayList<Hydrant>();
		// agent.reachableAreas = new TreeMap<Integer,
		// rescuecore2.standard.entities.Area>();
		// agent.reachableEnterances = new TreeMap<Integer, Enterance>();
		agent.reachableAreas = new ArrayList<rescuecore2.standard.entities.Area>();
		// agent.reachableAreas = new
		// HashSet<rescuecore2.standard.entities.Area>();

		clearAreas();
		for (Area area : areas) {
			area.distanceFromSelf = -1;
			area.isReachable = false;
			area.buildingReachability = false;
			area.buildingReachabilityCalculated = false;
			for (Enterance e : area.enterances)
				e.isReachable = false;
		}

		distance = 0;
		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		for (Enterance enterance : start)
			if (!enterance.mark) {
				addReachableArea(enterance);
				enterance.mark = true;
				layer.add(enterance);
			}

		while (layer.size() > 0) {
			ArrayList<Enterance> newLayer = new ArrayList<Enterance>();
			distance++;
			for (Enterance enterance : layer) {
				if (enterance.isItConnectedToNeighbour
						&& !enterance.neighbour.mark) {
					Enterance neighbour = enterance.neighbour;
					addReachableArea(neighbour);
					neighbour.mark = true;
					for (Enterance internal : neighbour.internalEnterances) {
						if (!internal.mark) {
							addReachableArea(internal);
							internal.mark = true;
							newLayer.add(internal);
						}
					}
				}
			}

			layer = newLayer;
		}
	}

	// Ambulance getMinPath
	public ArrayList<Civilian> getMinPathForAmbulance(ArrayList<Enterance> start) {
		clearAreas();
		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		for (Enterance enterance : start)
			if (enterance.isTarget)
				return enterance.civilians;
			else if (!enterance.mark) {
				enterance.mark = true;
				layer.add(enterance);
			}

		while (layer.size() > 0) {
			ArrayList<Enterance> newLayer = new ArrayList<Enterance>();
			for (Enterance enterance : layer)
				if (enterance.isItConnectedToNeighbour
						&& !enterance.neighbour.mark) {
					Enterance neighbour = enterance.neighbour;
					if (neighbour.isTarget)
						return neighbour.civilians;

					neighbour.mark = true;
					for (Enterance internal : neighbour.internalEnterances)
						if (!internal.mark) {
							if (internal.isTarget)
								return internal.civilians;

							internal.mark = true;
							newLayer.add(internal);
						}
				}

			layer = newLayer;
		}

		return null;
	}

	public ArrayList<EntityID> getMinPathDijk(EntityID end,
			boolean checkBlockades) {
		return getMinPathDijk(end, checkBlockades, false);
	}

	public ArrayList<EntityID> getMinPathDijk(EntityID end,
			boolean checkBlockades, boolean isCalledForMove) {
		Area target = ((rescuecore2.standard.entities.Area) model
				.getEntity(end)).worldGraphArea;
		TreeSet<Enterance> allEnterances = new TreeSet<Enterance>();
		Human h = (Human) model.getEntity(agent.getID());
		Area a = ((rescuecore2.standard.entities.Area) model.getEntity(h
				.getPosition())).worldGraphArea;
		if(a.equals(target)){
			ArrayList<EntityID> path = new ArrayList<EntityID>();
			path.add(end);
			return path;
		}
		if (!isCalledForMove) {
			ArrayList<EntityID> p = getMinPath(a.modelArea.getID(), end,
					checkBlockades);
			if (p.size() > 30)
				end = p.get(20);
		}
		// logger.log("path.size <= 30");
		for (Area area : areas)
			for (Enterance ents : area.enterances) {
				ents.distForDijk = 1000000000;
				ents.prev = null;
				ents.isTarget = false;
			}

		ArrayList<Enterance> firstEnterances = myEnterances;
		if (!checkBlockades)
			firstEnterances = a.enterances;
		for (Enterance e : firstEnterances) {
			Point p2 = new Point(a.modelArea.getX(), a.modelArea.getY());
			e.distForDijk = p2.getDistance(e.center);
		}

		logger.log("ghabl az pr kardane allEnternaces");
		for (Area area : areas) {
			allEnterances.addAll(area.enterances);
		}
		logger.log("allEnterances por shod");
		for (Enterance e : target.enterances)
			e.isTarget = true;

		while (allEnterances.size() > 0) {
			Enterance e = allEnterances.first();
			allEnterances.remove(e);
			ArrayList<Enterance> neighbours = new ArrayList<Enterance>();
			if (e.isItConnectedToNeighbour || !checkBlockades) {
				neighbours.add(e.neighbour);
			}
			if (checkBlockades)
				neighbours.addAll(e.internalEnterances);
			else
				for (Enterance enterance : e.area.enterances)
					if (enterance != e)
						neighbours.add(enterance);
			Point ep = e.center;
			for (Enterance enter : neighbours) {
				Point np = enter.center;
				double dist0 = ep.getDistance(np);
				if (!e.internalEnterances.contains(enter)
						|| !enter.isItConnectedToNeighbour)
					dist0 += 5000;
				if (enter.area.modelArea.isNearFire) {
					if (enter.area.modelArea instanceof Building
							&& ((Building) enter.area.modelArea).isOnFire())
						dist0 *= 200;
					else
						dist0 = 1;
				} else if (enter.area.modelArea instanceof Building
						&& ((Building) enter.area.modelArea).isOnFire())
					dist0 *= 200;
				if (e.distForDijk + dist0 < enter.distForDijk) {
					allEnterances.remove(enter);
					enter.distForDijk = e.distForDijk + dist0;
					allEnterances.add(enter);
					enter.prev = e;
					if (enter.isTarget) {
						// logger.log("isTarget: "
						// + enter.area.modelArea.getID().getValue());
						Enterance enterance = enter;
						ArrayList<EntityID> way1 = new ArrayList<EntityID>();
						ArrayList<EntityID> way2 = new ArrayList<EntityID>();
						while (enterance != null) {
							EntityID entityID = enterance.area.modelArea
									.getID();
							if (!way1.contains(entityID))
								way1.add(entityID);
							enterance = enterance.prev;
						}
						for (int i = way1.size() - 1; i >= 0; i--)
							way2.add(way1.get(i));
						// logger.log("way2: " + way2);
						return way2;
					}
				}
			}
		}
		return null;
	}
}
