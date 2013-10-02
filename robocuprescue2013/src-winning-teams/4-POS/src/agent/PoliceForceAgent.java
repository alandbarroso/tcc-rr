package agent;

import geometry.Degree;
import geometry.Line;
import geometry.Mathematic;
import geometry.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import rescuecore2.messages.Command;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import worldGraph.Enterance;

public class PoliceForceAgent extends Agent<PoliceForce> {
	private static final String DISTANCE_KEY = "clear.repair.distance";
	private int distance;
	private ArrayList<Integer> clearedIDs = new ArrayList<Integer>();
	public static int MAX_DIST_FOR_HELP = 30000;
	public String InAddress = null;
	public int cvSayingDist = 30000;
	public ArrayList<Building> builsOfP = new ArrayList<Building>();
	public boolean isNeedToMakeAllPathes = false;

	int myTarget = -1;
	int heardTarget = -1;
	int FIRST_ATTACK = 70;
	int SECOND_ATTACK = 200;
	boolean haveReachableRefuge = false;
	ArrayList<Building> sookhtePoses = new ArrayList<Building>();

	ArrayList<EntityID> path = new ArrayList<EntityID>();

	int nearAgentPOS = -1;

	public EntityID target = null;

	ArrayList<PoliceForce> policeHayeAzadRefactor = new ArrayList<PoliceForce>();
	ArrayList<PoliceForce> policeHayeAzad = new ArrayList<PoliceForce>();
	Zone[] newZones;
	public ArrayList<FireZone> modelZones = null;
	private FireZone zone = null;

	ArrayList<EntityID> pathM = new ArrayList<EntityID>();

	Path pBuild = null;
	Building endb = null;

	public Path hadaf = null;
	public ArrayList<Area> hadafHa = null;
	public PathArray rahHayeMan = new PathArray();
	public Area hadafFeliMan = null;
	boolean areAllBuildingsChecked = false;
	Building minB = null;

	int timeForBackingToHisPath = 0;
	int minLTS = -1;

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	protected void postConnect() {
		super.postConnect();

		distance = config.getIntValue("clear.repair.distance");

		if (map != MapType.VC && map != MapType.Kobe)
			MAX_DIST_FOR_HELP = 50000;
		littleZoneMaking(10);

		if (newStructure)
			for (Path p : hameyeRahHa.pathes)
				p.setNeighbours(model, logger, hameyeRahHa);
	}

	public void updateDistFromSelfWOB() {
		wg.clearAreas();
		Area area = (Area) me().getPosition(model);
		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		layer.addAll(area.worldGraphArea.enterances);
		for (Enterance e : layer)
			e.mark = true;
		area.worldGraphArea.distFromeSelfWOB = 0;
		int dist = 1;

		while (layer.size() > 0) {
			ArrayList<Enterance> newLayer = new ArrayList<Enterance>();
			for (Enterance enterance : layer) {
				if (!enterance.neighbour.mark) {
					enterance.neighbour.area.distFromeSelfWOB = dist;
					enterance.neighbour.mark = true;

					for (Enterance internal : enterance.neighbour.internalEnterances)
						if (!internal.mark) {
							newLayer.add(internal);
							internal.mark = true;
						}
				}
			}
			dist++;
			layer = newLayer;
		}
	}

	protected void decide() throws ActionCommandException {
		updateDistFromSelfWOB();

		if (me().hasBuriedness)
			rest();

		if (wg.myEnterances.size() == 0)
			fastClear((Area) me().getPosition(model));

		zoning();
		for (int i = 0; i < sookhteBuildings.size(); i++) {
			Building bu = sookhteBuildings.get(i);
			if (!bu.isOnFire()) {
				sookhteBuildings.remove(bu);
				i--;
				continue;
			}
			if (findOutIfTheFireIsNeededToClear(bu)
					&& !sookhtePoses.contains(bu)) {
				sookhtePoses.add(bu);
			}
		}

		log("clearStuck");
		clearStuckAgents();

		log("helpnearStuck");
		helpNearStuckAgents(MAX_DIST_FOR_HELP);

		if (!isNeedToMakeAllPathes) {
			selectPathArray();
		}
		log("clearTarget");
		clearTargets();

		if ((map == MapType.Kobe || map == MapType.VC) && !noCommi) {
			log("clearRoad");
			clearRoads(true, 0, false);
		}

		log("clearBuilding");
		clearBuildings();

		if (noCommi) {
			checkForRemainBuildingsInTheMap();
		} else {
			log("time for backing to his path: " + timeForBackingToHisPath + " " + minLTS);
			if (timeForBackingToHisPath == 20) {
				areAllBuildingsChecked = true;
				timeForBackingToHisPath = 0;
				minLTS = time;
			}
			if (!areAllBuildingsChecked) {
				checkForRemainBuildingsInTheMap();
			}
		}
		if (minLTS == -1) {
			minLTS = Integer.MAX_VALUE;
			for (Path p : rahHayeMan.pathes)
				for (Area a : p.way)
					if (a.lastTimeSeen < minLTS)
						minLTS = a.lastTimeSeen;
			minLTS++;
		}
		clearRoads(false, minLTS, true);

		areAllBuildingsChecked = false;
		isNeedToMakeAllPathes = true;

		decide();
	}

	public void clearStuckAgents() throws ActionCommandException {
		if (me().stuckTarget != null && !addToClearedIds(me().stuckTarget)) {
			nearAgentPOS = -1;
			fastClear(((PoliceForce) me()).stuckTarget);
		}
	}

	public boolean addToClearedIds(Area area) {
		if (clearedIDs.contains(area.getID().getValue())) {
			log("inja dare true mifreste: ");
			return true;
		}
		if ((isInChangeSet(area.getID()))
				&& ((!(area instanceof Road)) || (!area.isBlockadesDefined()) || (area
						.getBlockades().size() == 0))
				&& (isReachable(area.getID()))) {
			path = wg.getMinPath(((PoliceForce) me()).getPosition(),
					area.getID(), false);
			if (path != null)
				for (int i = path.size() - 1; i >= 0; i--)
					if (((Area) model.getEntity((EntityID) path.get(i))).lastTimeSeen < 0)
						return false;
			clearedIDs.add(Integer.valueOf(area.getID().getValue()));
			return true;
		}

		return false;
	}

	public void helpNearStuckAgents(int distance) throws ActionCommandException {
		int cvDist = distance;
		int refugeDist = distance;
		int sookhteDist = distance;
		if (!lowCommi && !noCommi) {
			distance = 0;
		}

		Area area = null;
		if (nearAgentPOS != -1) {
			area = (Area) model.getEntityByInt(nearAgentPOS);
			if (addToClearedIds(area)) {
				nearAgentPOS = -1;
			}
		}

		if (nearAgentPOS == -1) {
			ArrayList<Integer> targets = new ArrayList<Integer>();
			for (Area nhpPos : sookhtePoses)
				if (!clearedIDs.contains(nhpPos.getID().getValue())
						&& Mathematic.getDistance(nhpPos.getX(), nhpPos.getY(),
								((Human) me()).getX(), ((Human) me()).getY()) < sookhteDist)
					targets.add(nhpPos.getID().getValue());

			for (int nhpPos : longnHP.values())
				if (!clearedIDs.contains(nhpPos)
						&& Mathematic.getDistance(
								((Area) model.getEntityByInt(nhpPos)).getX(),
								((Area) model.getEntityByInt(nhpPos)).getY(),
								((Human) me()).getX(), ((Human) me()).getY()) < distance)
					targets.add(nhpPos);

			for (Entity civil : model
					.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
				Entity position = model.getEntity(((Civilian) civil)
						.getPosition());
				if (position instanceof Building
						&& !clearedIDs.contains(position.getID().getValue())
						&& !((Building) position).isOnFire()
						&& ((Civilian) civil).deadtime - time > 10) {
					double distFromMe = Math.hypot(me().getX()
							- ((Civilian) civil).getX(), me().getY()
							- ((Civilian) civil).getY());
					if (distFromMe < cvDist)
						targets.add(position.getID().getValue());
				}

			}
			for (Entity entity : model.getObjectsInRange(((Human) me()).getX(),
					((Human) me()).getY(), refugeDist))
				if (entity instanceof Refuge
						&& !clearedIDs.contains(entity.getID().getValue()))
					targets.add(entity.getID().getValue());

			int minDist = Integer.MAX_VALUE;
			for (int a : targets) {
				Area e = (Area) model.getEntityByInt(a);
				if (!addToClearedIds(e)) {
					int steps = e.worldGraphArea.distFromeSelfWOB;
					if (minDist > steps) {
						minDist = steps;
						nearAgentPOS = a;
					}
				}
			}
		}
		if (nearAgentPOS != -1) {
			int badal = nearAgentPOS;
			Entity mainTarg = model.getEntityByInt(nearAgentPOS);
			if (((mainTarg instanceof Building))
					&& (((Building) mainTarg).isOnFire())) {
				ArrayList<EntityID> path = new ArrayList<EntityID>();
				path.addAll(wg.getMinPath(((PoliceForce) me()).getPosition(),
						mainTarg.getID(), false));
				if (path.size() > 1)
					badal = ((EntityID) path.get(path.size() - 2)).getValue();
			}
			Area a = (Area) model.getEntityByInt(badal);
			ArrayList<EntityID> pathD = wg.getMinPathDijk(a.getID(), false);
			log("jaeie ke mikham too hepNearStuck pak konam "
					+ a.getID().getValue());
			if ((pathD == null) || (pathD.size() <= 3))
				fastClear(a);
			else
				fastClear(a);
		}
	}

	public void printBlockade(Blockade b) {
		String line = " ";
		for (int i = 0; i < b.getApexes().length - 2; i += 2) {
			line = line + "line(" + b.getApexes()[i] + ", "
					+ b.getApexes()[(i + 1)] + ")(" + b.getApexes()[(i + 2)]
					+ ", " + b.getApexes()[(i + 3)] + ") 4; ";
		}
	}

	private boolean heardCV() {
		boolean shenidam = false;
		if (heard.size() > 0) {
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& (entity instanceof Civilian)) 
					shenidam = true;
			}
		}
		return shenidam;
	}

	private ArrayList<Building> getDoriBuilds() {
		ArrayList<Building> doram = new ArrayList<Building>();
		EntityID myPosID = me().getPosition();
		for (StandardEntity e : model.getObjectsInRange(myPosID, cvSayingDist)) {
			if (e instanceof Building) {
				if (((Building) e).lastTimeVisit < 0
						&& (((Building) e).isFierynessDefined() && ((Building) e)
								.getFieryness() == 0)
						|| !((Building) e).isFierynessDefined()) {
					double dist = Math.hypot(((Building) e).getX()
							- me().getX(), ((Building) e).getY() - me().getY());
					if (dist < 30000) {
						log("ino add konam: "
								+ ((Building) e).getID().getValue());
						doram.add((Building) e);
					}
				}
			}
		}
		return doram;
	}

	private void SclearArea(Blockade nearestBlock, Point endPoint, int rad,
			Area areaOfEndPoint) throws ActionCommandException {
		log("endpoint: " + endPoint);
		if (!isInChangeSet(nearestBlock.getID())) {
			ArrayList<EntityID> wayTONearBlock = wg
					.getMinPath(
							wg.myEnterances,
							((Area) model.getEntity(nearestBlock.getPosition())).worldGraphArea,
							false);
			if (wayTONearBlock.size() == 0)
				path.add(me().getPosition());
			move(wayTONearBlock, nearestBlock.getX(), nearestBlock.getY(),
					false);
		}
		double distToEndPoint = Math.hypot(endPoint.getX() - me().getX(),
				endPoint.getY() - me().getY());
		Vector2D v = new Vector2D(endPoint.getX() - me().getX(),
				endPoint.getY() - me().getY());
		v = v.normalised().scale(Math.min(distToEndPoint, distance - 50));
		Vector2D v2 = new Vector2D(v.getX() + me().getX(), v.getY()
				+ me().getY());
		Point pointForClear = new Point((int) v2.getX(), (int) v2.getY());

		Set<worldGraph.Blockade> badBlockades = ((Area) model
				.getEntity(nearestBlock.getPosition())).worldGraphArea
				.getBadBlockades(new Point(me().getX(), me().getY()),
						pointForClear, rad);
		log("point For clear: "
				+ pointForClear
				+ " block.pas: "
				+ ((Area) model.getEntity(nearestBlock.getPosition())).getID()
						.getValue());
		if (badBlockades.size() > 0) {
			if (distToEndPoint < distance) {
				v = v.normalised().scale(distance);
				endPoint.setX((int) (v.getX() + me().getX()));
				endPoint.setY((int) (v.getY() + me().getY()));
			}
			log("my point: " + me().getX() + " " + me().getY() + " endpoint: "
					+ endPoint);
			clearArea(endPoint.getX(), endPoint.getY());
		} else {
			move(areaOfEndPoint.getID(), false);
		}
	}

	public void fastClear(Area target) throws ActionCommandException {
		log("target: " + target.getID().getValue());
		ArrayList<EntityID> way = wg.getMinPathDijk(target.getID(), false);
		int x = target.getX();
		int y = target.getY();
		int rad = 500;

		Point startPoint = new Point(me().getX(), me().getY());
		int minDistToBlock = Integer.MAX_VALUE;
		Blockade nearestBlock = null;
		log("way.size: " + way.size() + " " + (Area) me().getPosition(model)
				+ " " + way.get(0));
		if (way.size() <= 1) {
			for (worldGraph.Blockade badBlockade : ((Area) model.getEntity(me()
					.getPosition())).worldGraphArea.blockades)
				if (minDistToBlock > findDistanceTo(badBlockade.modelBlockade,
						me().getX(), me().getY())) {
					minDistToBlock = findDistanceTo(badBlockade.modelBlockade,
							me().getX(), me().getY());
					nearestBlock = badBlockade.modelBlockade;
				}
			if (nearestBlock != null)
				Sclear(nearestBlock);
			else
				move(way, x, y, true);
		}

		Enterance endEnter = null;
		for (Enterance e : ((Area) model.getEntity(me().getPosition())).worldGraphArea.enterances) {
			if (e.neighbour.area.modelArea.getID().getValue() == model
					.getEntity(way.get(1)).getID().getValue()) {
				endEnter = e;
				break;
			}
		}
		Point endPoint = new Point(endEnter.realCenter.getX(),
				endEnter.realCenter.getY());
		Area areaOfMyPos = (Area) model.getEntity(me().getPosition());
		Set<worldGraph.Blockade> badBlockadesOfMyPos = areaOfMyPos.worldGraphArea
				.getBadBlockades(startPoint, endPoint, rad);
		minDistToBlock = Integer.MAX_VALUE;
		nearestBlock = null;
		for (worldGraph.Blockade badBlockade : badBlockadesOfMyPos) {
			if (minDistToBlock > findDistanceTo(badBlockade.modelBlockade, me()
					.getX(), me().getY())) {
				minDistToBlock = findDistanceTo(badBlockade.modelBlockade, me()
						.getX(), me().getY());
				nearestBlock = badBlockade.modelBlockade;
			}
		}
		if (nearestBlock != null) {
			SclearArea(nearestBlock, endPoint, rad,
					(Area) model.getEntity(way.get(1)));
		}

		for (int i = 1; i <= 5; i++) {
			if (way.size() > i + 1) {
				startPoint = endPoint;
				Entity dest = model.getEntity((EntityID) way.get(i));
				Entity dest2 = model.getEntity((EntityID) way.get(i + 1));
				if (dest instanceof Area && dest2 instanceof Area) {
					Area area = (Area) dest;
					Area nextArea = (Area) dest2;
					for (Enterance e : area.worldGraphArea.enterances)
						if (e.neighbour.area.modelArea.getID().getValue() == nextArea
								.getID().getValue()) {
							endPoint = new Point(e.realCenter.getX(),
									e.realCenter.getY());
							break;
						}
					Set<worldGraph.Blockade> badBlockades = area.worldGraphArea
							.getBadBlockades(startPoint, endPoint, rad);
					minDistToBlock = Integer.MAX_VALUE;
					nearestBlock = null;
					for (worldGraph.Blockade badBlockade : badBlockades)
						if (minDistToBlock > findDistanceTo(
								badBlockade.modelBlockade,
								((PoliceForce) me()).getX(),
								((PoliceForce) me()).getY())) {
							minDistToBlock = findDistanceTo(
									badBlockade.modelBlockade,
									((PoliceForce) me()).getX(),
									((PoliceForce) me()).getY());
							nearestBlock = badBlockade.modelBlockade;
						}
					if (nearestBlock != null)
						SclearArea(nearestBlock, endPoint, rad, nextArea);
				}
			} else {
				minDistToBlock = Integer.MAX_VALUE;
				nearestBlock = null;
				for (worldGraph.Blockade blockade : ((Area) model.getEntity(way
						.get(i))).worldGraphArea.blockades) {
					if (minDistToBlock > findDistanceTo(blockade.modelBlockade,
							me().getX(), me().getY())) {
						minDistToBlock = findDistanceTo(blockade.modelBlockade,
								me().getX(), me().getY());
						nearestBlock = blockade.modelBlockade;
					}
				}
				if (nearestBlock != null)
					Sclear(nearestBlock);
				break;
			}

		}
		move(way, x, y, true);
	}

	private void clear(int end, boolean checkAllBlocks)
			throws ActionCommandException {
		isSafeBlockadesNeeded = isSafeBlockadesNeeded || checkAllBlocks;
		Collection<Blockade> blockades = ((Area) model
				.getEntity(((PoliceForce) me()).getPosition())).worldGraphArea
				.getBadBlockades(model, ((PoliceForce) me()).getX(),
						((PoliceForce) me()).getY(), 500, isSafeBlockadesNeeded);
		if (blockades.size() != 0) {
			Blockade blockade = (Blockade) blockades.iterator().next();
			Sclear(blockade);
			return;
		}
		ArrayList<EntityID> way = wg.getMinPathDijk(model.getEntityByInt(end)
				.getID(), false);
		for (int i = 1; i <= 4; i++)
			if (way.size() > i) {
				Entity dest = model.getEntity((EntityID) way.get(i));
				if ((dest instanceof Area)) {
					Area area = (Area) dest;
					Collection<Blockade> badBlockades = area.worldGraphArea
							.getBadBlockades(model,
									((PoliceForce) me()).getX(),
									((PoliceForce) me()).getY(), 500,
									isSafeBlockadesNeeded);
					for (Blockade badBlockade : badBlockades) {
						if (findDistanceTo(badBlockade,
								((PoliceForce) me()).getX(),
								((PoliceForce) me()).getY()) < distance) {
							Sclear(badBlockade);
							return;
						}
					}
				}
			}
		int minDistFromBlockade = Integer.MAX_VALUE;
		Blockade targBlockade = null;
		for (Road r : ((Area) model.getEntity(me().getPosition())).nearRoads) {
			Entity roadEntity = ((Area) model.getEntity(r.getID()));
			if (((Area) roadEntity).isBlockadesDefined()) {
				Blockade blockade = null;
				for (Blockade b : ((Area) roadEntity).worldGraphArea
						.getBadBlockades(model, me().getX(), me().getY(), 500,
								isSafeBlockadesNeeded)) {
					blockade = b;
					double d = findDistanceTo(blockade, me().getX(), me()
							.getY());
					if (d < distance / 4 && isInChangeSet(blockade.getID())
							&& d < minDistFromBlockade) {
						minDistFromBlockade = (int) d;
						targBlockade = blockade;
					}
				}
			}
		}
		if (targBlockade != null)
			clear(targBlockade.getID());
		if (way.size() == 0)
			return;
		move(way, true);
	}

	private void Sclear(Blockade b) throws ActionCommandException {
		ArrayList<EntityID> path = wg
				.getMinPath(
						wg.myEnterances,
						((Area) model.getEntity(b.getPosition())).worldGraphArea,
						false);
		if (path.size() == 0)
			path.add(me().getPosition());
		double dist = model.getDistance(me(), b);
		for (int i = 2; i < b.getApexes().length; i += 2) {
			Point2D po = new Point2D(
					(b.getApexes()[i - 2] + b.getApexes()[i]) / 2,
					(b.getApexes()[i - 1] + b.getApexes()[i + 1]) / 2);
			double tmpDist = GeometryTools2D.getDistance(new Point2D(me()
					.getX(), me().getY()), po);
			if (tmpDist < dist) {
				dist = tmpDist;
			}
		}
		Point2D po = new Point2D(
				(b.getApexes()[0] + b.getApexes()[b.getApexes().length - 2]) / 2,
				(b.getApexes()[1] + b.getApexes()[b.getApexes().length - 1]) / 2);
		double tmpDist = GeometryTools2D.getDistance(new Point2D(me().getX(),
				me().getY()), po);
		if (tmpDist < dist) {
			dist = tmpDist;
		}

		if (dist < distance && isInChangeSet(b.getID())) {
			clear(b.getID());
		} else
			move(path, b.getX(), b.getY(), false);
	}

	private int findDistanceTo(Blockade b, int x, int y) {
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
		return (int) best;
	}

	public void selectPathArray() {
		boolean hasChanged = false;
		boolean sizesh0e = false;
		if (policeHayeAzadRefactor.size() == 0)
			sizesh0e = true;
		if (policeStatus.size() > 0) {
			for (int i = 0; i < policeStatus.size(); i++)
				if (policeStatus.get(i) != -1) {
					if (sizesh0e) {
						policeHayeAzadRefactor.add(i, modelPoliceForces.get(i));
						hasChanged = true;
						log("1");
					} else if (!policeHayeAzadRefactor
							.contains(modelPoliceForces.get(i))) {
						log("koodomm mongoli?: " + modelPoliceForces.get(i).getID().getValue() + " " + policeStatus.get(i));
						policeHayeAzadRefactor.set(i, modelPoliceForces.get(i));
						log("2");
						hasChanged = true;
					}
				} else if (sizesh0e) {
					log("3");
					hasChanged = true;
					policeHayeAzadRefactor.add(i, null);
				} else if (policeHayeAzadRefactor.contains(modelPoliceForces
						.get(i))) {
					log("4");
					hasChanged = true;
					policeHayeAzadRefactor.set(i, null);
				}
		} else {
			for (int i = 0; i < modelPoliceForces.size(); i++) {
				PoliceForce pf = modelPoliceForces.get(i);
				if (!pf.hasBuriedness || noCommi) {
					if (sizesh0e) {
						policeHayeAzadRefactor.add(i, pf);
						log("5");
						hasChanged = true;
					} else if (!policeHayeAzadRefactor
							.contains(modelPoliceForces.get(i))) {
						policeHayeAzadRefactor.set(i, pf);
						log("6");
						hasChanged = true;
					}
				} else if (sizesh0e) {
					policeHayeAzadRefactor.add(i, null);
					log("7");
					hasChanged = true;
				} else if (policeHayeAzadRefactor.contains(modelPoliceForces
						.get(i))) {
					policeHayeAzadRefactor.set(i, null);
					log("8");
					hasChanged = true;
				}
			}

		}
		policeHayeAzad = new ArrayList<PoliceForce>();
		for (PoliceForce pf : policeHayeAzadRefactor)
			if (pf != null)
				policeHayeAzad.add(pf);
		if (!policeHayeAzadRefactor.contains(me())) {
			rahHayeMan = new PathArray();
			rahHayeMan.pathes.addAll((ArrayList<Path>) hameyeRahHa.pathes
					.clone());
		} else if (hasChanged) {
			ArrayList<PathArray> newPathArrays = new ArrayList<PathArray>();
			if (newStructure) {
				newPathArrays = vaghTtoGeryeMikoni(policeHayeAzad.size());
				for (PathArray patA : newPathArrays)
					patA.setCenter();
			} else {
				newSetMyZone(policeHayeAzad);
				for (Path p : hameyeRahHa.pathes) {
					PathArray pa = new PathArray();
					pa.pathes.add(p);
					pa.centerX = p.centralX;
					pa.centerY = p.centralY;
					newPathArrays.add(pa);
				}
			}
			HungarianAlgorithm ha = new HungarianAlgorithm();
			ha.execute(newPathArrays, policeHayeAzad);

			int myPathNum = ha.getMyPathNum(policeHayeAzad.indexOf(me()));
			rahHayeMan = newPathArrays.get(myPathNum);
			rahHayeMan.pathNums = new ArrayList<Integer>();
			for (Path p : rahHayeMan.pathes)
				rahHayeMan.pathNums.add(p.pathNum);
			areAllBuildingsChecked = true;
			timeForBackingToHisPath = 0;
			minLTS = time;
		}
	}

	protected void newSetMyZone(ArrayList<PoliceForce> polices) {
		numberOfZones = Math.min(5, (polices.size() + 1) / 2);

		newZoning(numberOfZones);
		int maghsom = polices.size();
		int maghsomonelayh = zones.length;
		int kharejeGhesmat = maghsom / maghsomonelayh;
		int baghimande = maghsom - (kharejeGhesmat * maghsomonelayh);

		for (Zone z : zones) {
			if (z.zoneNumber < baghimande)
				z.dedicatedto = kharejeGhesmat + 1;
			else
				z.dedicatedto = kharejeGhesmat;
		}
		for (Zone z : zones)
			z.setSubZonesForPolice(z.dedicatedto);
		hameyeRahHa = new PathArray();
		for (Zone z : zones)
			subZoningForPolice(z.zoneNumber, z.subZones.length);
		for (Zone z : zones)
			for (SubZone sz : zones[z.zoneNumber].subZones) {
				Path p = new Path();
				for (Area a : sz.areas)
					if (a instanceof Road)
						p.way.add(a);
					else if (a instanceof Building)
						p.buildings.add((Building) a);
				p.setCenter();
				hameyeRahHa.pathes.add(p);
			}
		setPathNumber();
	}

	private void newZoning(int zonesCount) {
		zones = new Zone[zonesCount];
		for (int i = 0; i < zonesCount; i++)
			zones[i] = new Zone(i);
		for (Area area : modelAreas) {
			float theta = (new Line(centerOfMap, new Point(area.getX(),
					area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;
			int zoneNumber = (int) (theta / (360.0 / zonesCount));
			area.worldGraphArea.ZoneNumberForPolice = zoneNumber;
			zones[zoneNumber].areas.add(area);
			if (area instanceof Building)
				zones[zoneNumber].buildings.add((Building) area);
			else if (area instanceof Road)
				zones[zoneNumber].roads.add((Road) area);
		}
		for (Zone z : zones) {
			long allX = 0, allY = 0;
			int tedad = 0;
			for (Area area : z.areas) {
				allX += area.getX();
				allY += area.getY();
				tedad++;
			}
			z.center = new Point((int) (allX / tedad), (int) (allY / tedad));
		}
	}

	public void subZoningForPolice(int zoneNumber, int subZonesCount) {
		for (Area area : zones[zoneNumber].areas) {
			float theta = (new Line(zones[zoneNumber].center, new Point(
					area.getX(), area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;
			int subZoneNumber = (int) (theta / (360.0 / subZonesCount));
			zones[zoneNumber].subZones[subZoneNumber].areas.add(area);
			if (area instanceof Building)
				zones[zoneNumber].subZones[subZoneNumber].buildings
						.add((Building) area);
			else if (area instanceof Road)
				zones[zoneNumber].subZones[subZoneNumber].roads
						.add((Road) area);
		}
	}

	protected void setPathNumber() {
		Collections.sort(hameyeRahHa.pathes, new Comparator<Path>() {
			public int compare(Path a, Path b) {
				Point p = a.way.get(0).worldGraphArea.getCenterPoint();
				Point p2 = b.way.get(0).worldGraphArea.getCenterPoint();
				Point mainPoint = new Point(-1, -1);
				double dist1 = p.getDistance(mainPoint);
				double dist2 = p2.getDistance(mainPoint);
				if (dist1 < dist2)
					return 1;
				else if (dist2 < dist1)
					return -1;
				return 0;
			}
		});
		int i = 0;
		for (Path p : hameyeRahHa.pathes) {
			for (Area a : p.way)
				a.pathNum = i;
			for (Building b : p.buildings)
				b.pathNum = i;
			i++;
		}
	}

	public ArrayList<PathArray> vaghTtoGeryeMikoni(int tedadPolica) {
		ArrayList<PathArray> aghaPoliceMehraboon = new ArrayList<PathArray>();

		PathArray onZoninge = subZoningForCenters(policeHayeAzad);
		for (Path p : hameyeRahHa.pathes)
			p.isChosen = false;
		for (Path o : onZoninge.pathes)
			o.isChosen = true;
		for (Path pat : onZoninge.pathes) {
			PathArray o = new PathArray();
			o.pathes.add(pat);
			o.pathNums.add(pat.pathNum);
			aghaPoliceMehraboon.add(o);
		}

		int size = 1;
		for (int i = 0; i < size; i++)
			for (PathArray paAsli : aghaPoliceMehraboon)
				if (i < paAsli.pathes.size()) {
					for (Path pn : ((Path) paAsli.pathes.get(i)).neighbours)
						if (!pn.isChosen) {
							paAsli.pathes.add(pn);
							paAsli.pathNums.add(Integer.valueOf(pn.pathNum));
							pn.isChosen = true;
						}
					if (size < paAsli.pathes.size())
						size = paAsli.pathes.size();
				}
		return aghaPoliceMehraboon;
	}

	public PathArray zoningForCenters(int tedadPolica) {
		newZones = new Zone[tedadPolica];
		for (Area area : modelAreas) {
			float theta = (new Line(centerOfMap, new Point(area.getX(),
					area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;
			int zoneNumber = (int) (theta / (360.0 / tedadPolica));
			if (newZones[zoneNumber] == null)
				newZones[zoneNumber] = new Zone(zoneNumber);
			newZones[zoneNumber].areas.add(area);
			if (area instanceof Building)
				newZones[zoneNumber].buildings.add((Building) area);
		}
		PathArray ghablesh = new PathArray();
		for (Zone z : newZones) {
			int allX = 0, allY = 0, tedad = 0;
			for (Area area : z.areas) {
				allX += area.getX();
				allY += area.getY();
				tedad++;
			}
			z.center = new Point(allX / tedad, allY / tedad);
			double minDist = Double.MAX_VALUE;
			Path minP = null;
			for (Path p : hameyeRahHa.pathes)
				p.chosenForBFS = false;
			for (Path p : hameyeRahHa.pathes)
				if (Math.hypot(p.centralX - z.center.getX(), p.centralY
						- z.center.getY()) < minDist
						&& !p.chosenForBFS) {
					minDist = Math.hypot(p.centralX - z.center.getX(),
							p.centralY - z.center.getY());
					minP = p;
					p.chosenForBFS = true;
				}
			ghablesh.pathes.add(minP);
		}
		return ghablesh;
	}

	protected PathArray subZoningForCenters(ArrayList<PoliceForce> polices) {
		numberOfZones = Math.min(5, (polices.size() + 1) / 2);

		newZoning(numberOfZones);
		int maghsom = polices.size();
		int maghsomonelayh = zones.length;
		int kharejeGhesmat = maghsom / maghsomonelayh;
		int baghimande = maghsom - (kharejeGhesmat * maghsomonelayh);

		for (Zone z : zones) {
			if (z.zoneNumber < baghimande)
				z.dedicatedto = kharejeGhesmat + 1;
			else
				z.dedicatedto = kharejeGhesmat;
		}
		for (Zone z : zones)
			z.setSubZonesForPolice(z.dedicatedto);
		for (Zone z : zones)
			subZoningForPolice(z.zoneNumber, z.subZones.length);
		PathArray ghablesh = new PathArray();
		for (Zone z : zones)
			for (SubZone sz : z.subZones) {
				int allX = 0, allY = 0, tedad = 0;
				for (Area area : sz.areas) {
					allX += area.getX();
					allY += area.getY();
					tedad++;
				}
				sz.center = new Point(allX / tedad, allY / tedad);
			}
		for (Path p : hameyeRahHa.pathes)
			p.chosenForBFS = false;
		for (Zone z : zones)
			for (SubZone sz : z.subZones) {
				double minDist = Double.MAX_VALUE;
				Path minP = null;
				for (Path p : hameyeRahHa.pathes)
					if (Math.hypot(p.centralX - sz.center.getX(), p.centralY
							- sz.center.getY()) < minDist
							&& !p.chosenForBFS) {
						minDist = Math.hypot(p.centralX - sz.center.getX(),
								p.centralY - sz.center.getY());
						minP = p;
					}
				ghablesh.pathes.add(minP);
				minP.chosenForBFS = true;
			}
		return ghablesh;
	}

	public boolean findOutIfTheFireIsNeededToClear(Building bu) {
		for (Building b : modelZones.get(bu.zoneNumber).buildings)
			if (sookhtePoses.contains(b))
				return false;
		return true;
	}

	public void zoning() {
		modelZones = new ArrayList<FireZone>();
		for (Building isCheckingbuilding : modelBuildings) {
			isCheckingbuilding.zoneNumber = -1;
			isCheckingbuilding.haveZone = false;
		}
		for (Building isCheckingbuilding : modelBuildings) {
			if (isCheckingbuilding.isOnFire()) {
				if (!isCheckingbuilding.haveZone) {
					zone = new FireZone(modelZones.size(), me().getID()
							.getValue());
					checkingNeighboursForZoning(isCheckingbuilding);
					modelZones.add(zone);
				}
			}
		}
	}

	private void checkingNeighboursForZoning(Building isChecking) {
		isChecking.zoneNumber = zone.zoneNum;
		zone.buildings.add(isChecking);
		isChecking.haveZone = true;
		for (Building neighbour : isChecking.nearBuildings)
			if ((neighbour.isFierynessDefined() && neighbour.getFieryness() != 0)
					&& !neighbour.haveZone)
				checkingNeighboursForZoning(neighbour);
	}

	public boolean isInMyPathArray(Entity e) {
		boolean b = false;
		Area a = (Area) e;
		if (rahHayeMan.pathNums.contains(a.pathNum))
			b = true;
		return b;
	}

	public void clearTargets() throws ActionCommandException {
		ArrayList<EntityID> targs = new ArrayList<EntityID>();

		for (Area nhPE : sookhtePoses) {
			if (!addToClearedIds(nhPE) && isInMyPathArray(nhPE))
				targs.add(nhPE.getID());
		}
		if (lowCommi || noCommi)
			for (int nhP : longnHP.values()) {
				Area nhpE = (Area) model.getEntityByInt(nhP);
				if ((!addToClearedIds(nhpE)) && (isInMyPathArray(nhpE)))
					targs.add(nhpE.getID());
			}
		if (targs.size() > 0) {
			int target = -1;
			int minDist = Integer.MAX_VALUE;
			for (int a = 0; a < targs.size(); a++) {
				Area e = (Area) model.getEntity(targs.get(a));
				int dist = e.worldGraphArea.distFromeSelfWOB;
				if (minDist > dist) {
					minDist = dist;
					target = e.getID().getValue();
				}
			}

			Entity mainTarg = model.getEntityByInt(target);
			if (mainTarg instanceof Building
					&& ((Building) mainTarg).isFierynessDefined()
					&& ((Building) mainTarg).isOnFire()) {
				ArrayList<EntityID> path = new ArrayList<EntityID>();
				path.addAll(wg.getMinPathDijk(mainTarg.getID(), false));
				if (path.size() > 2) {
					target = path.get(path.size() - 2).getValue();
				}
			}
			Area a = (Area) model.getEntityByInt(target);
			fastClear(a);
		}

		ArrayList<Refuge> myPathRefuges = new ArrayList<Refuge>();
		for (Refuge r : modelRefuges) {
			if (r.getID().getValue() == 24504)
				log("isinMYPathArray: " + isInMyPathArray(r));
			if (isInMyPathArray(r)) {
				myPathRefuges.add(r);
				if (addToClearedIds(r)) {
					haveReachableRefuge = true;
					break;
				}
			}
		}
		if (model.getEntity(me().getPosition()) instanceof Refuge) {
			haveReachableRefuge = true;
		} else if (!haveReachableRefuge && myPathRefuges.size() > 0) {
			for (Refuge r : myPathRefuges)
				wg.setTarget(r.worldGraphArea);
			ArrayList<EntityID> path = wg
					.getMinPath(
							((Area) model.getEntity(me().getPosition())).worldGraphArea.enterances,
							false);
			fastClear((Area) model.getEntity(path.get(path.size() - 1)));
		}
	}

	public void clearRoads(boolean realLTS, int KamineyeLTS,
			boolean checkAllBlocks) throws ActionCommandException {
		PathArray remainPathesForRoads = new PathArray();
		for (Path myPath : rahHayeMan.pathes) {
			for (Area a : myPath.way) {
				int lts = a.lastTimeSeen;
				if (realLTS)
					lts = a.realLastTimeSeen;
				if (a.getID().getValue() == 654)
					log("lts: " + lts);
				if (lts < KamineyeLTS
						|| a.worldGraphArea.enterances.get(0).internalEnterances
								.size() != a.worldGraphArea.enterances.size() - 1) {
					remainPathesForRoads.pathes.add(myPath);
					break;
				}
			}
		}
		if (!remainPathesForRoads.pathes.contains(hadaf))
			hadaf = null;

		if (hadaf != null)
			clear(hadaf, realLTS, KamineyeLTS, checkAllBlocks);

		if (remainPathesForRoads.pathes.size() != 0) {
			int nearestWay = Integer.MAX_VALUE;
			for (Path path : remainPathesForRoads.pathes) {
				int dist = Math
						.min(path.way.get(0).worldGraphArea.distFromeSelfWOB,
								path.way.get(path.way.size() - 1).worldGraphArea.distFromeSelfWOB);
				if (dist < nearestWay) {
					nearestWay = dist;
					hadaf = path;
				}
			}
		}

		if (hadaf != null)
			clear(hadaf, realLTS, KamineyeLTS, checkAllBlocks);
	}

	public void clearBuildings() throws ActionCommandException {
		if (endb != null) {
			if (endb.lastTimeVisit >= 0
					|| (endb.isFierynessDefined() && endb.getFieryness() > 0))
				endb = null;
			else
				fastClear(endb);
		}
		PathArray remainPathesForBuildings = new PathArray();
		for (Path myPath : rahHayeMan.pathes)
			for (Building b : myPath.buildings)
				if (b.lastTimeVisit < 0
						&& (!b.isFierynessDefined() || b.getFieryness() == 0)) {
					remainPathesForBuildings.pathes.add(myPath);
					break;
				}

		if (!remainPathesForBuildings.pathes.contains(pBuild))
			pBuild = null;
		if (pBuild == null && remainPathesForBuildings.pathes.size() != 0) {
			int nearestPath = Integer.MAX_VALUE;
			for (Path path : remainPathesForBuildings.pathes) {
				int dist = Math
						.min(path.way.get(0).worldGraphArea.distFromeSelfWOB,
								path.way.get(path.way.size() - 1).worldGraphArea.distFromeSelfWOB);
				if (dist < nearestPath) {
					nearestPath = dist;
					pBuild = path;
				}
			}
		}

		if (builsOfP != null && builsOfP.size() != 0) {
			for (int i = 0; i < builsOfP.size(); i++) {
				if (builsOfP.get(i).lastTimeVisit > 0
						|| (builsOfP.get(i).isFierynessDefined() && builsOfP
								.get(i).getFieryness() > 0)) {
					builsOfP.remove(i);
					i--;
				}
			}
		}

		if (builsOfP != null && builsOfP.size() != 0) {
			int dist = -1;
			int lastDist = Integer.MAX_VALUE;
			for (int k = 0; k < builsOfP.size(); k++) {
				dist = builsOfP.get(k).worldGraphArea.distFromeSelfWOB;
				if (lastDist > dist) {
					endb = builsOfP.get(k);
					lastDist = dist;
				}
			}
			fastClear(endb);
		} else if (heardCV()) {
			builsOfP = getDoriBuilds();
			int dist = -1;
			int lastDist = Integer.MAX_VALUE;
			for (int k = 0; k < builsOfP.size(); k++) {
				dist = builsOfP.get(k).worldGraphArea.distFromeSelfWOB;
				if (lastDist > dist) {
					endb = builsOfP.get(k);
					lastDist = dist;
				}
			}
			if (endb != null)
				fastClear(endb);
		} else if (pBuild != null && endb == null) {
			ArrayList<Building> builsOfP2 = new ArrayList<Building>();
			for (int j = 0; j < pBuild.buildings.size(); j++)
				if (pBuild.buildings.get(j).lastTimeVisit < 0
						&& (!pBuild.buildings.get(j).isFierynessDefined() || pBuild.buildings
								.get(j).getFieryness() == 0))
					builsOfP2.add(pBuild.buildings.get(j));
			int dist2 = -1;
			int lastDist2 = Integer.MAX_VALUE;
			for (int k = 0; k < builsOfP2.size(); k++) {
				dist2 = builsOfP2.get(k).worldGraphArea.distFromeSelfWOB;
				if (lastDist2 > dist2) {
					endb = builsOfP2.get(k);
					lastDist2 = dist2;
				}
			}
			fastClear(endb);
		}
	}

	public void clear(Path path, boolean realLTS, int kamineyeLastTimeSeen,
			boolean checkAllBlocks) throws ActionCommandException {
		ArrayList<Area> moshkelDarHa = new ArrayList<Area>();
		for (Area a : path.way) {
			int lts = 0;
			if (realLTS)
				lts = a.realLastTimeSeen;
			else
				lts = a.lastTimeSeen;
			if (lts < kamineyeLastTimeSeen
					|| a.worldGraphArea.enterances.get(0).internalEnterances
							.size() != a.worldGraphArea.enterances.size() - 1) {
				if (a.getID().getValue() == 654)
					log("dare add mishe be moshkeldar ha "
							+ kamineyeLastTimeSeen + " lts: " + lts);
				moshkelDarHa.add(a);
			}
		}
		if (!moshkelDarHa.contains(hadafFeliMan)
				|| (isInChangeSet(hadafFeliMan.getID()) && hadafFeliMan.worldGraphArea.enterances
						.get(0).internalEnterances.size() == hadafFeliMan.worldGraphArea.enterances
						.size() - 1))
			hadafFeliMan = null;
		if (hadafFeliMan == null) {
			double g = Double.MAX_VALUE;
			for (Area a : moshkelDarHa) {
				double gHere = Math.hypot(a.getX() - me().getX(), a.getY()
						- me().getY());
				if (gHere < g) {
					gHere = g;
					hadafFeliMan = a;
				}
			}
		}
		fastClear(hadafFeliMan);
	}

	public void checkForRemainBuildingsInTheMap() throws ActionCommandException {
		int minLTV = Integer.MAX_VALUE;
		for (Building b : modelBuildings)
			if (b.lastTimeVisit < minLTV)
				minLTV = b.lastTimeVisit;
		double minDist = Double.MAX_VALUE;
		double dist = -1;
		if (minB != null
				&& (minB.lastTimeVisit > minLTV || (minB.isFierynessDefined() && minB
						.getFieryness() > 0)))
			minB = null;
		if (minB == null)
			for (Building b : modelBuildings)
				if (b.lastTimeVisit == minLTV
						&& (!b.isFierynessDefined() || b.getFieryness() == 0)) {
					dist = Math.hypot(((PoliceForce) me()).getX() - b.getX(),
							((PoliceForce) me()).getY() - b.getY());
					if (dist < minDist) {
						minDist = dist;
						minB = b;
					}
				}
		if (minB != null) {
			timeForBackingToHisPath++;
			fastClear(minB);
		}
		areAllBuildingsChecked = true;
		minLTS = -1;
	}
}
