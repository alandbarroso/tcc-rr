package agent;

import geometry.Degree;
import geometry.Line;
import geometry.Mathematic;
import geometry.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeSet;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import worldGraph.Blockade;
import worldGraph.Enterance;
import agent.Agent.MapType;

//import rescuecore2.standard.entities.Human;

public class FireBrigadeAgent2012 {
	int maxWater = -1;
	int maxPower = -1;
	private Building extTarget;
	private Building beingExtinguished;
	private Building myBuilding;
	private FireZone myZone = null;
	private LittleZone mySearchZone = null;
	private LittleZone mySearchAroundZone = null;
	public Point centralPointOfMap = new Point();
	public boolean scannerError = false;
	public boolean bigMap = true;
	public boolean salam = false;
	private boolean searchingAroundZone = false;
	public boolean IamOnfire = false;
	private boolean realRandom = false;
	public Random random = new Random(123);
	private ArrayList<FireZone> zoneChanging;

	@SuppressWarnings({ "rawtypes" })
	public FireBrigadeAgent agent = null;

	// protected void postConnect() {
	// super.postConnect();
	// random = new Random(me().getID().getValue());
	// maxWater = config.getIntValue(MAX_WATER_KEY);
	// maxPower = config.getIntValue(MAX_POWER_KEY);
	// System.out.println("PreCompuation of Fire Started");
	//
	// long before = System.nanoTime();
	//
	// String fileName = "./Configs/Fire-" + mapHashCode() + ".bin";
	// boolean success = true;
	// ObjectInputStream in = null;
	// try {
	// in = new ObjectInputStream(new FileInputStream(new File(fileName)));
	// ArrayList<HashMap<Integer, ArrayList<Point>>> all =
	// (ArrayList<HashMap<Integer, ArrayList<Point>>>) in
	// .readObject();
	// Iterator<Building> iterator = modelBuildings.iterator();
	// for (HashMap<Integer, ArrayList<Point>> roads : all) {
	// Building building = iterator.next();
	// for (Entry<Integer, ArrayList<Point>> e : roads.entrySet())
	// building.visionRoads.put((Road) allRanks.get(e.getKey()),
	// e.getValue());
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// success = false;
	// } finally {
	// if (in != null)
	// try {
	// in.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// System.out.println("Success: " + success);
	// if (!success) {
	// for (Building building : modelBuildings) {
	// for (Road road : building.nearRoads) {
	// ArrayList<Point> visionPoints = getVisionPoints(road,
	// building,
	// Math.min(MAX_DISTANCE_VIEW, maxDistanceExtingiush));
	// if (visionPoints != null && visionPoints.size() != 0)
	// building.visionRoads.put(road, visionPoints);
	// }
	// for (EntityID entityId : building.getNeighbours()) {
	// Entity entity = model.getEntity(entityId);
	// if (entity instanceof Road) {
	// Road r = (Road) entity;
	// if (!building.visionRoads.containsKey(r)) {
	// ArrayList<Point> rPoint = new ArrayList<Point>();
	// Point point = new Point(r.getX(), r.getY());
	// rPoint.add(point);
	// building.visionRoads.put(r, rPoint);
	// }
	// }
	// }
	// }
	// }
	//
	// long after = System.nanoTime();
	// System.out.println("Time: " + (after - before) / 1000000L);
	// visionBuildings();
	// littleZoneMaking(15);
	// searchZones = new ArrayList<LittleZone>();
	// searchZones.addAll(newZone);
	// System.out.println("PreCompuation of Fire Finished");
	// }
	//
	// public void scanner() {
	// try {
	// Scanner scanner;
	// try {
	// scanner = new Scanner(new File("./Configs/My-File.txt"));
	// if (modelBuildings.size() == 1426 && modelRoads.size() == 3385)
	// scanner = new Scanner(new File("./Configs/In-Berlin.txt"));
	// else if (modelBuildings.size() == 1244
	// && modelRoads.size() == 3337)
	// scanner = new Scanner(new File("./Configs/In-Istanbul.txt"));
	// else if (modelBuildings.size() == 736
	// && modelRoads.size() == 1515)
	// scanner = new Scanner(new File("./Configs/In-Kobe.txt"));
	// else if (modelBuildings.size() == 1618
	// && modelRoads.size() == 3025)
	// scanner = new Scanner(new File("./Configs/In-Paris.txt"));
	// else if (modelBuildings.size() == 1263
	// && modelRoads.size() == 1954)
	// scanner = new Scanner(new File("./Configs/In-VC.txt"));
	// salam = true;
	// ArrayList<Area> hamunModelAreas = (ArrayList<Area>) modelAreas
	// .clone();
	// Collections.sort(hamunModelAreas, areaSorter);
	// while (scanner.hasNext()) {
	// int n = scanner.nextInt();
	// ArrayList<Building> builOfZone = new ArrayList<Building>();
	// for (int i = 0; i < n; i++) {
	// int id = scanner.nextInt();
	// if (hamunModelAreas.get(id) instanceof Building) {
	// Building b = (Building) hamunModelAreas.get(id);
	// builOfZone.add(b);
	// }
	// }
	// LittleZone littleZone = new LittleZone();
	// littleZone.buildings.addAll(builOfZone);
	// if (littleZone.buildings.size() != 0) {
	// littleZone.id = searchZones.size();
	// searchZones.add(littleZone);
	// }
	// }
	// } catch (FileNotFoundException e) {
	// salam = false;
	// e.printStackTrace();
	// }
	// } catch (Exception e) {
	// salam = false;
	// e.printStackTrace();
	// if (logger.getWriter() != null) {
	// System.out.println("me : " + me().getID().getValue());
	// e.printStackTrace(logger.getWriter());
	// }
	// }
	// }

	private void beforeSearch() throws ActionCommandException {
		int shomarande = 0;
		ArrayList<Building> reachableBuildings = agent.reachableBuildings;
		for (Building building : reachableBuildings)
			if (building.lastTimeVisit <= -1 && building.stFire == 0)
				shomarande++;
		if (agent.reachableBuildings.size() < 10 || shomarande == 0) {
			Area targetToGo = null;
			ArrayList<Area> toBeSeen = new ArrayList<Area>();
			toBeSeen.addAll(agent.reachableRoads);
			for (Building building : reachableBuildings)
				if (building.nearBuildings.size() != 0 && building.stFire == 0)
					toBeSeen.add(building);
			int minLTV = Integer.MAX_VALUE;
			for (Area area : toBeSeen) {
				if (area.lastTimeVisit < minLTV) {
					targetToGo = area;
					minLTV = area.lastTimeVisit;
				}
			}
			if (targetToGo != null)
				agent.move(targetToGo.getID());
		}
	}

	// private LittleZone selectNearestZoneForSearch(ArrayList<LittleZone> allZ)
	// {
	// int x = random.nextInt(allZ.size());
	// LittleZone lz = allZ.get(x);
	// lz.searchBuildings = (ArrayList<Building>) lz.buildings.clone();
	// return lz;
	// }

	private LittleZone selectZoneForSearch(ArrayList<LittleZone> allZ) {
		ArrayList<LittleZone> behtarinZones = new ArrayList<LittleZone>();
		double m = 0.0001;
		while (behtarinZones.size() == 0) {
			for (LittleZone littleZone : allZ)
				if (littleZone.percent <= m)
					behtarinZones.add(littleZone);
			if (m >= 1)
				break;
			m += 0.2;
		}
		for (LittleZone littleZone : behtarinZones)
			littleZone.priority = 5 - distToFire(littleZone.buildings.get(0).worldGraphArea.distanceFromSelf);
		ArrayList<LittleZone> saving = new ArrayList<LittleZone>();
		for (LittleZone littlezone : behtarinZones) {
			for (int i = 0; i < littlezone.priority; i++)
				saving.add(littlezone);
		}
		behtarinZones.addAll(saving);
		agent.log("      hoooofffff    " + behtarinZones.size());
		int x = random.nextInt(behtarinZones.size());
		agent.log("XXX:   " + x);
		LittleZone lz = behtarinZones.get(x);
		lz.searchBuildings = (ArrayList<Building>) lz.buildings.clone();
		return lz;
	}

	private int akhir = 0;
	private int[] position = new int[3];
	private Building tar = null;
	private Building hey = null;
	private Building searchAroundTar = null;
	private int akhir1 = 0, targetCounter = 0;

	private void search() throws ActionCommandException {

		int minlts1 = Integer.MAX_VALUE;
		int mindist = Integer.MAX_VALUE;
		ArrayList<Building> minLastTimeSeen = new ArrayList<Building>();
		ArrayList<Building> reachableBuildVisionRoads = new ArrayList<Building>();
		Building tar2 = null;
		for (Building building : agent.modelBuildings)
			if (buildingReachability(building)) {
				reachableBuildVisionRoads.add(building);
			}
		for (Building building : reachableBuildVisionRoads)
			if (building.lastTimeSeen <= minlts1) {
				minlts1 = building.lastTimeSeen;
			}
		for (Building building : reachableBuildVisionRoads) {
			if (building.lastTimeSeen == minlts1)
				minLastTimeSeen.add(building);
		}
		for (Building building : minLastTimeSeen) {
			if (building.worldGraphArea.distanceFromSelf <= mindist) {
				mindist = building.worldGraphArea.distanceFromSelf;
				tar2 = building;
			}
		}
		if (hey == null || !buildingReachability(hey)
				|| (tar2 != null && tar2.lastTimeSeen < hey.lastTimeSeen))
			hey = tar2;

		if (hey != null)
			moveToFire(hey);
		agent.randomWalk();
	}

	int seenBuildings = 0;

	private void newSearch() throws ActionCommandException {
		agent.log("IN NEwSearch  ");
		// for (LittleZone littleZone : searchZones) {
		// int countSeen = 0;
		// for (Building building : littleZone.buildings)
		// if (building.realLastTimeSeen > -1)
		// countSeen++;
		// littleZone.percent = countSeen / littleZone.buildings.size();
		// }
		if (mySearchZone != null) {
			for (int i = 0; i < mySearchZone.searchBuildings.size(); i++)
				if (agent.isInChangeSet(mySearchZone.searchBuildings.get(i)
						.getID())) {
					seenBuildings++;
					mySearchZone.searchBuildings.remove(i);
					i--;
				}
			for (int i = 0; i < mySearchZone.searchBuildings.size(); i++)
				if (!buildingReachability(mySearchZone.searchBuildings.get(i))) {
					mySearchZone.searchBuildings.remove(i);
					i--;
				}
		}
		beingExtinguished = null;
		extTarget = null;
		if (mySearchZone == null
				|| mySearchZone.searchBuildings.size() == 0
				|| (mySearchZone.owner != agent.me().getID().getValue() && mySearchZone.owner != -1)
				|| mySearchZone.isEnd) {
			tar = null;
			if (mySearchZone != null
					&& mySearchZone.owner == agent.me().getID().getValue()
					&& mySearchZone.buildings.size() == seenBuildings) {
				agent.sendEmptyBuildings(mySearchZone.id, true);
			}
			seenBuildings = 0;
			ArrayList<LittleZone> allZ = new ArrayList<LittleZone>();
			for (LittleZone littleZone : agent.searchZones) {
				for (Building building : littleZone.buildings)
					if (buildingReachability(building)) {
						if (!littleZone.isChooseByFire)
							for (int i = 0; i < 3; i++)
								allZ.add(littleZone);
						else
							allZ.add(littleZone);
						break;
					}
			}
			ArrayList<LittleZone> allUnChosenSearchZones = new ArrayList<LittleZone>();
			ArrayList<LittleZone> allChosenSearchZones = new ArrayList<LittleZone>();
			ArrayList<LittleZone> allSearchZones = new ArrayList<LittleZone>();
			for (LittleZone littleZone : allZ)
				if (!littleZone.isEnd) {
					if (littleZone.owner == -1)
						allUnChosenSearchZones.add(littleZone);
					else
						allChosenSearchZones.add(littleZone);
				} else
					allSearchZones.add(littleZone);
			mySearchZone = null;
			if (allUnChosenSearchZones.size() != 0) {
				mySearchZone = selectZoneForSearch(allUnChosenSearchZones);
				// if (time < 20
				// || (commandHistory.get(time - 1).getAction()
				// .equals(StandardMessageURN.AK_EXTINGUISH) || (realRandom &&
				// !searchingAroundZone)))
				// selectNearestZoneForSearch(allUnChosenSearchZones);
				// else
				// search();
			} else if (allSearchZones.size() != 0) {
				mySearchZone = selectZoneForSearch(allSearchZones);
				// if (time < 20
				// || (commandHistory.get(time - 1).getAction()
				// .equals(StandardMessageURN.AK_EXTINGUISH) || (realRandom &&
				// !searchingAroundZone)))
				// mySearchZone =
				// selectNearestZoneForSearch(allUnChosenSearchZones);
				// else
				// search();
			} else if (allChosenSearchZones.size() != 0) {
				mySearchZone = selectZoneForSearch(allChosenSearchZones);
				// if (time < 20
				// || (commandHistory.get(time - 1).getAction()
				// .equals(StandardMessageURN.AK_EXTINGUISH) || (realRandom &&
				// !searchingAroundZone)))
				// selectNearestZoneForSearch(allUnChosenSearchZones);
				// else
				// search();
			}
			// TODO tavajoh
			if (mySearchZone == null) {
				agent.log(" search rest ");
				agent.rest();
			}
			mySearchZone.isChooseByFire = true;
			agent.sendEmptyBuildings(mySearchZone.id, false);
		}
		moveToSearch();
	}

	public void writer() {
		System.out.println("Writer");
		try {
			PrintWriter writer = new PrintWriter(new File(
					"/home/poseidon/Desktop/MyFile.txt"));
			for (LittleZone zone : agent.newZone) {
				for (Building building : zone.buildings)
					writer.print(" " + building.getID().getValue());
				writer.println(" ; ");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Point> getVisionPoints(Area area, Building building,
			int minMax) {
		return getVisionPoints(agent.model, area, building,
				agent.maxDistanceExtingiush, minMax);
	}

	public static ArrayList<Point> getVisionPoints(StandardWorldModel model,
			Area area, Building building, int maxDistance, int minMax) {
		maxDistance *= 4;
		float minDegree = Integer.MAX_VALUE, maxDegree = -Integer.MAX_VALUE;
		ArrayList<Point> roadPoints = area.worldGraphArea.points;
		ArrayList<Point> visionPoints = new ArrayList<Point>();
		Point buildingPoint = new Point(building.getX(), building.getY());
		Point minPoint = null, maxPoint = null;
		for (int i = 0; i < roadPoints.size() - 1; i++) {
			Line line = new Line(buildingPoint, roadPoints.get(i));
			float degree = Degree.absoluteAngle(line.getTheta());

			if (i == 0) {
				minDegree = maxDegree = degree;
				minPoint = maxPoint = roadPoints.get(i);
			} else {
				if (Degree.isClockWise(roadPoints.get(i - 1), buildingPoint,
						roadPoints.get(i))) {
					if (Degree.isClockWise(minPoint, buildingPoint,
							roadPoints.get(i))) {
						minDegree = degree;
						minPoint = roadPoints.get(i);
					}
				} else {
					if (!Degree.isClockWise(maxPoint, buildingPoint,
							roadPoints.get(i))) {
						maxDegree = degree;
						maxPoint = roadPoints.get(i);
					}
				}
			}
		}
		int m = 3;
		if (area.worldGraphArea.points.size() < 7)
			m = 1;

		ArrayList<Building> nearBuildings = new ArrayList<Building>();
		for (Area a : building.nearAreas50000)
			if (a instanceof Building)
				nearBuildings.add((Building) a);
		float deg1 = Degree.absoluteAngle((minDegree + maxDegree) / 2);
		for (int j = 0; j < m; j++) {
			if (j == 1)
				deg1 = Degree.absoluteAngle((minDegree + maxDegree) / 2
						- (Degree.getDeltaAngle(maxDegree, minDegree)) / 4);
			if (j == 2)
				deg1 = Degree.absoluteAngle((minDegree + maxDegree) / 2
						+ (Degree.getDeltaAngle(maxDegree, minDegree)) / 4);
			float deg2 = Degree.absoluteAngle((float) Math.PI + deg1);
			float degree = deg1;
			Point tmpPoint = new Point(building.getX() + 10
					* (int) Math.cos(deg2), building.getY() + 10
					* (int) Math.sin(deg2));
			if (Degree.isClockWise(tmpPoint, buildingPoint, minPoint)
					&& !Degree.isClockWise(tmpPoint, buildingPoint, maxPoint))
				degree = deg2;
			int x = (int) (building.getX() + maxDistance * Math.cos(degree));
			int y = (int) (building.getY() + maxDistance * Math.sin(degree));
			Line buildingToRoad = new Line(buildingPoint, new Point(x, y));
			float minDist = Integer.MAX_VALUE, maxDist = -Integer.MAX_VALUE;
			for (int i = 0; i < roadPoints.size() - 1; i++) {
				Line line = new Line(roadPoints.get(i), roadPoints.get(i + 1));
				Point intersectPoint = buildingToRoad.getIntersectPoint(line);
				if (intersectPoint != null) {
					float dist = intersectPoint.getDistance(buildingPoint);
					if (dist < minDist)
						minDist = dist;
					if (dist > maxDist)
						maxDist = dist;
				}
			}
			if (maxDist < 0)
				continue;
			if (minDist > minMax)
				continue;
			maxDist = Math.min(maxDist, minMax);
			for (int i = 0; i < 3; i++) {
				float dist = (minDist + maxDist) / 2;
				if (i == 1)
					dist = maxDist - 500; // TAVAJJOH
				if (i == 2)
					dist = minDist + 500; // TAVAJJOH
				int newX = (int) (building.getX() + dist * Math.cos(degree));
				int newY = (int) (building.getY() + dist * Math.sin(degree));
				Point newPoint = new Point(newX, newY);
				buildingToRoad.setSecondPoint(newPoint);

				boolean hasIntersect = false;
				done: for (Building bld : nearBuildings)
					for (Edge edge : bld.getEdges())
						if (edge.getNeighbour() == null) {
							Line lineEdge = new Line(new Point(
									edge.getStartX(), edge.getStartY()),
									new Point(edge.getEndX(), edge.getEndY()));
							if (Line.isIntersectBetweenLines(buildingToRoad,
									lineEdge)) {
								hasIntersect = true;
								break done;
							}
						}

				if (!hasIntersect)
					visionPoints.add(buildingToRoad.getSecondPoint());
			}
		}

		return visionPoints;
	}

	public EntityID target = null;

	private void checkWater() throws ActionCommandException {
		double minDist = Integer.MAX_VALUE;
		Refuge minRefuge = null;
		int minlts = Integer.MAX_VALUE;
		if (agent.modelRefuges.size() == 0
				&& ((FireBrigade) agent.me()).getWater() == 0)
			newSearch();
		else {
			if (agent.model.getEntity(((Human) agent.me()).getPosition()) instanceof Refuge
					&& ((FireBrigade) agent.me()).getWater() != maxWater) {
				myZone = null;
				if (extTarget != null)
					// agent.sendCheck(extTarget.getID().getValue(), false);
					extTarget = null;
				beingExtinguished = null;
				agent.log(" refuge ");
				agent.rest();
			}

			if (((FireBrigade) agent.me()).getWater() == 0) {
				Refuge refuge = null;
				if (agent.reachableRefuges != null
						&& agent.reachableRefuges.size() != 0)
					for (Refuge refuge2 : agent.reachableRefuges) {
						if (Math.hypot((agent.me()).getX() - refuge2.getX(),
								(agent.me()).getY() - refuge2.getY()) <= minDist) {
							minDist = Math.hypot(
									(agent.me()).getX() - refuge2.getX(),
									(agent.me()).getY() - refuge2.getY());
							minRefuge = refuge2;
						}
					}
				else {
					for (Refuge refuge2 : agent.modelRefuges) {
						if (refuge2.lastTimeSeen < minlts) {
							minlts = refuge2.lastTimeSeen;
							minRefuge = refuge2;
						}
					}
					if (extTarget != null) {
						// sendCheck(extTarget.getID().getValue(), false);
						extTarget = null;
					}
					beingExtinguished = null;
					agent.move(minRefuge.getID(), false);
				}

				refuge = minRefuge;
				if (refuge != null) {
					if (extTarget != null) {
						// sendCheck(extTarget.getID().getValue(), false);
						extTarget = null;
					}
					beingExtinguished = null;
					agent.move(refuge.getID());
				}
				newSearch();
			}
		}
	}

	public static final double w1 = 100, w2 = 5, w3 = -2.5, w4 = -5;

	private ArrayList<Building> reachableAndIgnitedBuildings = new ArrayList<Building>();
	public int counter = 10;

	private Building selectTarget() {
		if (myZone == null)
			return null;
		if (extTarget == null || !buildingReachability(extTarget)
				|| extTarget.stFire == 0 || counter >= 5) {
			counter = 0;

			double minevalue = Double.MAX_VALUE;
			Building minBuil = null;
			ArrayList<Building> aroundReachable = new ArrayList<Building>();
			for (Building building : myZone.aroundBuildings)
				if (buildingReachability(building))
					aroundReachable.add(building);
			if (aroundReachable.size() != 0) {
				for (Building buil : aroundReachable)
					if ((minevalue == Double.MAX_VALUE || buil.value < minevalue)) {
						minevalue = buil.value;
						minBuil = buil;
					}
			} else {
				FireZone tmpZone = new FireZone(myZone.zoneNum, agent.me()
						.getID().getValue());
				for (Building building : myZone.ignitedBuilds)
					if (buildingReachability(building))
						tmpZone.ignitedBuilds.add(building);
				tmpZone.time = agent.time;
				tmpZone.logger = agent.logger;
				// TODO oh oh!
				if (tmpZone.ignitedBuilds.size() != 0)
					tmpZone.setAroundBuildings(bigMap);
				for (Building building : tmpZone.aroundBuildings)
					setValueForBuildings(building);
				if (tmpZone.aroundBuildings.size() != 0)
					for (Building building : tmpZone.aroundBuildings) {

						if ((minevalue == Double.MAX_VALUE || building.value < minevalue)
								&& buildingReachability(building)) {
							minevalue = building.value;
							minBuil = building;
						}
					}
				else
					minBuil = null;
			}

			return minBuil;
		}
		return extTarget;
	}

	private boolean buildingReachability(Building building) {
		return agent.buildingReachability(building);
	}

	private void moveAndExtinguish(Building fireTarget)
			throws ActionCommandException {
		agent.log(" FireTarget: " + fireTarget.getID().getValue());
		if (fireTarget != null) {
			((FireBrigadeAgent) agent).beforeExt(fireTarget);
			if (agent.isInChangeSet(fireTarget.getID())
					&& canExtinguish(fireTarget) && !IamOnfire) {
				if (myZone.buildings.contains(fireTarget))
					myZone.ExtinguishedByMe = true;
				beingExtinguished = fireTarget;
				agent.extinguish(fireTarget.getID(), maxPower);
			}
			moveToFire(fireTarget);
		}
	}

	private void setZoneSurface() {
		for (FireZone fireZone : modelZones) {
			for (Building building : fireZone.ignitedBuilds)
				fireZone.valueForWorkwerNum += building.getGroundArea()
						/ agent.averageOfSurfaces;
		}
	}

	private void setWorkersNum() {
		for (FireZone fireZone : modelZones) {
			if (fireZone.valueForWorkwerNum > 22)
				fireZone.workersNum = 15;
			if (fireZone.valueForWorkwerNum <= 17)
				fireZone.workersNum = 9;
			if (fireZone.valueForWorkwerNum <= 12)
				fireZone.workersNum = 6;
			if (fireZone.valueForWorkwerNum <= 7)
				fireZone.workersNum = 3;
			if (agent.map == MapType.Berlin || agent.map == MapType.Istanbul
					|| agent.map == MapType.Paris)
				fireZone.workersNum *= 2;
		}
	}

	public int countReachableBuilds = 0;

	public void simulator(Building building) {
		for (Building building2 : building.nearBuildings) {
			if (building2.stFire == 0) {
				building.nearForSimulator.add(building2);
			}

		}
	}

	public void setVolumeSimulator(Building buil) {
		buil.volume = 0;
		for (Building hf : buil.nearForSimulator) {
			buil.volume += hf.getFloors() * hf.getGroundArea()
					/ agent.maxVolumeOfAllBuildings;
		}
	}

	public boolean zoneChange() throws ActionCommandException {
		if (myZone == null)
			return true;
		ArrayList<FireZone> copy = new ArrayList<FireZone>();
		boolean tof = myZone.ExtinguishedByMe;
		FireZone myZoneCopy = myZone;
		FireZone lastZone = myZone;
		selectMyZone(true);
		copy.addAll(modelZones);
		if (modelZones.size() == 0)
			myZoneCopy = null;
		if (copy.size() != zoneChanging.size()) {
			extTarget = null;
			return true;
		}
		for (FireZone fireZone : zoneChanging) {
			int counter = 0;
			for (int i = 0; i < copy.size(); i++)
				if (copy.get(i).buildings.contains(fireZone.buildings.get(0))) {
					if (agent.map == MapType.Berlin
							|| agent.map == MapType.Paris
							|| agent.map == MapType.Istanbul) {
						if (Math.abs(fireZone.assignedWorkerNum
								- copy.get(i).assignedWorkerNum) > 21) {
							extTarget = null;
							return true;
						}
					} else if (Math.abs(fireZone.assignedWorkerNum
							- copy.get(i).assignedWorkerNum) > 7) {
						extTarget = null;
						return true;
					}
					counter++;
					if ((fireZone.buildings.get(0)).getID().getValue() == myBuilding
							.getID().getValue())
						myZoneCopy = copy.get(i);
					copy.remove(i);
					i--;
				}
			if (counter == 0) {
				extTarget = null;
				return true;
			}
		}
		if (!lastZone.isInhibitor && myZoneCopy.isInhibitor) {
			extTarget = null;
			return true;
		}
		myZone = myZoneCopy;
		myZone.ExtinguishedByMe = tof;

		return false;
	}

	private boolean zoneFinish() {
		for (FireZone fireZone : modelZones)
			if (fireZone.buildings.contains(myBuilding)) {
				return false;
			}
		return true;
	}

	int timeSearch = 0;

	protected void decide() throws ActionCommandException {
		agent.log(" Decide Fire 2012 hooooooooooooooo!!");
		if (agent.me().hasBuriedness)
			agent.rest();
		IamOnfire = false;
		realRandom = false;
		zoneChanging = new ArrayList<FireZone>();
		Entity myPos = agent.model
				.getEntity(((Human) agent.me()).getPosition());
		if (myPos instanceof Building && ((Building) myPos).stFire > 0) {
			IamOnfire = true;
		}
		ArrayList<Building> isReachableAndNotIgnited = new ArrayList<Building>();
		for (Building building : agent.modelBuildings) {
			if (building.worldGraphArea.isReachable && building.stFire == 0)
				isReachableAndNotIgnited.add(building);
		}
		if (agent.reachableRoads.size() == 0
				&& isReachableAndNotIgnited.size() == 0)
			IamOnfire = false;
		if (agent.map == MapType.Kobe || agent.map == MapType.VC)
			bigMap = false;
		for (EntityID e : agent.changeSet.getChangedEntities())
			if (agent.model.getEntity(e) instanceof Building) {
				Building building = (Building) agent.model.getEntity(e);
				if (building.stFire > 0)
					for (Building b : building.nearBuildings)
						if (b.lastTimeSeen < 0 && (b.stFire == 0))
							b.lastTimeSeen = Agent.LTSDORI;
			}

		initialBuildings();
		for (Area ar : agent.modelAreas)
			ar.worldGraphArea.fB.clear();
		for (FireBrigade fire : agent.modelFireBrigades) {
			if (fire.getID().getValue() != agent.me().getID().getValue()
					&& fire.hasBuriedness)
				continue;
			worldGraph.Area area = (((Area) ((StandardWorldModel) agent.model)
					.getEntity(fire.getPosition())).worldGraphArea);
			area.fB.add(fire);
		}
		if (modelZones != null)
			zoneChanging = modelZones;
		if (myZone != null)
			myBuilding = myZone.buildings.get(0);
		Building buil = (Building)agent.model.getEntityByInt(41684);

		zoning();

		for (FireZone fireZone : modelZones) {
			fireZone.time = agent.time;
			fireZone.logger = agent.logger;
			fireZone.setAroundBuildings(bigMap);
		}
		zoneMatching();
		for (Building building : agent.modelBuildings)
			building.nearForSimulator.clear();
		setValueForAroundBuildings();
		if (modelZones.size() != 0)
			setZoneCenter();
		setZoneSurface();
		setWorkersNum();
		for (FireZone fireZone : modelZones) {
			if (fireZone.ignitedBuilds.size() <= Integer
					.parseInt(agent.psdConfig.get("Fire", "Zone",
							"InhibiterBuildingsCount")))
				fireZone.isInhibitor = true;
		}
		if (!searchingAroundZone && myZone != null
				&& myZone.buildings.size() <= 12 && zoneFinish()
				&& myZone.ExtinguishedByMe) {
			timeSearch = agent.time;
			searchingAroundZone = true;
			setSearchAroundZone();
			myZone = null;
		}
		if (searchingAroundZone && !zoneFinish())
			searchingAroundZone = false;
		int x = 8;
		if (bigMap)
			x = 15;
		if (searchingAroundZone && !agent.amIStucking
				&& agent.time - timeSearch < x) {
			if (mySearchAroundZone != null) {
				for (int i = 0; i < mySearchAroundZone.searchBuildings.size(); i++)
					if (agent.isInChangeSet(mySearchAroundZone.searchBuildings
							.get(i).getID())) {
						mySearchAroundZone.searchBuildings.remove(i);
						i--;
					}
				for (int i = 0; i < mySearchAroundZone.searchBuildings.size(); i++)
					if (!buildingReachability(mySearchAroundZone.searchBuildings
							.get(i))) {
						mySearchAroundZone.searchBuildings.remove(i);
						i--;
					}
			}
			// if (mySearchAroundZone.searchBuildings.size() != 0)
			// moveToSearchAroundZone(mySearchAroundZone);
			// else {
			// searchingAroundZone = false;
			// realRandom = true;
			// }
		}
		FireZone exZone = null;
		for (FireZone fz : modelZones) {
			for (Building b : fz.buildings) {
				if (agent.me().aim == b) {
					exZone = myZone;
					break;
				}
			}
		}

		if (agent.noCommi && modelZones.size() != 0)
			selectMyZoneForNoCommi();
		else if (modelZones.size() != 0
				&& (myZone == null || myZone.emergency || zoneChange()))
			selectMyZone(false);
		if (modelZones.size() == 0)
			myZone = null;
		if (exZone != null
				&& myZone != null
				&& ((myZone.isInhibitor && !exZone.isInhibitor) || (extTarget != null
						&& !myZone.ignitedBuilds.contains(extTarget) && myZone.value < exZone.value))) {
			extTarget = null;
			beingExtinguished = null;
		}
		if (myZone != null)
			counter++;
		checkEmergencySituation();
		if (agent.amIStucking == true) {
			agent.log(" randomWalk ");
			agent.randomWalk();
		}
		checkWater();
		// log(" !checkwater ");
		Building lastExtTarget = extTarget;
		Building fireTarget = selectTargetNew();
		if (!bigMap
				&& lastExtTarget != null
				&& (fireTarget == null || lastExtTarget.getID().getValue() != fireTarget
						.getID().getValue()))
			agent.log("first if");
		// agent.sendCheck(lastExtTarget.getID().getValue(), false);
		// if (fireTarget != null
		// && !fireTarget.owners.contains(agent.me().getID().getValue())
		// && !bigMap)
		// agent.sendCheck(fireTarget.getID().getValue(), true);
		agent.log("secound if");
		if (fireTarget != null) {
			agent.log("gereftam");
			extTarget = fireTarget;
			agent.log("daram mirambara extinguish");
			((FireBrigadeAgent) agent).lastZone = modelZones
					.get(fireTarget.zoneNumber);

			agent.log("2012_1");

			((FireBrigadeAgent) agent).ExtinguishInNocom = true;

			for (FireZone z : modelZones)
				z.setAroundBuildings(bigMap);
			if (fireTarget.isTemperatureDefined())
				agent.log("re: " + buildingReachability(fireTarget) + " temp "
						+ fireTarget.getTemperature());
			moveAndExtinguish(fireTarget);
		}
		if (agent.time < 3)
			agent.rest();
		// beforeSearch();
		// newSearch();
	}

	private void setZoneCenter() {
		for (FireZone fireZone : modelZones) {
			int allX = 0, allY = 0;
			for (Building building : fireZone.buildings) {
				allX += building.getX();
				allY += building.getY();
			}
			fireZone.centerX = allX / fireZone.buildings.size();
			fireZone.centerY = allY / fireZone.buildings.size();
		}
	}

	public void zoneMatched(FireZone asli, FireZone farE) {
		for (Building build : farE.buildings) {
			build.zoneNumber = asli.zoneNum;
		}
		asli.buildings.addAll(farE.buildings);
		asli.ignitedBuilds.addAll(farE.ignitedBuilds);
	}

	public void zoneMatching() {
		int y = 0;
		if (agent.map == MapType.Berlin)
			y = 150000;
		if (agent.map == MapType.Paris)
			y = 35000;
		if (agent.map == MapType.Istanbul)
			y = 60000;
		if (bigMap)
			while (true) {
				FireZone toBeRemoved = null;
				FireZone topZone = null;
				done: for (FireZone fireZone : modelZones)
					for (Building building : fireZone.aroundBuildings)
						for (StandardEntity e : agent.model.getObjectsInRange(
								building, y))
							if (e instanceof Building
									&& ((Building) e).stFire > 0
									&& ((Building) e).haveZone
									&& ((Building) e).zoneNumber != building.zoneNumber) {
								toBeRemoved = modelZones
										.get(((Building) e).zoneNumber);
								zoneMatched(fireZone, toBeRemoved);
								topZone = fireZone;
								break done;
							}
				if (toBeRemoved == null)
					break;
				topZone.setAroundBuildings(bigMap);
				modelZones.remove(toBeRemoved);
				int i = 0;
				for (FireZone fz : modelZones) {
					fz.zoneNum = i;
					for (Building b : fz.buildings)
						b.zoneNumber = i;
					i++;
				}
			}
	}

	private void setSearchAroundZone() {
		int a = 10000;
		if (bigMap)
			a = 35000;
		mySearchAroundZone = new LittleZone();
		FireZone copy = new FireZone(-2, agent.me().getID().getValue());
		copy.ignitedBuilds = (ArrayList<Building>) myZone.buildings.clone();
		copy.setAroundBuildings(10, copy.ignitedBuilds);
		for (Building build : copy.aroundBuildings) {
			for (StandardEntity entity : agent.model
					.getObjectsInRange(build, a))
				if (entity instanceof Building
						&& !mySearchAroundZone.searchBuildings
								.contains((Building) entity))
					mySearchAroundZone.searchBuildings.add((Building) entity);
		}
	}

	private Building selectTargetNew() {	
	agent.log("select target : " );
		if (myZone == null || agent.me().hasBuriedness) {
			agent.log("target NUll Buriedness");
			return null;
		}
		if (extTarget != null
				&& (extTarget.stFire == 0
						|| (extTarget.owners.size() >= extTarget.hamraheAval && !extTarget.owners
								.contains(agent.me().getID().getValue())) || !buildingReachability(extTarget))) {
			extTarget = null;
		}
		if (extTarget == null || counter >= 5) {
			counter = 0;
			ArrayList<Building> suitableAround = new ArrayList<Building>();
			String A = "";
			
			for (Building building : myZone.aroundBuildings) {
				A += building.getID().getValue()+" ";
				if (buildingReachability(building)
						&& building.owners.size() < building.hamraheAval) {
					suitableAround.add(building);
				}
			}
			agent.log("Around in 2012 : " + A);
			double minevalue = Double.MAX_VALUE;
			Building minBuil = null;
			if (suitableAround.size() != 0) {
				for (Building buil : suitableAround)
					if ((/* minevalue == Double.MAX_VALUE || */buil.value < minevalue)) {
						minevalue = buil.value;
						minBuil = buil;
					}
			} else {
				agent.log("toye else 2012");
				FireZone tmpZone = new FireZone(myZone.zoneNum, agent.getID()
						.getValue());
				for (Building building : myZone.ignitedBuilds)
					if (buildingReachability(building))
						tmpZone.ignitedBuilds.add(building);
				tmpZone.time = agent.time;
				tmpZone.logger = agent.logger;
				if (tmpZone.ignitedBuilds.size() != 0)
					tmpZone.setAroundBuildings(bigMap);
				for (Building building : tmpZone.aroundBuildings)
					setValueForBuildings(building);
				if (tmpZone.aroundBuildings.size() != 0)
					for (Building building : tmpZone.aroundBuildings) {

						if ((minevalue == Double.MAX_VALUE || building.value < minevalue)
								&& buildingReachability(building)
								&& building.owners.size() < building.hamraheAval) {
							minevalue = building.value;
							minBuil = building;
						}
					}
			}
			if (minBuil != null) {
				agent.log(" target entekhab karde az minbuil");
				return minBuil;
			} else {
				agent.log(" previous select target ");
				return selectTarget();
			}
		}
		agent.log(" exttarget " + extTarget);
		return extTarget;
	}

	private HashSet<FireBrigade> bFSForFire() {
		HashSet<Enterance> laye = new HashSet<Enterance>();
		HashSet<FireBrigade> list = new HashSet<FireBrigade>();
		agent.wg.clearAreas();
		list.add((FireBrigade) agent.me());
		for (Enterance enterance : agent.wg.myEnterances) {
			if (!enterance.area.checkSituation4Fire
					&& enterance.area.fB != null
					&& enterance.area.fB.size() > 0) {
				enterance.area.checkSituation4Fire = true;
				list.addAll(enterance.area.fB);
			}

			laye.add(enterance);
		}
		while (laye.size() > 0) {
			HashSet<Enterance> newLaye = new HashSet<Enterance>();
			for (Enterance enterance : laye) {
				Enterance neighbourE = enterance.neighbour;
				if (!neighbourE.area.checkSituation4Fire
						&& enterance.isItConnectedToNeighbour) {
					if (neighbourE.area.fB != null
							&& neighbourE.area.fB.size() != 0)
						list.addAll(neighbourE.area.fB);
				}
				enterance.mark = true;
				neighbourE.area.checkSituation4Fire = true;
				neighbourE.mark = true;
				for (Enterance internal : neighbourE.internalEnterances)
					if (!internal.mark)
						newLaye.add(internal);
			}

			laye = newLaye;
		}
		return list;
	}

	private void selectMyZoneForNoCommi() throws ActionCommandException {
		// ArrayList<FireZone> zones = new ArrayList<FireZone>();
		// FireZone fireZone1 = null;
		// int minSize = Integer.MAX_VALUE;
		// if (me().hasBuriedness)
		// return;
		// myZone = null;
		// if (modelZones.size() != 0)
		// for (FireZone fireZone : modelZones) {
		// zones.add(fireZone);
		// }
		// else
		// return;
		//
		// for (FireZone fireZone : zones) {
		// if (fireZone.ignitedBuilds.size() < minSize) {
		// minSize = fireZone.ignitedBuilds.size();
		// fireZone1 = fireZone;
		// }
		// }
		// myZone = fireZone1;
		ArrayList<FireZone> hibitorFireZones = new ArrayList<FireZone>();
		for (FireZone firezone : modelZones)
			if (firezone.isInhibitor)
				hibitorFireZones.add(firezone);
		if (hibitorFireZones.size() > 0) {
			for (FireZone fireZone : hibitorFireZones) {
				double distToZone = Double.MAX_VALUE;
				for (Building buil : fireZone.aroundBuildings) {
					if (getDistToFire(buil) < distToZone)
						distToZone = getDistToFire(buil);
				}
				fireZone.distToFire = distToZone;
			}
			Collections.sort(hibitorFireZones, zoneComparatorbydist);
			myZone = hibitorFireZones.get(0);
			if (myZone != null && myZone.emergency)
				unemploymentSituation(myZone);
		} else {
			for (FireZone fireZone : modelZones) {
				double distToZone = Double.MAX_VALUE;
				for (Building buil : fireZone.aroundBuildings) {
					if (getDistToFire(buil) < distToZone)
						distToZone = getDistToFire(buil);
				}
				fireZone.distToFire = distToZone;
			}
			Collections.sort(hibitorFireZones, zoneComparatorbydist);
			myZone = modelZones.get(0);
			if (myZone != null && myZone.emergency)
				unemploymentSituation(myZone);
		}
		return;
	}

	public int noCommiTime = -1;

	public Comparator<FireBrigade> idComparator = new Comparator<FireBrigade>() {
		public int compare(FireBrigade a, FireBrigade b) {
			if (a.getID().getValue() > b.getID().getValue())
				return 1;
			if (a.getID().getValue() < b.getID().getValue())
				return -1;
			return 0;
		}
	};
	public Comparator<FireZone> zoneComparator = new Comparator<FireZone>() {
		public int compare(FireZone a, FireZone b) {
			if (a.value > b.value)
				return 1;
			if (a.value < b.value)
				return -1;
			return 0;
		}
	};
	public Comparator<FireZone> zoneComparatorbydist = new Comparator<FireZone>() {
		public int compare(FireZone a, FireZone b) {
			if (a.distToFire > b.distToFire)
				return 1;
			if (a.distToFire < b.distToFire)
				return -1;
			return 0;
		}
	};

	private int valueForFireZone(double value) {
		int[] arr = { 3, 6, 10, 15, 25 };
		for (int i = 0; i < arr.length; i++)
			if (value <= arr[i])
				return i;

		return arr.length;
	}

	private void selectMyZone(boolean isCalledByZoneChange)
			throws ActionCommandException {
		ArrayList<FireZone> sortedZone = new ArrayList<FireZone>();
		ArrayList<FireBrigade> fireBrigades = new ArrayList<FireBrigade>();
		ArrayList<FireZone> hibitorFireZones = new ArrayList<FireZone>();
		ArrayList<FireZone> otherZones = new ArrayList<FireZone>();
		if (agent.me().hasBuriedness)
			return;
		for (FireBrigade brigade : bFSForFire())
			if (!brigade.hasBuriedness)
				fireBrigades.add(brigade);
		myZone = null;
		for (FireZone fireZone : modelZones) {
			fireZone.value = 0;
			fireZone.assignedWorkerNum = 0;
			for (Building building : fireZone.buildings)
				fireZone.value += building.getGroundArea()
						/ agent.averageOfSurfaces;
			fireZone.value = valueForFireZone(fireZone.value);
		}
		for (FireZone fireZone : modelZones) {
			if (fireZone.isInhibitor)
				hibitorFireZones.add(fireZone);
			else
				otherZones.add(fireZone);
		}

		for (FireBrigade FB : fireBrigades)
			FB.distToFire = new ArrayList<Double>();
		Collections.sort(fireBrigades, idComparator);
		boolean selectNearestHibitor = false;
		if (bigMap) {
			Collections.sort(hibitorFireZones, zoneComparator);
			sortedZone.addAll(hibitorFireZones);
			Collections.sort(otherZones, zoneComparator);
			sortedZone.addAll(otherZones);
			for (FireZone fireZone : sortedZone) {
				for (FireBrigade fireB : fireBrigades) {
					double distToZone = Double.MAX_VALUE;
					for (Building buil : fireZone.aroundBuildings) {
						if (((Double) Math.hypot(buil.getX() - fireB.getX(),
								buil.getY() - fireB.getY())) < distToZone)
							distToZone = ((Double) Math.hypot(buil.getX()
									- fireB.getX(), buil.getY() - fireB.getY()));
					}
					fireB.distToFire.add(distToZone);
				}
			}
			done: for (int g = 0; g < sortedZone.size(); g++) {
				for (int i = 0; i < sortedZone.get(g).workersNum; i++) {
					double min = Double.MAX_VALUE;
					FireBrigade f = null;
					for (int j = 0; j < fireBrigades.size(); j++)
						if (fireBrigades.get(j).distToFire.get(g) < min) {
							min = fireBrigades.get(j).distToFire.get(g);
							f = fireBrigades.get(j);
						}
					if (agent.me().getID().getValue() == f.getID().getValue()) {
						myZone = sortedZone.get(g);
						if (myZone != null && myZone.emergency
								&& !isCalledByZoneChange)
							unemploymentSituation(myZone);
						if (myZone.isInhibitor)
							selectNearestHibitor = true;
					}
					if (f.getID().getValue() != agent.me().getID().getValue()
							|| !selectNearestHibitor) {
						sortedZone.get(g).assignedWorkerNum++;
					}
					fireBrigades.remove(f);
					if (fireBrigades.size() == 0)
						break done;
				}
			}
			if (selectNearestHibitor) {
				double minDist = Double.MAX_VALUE;
				int save = 0;
				for (int i = 0; i < agent.me().distToFire.size(); i++)
					if (agent.me().distToFire.get(i) < minDist
							&& sortedZone.get(i).isInhibitor) {
						minDist = agent.me().distToFire.get(i);
						save = i;
					}
				sortedZone.get(save).assignedWorkerNum++;
				myZone = sortedZone.get(save);
				if (myZone != null && myZone.emergency && !isCalledByZoneChange)
					unemploymentSituation(myZone);
				return;
			}
			if (myZone != null)
				return;
		} else {
			Collections.sort(hibitorFireZones, zoneComparator);
			sortedZone.addAll(hibitorFireZones);
			Collections.sort(otherZones, zoneComparator);
			sortedZone.addAll(otherZones);

			for (FireZone fireZone : sortedZone) {
				for (FireBrigade fireB : fireBrigades) {
					double distToZone = Double.MAX_VALUE;
					for (Building buil : fireZone.aroundBuildings) {
						if (((Double) Math.hypot(buil.getX() - fireB.getX(),
								buil.getY() - fireB.getY())) < distToZone)
							distToZone = ((Double) Math.hypot(buil.getX()
									- fireB.getX(), buil.getY() - fireB.getY()));
					}
					fireB.distToFire.add(distToZone);
				}
			}
			done: for (int g = 0; g < sortedZone.size(); g++) {
				for (int i = 0; i < sortedZone.get(g).workersNum + 2; i++) {
					double min = Double.MAX_VALUE;
					FireBrigade f = null;
					for (int j = 0; j < fireBrigades.size(); j++)
						if (fireBrigades.get(j).distToFire.get(g) < min) {
							min = fireBrigades.get(j).distToFire.get(g);
							f = fireBrigades.get(j);
						}
					if (agent.me().getID().getValue() == f.getID().getValue()) {
						myZone = sortedZone.get(g);
						if (myZone != null && myZone.emergency
								&& !isCalledByZoneChange)
							unemploymentSituation(myZone);
						if (myZone.isInhibitor)
							selectNearestHibitor = true;
					}
					fireBrigades.remove(f);
					if (f.getID().getValue() == f.getID().getValue()
							&& !selectNearestHibitor)
						sortedZone.get(g).assignedWorkerNum++;
					if (fireBrigades.size() == 0)
						break done;
				}
			}
			if (selectNearestHibitor) {
				double minDist = Double.MAX_VALUE;
				int save = 0;
				for (int i = 0; i < agent.me().distToFire.size(); i++)
					if (agent.me().distToFire.get(i) < minDist
							&& sortedZone.get(i).isInhibitor) {
						minDist = agent.me().distToFire.get(i);
						save = i;
					}
				sortedZone.get(save).assignedWorkerNum++;
				myZone = sortedZone.get(save);
				if (myZone != null && myZone.emergency && !isCalledByZoneChange)
					unemploymentSituation(myZone);
				return;
			}
		}
		if (myZone != null)
			return;
		FireZone minZone = null;
		double minDist = Integer.MAX_VALUE, dist;
		for (FireZone z : modelZones) {
			dist = Math.hypot((agent.me()).getX() - z.centerX,
					(agent.me()).getY() - z.centerY);
			if (dist <= minDist) {
				minDist = dist;
				minZone = z;
			}
		}
		myZone = minZone;
		if (myZone != null && myZone.emergency && !isCalledByZoneChange)
			unemploymentSituation(myZone);
		return;
	}

	private int distToFire(int v) {
		int[] arr = { 10, 20, 30, 40, 50, 60 };
		for (int i = 0; i < arr.length; i++)
			if (v <= arr[i])
				return i;

		return arr.length;
	}

	public int getDistToFire(Building building) {
		int minDist = Integer.MAX_VALUE;
		TreeSet<Road> roads = new TreeSet<Road>(new Comparator<Road>() {
			public int compare(Road arg0, Road arg1) {
				if (arg0.worldGraphArea.distanceFromSelf < arg1.worldGraphArea.distanceFromSelf)
					return -1;
				if (arg0.worldGraphArea.distanceFromSelf > arg1.worldGraphArea.distanceFromSelf)
					return 1;
				if (arg0.getID().getValue() > arg1.getID().getValue())
					return 1;
				if (arg0.getID().getValue() < arg1.getID().getValue())
					return -1;
				return 0;
			}
		});
		roads.addAll(building.getVisionRoads().keySet());
		done: for (Road road : roads) {
			if (road.worldGraphArea.isReachable)
				for (Point point : building.getVisionRoads().get(road)) {
					boolean i = true;
					for (Blockade blockade : road.worldGraphArea.blockades)
						if (blockade.isInShape(point)) {
							i = false;
							break;
						}

					if (i == true) {
						if (((Human) agent.me()).getPosition().getValue() == road
								.getID().getValue())
							return 1;
						minDist = road.worldGraphArea.distanceFromSelf;
						break done;
					}
				}
		}

		for (Building buil : building.visionBuildings.keySet()) {
			if (buil.worldGraphArea.isReachable) {
				if (((Human) agent.me()).getPosition().getValue() == buil
						.getID().getValue())
					return 1;
				if (buil.worldGraphArea.distanceFromSelf < minDist)
					minDist = buil.worldGraphArea.distanceFromSelf;
				break;
			}
		}
		if (minDist == Integer.MAX_VALUE)
			return 1000;
		return minDist;
	}

	private void moveToFire(Building target) throws ActionCommandException {
		agent.moveToFire(target);
}

	private boolean canExtinguish(Building target) {
		agent.log("MaxDis: " + agent.maxDistanceExtingiush);
		agent.log("Dist : "
				+ Mathematic.getDistance(target.getX(), target.getY(),
						(agent.me()).getX(), (agent.me()).getY()));
		return Mathematic.getDistance(target.getX(), target.getY(),
				(agent.me()).getX(), (agent.me()).getY()) < agent.maxDistanceExtingiush;
	}

	private void moveToSearchAroundZone(LittleZone searchZone)
			throws ActionCommandException {
		int minlts1 = Integer.MAX_VALUE;
		int mindist = Integer.MAX_VALUE;
		ArrayList<Building> minLastTimeSeen = new ArrayList<Building>();
		Building tar3 = null;
		for (Building building : searchZone.searchBuildings)
			if (building.lastTimeSeen <= minlts1)
				minlts1 = building.lastTimeSeen;
		for (Building building : searchZone.searchBuildings)
			if (building.lastTimeSeen == minlts1)
				minLastTimeSeen.add(building);
		for (Building building : minLastTimeSeen)
			if (building.worldGraphArea.distanceFromSelf <= mindist) {
				mindist = building.worldGraphArea.distanceFromSelf;
				tar3 = building;
			}
		if (searchAroundTar == null
				|| !buildingReachability(searchAroundTar)
				|| (tar3 != null && tar3.lastTimeSeen < searchAroundTar.lastTimeSeen))
			searchAroundTar = tar3;
		if (searchAroundTar != null) {
			agent.log(" moveTosearch around zone: "
					+ searchAroundTar.getID().getValue());
			moveToFire(searchAroundTar);
		}
		agent.randomWalk();
	}

	private void moveToSearch() throws ActionCommandException {
		position[akhir] = ((Human) agent.me()).getPosition().getValue();
		akhir++;
		if (akhir == 3)
			akhir = 0;
		if (position[0] == position[1] && position[1] == position[2]
				&& agent.reachableRoads.size() > 0) {
			int i = (new Random()).nextInt(agent.reachableRoads.size());
			target = agent.reachableRoads.get(i).getID();
			position[0] = 1;
			position[1] = 2;
			position[2] = 3;
		}
		for (int i = 0; i < mySearchZone.searchBuildings.size(); i++)
			if (agent
					.isInChangeSet(mySearchZone.searchBuildings.get(i).getID())) {
				seenBuildings++;
				mySearchZone.searchBuildings.remove(i);
				i--;
			}
		for (int i = 0; i < mySearchZone.searchBuildings.size(); i++)
			if (!buildingReachability(mySearchZone.searchBuildings.get(i))) {
				mySearchZone.searchBuildings.remove(i);
				i--;
			}
		int minlts1 = Integer.MAX_VALUE;
		int mindist = Integer.MAX_VALUE;
		ArrayList<Building> minLastTimeSeen = new ArrayList<Building>();
		Building tar3 = null;
		for (Building building : mySearchZone.searchBuildings)
			if (building.lastTimeSeen <= minlts1)
				minlts1 = building.lastTimeSeen;
		for (Building building : mySearchZone.searchBuildings)
			if (building.lastTimeSeen == minlts1)
				minLastTimeSeen.add(building);
		for (Building building : minLastTimeSeen)
			if (building.worldGraphArea.distanceFromSelf <= mindist) {
				mindist = building.worldGraphArea.distanceFromSelf;
				tar3 = building;
			}
		if (tar == null || !buildingReachability(tar)
				|| (tar3 != null && tar3.lastTimeSeen < tar.lastTimeSeen))
			tar = tar3;
		if (tar != null) {
			agent.log(" moveTosearch: " + tar.getID().getValue());
			moveToFire(tar);
		}
		agent.randomWalk();
	}

	private int valueForNotIgnited(int v) {
		int[] arr = { 0, 1, 5, 10, 15, 20, 30, 40, 70 };
		for (int i = 0; i < arr.length; i++)
			if (v <= arr[i])
				return i;

		return arr.length;
	}

	private void setValueForBuildings(Building build) {
		int w5 = 0;
		if (beingExtinguished != null
				&& build.getID().getValue() == beingExtinguished.getID()
						.getValue())
			w5 = -5;
		build.value = w1
				* (build.getGroundArea() * build.getFloors() / agent.maxVolumeOfAllBuildings)
				+ w2 * distToFire(getDistToFire(build)) + w3
				* (valueForNotIgnited(build.nearForSimulator.size())) + w4
				* build.volume + w5;
	}

	private void setValueForAroundBuildings() {
		for (FireZone fireZone : modelZones)
			for (Building build : fireZone.aroundBuildings) {
				if (buildingReachability(build)) {
					simulator(build);
					setVolumeSimulator(build);
					setValueForBuildings(build);

				}
			}
	}

	private void initialBuildings() {
		reachableAndIgnitedBuildings = agent.reachableAndIgnitedBuildings;
	}

	private Building tar1 = null;

	private void unemploymentSituation(FireZone FZ)
			throws ActionCommandException {
		agent.log(" unemployment ");
		checkWater();
		Building buil1 = null;
		int maxLast = 0;
		for (StandardEntity e : agent.model.getObjectsInRange(
				agent.me().getX(), agent.me().getY(),
				agent.maxDistanceExtingiush))
			if (e instanceof Building && ((Building) e).stFire > 0
					&& canExtinguish((Building) e)
					&& !buildingReachability((Building) e)
					&& ((Building) e).lastTimeSeen > maxLast
					&& ((Building) e).lastTimeSeen + 10 >= agent.time) {
				maxLast = ((Building) e).lastTimeSeen;
				buil1 = ((Building) e);
			}
		if (buil1 != null) {
			agent.log("in unemployment : ext : " + buil1.getID());
			agent.extinguish(buil1.getID(), maxPower);
		}
		int maxheard = 0;
		Building tar = null;
		for (Building building : FZ.buildings)
			if (agent.me().unreachableAndIgnitedBuildings.contains(building)
					&& building.lastTimeSeen > maxheard) {
				maxheard = building.lastTimeSeen;
				tar = building;
			}
		if (tar1 == null || tar1.stFire == 0 || !FZ.buildings.contains(tar1))
			tar1 = tar;
		if (tar1 != null) {
			agent.wg.clearTargets();
			for (Area area : tar1.nearAreas50000)
				if (area instanceof Road
						&& Math.hypot(area.getX() - tar1.getX(), area.getY()
								- tar1.getY()) < agent.maxDistanceExtingiush)
					area.worldGraphArea.setAreaAsTarget(true);
			ArrayList<EntityID> path = agent.wg.getMinPath(
					agent.wg.myEnterances, true);
			if (path.size() != 0) {
				agent.log("in unemployment : move : " + path);
				agent.move(path, false);
			}
		}
	}

	public void checkEmergencySituation() throws ActionCommandException {
		agent.log("check emergency !");
		if (agent.reachableAreas.size() <= 6) {
			agent.log("agent.reachableAreas.size() <= 6");
			for (Building build : agent.modelBuildings) {
				if (build.stFire > 0 && canExtinguish(build)) {
					agent.extinguish(build.getID(), maxPower);
				}
			}
		}
	}

	public ArrayList<FireZone> modelZones = null;
	private FireZone zone = null;

	public void zoning() {
		modelZones = agent.modelZones;
	}

	private void checkingNeighboursForZoning(Building isChecking) {
		isChecking.zoneNumber = zone.zoneNum;
		zone.buildings.add(isChecking);
		if (isChecking.stFire > 0)
			zone.ignitedBuilds.add(isChecking);
		isChecking.haveZone = true;
		for (Building neighbour : isChecking.nearBuildings)
			if ((neighbour.stFire > 0) && !neighbour.haveZone) {
				checkingNeighboursForZoning(neighbour);
			}
	}

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}
}
