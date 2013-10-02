package agent;

import geometry.Degree;
import geometry.Line;
import geometry.Mathematic;
import geometry.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import rescuecore2.messages.Command;
import rescuecore2.registry.Registry;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.StandardMessageURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import utilities.Logger;
import utilities.Timer;
import utilities.config.Config;
import worldGraph.Enterance;
import worldGraph.WorldGraph;

public abstract class Agent<E extends StandardEntity> extends StandardAgent<E> {
	public Config psdConfig = null;
	public Logger logger = null;
	protected Timer cycleTimer = null;
	public WorldGraph wg = null;
	public SubZone mySubZone = null;
	public PoliceZone myPoliceZone = null;
	public int myZoneNumber;
	public int mySubZoneNumber;
	public ArrayList<Integer> lastIDs = new ArrayList<Integer>();
	public int powNumForPolices = -1;
	public int refugePow = -1;
	int SBNum = -1;
	boolean isSafeBlockadesNeededForRandomWalk = false;
	public double maxVolumeOfAllBuildings = -Integer.MAX_VALUE;
	public double averageOfSurfaces = 0;
	// These Arrays could change to some trees for faster search
	public ArrayList<Building> reachableBuildings = new ArrayList<Building>();
	public ArrayList<Road> reachableRoads = new ArrayList<Road>();
	public ArrayList<Refuge> reachableRefuges = new ArrayList<Refuge>();
	public ArrayList<Hydrant> reachableHydrants = new ArrayList<Hydrant>();
	// public TreeMap<Integer, Area> reachableAreas = new TreeMap<Integer,
	// Area>();
	// public TreeMap<Integer, Enterance> reachableEnterances = new
	// TreeMap<Integer, Enterance>();
	public List<Area> reachableAreas = new ArrayList<Area>();

	public ArrayList<Building> modelBuildings = new ArrayList<Building>();
	public ArrayList<Road> modelRoads = new ArrayList<Road>();
	public ArrayList<Refuge> modelRefuges = new ArrayList<Refuge>();
	public ArrayList<Area> modelAreas = new ArrayList<Area>();
	public ArrayList<FireBrigade> modelFireBrigades = new ArrayList<FireBrigade>();
	public ArrayList<PoliceForce> modelPoliceForces = new ArrayList<PoliceForce>();
	public ArrayList<AmbulanceTeam> modelAmbulanceTeams = new ArrayList<AmbulanceTeam>();
	public ArrayList<Hydrant> modelHydrants = new ArrayList<Hydrant>();
	public ArrayList<Human> modelAgents = new ArrayList<Human>();
	public ArrayList<FireStation> modelFireStations = new ArrayList<FireStation>();
	public ArrayList<PoliceOffice> modelPoliceOffices = new ArrayList<PoliceOffice>();
	public ArrayList<AmbulanceCentre> modelAmbulanceCentres = new ArrayList<AmbulanceCentre>();
	protected Map<Integer, Integer> needHelpHeard = new HashMap<Integer, Integer>();
	public HashMap<Integer, Integer> allEntities = new HashMap<Integer, Integer>();
	public HashMap<Integer, Entity> allRanks = new HashMap<Integer, Entity>();
	public TreeMap<Integer, byte[]> lastFiredBuildings = new TreeMap<Integer, byte[]>();
	// from 2009 - next line
	public double modelGroundArea = 0;
	public int powNum = -1;
	public boolean noCommi = false;
	public boolean commiLess = false;
	public int counterForHelp = 0;
	public int counterForAmbHelp = 0;
	public int counterForUnHelp = 0;
	public int counterForAmbUnHelp = 0;
	public final int cvRecognizer = 32;
	public static int counterForBusiness = 0;
	public int counterForPoliceMap = 0;
	int centerForPolice = 0;
	int centerForAmb = 0;
	int centerForFire = 0;
	ArrayList<Integer> policeDecisionHeard1;
	ArrayList<Integer> ambDecisionHeard;
	protected HashMap<Integer, Integer> longnHP = new HashMap<Integer, Integer>();
	protected HashMap<Integer, Integer> longnAP = new HashMap<Integer, Integer>();
	protected HashMap<Integer, Integer> uNAP = new HashMap<Integer, Integer>();
	protected HashMap<Integer, Integer> uNHP = new HashMap<Integer, Integer>();
	protected HashMap<Integer, Integer> longnCP = new HashMap<Integer, Integer>();
	protected HashMap<Integer, Integer> longnBP = new HashMap<Integer, Integer>();
	protected HashMap<Integer, EntityID> IntBeEntity = new HashMap<Integer, EntityID>();
	protected HashMap<EntityID, Integer> EntityBeInt = new HashMap<EntityID, Integer>();
	protected HashMap<EntityID, EntityID> IDandPOS = new HashMap<EntityID, EntityID>();
	protected Radar radar = null;
	// EntityID lastRescuedObject = null;
	protected int lastRescuedTime = -1;
	protected int lastSaidUnLoadTime = -1;
	protected TreeSet<Integer> heardclearers = new TreeSet<Integer>();
	public Zone myZone1;
	public ArrayList<Integer> sentBlockades = new ArrayList<Integer>();
	public static final int MAX_DISTANCE_VIEW = 30000;
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	public static int maxDistanceExtingiush = -1;
	// public static final int MAX_DISTANCE_EXTINGUISH = 50000;
	public HashMap<Integer, Integer> policeFirstPositions = null;
	boolean wasItInLongnAP = false;
	boolean wasItInLongnHP = false;
	int areaPow = -1;
	int lastSaidPositionCycle = -1;
	public static final int cyclesBtweenSayingPos = 2;
	public static int MAX_GROUPING_DIST = 20000;
	protected SampleSearch search;
	public int mySubZoneID = -1;
	public boolean shouldBaseDecideForRandomWalk = true;
	public boolean isSafeBlockadesNeeded = true;
	int counterForIsSafeBlock = 0;
	public static final int LTSDORI = -50000000;
	public boolean movingToRefuge = false;
	// // LittleZoning
	// public LittleZone myLZone = null;
	// LittleZone littlezones[];
	// From 2009
	public int MAX_ZONING_DIST = 15000;
	public int SIZE_FOR_ZONING = 300;
	public int policeCount = 0;
	public int timeForNewWayMemory = -1;
	public double A0 = 1.5, A1 = 2.5, A2 = 3.5, A3 = 1.3, A4 = 5, A5 = 1;
	public int MAX_PCOUNT_FOR_ZONING = 10;
	public Random MY_RAND = new Random(100000);
	public Area cityCenter = null;
	public boolean newStructure = false;
	public PathArray hameyeRahHa = new PathArray();
	public String InAddress = null;
	public int irancell = 2;
	public int stuckToolKeshid = 0;
	public int akharinStuckTime = 0;
	public int lastHeardPosFrom = -1;
	public boolean preCompute = false;

	// till Here

	// public ArrayList<Enterance> allEnterances = new ArrayList<Enterance>();

	public enum MapType {
		VC, Paris, Kobe, Berlin, Istanbul, Unknown
	}

	public MapType map = MapType.Unknown;

	public double setDistance(Area building) {
		double distance = 0;
		double max = 1;
		for (int i = 0; i < building.getApexList().length; i = i + 2) {
			distance = Math.hypot(building.getX() - building.getApexList()[i],
					building.getY() - building.getApexList()[i + 1]);
			if (distance > max)
				max = distance;
		}
		return max;

	}

	ArrayList<LittleZone> newZone = new ArrayList<LittleZone>();
	ArrayList<LittleZone> searchZones = new ArrayList<LittleZone>();

	protected void postConnect() {
		super.postConnect();
		maxDistanceExtingiush = config.getIntValue(MAX_DISTANCE_KEY);
		Human.HP_PRECISION = config.getIntValue("perception.los.precision.hp");
		Human.DAMAGE_PRECISION = config
				.getIntValue("perception.los.precision.damage");
		System.out.println("Start of Precomputation");
		cycleTimer = new Timer();
		cycleTimer.setTime(0);
		if (psdConfig.get("Agent", "AgentLog", "preCompute").equals("True")) {
			System.out.println("miiam injaW ke true she:D");
			preCompute = true;
		}
		if (psdConfig.get("Agent", "AgentLog", "LogToFile").equals("True"))
			logger = new Logger(
					psdConfig.get("Agent", "AgentLog", "LogAddress") + "/"
							+ getName().replaceAll("agent.", "") + "-"
							+ me().getID().getValue(), cycleTimer);
		else
			logger = new Logger(null, cycleTimer);
		int Pgi = 1;

		for (StandardEntity next : model.getAllEntities()) {
			if (next instanceof Area)
				modelAreas.add((Area) next);
			if (next instanceof PoliceForce || next instanceof FireBrigade
					|| next instanceof AmbulanceTeam)
				modelAgents.add((Human) next);
			if (next instanceof Refuge) {
				modelRefuges.add((Refuge) next);
				modelBuildings.add((Building) next);
			} else if (next instanceof Building) {
				IntBeEntity.put(Pgi, next.getID());
				EntityBeInt.put(next.getID(), Pgi);
				Pgi++;
				modelBuildings.add((Building) next);
			} else if (next instanceof Road)
				modelRoads.add((Road) next);
			if (next instanceof Hydrant)
				modelHydrants.add((Hydrant) next);
			if (next instanceof FireBrigade)
				modelFireBrigades.add((FireBrigade) next);
			else if (next instanceof PoliceForce)
				modelPoliceForces.add((PoliceForce) next);
			else if (next instanceof AmbulanceTeam)
				modelAmbulanceTeams.add((AmbulanceTeam) next);
			else if (next instanceof FireStation)
				modelFireStations.add((FireStation) next);
			else if (next instanceof PoliceOffice)
				modelPoliceOffices.add((PoliceOffice) next);
			else if (next instanceof AmbulanceCentre)
				modelAmbulanceCentres.add((AmbulanceCentre) next);
		}

		System.out.println("Before sort");

		Collections.sort(modelPoliceForces, IDcomparator);
		Collections.sort(modelAreas, areaSorter);
		Collections.sort(modelPoliceForces, IDcomparator);
		Collections.sort(modelPoliceOffices, IDcomparator);
		Collections.sort(modelFireBrigades, IDcomparator);
		Collections.sort(modelFireStations, IDcomparator);
		Collections.sort(modelAmbulanceTeams, IDcomparator);
		Collections.sort(modelAmbulanceCentres, IDcomparator);
		Collections.sort(modelRefuges, IDcomparator);

		refugePow = Radar.findPow(modelRefuges.size());
		for (Building b : modelBuildings) {
			b.model = model;
			b.maxDistanceExtingiush = maxDistanceExtingiush;
		}

		int i = 0;

		for (Area area : modelAreas) {
			area.isEmpty = new boolean[cvRecognizer];
			allEntities.put(area.getID().getValue(), i);
			allRanks.put(i, area);
			i++;
		}

		areaPow = Radar.findPow(i - 1);

		for (PoliceForce policeForce : modelPoliceForces) {
			allEntities.put(policeForce.getID().getValue(), i);
			allRanks.put(i, policeForce);
			i++;
		}
		for (PoliceOffice policeOffice : modelPoliceOffices) {
			allEntities.put(policeOffice.getID().getValue(), i);
			allRanks.put(i, policeOffice);
			i++;
		}
		for (FireBrigade fireAgent : modelFireBrigades) {
			allEntities.put(fireAgent.getID().getValue(), i);
			allRanks.put(i, fireAgent);
			i++;
		}
		for (FireStation fireStation : modelFireStations) {
			allEntities.put(fireStation.getID().getValue(), i);
			allRanks.put(i, fireStation);
			i++;
		}
		for (AmbulanceTeam ambulanceTeam : modelAmbulanceTeams) {
			allEntities.put(ambulanceTeam.getID().getValue(), i);
			allRanks.put(i, ambulanceTeam);
			i++;
		}
		for (AmbulanceCentre ambulanceCentre : modelAmbulanceCentres) {
			allEntities.put(ambulanceCentre.getID().getValue(), i);
			allRanks.put(i, ambulanceCentre);
			i++;
		}

		System.out.println("After sort");

		powNumForPolices = Radar.findPow(modelPoliceForces.size());
		powNum = Radar.findPow(allEntities.size());

		selectChannel(); // XXX Afsoon
		if (noCommi)
			timeForNewWayMemory = 30;
		else
			timeForNewWayMemory = 100;// Math.min(100,
		// config.getIntValue("kernel.timesteps"));

		System.out.println("Before Search Computation");
		search = new SampleSearch(model);
		neighbours = search.getGraph();
		System.out.println("After Search Computation");

		System.out.println("Start of WorldGraph Precomputation");
		wg = new WorldGraph(model, modelBuildings, modelRoads, modelRefuges);

		try {
			if (modelBuildings.size() == 735 && modelRoads.size() == 1515) { // MAP
				// BUGS:
				// KOBE
				removeConnectivity(31259, 31261);
				removeConnectivity(3930, 27209);
			}
			// VC1:
			if (modelBuildings.size() == 1258 && modelRoads.size() == 1954) {
				removeConnectivity(48405, 4264);
				removeConnectivity(49086, 53219);
				removeConnectivity(49086, 49087);
				removeConnectivity(48442, 7585);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (logger.getWriter() != null) {
				e.printStackTrace(logger.getWriter());
			}
		}

		log("OfflineFile: "
				+ psdConfig.get("Agent", "AgentLog", "OffLogFileName"));
		System.out.println("Precomputation finished");
		if (1253 <= modelBuildings.size() && modelBuildings.size() <= 1273
				&& 1944 <= modelRoads.size() && modelRoads.size() <= 1964)
			map = MapType.VC;
		if (1608 <= modelBuildings.size() && modelBuildings.size() <= 1628
				&& 3015 <= modelRoads.size() && modelRoads.size() <= 3035)
			map = MapType.Paris;
		if (1416 <= modelBuildings.size() && modelBuildings.size() <= 1436
				&& 3375 <= modelRoads.size() && modelRoads.size() <= 3395)
			map = MapType.Berlin;
		if ((726 <= modelBuildings.size() && modelBuildings.size() <= 746
				&& 1505 <= modelRoads.size() && modelRoads.size() <= 1525)
				|| (747 <= modelBuildings.size()
						&& modelBuildings.size() <= 767
						&& 1592 <= modelRoads.size() && modelRoads.size() <= 1612))
			map = MapType.Kobe;
		if (1234 <= modelBuildings.size() && modelBuildings.size() <= 1254
				&& 3327 <= modelRoads.size() && modelRoads.size() <= 3347)
			map = MapType.Istanbul;

		int x = 8000;
		if (map == MapType.Kobe || map == MapType.VC)
			x = 200;
		// if (!(map == MapType.Kobe || map == MapType.VC))
		// wg.RAD = 600;
		for (Area area : modelAreas) {
			if (area instanceof Building) {
				int dist = Math.max(MAX_DISTANCE_VIEW, (int) setDistance(area)
						+ x);
				do {
					for (StandardEntity e : model.getObjectsInRange(
							area.getID(), dist))
						if (e.getID().getValue() != area.getID().getValue()) {
							if (e instanceof Road)
								area.nearRoads.add((Road) e);
							else if (e instanceof Building
									&& !area.nearBuildings
											.contains((Building) e)) {
								area.nearBuildings.add((Building) e);
								if (area instanceof Building
										&& !((Building) e).nearBuildings
												.contains((Building) area)) {
									((Building) e).nearBuildings
											.add((Building) area);
								}
							}
						}
					dist += 5000;
				} while (area.nearBuildings.size() <= 5);
			} else {
				for (StandardEntity e : model.getObjectsInRange(area.getID(),
						MAX_DISTANCE_VIEW))
					if (e.getID().getValue() != area.getID().getValue()) {
						if (e instanceof Road)
							area.nearRoads.add((Road) e);
						else if (e instanceof Building)
							area.nearBuildings.add((Building) e);
					}
			}

			for (StandardEntity e : model.getObjectsInRange(area.getID(),
					maxDistanceExtingiush))
				if (e instanceof Area
						&& e.getID().getValue() != area.getID().getValue())
					area.nearAreas50000.add((Area) e);
			Collections.sort(area.nearRoads, IDcomparator);
			Collections.sort(area.nearBuildings, IDcomparator);
		}

		setCenterOfMap();
		if (map.equals(MapType.Berlin) || map.equals(MapType.Paris)
				|| map.equals(MapType.Istanbul)) {
			STUCKCYCLES *= 2;
			positions = new int[STUCKCYCLES];
		}

		for (Building building : modelBuildings) {
			double hajm = building.getGroundArea() * building.getFloors();
			averageOfSurfaces += building.getGroundArea();
			if (hajm > maxVolumeOfAllBuildings)
				maxVolumeOfAllBuildings = hajm;
		}
		averageOfSurfaces /= modelBuildings.size();

		setHamraheAval();

		selectCenterForPolice();
		selectCenterForAmb();
		selectCenterForFire();

		for (PoliceForce pf : modelPoliceForces)
			policeStatus.add(0);
		if (me() instanceof PoliceForce
				|| me().getID().getValue() == centerForPolice) {
			setAllPathes();
		}
		if (me() instanceof FireBrigade
				|| me().getID().getValue() == centerForFire) {
			System.out.println("man raftam k vision roads biiabam....: >"
					+ me().getID().getValue());
			visionRoadsAndBuildings();
		}
		// System.gc();
	}

	public String mapHashCode() {
		return String.valueOf(modelBuildings.size() * 2
				+ (long) Math.pow(modelRoads.size(), 3));
	}

	public void visionRoads() {
		// age file bood k bkhune!
		String fileName = "./precompute/vision" + mapHashCode() + ".bin";
		boolean success = true;
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(new File(fileName)));
			ArrayList<HashMap<Integer, ArrayList<Point>>> all = (ArrayList<HashMap<Integer, ArrayList<Point>>>) in
					.readObject();

			Iterator<Building> iterator = modelBuildings.iterator();
			for (HashMap<Integer, ArrayList<Point>> roads : all) {
				Building building = iterator.next();
				building.visionRoads = new HashMap<Road, ArrayList<Point>>();
				for (Entry<Integer, ArrayList<Point>> e : roads.entrySet()) {
					building.visionRoads.put((Road) allRanks.get(e.getKey()),
							e.getValue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		// age nabood!
		ObjectOutputStream out = null;
		if (!success && preCompute) {
			try {
				out = new ObjectOutputStream(new FileOutputStream(new File(
						fileName)));
				ArrayList<HashMap<Integer, ArrayList<Point>>> all = new ArrayList<HashMap<Integer, ArrayList<Point>>>();
				for (Building b : modelBuildings) {
					HashMap<Integer, ArrayList<Point>> roads = new HashMap<Integer, ArrayList<Point>>();
					for (Entry<Road, ArrayList<Point>> e : b.getVisionRoads()
							.entrySet()) {
						roads.put(
								allEntities.get(e.getKey().getID().getValue()),
								e.getValue());
					}
					all.add(roads);
				}
				out.writeObject(all);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void visionBuildings() {
		for (Building building : modelBuildings) {
			for (Edge edge : building.getEdges()) {
				if (edge.getNeighbour() == null)
					continue;
				Entity entity = model.getEntity(edge.getNeighbour());
				if (entity instanceof Building) {
					Building area = (Building) entity;
					Point c = new Point(
							(edge.getStartX() + edge.getEndX()) / 2,
							(edge.getStartY() + edge.getEndY()) / 2);
					done: for (int i = 5000; i >= 5000; i -= 5000) {
						for (float theta = 0; theta < 2 * Math.PI; theta += 2 * Math.PI / 20.0) {
							Point p = new Point(theta, i, true);
							p = p.plus(c);
							if (area.worldGraphArea.isInShape(p)) {
								building.visionBuildings.put(area, p);
								break done;
							}
						}
					}
				}
			}
		}
	}

	public boolean isVisionsCalculated = false;

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
			// if (building.getID().getValue() == 52570
			// && area.getID().getValue() == 14953)
			// System.err.println(" point: " + minMax + "  ");
			if (maxDist < 0)
				continue;
			// if (minDist > minMax)// remove
			// continue;
			// maxDist = Math.min(maxDist, minMax);// remove
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

				float kamine = buildingPoint.getDistance(newPoint);
				Point behtarinPoint = buildingPoint;
				for (Edge edge : building.getEdges()) {
					Line lineEdge = new Line(new Point(edge.getStartX(),
							edge.getStartY()), new Point(edge.getEndX(),
							edge.getEndY()));
					if (Line.isIntersectBetweenLines(buildingToRoad, lineEdge)) {
						Point intersectPoint = buildingToRoad
								.getIntersectPoint(lineEdge);
						if (intersectPoint.getDistance(newPoint) < kamine) {
							kamine = intersectPoint.getDistance(newPoint);
							behtarinPoint = intersectPoint;
						}
					}
				}
				while (!building.worldGraphArea.isInShape(behtarinPoint)) {
					Point c = behtarinPoint.minus(buildingPoint);
					c.setX((int) (c.getX() * 0.99));
					c.setY((int) (c.getY() * 0.99));
					behtarinPoint = c.plus(buildingPoint);
				}

				buildingToRoad.setFirstPoint(behtarinPoint);
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
				if (behtarinPoint.getDistance(newPoint) < MAX_DISTANCE_VIEW
						&& newPoint.getDistance(buildingPoint) < maxDistance
						&& !hasIntersect
						&& buildingToRoad.getSecondPoint().getDistance(
								new Point(building.getX(), building.getY())) < maxDistanceExtingiush)
					visionPoints.add(buildingToRoad.getSecondPoint());
			}
		}
		return visionPoints;
	}

	public ArrayList<Point> getVisionPoints(Area area, Building building,
			int minMax) {
		return getVisionPoints(model, area, building, maxDistanceExtingiush,
				minMax);
	}

	public void visionRoadsAndBuildings() {
		visionRoads();
		visionBuildings();
	}

	public void setAllPathes() {
		try {
			try {
				if (modelBuildings.size() == 736 && modelRoads.size() == 1515)
					InAddress = "./Configs/In-Kobe.txt";
				if (modelBuildings.size() == 757 && modelRoads.size() == 1602)
					InAddress = "./Configs/In-Kobe2.txt";
				if (modelBuildings.size() == 1263 && modelRoads.size() == 1954)
					InAddress = "./Configs/In-VC.txt";
				if (modelBuildings.size() == 1426 && modelRoads.size() == 3385)
					InAddress = "./Configs/In-Berlin.txt";
				if (modelBuildings.size() == 1618 && modelRoads.size() == 3025)
					InAddress = "./Configs/In-Paris2.txt";
				if (modelBuildings.size() == 1244 && modelRoads.size() == 3337)
					InAddress = "./Configs/In-Istanbul.txt";
				if (modelBuildings.size() == 1308 && modelRoads.size() == 5172)
					InAddress = "./Configs/In-Eindhoven.txt";
				if (modelBuildings.size() == 1556 && modelRoads.size() == 5108)
					InAddress = "./Configs/In-Mexico.txt";
				newStructure = true;
				Path p = new Path();
				Scanner scanner = new Scanner(new InputStreamReader(
						new FileInputStream(new File(InAddress))));
				int i = 0;
				while (scanner.hasNext()) {
					p = new Path();
					int k = scanner.nextInt();
					for (int j = 0; j < k; j++) {
						Area a = modelAreas.get(scanner.nextInt());
						a.pathNum = i;
						p.way.add(a);
					}
					k = scanner.nextInt();
					for (int j = 0; j < k; j++) {
						Building b = (Building) modelAreas.get(scanner
								.nextInt());
						p.buildings.add(b);
						b.pathNum = i;
					}
					k = scanner.nextInt();
					for (int j = 0; j < k; j++) {
						Road r = (Road) modelAreas.get(scanner.nextInt());
						p.extra.add(r);
						r.pathNum = i;
					}
					p.pathNum = i;
					p.setCenter();
					hameyeRahHa.pathes.add(p);
					hameyeRahHa.pathNums.add(i);
					i++;
				}
				scanner.close();

				Collections.sort(hameyeRahHa.pathes, new Comparator<Path>() {
					public int compare(Path arg0, Path arg1) {
						double dist0 = Math.hypot(arg0.centralX, arg0.centralY);
						double dist1 = Math.hypot(arg1.centralX, arg1.centralY);
						if (dist0 < dist1)
							return -1;
						else if (dist0 > dist1)
							return 1;
						else
							return 0;
					}
				});

				for (PoliceForce pf : modelPoliceForces) {
					pf.firstX = pf.getX();
					pf.firstY = pf.getY();
				}
			} catch (FileNotFoundException e) {
				newStructure = false;
				e.printStackTrace();
			}
		} catch (Exception e) {
			newStructure = false;
			e.printStackTrace();
			if (logger.getWriter() != null) {
				e.printStackTrace(logger.getWriter());
			}
		}
	}

	public Comparator<Area> areaSorter = new Comparator<Area>() {
		@Override
		public int compare(Area a1, Area a2) {
			int x1 = a1.getX(), y1 = a1.getY(), x2 = a2.getX(), y2 = a2.getY();
			if (x1 < x2)
				return 1;
			if (x1 > x2)
				return -1;
			if (y1 < y2)
				return 1;
			if (y1 > y2)
				return -1;
			return 0;
		}
	};

	public void setHamraheAval() {
		if (map == MapType.VC || map == MapType.Kobe) {
			int c = 7;
			double vV = maxVolumeOfAllBuildings / c;
			for (Building building : modelBuildings) {
				double v = building.getGroundArea() * building.getFloors();
				for (int i = c; i > 0; i--) {
					if (v <= i * vV && v > (i - 1) * vV) {
						if (map == MapType.Istanbul || map == MapType.Berlin
								|| map == MapType.Paris)
							building.hamraheAval = i * 3 + 2;
						else
							building.hamraheAval = i + 2;
						break;
					}
				}
			}
		} else
			for (Building building : modelBuildings)
				building.hamraheAval = 20;
	}

	public Point setcenter(ArrayList<Building> buildings) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (Building build : buildings) {
			if (build.getX() > maxX)
				maxX = build.getX();
			if (build.getX() < minX)
				minX = build.getX();
			if (build.getY() > maxY)
				maxY = build.getY();
			if (build.getY() < minY)
				minY = build.getY();
		}
		return new Point((minX + maxX) / 2, (minY + maxY) / 2);
	}

	public void removeConnectivity(int id1, int id2) {
		worldGraph.Area area = ((Area) model.getEntityByInt(id1)).worldGraphArea;
		Enterance enterance = null;
		for (Enterance e : area.enterances)
			if (e.neighbourAreaInModel.getID().getValue() == id2) {
				enterance = e;
				break;
			}
		Enterance neighbour = enterance.neighbour;
		area.enterances.remove(enterance);
		neighbour.area.enterances.remove(neighbour);
	}

	int tedadByteChannel0 = 0;
	int tedadByteChannel1 = 0;
	int tedad = 0;
	int tedadChannelAgent = 0;
	int tedadChannelCenter = 0;
	int behtarinChannel1 = 1;
	int behtarinChannel2 = 2;
	public boolean lowCommi = false;

	protected void selectChannel() {
		tedad = config.getIntValue("comms.channels.count");
		tedadByteChannel0 = config
				.getIntValue("comms.channels.0.messages.size");
		tedadChannelAgent = config.getIntValue("comms.channels.max.platoon");
		tedadChannelCenter = config.getIntValue("comms.channels.max.centre");
		if (tedad <= 1) {
			noCommi = true;
			behtarinChannel1 = 0;
		}
		if (!noCommi) {
			int a = 0;
			int tedadByteBehtarinChannal1 = 0;
			int tedadByteBehtarinChannal2 = 0;
			if (tedad == 2) {
				behtarinChannel1 = 1;
				behtarinChannel2 = 0;
			} else {
				for (int i = 2; i < tedad; i++) {
					a = config
							.getIntValue("comms.channels." + i + ".bandwidth");
					tedadByteBehtarinChannal1 = config
							.getIntValue("comms.channels." + behtarinChannel1
									+ ".bandwidth");
					tedadByteBehtarinChannal2 = config
							.getIntValue("comms.channels." + behtarinChannel2
									+ ".bandwidth");
					if (a > tedadByteBehtarinChannal1) {
						behtarinChannel2 = behtarinChannel1;
						behtarinChannel1 = i;
					} else if (a >= tedadByteBehtarinChannal2)
						behtarinChannel2 = i;
				}
			}
			if (config.getIntValue("comms.channels." + behtarinChannel1
					+ ".bandwidth") < 500)
				lowCommi = true;
		}
	}

	protected void selectCenterForPolice() {
		centerForPolice = 0;
		if (model.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE).size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE)) {
				centerForPolice = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE)
				.size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE)) {
				centerForPolice = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.FIRE_STATION).size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.FIRE_STATION)) {
				centerForPolice = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).size() > 0) {
			ArrayList<Integer> policeForce = new ArrayList<Integer>();
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.POLICE_FORCE))
				policeForce.add(e.getID().getValue());
			Collections.sort(policeForce);
			centerForPolice = policeForce.get(0);
		}
	}

	protected void selectCenterForAmb() {
		centerForAmb = 0;
		if (model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE).size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE)) {
				centerForAmb = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE)
				.size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE)) {
				centerForAmb = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.FIRE_STATION).size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.FIRE_STATION)) {
				centerForAmb = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)
				.size() > 0) {
			ArrayList<Integer> ambTeam = new ArrayList<Integer>();
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM))
				ambTeam.add(e.getID().getValue());
			Collections.sort(ambTeam);
			centerForAmb = ambTeam.get(0);
		}
	}

	protected void selectCenterForFire() {
		centerForFire = 0;
		if (model.getEntitiesOfType(StandardEntityURN.FIRE_STATION).size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.FIRE_STATION)) {
				centerForFire = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE)
				.size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE)) {
				centerForFire = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE)
				.size() > 0)
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE)) {
				centerForFire = e.getID().getValue();
				break;
			}
		else if (model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size() > 0) {
			ArrayList<Integer> fireBg = new ArrayList<Integer>();
			for (Entity e : model
					.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE))
				fireBg.add(e.getID().getValue());
			Collections.sort(fireBg);
			centerForFire = fireBg.get(0);
		}
	}

	// protected void sendCheck(int id, boolean maleMan) {
	// BitSet bits = new BitSet();
	// int start = 0;
	// start = Radar.calcWithBitSet(bits, Radar.HEADER_CHECK, start,
	// Radar.headerSize);
	// if (me() instanceof AmbulanceTeam)
	// start = Radar.calcWithBitSet(bits, id, start, 20);
	// else if (me() instanceof FireBrigade)
	// start = Radar.calcWithBitSet(bits, allEntities.get(id), start,
	// powNum);
	// int a;
	// if (maleMan)
	// a = 1;
	// else
	// a = 0;
	// start = Radar.calcWithBitSet(bits, a, start, 1);
	// byte[] byteArray = Radar.toByteArray(bits);
	// sendSpeak(time, behtarinChannel1, byteArray);
	// if (behtarinChannel1 != 0)
	// sendSpeak(time, 0, byteArray);
	// }

	HashSet<Building> buildFix = new HashSet<Building>();
	ArrayList<Integer> humFix = new ArrayList<Integer>();
	HashMap<Integer, ArrayList<Integer>> unknownCivilian = new HashMap<Integer, ArrayList<Integer>>();
	HashMap<Integer, ArrayList<Integer>> newUnknownCivilian = new HashMap<Integer, ArrayList<Integer>>();

	// protected void hearCheck() {
	// buildFix = new HashSet<Building>();
	// humFix = new ArrayList<Integer>();
	// if (heard.size() > 0) {
	// byte[] recivedSpeak = null;
	// for (Command cmd : heard) {
	// Entity entity = model.getEntityByInt(cmd.getAgentID()
	// .getValue());
	// if (cmd instanceof AKSpeak && entity != null
	// && !(entity instanceof Civilian)) {
	// AKSpeak as = (AKSpeak) cmd;
	// recivedSpeak = as.getContent();
	// if (recivedSpeak.length > 0) {
	// if (radar.tellHeader(recivedSpeak) == Radar.HEADER_CHECK) {
	// BitSet bits = Radar.fromByteArray(recivedSpeak);
	// if (model.getEntity(cmd.getAgentID()) instanceof Human) {
	// Human him = (Human) model.getEntity(cmd
	// .getAgentID());
	// if (me() instanceof AmbulanceTeam
	// && him instanceof AmbulanceTeam) {
	// int a = Radar.fromMabnaye2ToMabnaye10(bits,
	// Radar.headerSize + 20,
	// Radar.headerSize);
	// boolean maleOn = false;
	// if (Radar.fromMabnaye2ToMabnaye10(bits,
	// Radar.headerSize + 21,
	// Radar.headerSize + 20) == 1)
	// maleOn = true;
	// Entity e = model.getEntityByInt(a);
	// ArrayList<Integer> ownerha, newOwnerHa;
	// Human humForAmb = (Human) e;
	// if (humForAmb == null) {
	// if (unknownCivilian.containsKey(a)) {
	// ownerha = unknownCivilian.get(a);
	// newOwnerHa = newUnknownCivilian
	// .get(a);
	// } else {
	// ownerha = new ArrayList<Integer>();
	// newOwnerHa = new ArrayList<Integer>();
	// unknownCivilian.put(a, ownerha);
	// newUnknownCivilian.put(a,
	// newOwnerHa);
	// }
	// } else {
	// ownerha = humForAmb.Owners;
	// newOwnerHa = humForAmb.newOwners;
	// }
	// int id = cmd.getAgentID().getValue();
	// if (maleOn == false) {
	// if (ownerha.contains(id))
	// ownerha.remove(ownerha.indexOf(id));
	// } else {
	// if (!ownerha.contains(id)) {
	// AmbulanceTeam un = (AmbulanceTeam) him;
	// if (un.aim != null
	// && un.aim.Owners
	// .contains(id))
	// un.aim.Owners
	// .remove(un.aim.Owners
	// .indexOf(id));
	// if (!newOwnerHa.contains(id)) {
	// newOwnerHa.add(id);
	// un.aim = humForAmb;
	// }
	// }
	// if (!humFix.contains(a))
	// humFix.add(a);
	// }
	// } else if (me() instanceof FireBrigade
	// && him instanceof FireBrigade) {
	// FireBrigade fireb = (FireBrigade) him;
	// int a = Radar.fromMabnaye2ToMabnaye10(bits,
	// powNum + Radar.headerSize,
	// Radar.headerSize);
	// Building buil = (Building) allRanks.get(a);
	// boolean maleOn = false;
	// if (Radar.fromMabnaye2ToMabnaye10(bits,
	// Radar.headerSize + powNum + 1,
	// Radar.headerSize + powNum) == 1)
	// maleOn = true;
	// if (!maleOn
	// && buil.owners.contains(him.getID()
	// .getValue())) {
	// for (int i = 0; i < buil.owners.size(); i++)
	// if (him.getID().getValue() == buil.owners
	// .get(i)) {
	// buil.owners.remove(i);
	// break;
	// }
	// buildFix.add(buil);
	// }
	// if (maleOn) {
	// if (fireb.myZoneBuil != null
	// && fireb.myZoneBuil.owners
	// .contains(him.getID()
	// .getValue())) {
	// for (int i = 0; i < fireb.myZoneBuil.owners
	// .size(); i++)
	// if (fireb.myZoneBuil.owners
	// .get(i) == fireb
	// .getID().getValue()) {
	// fireb.myZoneBuil.owners
	// .remove(i);
	// break;
	// }
	// buildFix.add(fireb.myZoneBuil);
	// }
	// buil.owners.add(him.getID().getValue());
	// fireb.myZoneBuil = buil;
	// buildFix.add(fireb.myZoneBuil);
	// }
	// }
	// }
	// }
	// }
	// }
	// }
	// }
	// for (Building building : buildFix)
	// building.fixOwners();
	// for (int eh : humFix) {
	// Human m = (Human) model.getEntityByInt(eh);
	// if (m != null) {
	// fixOwners(m.Owners, m.newOwners);
	// ((Human) m).newOwners = new ArrayList<Integer>();
	// } else {
	// ArrayList<Integer> azGhabl = unknownCivilian.get(eh);
	// ArrayList<Integer> thisTime = newUnknownCivilian.get(eh);
	// fixOwners(azGhabl, thisTime);
	// }
	// }
	// }

	public void fixOwners(ArrayList<Integer> azGhabl,
			ArrayList<Integer> thisTime) {
		Collections.sort(thisTime);
		int i = 0;
		int x = 5;
		if (time > 250)
			x = 6;
		while (azGhabl.size() < x && i < thisTime.size()) {
			if (!azGhabl.contains(thisTime.get(i)))
				azGhabl.add(thisTime.get(i));
			i++;
		}
	}

	protected void sendHelp() {
		Human me = (Human) me();
		int id = me.getID().getValue(), pos = me.getPosition().getValue();
		if (!longnHP.containsKey(id) || counterForHelp < 5) {
			lastSaidPositionCycle = time;
			byte[] byteArray = radar.codePlusOffset(Radar.HEADER_HELPUNHELP,
					allEntities.get(pos), 1);
			sendSpeak(time, behtarinChannel1, byteArray);
			if (behtarinChannel1 != 0) {
				sendSpeak(time, 0, byteArray);
			}
		}
	}

	protected void unSendHelp() {
		stuckToolKeshid = 0;
		Human me = (Human) me();
		int pos = me.getPosition().getValue();
		counterForHelp = 0;
		if (counterForUnHelp < 5) {
			lastSaidPositionCycle = time;
			byte[] byteArray = radar.codePlusOffset(Radar.HEADER_HELPUNHELP,
					allEntities.get(pos), 0);
			sendSpeak(time, behtarinChannel1, byteArray);
			if (behtarinChannel1 != 0) {
				sendSpeak(time, 0, byteArray);
			}
		}
	}

	protected void sendHelpForAmb() {
		Human me = (Human) me();
		int id = me.getID().getValue();
		if (!longnAP.containsKey(id) || counterForAmbHelp < 5) {
			lastSaidPositionCycle = time;
			int a = 0;
			if (me.isBuriednessDefined()) {
				if (me.getBuriedness() < 128)
					a = me.getBuriedness();
				else
					a = 127;
			} else
				a = 0;
			byte[] byteArray = radar.codeForAmbHelp(Radar.HEADER_AMBHELP,
					allEntities.get(me.getPosition().getValue()), me.deadtime,
					a);
			sendSpeak(time, behtarinChannel1, byteArray);
			if (behtarinChannel1 != 0) {
				sendSpeak(time, 0, byteArray);
			}
		}
	}

	protected void unSendHelpForAmb() {
		Human me = (Human) me();
		int pos = me.getPosition().getValue();
		byte[] byteArray = radar.code(Radar.HEADER_AMBUNHELP,
				allEntities.get(pos));
		lastSaidPositionCycle = time;
		sendSpeak(time, behtarinChannel1, byteArray);
		if (behtarinChannel1 != 0)
			sendSpeak(time, 0, byteArray);
	}

	protected Boolean checkCVInChangeSet(EntityID entityID) {
		for (EntityID eID : changeSet.getChangedEntities())
			if (eID == entityID)
				return true;
		return false;
	}

	protected int readmessage(ArrayList<Integer> targetID) {
		int pow = 1;
		int AgPosition = 0;
		for (int i = 0; i < targetID.size(); i++) {
			AgPosition += pow * targetID.get(i);
			pow *= 2;
		}
		return AgPosition;
	}

	protected int changeToMabnaye2(BitSet bits, int num, int start) {
		ArrayList<Integer> baghiMandeHa = new ArrayList<Integer>();
		ArrayList<Integer> barMabnaye2 = new ArrayList<Integer>();
		int maghsom = num;
		int messagesize = modelAreas.size();
		int powNumber = 0;
		while (messagesize > 0) {
			messagesize /= 2;
			powNumber++;
		}
		while (maghsom >= 2) {
			baghiMandeHa.add(maghsom % 2);
			maghsom = maghsom / 2;
		}
		baghiMandeHa.add(maghsom);
		for (int i = baghiMandeHa.size() - 1; i >= 0; i--) {
			barMabnaye2.add(baghiMandeHa.get(i));
		}
		int messageNum = barMabnaye2.size();
		int tafazol = powNumber - messageNum;
		int tool = start + tafazol;
		for (int r = start; r < tool; r++) {
			bits.set(r, false);
			start++;
		}
		for (int j = 0; j < barMabnaye2.size(); j++) {
			if (barMabnaye2.get(j) == 1) {
				bits.set(start, true);
			}
			start++;
		}
		return start;
	}

	protected void setmessage(BitSet bits) {
		Human me = (Human) me();
		int pos = me.getPosition().getValue();
		int start = Radar.headerSize;
		// ArrayList<Integer > msg=radar.toMabnaye2(allEntities.get(pos));
		// for (int j = 0; j < msg.size(); j++) {
		// if (msg.get(j) == 1)
		// bits.set(start, true);
		// start++;
		// }
		start = changeToMabnaye2(bits, allEntities.get(pos), start);
	}

	protected void sendAgPos() {
		if (time > 2) {
			BitSet bits = new BitSet();
			Radar.calcWithBitSet(bits, Radar.HEADER_AGEPOSITION, 0,
					Radar.headerSize);
			setmessage(bits);
			byte[] byteArray = Radar.toByteArray(bits);
			sendSpeak(time, behtarinChannel1, byteArray);
		}
	}

	protected HashMap<Human, Area> hearAGPosition = null;

	protected void hearAgPos() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;

			int messagesize = modelAreas.size();
			int powNumber = 0;
			while (messagesize > 0) {
				messagesize /= 2;
				powNumber++;
			}

			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						BitSet bits = new BitSet();
						bits = Radar.fromByteArray(recivedSpeak);
						if (header == Radar.HEADER_AGEPOSITION) {
							ArrayList<Integer> targetID = new ArrayList<Integer>();
							int startMessage = Radar.headerSize;
							for (int i = startMessage + powNumber - 1; i >= startMessage; i--) {
								if (bits.get(i))
									targetID.add(1);
								else
									targetID.add(0);
							}
							int read = readmessage(targetID);

							Area readRank = (Area) allRanks.get(read);
							Human h = (Human) model.getEntity(cmd.getAgentID());
							addAgentPositionToModel(h, readRank);
							hearAGPosition.put(h, readRank);
							IDandPOS.put(h.getID(), readRank.getID());
						}
					}
				}
			}
		}
	}

	protected void sendCVlInformationForNoCommi() {
		if (noCommi == true) {
			int gonjayesh = (tedadByteChannel0 - 135) * 8 - 20 - powNum - 9 - 7
					- Radar.headerSize - 1;
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_CIVILIAN, 0,
					Radar.headerSize);
			done: for (Entity entit : model
					.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
				if (start < gonjayesh) {
					Civilian cv = (Civilian) entit;
					if (cv.isPositionDefined()) {
						if (!(model.getEntity(cv.getPosition()) instanceof Building)
								|| model.getEntity(cv.getPosition()) instanceof Refuge)
							continue;
						start = Radar.calcWithBitSet(bits, cv.getID()
								.getValue(), start, radar.lengthOfCvID);
						if (model.getEntity(cv.getPosition()) instanceof AmbulanceTeam) {
							if (modelRefuges.size() > 0)
								start = Radar.calcWithBitSet(
										bits,
										allEntities.get(modelRefuges.get(0)
												.getID().getValue()), start,
										powNum);
							else
								continue done;
						} else {
							start = Radar.calcWithBitSet(bits, allEntities
									.get(cv.getPosition().getValue()), start,
									powNum);
						}
						start = Radar.calcWithBitSet(bits, cv.deadtime, start,
								9);
						if (cv.isBuriednessDefined())
							start = Radar.calcWithBitSet(bits,
									cv.getBuriedness(), start, 7);
						else
							start = Radar.calcWithBitSet(bits, 0, start, 7);
					}
				} else
					break;
			}
			bits.set(start);
			byte[] byteArray = Radar.codeToByteArray(bits);
			if (byteArray.length > 1)
				sendSpeak(time, 0, byteArray);
		}
	}

	protected void sendCVlInformation() {
		if (time < 3)
			return;
		Entity entity;
		ArrayList<Integer> properties = new ArrayList<Integer>();
		Set<EntityID> shouldCheck = new HashSet<EntityID>();
		shouldCheck.addAll(changeSet.getChangedEntities());
		for (Integer i : longnCP.keySet())
			if (model.getEntityByInt(i) instanceof Civilian) {
				shouldCheck.add(((Civilian) model.getEntityByInt(i)).getID());
			}
		done: for (EntityID entityID : shouldCheck) {
			entity = model.getEntity(entityID);
			if (entity instanceof Civilian) {
				boolean shouldSay = false;
				Civilian cv = (Civilian) entity;
				if (!cv.isPositionDefined())
					continue;
				if (cv.counterForSay < 3 && cv.counterForSay >= 0) {
					shouldSay = true;
				}
				if (!longnCP.containsKey(cv.getID().getValue())
						|| longnCP.get(cv.getID().getValue()) != cv
								.getPosition().getValue()) {
					shouldSay = true;
					cv.counterForSay = 0;
				}
				if ((cv.lastDeadTimeSaid < 0 || Math.abs(cv.lastDeadTimeSaid
						- cv.deadtime) > 25)
						&& isInChangeSet(cv.getID())) {
					shouldSay = true;
				}

				if (shouldSay) {
					if (modelAmbulanceTeams.contains(model.getEntityByInt(cv
							.getPosition().getValue())))
						continue done;
					properties.add(entity.getID().getValue());
					properties.add(cv.getPosition().getValue());
					properties.add(cv.deadtime);
					cv.lastDeadTimeSaid = cv.deadtime;
					if (cv.isBuriednessDefined()) {
						if (cv.getBuriedness() < 128)
							properties.add(cv.getBuriedness());
						else
							properties.add(127);
					} else
						properties.add(0);
				}

			}
		}

		if (properties.size() > 0) {
			BitSet bits = new BitSet();
			bits = radar.mapHeader(Radar.HEADER_CIVILIAN);
			int start = Radar.headerSize;
			int pos = 0;
			if (me() instanceof Human)
				pos = ((Human) me()).getPosition().getValue();
			else
				pos = me().getID().getValue();
			Radar.calcWithBitSet(bits, allEntities.get(pos), start, powNum);
			Area area = (Area) model.getEntityByInt(pos);
			int powNumNearBuildings = Radar
					.findPow(area.nearBuildings.size() + 1);
			start = start + powNum;
			for (int n = 0; n < properties.size(); n += 4) {
				Area a = (Area) model.getEntityByInt(properties.get(n + 1));
				if (area.nearBuildings.contains(a)) {
					start = radar.CVlCodeForID(bits, properties.get(n), start);
					start = radar.CVlCodeForPos(bits,
							area.nearBuildings.indexOf(a) + 1, start,
							powNumNearBuildings);
					start = radar
							.CVlCodeForDeadTimeAndBN(bits,
									properties.get(n + 2),
									properties.get(n + 3), start);
				} else if (pos == a.getID().getValue() && a instanceof Building) {
					start = radar.CVlCodeForID(bits, properties.get(n), start);
					start = radar.CVlCodeForPos(bits, 0, start,
							powNumNearBuildings);
					start = radar
							.CVlCodeForDeadTimeAndBN(bits,
									properties.get(n + 2),
									properties.get(n + 3), start);
				}
			}
			if (start > Radar.headerSize + powNum) {
				bits.set(start);
				byte[] byteArray = Radar.codeToByteArray(bits);
				lastSaidPositionCycle = time;
				sendSpeak(time, behtarinChannel1, Radar.codeToByteArray(bits));
				if (behtarinChannel1 != 0)
					sendSpeak(time, 0, byteArray);
			}
		}

	}

	public void sendingAUndifinededPositionCV(Civilian cv) {
		if (modelRefuges.size() > 0) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_CIVILIAN, 0,
					Radar.headerSize);
			start = Radar.calcWithBitSet(bits,
					allEntities.get(modelRefuges.get(0).getID().getValue()),
					start, powNum);
			start = radar.CVlCodeForID(bits, cv.getID().getValue(), start);
			int powNumNearBuildings = Radar
					.findPow(modelRefuges.get(0).nearBuildings.size() + 1);
			start = radar.CVlCodeForPos(bits, 0, start, powNumNearBuildings);
			start = radar.CVlCodeForDeadTimeAndBN(bits, 502, 0, start);
			bits.set(start);
			byte[] byteArray = Radar.codeToByteArray(bits);
			sendSpeak(time, behtarinChannel1, Radar.codeToByteArray(bits));
			if (behtarinChannel1 != 0)
				sendSpeak(time, 0, byteArray);
		}
	}

	protected void sendCheckForFires() {
		TreeMap<Integer, byte[]> negahdarande = new TreeMap<Integer, byte[]>();
		if (lastFiredBuildings.size() > 0) {
			for (Entry<Integer, byte[]> mf : lastFiredBuildings.entrySet()) {
				sendSpeak(time, behtarinChannel2, mf.getValue());
				if (behtarinChannel2 != 0)
					sendSpeak(time, 0, mf.getValue());
				if (mf.getKey() != 1)
					negahdarande.put(mf.getKey() - 1, mf.getValue());
			}
			lastFiredBuildings = negahdarande;
		}
	}

	protected void sendOnFireBuilds() {
		log("send on fire !");
		TreeSet<Building> hasFire = new TreeSet<Building>(
				new Comparator<Building>() {
					public int compare(Building b0, Building b1) {
						if (b0.lastTimeSeen > b1.lastTimeSeen)
							return -1;
						if (b0.lastTimeSeen < b1.lastTimeSeen)
							return 1;
						return 0;
					}
				});
		for (Building b : modelBuildings) {
			if (b.stFire == 1) {
				hasFire.add(b);
			}
		}
		ArrayList<Building> shouldSay = new ArrayList<Building>();
		int counter = 0;
		for (Building b : hasFire) {
			counter++;
			if (counter <= 7)
				shouldSay.add(b);
		}
		if (shouldSay.size() == 0)
			return;
		BitSet bits = new BitSet();
		int start = 0;
		bits = radar.mapHeader(Radar.HEADER_BUILDING);
		start = Radar.headerSize;
		start = Radar.calcWithBitSet(bits, shouldSay.size(), start, 3);
		for (Building b : shouldSay) {
			start = Radar.calcWithBitSet(bits, EntityBeInt.get(b.getID()),
					start, Radar.findPow(EntityBeInt.size()));
			start = Radar.calcWithBitSet(bits, Math.min(b.lastTimeSeen, 256),
					start, 8);
		}
		byte[] byteArray = Radar.toByteArray(bits);
		String s = "";
		// for(int i = 0; i < bits.size() ; i++){
		// if(bits.get(i))
		// s+="1";
		// else
		// s+="0";
		// }
		// log("chi goftamm?"+s);
		sendSpeak(time, 0, byteArray);
	}

	protected void hearOnFireBuilds() {
		log(" hear on fire builds");
		if (heard.size() > 0) {
			log(" heard > 0");
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntity(cmd.getAgentID());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_BUILDING) {
							BitSet bits = new BitSet();
							bits = Radar.fromByteArray(recivedSpeak);
							// String s = "";
							// for(int i = 0; i < bits.size() ; i++){
							// if(bits.get(i))
							// s+="1";
							// else
							// s+="0";
							// }
							// log("chi shenidammm?"+s + " az ki shenidam: " +
							// entity);
							int startMassage = Radar.headerSize;
							int shouldRead = Radar.fromMabnaye2ToMabnaye10(
									bits, startMassage + 3, startMassage);
							startMassage += 3;
							int powNumber = Radar.findPow(EntityBeInt.size());
							for (int i = 0; i < shouldRead; i++) {
								int onFireBuildingIndex = Radar
										.fromMabnaye2ToMabnaye10(bits,
												startMassage + powNumber,
												startMassage);
								Building onFireBuilding = (Building) model
										.getEntity(IntBeEntity
												.get(onFireBuildingIndex));
								startMassage += powNumber;
								int lastTimeSeen = Radar
										.fromMabnaye2ToMabnaye10(bits,
												startMassage + 8, startMassage);
								// log(" shenidam : "
								// + onFireBuilding.getID().getValue()+
								// "last time seen:"+ lastTimeSeen);
								startMassage += 8;
								if (onFireBuilding.lastTimeSeen < lastTimeSeen) {
									log("building:"
											+ onFireBuilding.getID().getValue());
									log("lts:" + onFireBuilding.lastTimeSeen);
									onFireBuilding.stFire = 1;
									onFireBuilding.lastTimeSeen = lastTimeSeen;
									log("lts2: " + onFireBuilding.lastTimeSeen);
								}
							}
						}
					}
				}
			}
		}
	}

	protected void sendBuildingInformation() {
		Entity entity;
		Building building;
		Area pos = null;
		if (me() instanceof Human)
			pos = (Area) model.getEntity(((Human) me()).getPosition());
		else
			pos = (Area) me();

		for (EntityID entityID : changeSet.getChangedEntities()) {
			entity = model.getEntity(entityID);
			if (entity instanceof Building) {
				building = (Building) entity;
				int rf = calculatedRF(building);
				if (building.shouldSend
						|| ((rf > 0 || rf != building.lastFireness) && (!longnBP
								.containsKey(building.getID().getValue()) || longnBP
								.get(building.getID().getValue()) != rf))) {
					boolean booli = false;
					done: for (Entry<Area, ArrayList<Building>> b : buildsOfFire
							.entrySet())
						if (b.getValue().contains(building)) {
							if (rf != building.lastFireness
									|| building.realLastTimeSeen != building.lastTimeSeen) {
								if (rf == building.lastFireness)
									booli = true;
								building.lastFireness = rf;
								if (b.getValue().size() == 1)
									buildsOfFire.remove(b.getKey());
								b.getValue().remove(building);
							}
							break done;
						}
					if (!booli) {
						if (buildsOfFire.keySet().contains(pos)) {
							buildsOfFire.get(pos).add(building);
							building.timesSaid = 0;
						} else {
							ArrayList<Building> build = new ArrayList<Building>();
							build.add(building);
							building.timesSaid = 0;
							buildsOfFire.put(pos, build);
						}
					}
				}
			}
		}
		afsoon: for (Iterator iterator = buildsOfFire.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<Area, ArrayList<Building>> entry = (Entry<Area, ArrayList<Building>>) iterator
					.next();
			afzal: for (Iterator iterator2 = entry.getValue().iterator(); iterator2
					.hasNext();) {
				Building buildin = (Building) iterator2.next();
				if (((buildin.timesSaid == 2 || buildin.timesSaid == -1) && !lowCommi)
						|| (lowCommi && (buildin.timesSaid == 5 || buildin.timesSaid == -1))
						|| (lowCommi && time - buildin.realLastTimeSeen > 7)
						|| (!lowCommi && time - buildin.realLastTimeSeen > 1)) {
					if (entry.getValue().size() == 1) {
						iterator.remove();
						continue afsoon;
					} else {
						iterator2.remove();
						continue afzal;
					}
				}
			}
		}

		ArrayList<Building> notInNears = new ArrayList<Building>();
		for (Entry<Area, ArrayList<Building>> b : buildsOfFire.entrySet()) {
			ArrayList<Building> thisAreaNears = new ArrayList<Building>();
			for (Building buildi : b.getValue()) {
				buildi.timesSaid++;
				if (b.getKey().nearBuildings.contains(buildi)
						|| buildi.equals(b.getKey())) {
					if (!thisAreaNears.contains(buildi))
						thisAreaNears.add(buildi);
				} else {
					if (!notInNears.contains(buildi))
						notInNears.add(buildi);
				}
			}
			if (thisAreaNears.size() > 0) {
				BitSet bits = new BitSet();
				int start = 0;
				bits = radar.mapHeader(Radar.HEADER_BUILDING);
				start = Radar.headerSize;
				start = Radar.calcWithBitSet(bits,
						allEntities.get(b.getKey().getID().getValue()), start,
						radar.powNum);
				int powNumNearBuildings = Radar
						.findPow(b.getKey().nearBuildings.size() + 1);
				for (Building bui : thisAreaNears) {
					if (b.getKey().nearBuildings.contains(bui)) {
						start = Radar.calcWithBitSet(bits,
								b.getKey().nearBuildings.indexOf(bui) + 1,
								start, powNumNearBuildings);
					} else if (bui.equals(b.getKey())) {
						start = Radar.calcWithBitSet(bits, 0, start,
								powNumNearBuildings);
					}
					// start = Radar.calcWithBitSet(bits, bui.stFire, start,
					// Radar.tedadBitFireness);
					start = Radar.calcWithBitSet(bits, calculatedRF(bui),
							start, Radar.tedadBitFireness);
					if (lowCommi) {
						start = Radar.calcWithBitSet(bits, time
								- bui.realLastTimeSeen, start, 3);
					} else {
						start = Radar.calcWithBitSet(bits, time
								- bui.realLastTimeSeen, start, 1);
					}
				}
				bits.set(start);
				byte[] byteArray = Radar.toByteArray(bits);
				sendSpeak(time, behtarinChannel1, byteArray);
				if (behtarinChannel1 != 0)
					sendSpeak(time, 0, byteArray);
			}
		}
		if (notInNears.size() > 0) {
			BitSet bits = new BitSet();
			bits = radar
					.mapHeader(Radar.HEADER_BUILDING_OUT_OF_REACH_NEAR_ROADS);
			int start = Radar.headerSize;
			for (Building buildin : notInNears) {
				if (lowCommi) {
					start = radar.buildingCode(bits,
							allEntities.get(buildin.getID().getValue()),
							calculatedRF(buildin), time
									- buildin.realLastTimeSeen, 3, start);
				} else
					start = radar.buildingCode(bits,
							allEntities.get(buildin.getID().getValue()),
							calculatedRF(buildin), time
									- buildin.realLastTimeSeen, 1, start);
			}
			bits.set(start);
			byte[] byteArray = Radar.toByteArray(bits);
			sendSpeak(time, behtarinChannel1, byteArray);
			if (behtarinChannel1 != 0)
				sendSpeak(time, 0, byteArray);
		}
	}

	protected int sendBlockades(Area _area, boolean[] isUsefull, BitSet bits,
			int start, int num, int areaPow) {
		worldGraph.Area area = _area.worldGraphArea;
		int a = area.enterances.size();
		int[][] array = new int[a][a];
		for (Enterance e : area.enterances)
			for (Enterance f : e.internalEnterances)
				array[e.id][f.id] = 1;
		for (Enterance e : area.enterances) {
			if (e.isItConnectedToNeighbour == true)
				array[e.id][e.id] = 1;
			else
				array[e.id][e.id] = 0;
		}
		int home = radar.calcForBlockade(array, isUsefull, bits, start, num,
				areaPow);
		return home;
	}

	protected void hearBlockades(byte[] byteArray, boolean isSelfHear,
			Entity IDon) {
		Area _area = (Area) allRanks.get(radar.decodeIDForBlockade(byteArray));
		if (IDon instanceof Human)
			addAgentPositionToModel(IDon, _area);
		if (isSelfHear)
			return;
		BitSet bits = Radar.fromByteArray(byteArray);
		int lastBitPos = -1;
		for (lastBitPos = byteArray.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int start = Radar.headerSize + powNum + 1;
		while (start < lastBitPos) {
			int thisID = Radar.fromMabnaye2ToMabnaye10(bits,
					start + Radar.findPow(_area.nearRoads.size() + 1), start);
			int[][] array = radar.unCalcForBlockade(bits, _area, start, _area
					.getID().getValue(), logger, model);
			if (thisID == 0)
				thisID = _area.getID().getValue();
			else
				thisID = _area.nearRoads.get(thisID - 1).getID().getValue();
			Area thisArea = (Area) model.getEntityByInt(thisID);
			thisArea.lastTimeSeen = time - 1;
			worldGraph.Area thisWorldGraphArea = thisArea.worldGraphArea;
			start = radar.shoro;
			if (isInChangeSet(thisArea.getID()))
				continue;
			for (int i = 0; i < array.length; i++)
				thisWorldGraphArea.enterances.get(i).internalEnterances.clear();
			for (int i = 0; i < array.length; i++)
				for (int j = i + 1; j < array.length; j++) {
					if (array[i][j] == 1) {
						thisWorldGraphArea.enterances.get(i).internalEnterances
								.add(thisWorldGraphArea.enterances.get(j));
						thisWorldGraphArea.enterances.get(j).internalEnterances
								.add(thisWorldGraphArea.enterances.get(i));
					}
				}

			for (int a = 0; a < array.length; a++) {
				boolean connectivity = (array[a][a] == 1);
				thisWorldGraphArea.enterances.get(a).isItConnectedToNeighbour = connectivity;
				thisWorldGraphArea.enterances.get(a).neighbour.isItConnectedToNeighbour = connectivity;
			}
			radar.sentBlockades.add(thisArea.getID().getValue());
		}
	}

	protected void hearBlockadesAD(byte[] byteArray, boolean isSelfHear) {
		if (isSelfHear)
			return;
		BitSet bits = Radar.fromByteArray(byteArray);
		int lastBitPos = -1;
		for (lastBitPos = byteArray.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int start = Radar.headerSize + 1;
		while (start < lastBitPos) {
			int[][] array = radar.unCalcForBlockadeAD(bits, start, allRanks,
					model, logger);
			Area thisArea = (Area) allRanks.get(Radar.fromMabnaye2ToMabnaye10(
					bits, start + powNum, start));
			thisArea.lastTimeSeen = time - 1;
			worldGraph.Area thisWorldGraphArea = thisArea.worldGraphArea;
			start = radar.shoroAD;
			if (isInChangeSet(thisArea.getID()))
				continue;
			for (int i = 0; i < array.length; i++)
				thisWorldGraphArea.enterances.get(i).internalEnterances.clear();
			for (int i = 0; i < array.length; i++)
				for (int j = i + 1; j < array.length; j++) {
					if (array[i][j] == 1) {
						thisWorldGraphArea.enterances.get(i).internalEnterances
								.add(thisWorldGraphArea.enterances.get(j));
						thisWorldGraphArea.enterances.get(j).internalEnterances
								.add(thisWorldGraphArea.enterances.get(i));
					}

				}
			for (int a = 0; a < array.length; a++) {
				boolean connectivity = (array[a][a] == 1) ? true : false;
				thisWorldGraphArea.enterances.get(a).isItConnectedToNeighbour = connectivity;
				thisWorldGraphArea.enterances.get(a).neighbour.isItConnectedToNeighbour = connectivity;
			}
			radar.sentBlockades.add(thisArea.getID().getValue());
		}
	}

	protected void sendNoWay(ArrayList<Integer> ids, Area myPosition, Boolean x) {
		BitSet bits = new BitSet();
		int start = 0;
		if (x == true) {
			lastSaidPositionCycle = time;
			start = Radar.calcWithBitSet(bits, Radar.HEADER_NOWAY, start,
					Radar.headerSize);
			start++;
			start = Radar.calcWithBitSet(bits,
					allEntities.get(myPosition.getID().getValue()), start,
					powNum);
			for (int i = 0; i < ids.size(); i++)
				start = Radar.calcWithBitSet(bits, ids.get(i), start,
						Radar.findPow(myPosition.nearRoads.size() + 1));
		} else {
			start = Radar.calcWithBitSet(bits, Radar.HEADER_NOWAY, start,
					Radar.headerSize);
			bits.set(start);
			start++;
			for (int i = 0; i < ids.size(); i++)
				start = Radar.calcWithBitSet(bits, allEntities.get(ids.get(i)),
						start, powNum);
		}
		bits.set(start);
		byte[] byteArray = Radar.codeToByteArray(bits);
		sendSpeak(time, behtarinChannel1, byteArray);
		if (behtarinChannel1 != 0)
			sendSpeak(time, 0, byteArray);
	}

	protected void hearNoWay() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					ArrayList<Integer> IDs = new ArrayList<Integer>();
					if (recivedSpeak.length > 0) {
						if (radar.tellHeader(recivedSpeak) == Radar.HEADER_NOWAY) {
							if (radar.tellHeaderForPC(recivedSpeak) == true) {
								IDs = radar.decodeIDForNoWay(this, entity,
										recivedSpeak, model, allRanks);
								for (int i = 1; i < IDs.size(); i++) {
									int ID = IDs.get(i);
									if (!changeSet.getChangedEntities()
											.contains(
													model.getEntityByInt(ID)
															.getID())) {
										Area _area = (Area) model
												.getEntityByInt(ID);
										_area.lastTimeSeen = time - 1;
										worldGraph.Area area = _area.worldGraphArea;
										for (Enterance enterance : area.enterances) {
											enterance.internalEnterances
													.clear();
											enterance.isItConnectedToNeighbour = enterance.neighbour.isItConnectedToNeighbour = false;
										}
										radar.sentBlockades.add(ID);
									}
								}
							} else if (radar.tellHeaderForPC(recivedSpeak) == false) {
								IDs = radar.decodeIDForNoWayAD(recivedSpeak,
										model, allRanks);
								for (int i = 0; i < IDs.size(); i++) {
									int ID = IDs.get(i);
									if (!changeSet.getChangedEntities()
											.contains(
													model.getEntityByInt(ID)
															.getID())) {
										Area _area = (Area) model
												.getEntityByInt(ID);
										_area.lastTimeSeen = time - 1;
										worldGraph.Area area = _area.worldGraphArea;
										for (Enterance enterance : area.enterances) {
											enterance.internalEnterances
													.clear();
											enterance.isItConnectedToNeighbour = enterance.neighbour.isItConnectedToNeighbour = false;
										}
										radar.sentBlockades.add(ID);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	protected void checkWays() {
		if (me() instanceof Human && !lowCommi) {
			ArrayList<Integer> sendNoWayIDs = new ArrayList<Integer>();
			ArrayList<Integer> sendNoWayAD = new ArrayList<Integer>();
			Human me = (Human) me();
			int start = 0;
			BitSet bits = new BitSet();
			ArrayList<Entity> notInNearestButInChangeSet = new ArrayList<Entity>();
			int pos = me.getPosition().getValue();
			start = Radar.calcWithBitSet(bits, Radar.HEADER_BLOCKADE, start,
					Radar.headerSize);
			start = Radar.calcWithBitSet(bits, 0, start, 1);
			start = Radar
					.calcWithBitSet(bits,
							allEntities.get(me.getPosition().getValue()),
							start, powNum);
			Area _area = (Area) (model.getEntityByInt(pos));
			int areaPow = Radar.findPow(_area.nearRoads.size() + 1);
			for (EntityID entityID : changeSet.getChangedEntities()) {
				if (model.getEntityByInt(entityID.getValue()) instanceof Road) {
					Entity entit = model.getEntityByInt(entityID.getValue());
					if (_area.nearRoads.contains(entit)
							&& !sentBlockades
									.contains(entit.getID().getValue())) {
						int num = _area.nearRoads.indexOf(entit);
						boolean[] isUseful = new boolean[1];
						isUseful[0] = true;
						start = sendBlockades((Area) entit, isUseful, bits,
								start, num + 1, areaPow);
						if (isUseful[0] == false)
							sendNoWayIDs.add(num + 1);
					} else if (!sentBlockades
							.contains(entit.getID().getValue())) {
						if (entit.getID().getValue() != me.getPosition()
								.getValue())
							notInNearestButInChangeSet.add(entit);
						else {
							boolean[] isUseful = new boolean[1];
							isUseful[0] = true;
							start = sendBlockades((Area) entit, isUseful, bits,
									start, 0, areaPow);
							if (isUseful[0] == false)
								sendNoWayIDs.add(0);
						}
					}
				}
			}
			bits.set(start);
			if (start > Radar.headerSize + powNum + 1) {
				byte[] byteArray = Radar.toByteArray(bits);
				sendSpeak(time, behtarinChannel1, byteArray);
				lastSaidPositionCycle = time;
				if (behtarinChannel1 != 0)
					sendSpeak(time, 0, byteArray);
			}
			if (notInNearestButInChangeSet.size() > 0) {
				bits = new BitSet();
				start = Radar.calcWithBitSet(bits, Radar.HEADER_BLOCKADE, 0,
						Radar.headerSize);
				start = Radar.calcWithBitSet(bits, 1, start, 1);
				boolean[] isUsefull = new boolean[1];
				for (Entity entit : notInNearestButInChangeSet) {
					isUsefull[0] = true;
					worldGraph.Area area_ = ((Area) entit).worldGraphArea;
					int a = area_.enterances.size();
					int[][] array = new int[a][a];
					for (Enterance e : area_.enterances)
						for (Enterance f : e.internalEnterances)
							array[e.id][f.id] = 1;
					for (Enterance e : area_.enterances) {
						if (e.isItConnectedToNeighbour == true)
							array[e.id][e.id] = 1;
						else
							array[e.id][e.id] = 0;
					}
					Radar.mark(array);
					int home = 0;
					BitSet jaSet = new BitSet();
					for (int b = 0; b < array.length; b++) {
						for (int i = b + 1; i < array.length; i++) {
							if (array[b][i] != 2) {
								if (array[b][i] == 1)
									jaSet.set(home);
								home++;
							}
						}
					}
					for (int b = 0; b < array.length; b++) {
						if (array[b][b] == 1)
							jaSet.set(home);
						home++;
					}
					int tedad1Ha = 0;
					for (int b = 0; b < home; b++)
						if (jaSet.get(b) == true)
							tedad1Ha++;
					if (tedad1Ha == home) {
						isUsefull[0] = false;
					} else {
						if (tedad1Ha == 0) {
							sendNoWayAD.add(entit.getID().getValue());
						} else {
							start = Radar.calcWithBitSet(bits,
									allEntities.get(entit.getID().getValue()),
									start, powNum);
							for (int i = 0; i < home; i++)
								if (jaSet.get(i) == true)
									bits.set(start + i);
							start += home;
						}
					}
				}

				bits.set(start);
				if (start > Radar.headerSize + 1) {
					byte[] byteArray = Radar.toByteArray(bits);
					sendSpeak(time, behtarinChannel1, byteArray);
					if (behtarinChannel1 != 0)
						sendSpeak(time, 0, byteArray);
				}
			}
			if (sendNoWayIDs.size() > 0)
				sendNoWay(sendNoWayIDs, _area, true);
			if (sendNoWayAD.size() > 0) {
				sendNoWay(sendNoWayAD, _area, false);
			}
		}
	}

	protected void hearWay() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						if (radar.tellHeader(recivedSpeak) == Radar.HEADER_BLOCKADE)
							if (radar.tellHeaderForPC(recivedSpeak) == true) {
								hearBlockades(recivedSpeak, cmd.getAgentID()
										.equals(me().getID()), entity);
							} else if (radar.tellHeaderForPC(recivedSpeak) == false)
								hearBlockadesAD(recivedSpeak, cmd.getAgentID()
										.equals(me().getID()));
					}
				}
			}
		}
	}

	protected void hearHelp() {
		int countereAzKhodShenide = 0;
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						BitSet bits = new BitSet();
						bits = Radar.fromByteArray(recivedSpeak);
						int header = Radar.fromMabnaye2ToMabnaye10(bits,
								Radar.headerSize, 0);
						if (header == Radar.HEADER_HELPUNHELP
								&& bits.get(Radar.headerSize) == true) {
							int id = entity.getID().getValue();
							Area pos = (Area) allRanks
									.get(Radar
											.fromMabnaye2ToMabnaye10(bits,
													Radar.headerSize
															+ radar.powNum + 1,
													Radar.headerSize + 1));
							longnHP.put(id, pos.getID().getValue());
							addAgentPositionToModel(entity, pos);
							pos.lastTimeSeen = time - 1;
							if (id == me().getID().getValue()) {
								countereAzKhodShenide++;
								counterForHelp++;
							}
						}
					}
				}
			}
		}
		if (countereAzKhodShenide == 2)
			counterForHelp--;
	}

	protected void unHearHelp() {
		int countereAzKhodShenide = 0;
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						// int header = radar.tellHeader(recivedSpeak);
						BitSet bits = new BitSet();
						bits = Radar.fromByteArray(recivedSpeak);
						int header = Radar.fromMabnaye2ToMabnaye10(bits,
								Radar.headerSize, 0);
						if (header == Radar.HEADER_HELPUNHELP
								&& bits.get(Radar.headerSize) == false) {
							int id = entity.getID().getValue();
							Area pos = (Area) allRanks
									.get(Radar
											.fromMabnaye2ToMabnaye10(bits,
													Radar.headerSize
															+ radar.powNum + 1,
													Radar.headerSize + 1));
							pos.lastTimeSeen = time - 1;
							addAgentPositionToModel(entity, pos);
							if (longnHP.containsKey(id))
								longnHP.remove(id);
							if (id == me().getID().getValue()) {
								counterForUnHelp++;
								countereAzKhodShenide++;
							}
						}
					}
				}
			}
		}
		if (countereAzKhodShenide == 2)
			counterForUnHelp--;
	}

	protected void hearHelpForAmb() {
		int counterAzKhodShenide = 0;
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_AMBHELP) {
							int id = entity.getID().getValue();
							ArrayList<Integer> IDP = radar
									.decodeForAmbHelp(recivedSpeak);
							if (entity instanceof Human) {
								Human human = ((Human) entity);
								Area pos = (Area) allRanks.get(IDP.get(0));
								human.deadtime = IDP.get(1);
								human.setBuriedness(IDP.get(2));
								if (lowCommi && human instanceof PoliceForce)
									policeStatus.set(
											modelPoliceForces.indexOf(human),
											-1);
								longnAP.put(id, pos.getID().getValue());
								pos.lastTimeSeen = time - 1;
								((Human) entity).hasBuriedness = true;
								if (id == me().getID().getValue()) {
									counterForAmbHelp++;
									counterAzKhodShenide++;
								}
								addAgentPositionToModel(entity, pos);
							}
						}
					}
				}
			}
		}
		if (counterAzKhodShenide == 2)
			counterForAmbHelp--;
	}

	protected void unHearHelpForAmb() {
		int countereAzKhodShenide = 0;
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_AMBUNHELP) {
							int id = entity.getID().getValue();
							longnAP.remove(id);
							Area pos = (Area) allRanks.get(radar
									.decode(recivedSpeak));
							pos.lastTimeSeen = time - 1;
							((Human) model.getEntityByInt(id)).hasBuriedness = false;
							if (lowCommi && entity instanceof PoliceForce)
								policeStatus.set(
										modelPoliceForces.indexOf(entity), 0);
							addAgentPositionToModel(entity, pos);
							if (id == me().getID().getValue()) {
								counterForAmbUnHelp++;
								countereAzKhodShenide++;
							}
						}
					}
				}
			}
		}
		if (countereAzKhodShenide == 2)
			counterForAmbUnHelp--;
	}

	protected void hearCVlInformation() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_CIVILIAN) {
							ArrayList<Integer> al = new ArrayList<Integer>();
							al.addAll(radar.CVlDecode(recivedSpeak, model,
									allRanks, noCommi));
							if (model.getEntityByInt(cmd.getAgentID()
									.getValue()) instanceof Human && !noCommi)
								addAgentPositionToModel(entity,
										(Area) model.getEntityByInt(al.get(0)));
							for (int i = 1; i < al.size(); i += 4) {
								int id = al.get(i);
								int pos = al.get(i + 1);
								if ((model.getEntityByInt(id) == null || !changeSet
										.getChangedEntities().contains(
												model.getEntityByInt(id)
														.getID()))
										&& pos != me().getID().getValue()) {
									int deadTime = al.get(i + 2);
									int BN = al.get(i + 3);
									if (cmd.getAgentID().equals(me().getID()) == false) {
										Entity e = model.getEntityByInt(id);
										if (e == null
												|| (e != null && checkCVInChangeSet(e
														.getID()) == false)) {

											addCVlToModel(id, pos, deadTime, BN);
										}
									}
								}
								if (model.getEntityByInt(id) instanceof Civilian
										&& cmd.getAgentID().getValue() == me()
												.getID().getValue()) {
									((Civilian) model.getEntityByInt(id)).counterForSay++;
									log(" ID:  "
											+ model.getEntityByInt(id).getID()
													.getValue()
											+ " az: "
											+ cmd.getAgentID().getValue()
											+ "  "
											+ ((Civilian) model
													.getEntityByInt(id)).counterForSay);
								}
								longnCP.put(id, pos);
							}
						}
					}
				}
			}
		}
	}

	protected void addCVlToModel(int num, int pos, int deadTime, int BN) {
		Entity existingEntity = model.getEntityByInt(num);
		if (existingEntity == null || !isInChangeSet(existingEntity.getID())) {
			EntityID eID = new EntityID(num);
			if (existingEntity == null) {
				existingEntity = Registry.getCurrentRegistry().createEntity(
						StandardEntityURN.CIVILIAN.toString(), eID);
				model.addEntity(existingEntity);
			}
			Entity e = model.getEntityByInt(pos);
			((Area) e).lastTimeSeen = time - 1;
			((Area) e).lastTimeVisit = time - 1;
			Civilian cv = ((Civilian) existingEntity);
			cv.setPosition(e.getID());
			cv.deadtime = deadTime;
			cv.setBuriedness(BN);
			if (BN != 0)
				cv.hasBuriedness = true;
			else
				cv.hasBuriedness = false;
			if (e instanceof Area) {
				cv.setX(((Area) e).getX());
				cv.setY(((Area) e).getY());
			}
		}
	}

	protected void updateConnections(Area lastPos, Area currentPos) {
		// log("inja am nmiai???");?
		if (lastPos.equals(currentPos))
			return;
		ArrayList<EntityID> minPath = wg.getMinPath(lastPos.getID(),
				currentPos.getID(), false);
		// log("last pos:" + lastPos);
		// log("current pos:" + currentPos);
		// log("andaze ye folan:" + minPath.size());
		Enterance lastEnt = null;
		for (int i = 0; i < minPath.size() - 1; i++) {
			EntityID next = minPath.get(i + 1);
			Area area = (Area) model.getEntity(minPath.get(i));
			Enterance enterance = null;
			for (Enterance e : area.worldGraphArea.enterances)
				if (e.neighbourAreaInModel.getID().equals(next)) {
					enterance = e;
					break;
				}
			// log("eee : " + enterance.center.getX() + ", "
			// + enterance.center.getY());
			enterance.isItConnectedToNeighbour = true;
			enterance.neighbour.isItConnectedToNeighbour = true;
			if (lastEnt != null
					&& !lastEnt.internalEnterances.contains(enterance)) {
				log("bababababababbabba");
				for (Enterance e : enterance.internalEnterances) {
					e.internalEnterances.addAll(lastEnt.internalEnterances);
					e.internalEnterances.add(lastEnt);
				}
				for (Enterance e : lastEnt.internalEnterances) {
					e.internalEnterances.addAll(enterance.internalEnterances);
					e.internalEnterances.add(enterance);
				}

				ArrayList<Enterance> copy = (ArrayList<Enterance>) lastEnt.internalEnterances
						.clone();
				lastEnt.internalEnterances.addAll(enterance.internalEnterances);
				lastEnt.internalEnterances.add(enterance);

				enterance.internalEnterances.addAll(copy);
				enterance.internalEnterances.add(lastEnt);
			}
			lastEnt = enterance.neighbour;
		}
	}

	public void addAgentPositionToModel(Entity existingEntity, Area pos) {
		if (pos instanceof Building)
			((Building) pos).lastTimeVisit = time - 1;

		if (existingEntity != null
				&& !(changeSet.getChangedEntities().contains(existingEntity))
				&& existingEntity != me() && existingEntity instanceof Human) {
			Human human = (Human) existingEntity;
			if (human.isPositionDefined()) {
				// log("ki e in?" + human.getID().getValue() + " chizesh :"
				// + human.lastHeardPosFrom);
				// if (human.lastHeardPosFrom == time - 1)
				// updateConnections((Area) human.getPosition(model), pos);
			}
			human.setX(pos.getX());
			human.setY(pos.getY());
			human.setPosition(pos.getID());
			human.lastHeardPosFrom = time;
			pos.lastTimeSeen = time - 1;
		}
	}

	protected void addBuildingToModel(Entity existingEntity, int firyness) {
		if (existingEntity != null
				&& !(changeSet.getChangedEntities().contains(existingEntity))) {
			Building b = (Building) existingEntity;
			hearRF(b, firyness);
			b.isSimulatedForFire = false;
			// b.isSimulsted = false;
			// b.setFieryness(firyness);
		}
	}

	ArrayList<Building> sookhteBuildings = new ArrayList<Building>();

	protected void hearBuildingInformation() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntity(cmd.getAgentID());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_BUILDING) {
							int id, fireness, lts;
							ArrayList<Integer> al = new ArrayList<Integer>();
							al.addAll(radar.BuildingDecode(recivedSpeak, model,
									allRanks, time, lowCommi));
							Area posesh = (Area) model
									.getEntityByInt(al.get(0));
							if (!cmd.getAgentID().equals(me().getID())
									&& entity instanceof Human
									&& ((Human) entity).getPosition()
											.getValue() != posesh.getID()
											.getValue()
									&& !isInChangeSet(entity.getID()))
								addAgentPositionToModel(entity, posesh);
							for (int i = 1; i < al.size(); i += 3) {
								id = al.get(i);
								fireness = al.get(i + 1);
								lts = al.get(i + 2);
								if (!cmd.getAgentID().equals(me().getID())) {
									Building e = (Building) model
											.getEntityByInt(id);
									if (isInChangeSet(e.getID()) == false
											&& lts >= e.lastTimeSeen) {
										addBuildingToModel(e, fireness);
										e.lastTimeSeen = lts;
									}
								}
								longnBP.put(id, fireness);
								Building b = (Building) model
										.getEntityByInt(id);
								if (b.isFierynessDefined()
										&& b.getFieryness() > 0
										&& !sookhteBuildings.contains(b))
									sookhteBuildings.add(b);
							}
						} else if (header == Radar.HEADER_BUILDING_OUT_OF_REACH_NEAR_ROADS) {
							ArrayList<Integer> al = new ArrayList<Integer>();
							al.addAll(radar.BuildingDecodeOutOfReach(
									recivedSpeak, time, lowCommi, noCommi));
							for (int i = 1; i < al.size(); i += 3) {
								Building id = (Building) allRanks
										.get(al.get(i));
								int fireness = al.get(i + 1);
								int lts = al.get(i + 2);
								if (cmd.getAgentID().equals(me().getID()) == false) {
									if (isInChangeSet(id.getID()) == false
											&& lts >= id.lastTimeSeen) {
										addBuildingToModel(id, fireness);
										id.lastTimeSeen = lts;
									}
								}
								longnBP.put(id.getID().getValue(), fireness);
								if (id.isFierynessDefined()
										&& id.getFieryness() > 0
										&& !sookhteBuildings.contains(id))
									sookhteBuildings.add(id);
							}
						}
					}
				}
			}
		}
	}

	public void hearRF(Building build, int rf) {
		if (rf % 2 == 0) {
			build.timeOfStartOfFire = -1;
			build.stFire = 0;
		} else {
			build.stFire = 1;
			if (build.timeOfStartOfFire == -1)
				build.timeOfStartOfFire = time;
		}
		if (rf < 2)
			build.setFieryness(0);
		if (rf == 2 || rf == 3)
			build.setFieryness(1);
		if (rf == 4 || rf == 5)
			build.setFieryness(5);
		if (rf == 6 || rf == 7)
			build.setFieryness(8);
		build.lastFireness = rf;
	}

	public void setSTFire() {
		for (EntityID id : changeSet.getChangedEntities()) {
			if (model.getEntity(id) instanceof Building) {
				Building build = (Building) model.getEntity(id);
				if (build.isFierynessDefined() && build.getFieryness() == 0) {
					if (build.isTemperatureDefined()
							&& build.getTemperature() > 20) {
						build.stFire = 1;
					} else {
						build.stFire = 0;
					}
				}
				if (build.isFierynessDefined()
						&& (build.getFieryness() > 0 && build.getFieryness() < 4)) {
					build.stFire = 1;
				}
				if (build.isFierynessDefined() && build.getFieryness() > 4) {
					build.stFire = 0;
				}
				if (build.isFierynessDefined() && build.getFieryness() == 4) {
					if (build.isTemperatureDefined()
							&& build.getTemperature() > 20) {
						build.stFire = 1;
					} else {
						build.stFire = 0;
					}
				}
				if (build instanceof Refuge || build instanceof PoliceOffice
						|| build instanceof FireStation
						|| build instanceof AmbulanceCentre) {
					build.stFire = 0;
				}
			}
		}
	}

	public int calculatedRF(Building build) {
		int rf = 0;
		if (build.isFierynessDefined()) {
			if (build.getFieryness() == 0)
				rf = build.stFire;
			if (build.isOnFire())
				rf = 2 + build.stFire;
			if (build.getFieryness() > 3 && build.getFieryness() < 8)
				rf = 4 + build.stFire;

			if (build.getFieryness() == 8)
				rf = 6 + build.stFire;
		}
		return rf;
	}

	ArrayList<Integer> subzoneIDs = new ArrayList<Integer>();

	public int nearestPolice(int x, int y, ArrayList<Integer> freePolices,
			boolean checkFirstPosition) {
		int nearest = Integer.MAX_VALUE;
		if (!checkFirstPosition) {
			double minpath = Double.MAX_VALUE;
			for (int p : freePolices) {
				Entity pe = model.getEntityByInt(p);
				double dist = Mathematic.getDistance(x, y, ((Human) pe).getX(),
						((Human) pe).getY());
				if (minpath > dist) {
					nearest = p;
					minpath = dist;
				}
			}
		}
		if (checkFirstPosition) {
			double minpath = Double.MAX_VALUE;
			for (int p : freePolices) {
				Entity pe = model.getEntityByInt(policeFirstPositions.get(p));
				double dist = Mathematic.getDistance(x, y, ((Area) pe).getX(),
						((Area) pe).getY());
				if (minpath > dist) {
					nearest = p;
					minpath = dist;
				}
			}
		}
		if (nearest != Integer.MAX_VALUE)
			return nearest;
		return -1;
	}

	byte[] lastSaidByteArray = null;
	private ArrayList<Road> notClearedRoads = new ArrayList<Road>();
	private ArrayList<byte[]> lastByteArrays = new ArrayList<byte[]>();

	protected void sendClearerArea() {
		if (!(this instanceof PoliceForceAgent))
			return;
		ArrayList<Road> clearedRoad = new ArrayList<Road>();
		for (EntityID ent : changeSet.getChangedEntities()) {
			Entity entity = model.getEntity(ent);
			if (entity instanceof Road) {
				worldGraph.Area area = ((Area) entity).worldGraphArea;
				if (area.enterances.size() == 0
						|| area.enterances.get(0).internalEnterances.size() == area.enterances
								.size() - 1)
					clearedRoad.add((Road) entity);
			}
		}

		lastByteArrays = new ArrayList<byte[]>();
		for (Road road : notClearedRoads)
			if (clearedRoad.contains(road)) {
				byte[] byteArray = radar.codeForClearer(
						allEntities.get(road.getID().getValue()),
						road.worldGraphArea);

				byte[] copyByteArray = new byte[byteArray.length];
				for (int i = 0; i < byteArray.length; i++) {
					copyByteArray[i] = byteArray[i];
				}
				lastByteArrays.add(copyByteArray);

				sendSpeak(time, behtarinChannel1, byteArray);
				if (behtarinChannel1 != 0)
					sendSpeak(time, 0, byteArray);
			}

		notClearedRoads = new ArrayList<Road>();
		for (EntityID ent : changeSet.getChangedEntities()) {
			Entity entity = model.getEntity(ent);
			if (entity instanceof Road) {
				worldGraph.Area area = ((Area) entity).worldGraphArea;
				if (area.enterances.size() > 0
						&& area.enterances.get(0).internalEnterances.size() != area.enterances
								.size() - 1)
					notClearedRoads.add((Road) entity);
			}
		}
	}

	protected void sendClearer() {
		if (this instanceof PoliceForceAgent) {
			if (time > 3) {
				if (commandHistory.get(time - 1).getAction()
						.equals(StandardMessageURN.AK_CLEAR)) {
					int lastClearedTarget = commandHistory.get(time - 1).lastClearedTarget
							.getValue();
					if (!heardclearers.contains(lastClearedTarget)) {
						Entity entit = model.getEntityByInt(lastClearedTarget);
						if (entit instanceof Road) {
							worldGraph.Area area = ((Area) entit).worldGraphArea;
							boolean isItCleanEnough = true;
							int pos = entit.getID().getValue();
							for (Enterance e : area.enterances)
								if (e.internalEnterances.size() != area.enterances
										.size() - 1)
									isItCleanEnough = false;
							if (isItCleanEnough == true) {
								byte[] byteArray = radar.codeForClearer(
										allEntities.get(pos), area);
								lastSaidByteArray = new byte[byteArray.length];
								for (int i = 0; i < byteArray.length; i++) {
									lastSaidByteArray[i] = byteArray[i];
								}
								sendSpeak(time, behtarinChannel1, byteArray);
								if (behtarinChannel1 != 0)
									sendSpeak(time, 0, byteArray);
							}
						}
					}
				}
			}
		}
	}

	protected void hearClearer() {
		int RAD = wg.RAD;
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_CLEAR) {
							int[] array = radar.decodeForClearer(recivedSpeak,
									model, allRanks);
							int pos = array[0];
							Entity entit = model.getEntityByInt(pos);
							Area _area = (Area) entit;
							worldGraph.Area area = _area.worldGraphArea;
							heardclearers.add(pos);
							if (!changeSet.getChangedEntities().contains(
									entit.getID())) {
								_area.lastTimeSeen = time - 1;
								area.blockades.clear();
								area.updateAvailablePoints();
								area.updateEnterancesConnectivity(RAD,
										changeSet, true);
								for (Enterance e : area.enterances) {
									e.internalEnterances.clear();
									for (Enterance f : area.enterances)
										if (f != e)
											e.internalEnterances.add(f);
								}
								for (int i = 1; i < array.length; i++)
									if (!changeSet
											.getChangedEntities()
											.contains(
													area.enterances.get(i - 1).neighbour.area.modelArea
															.getID()))
										area.enterances.get(i - 1).isItConnectedToNeighbour = area.enterances
												.get(i - 1).neighbour.isItConnectedToNeighbour = (array[i] == 1);
							}
						}
					}
				}
			}
		}
	}

	public Comparator<Entity> IDcomparator = new Comparator<Entity>() {
		public int compare(Entity a1, Entity a2) {
			if (a1.getID().getValue() > a2.getID().getValue())
				return 1;
			if (a1.getID().getValue() < a2.getID().getValue())
				return -1;
			return 0;
		}
	};

	protected void checkRechablePolicesAvailability() {
		boolean isStuck = true;
		for (PoliceForce police : modelPoliceForces) {
			if (isReachable(police.getPosition())
					&& police.getID().getValue() != me().getID().getValue()
					&& !police.hasBuriedness) {
				isStuck = false;
				break;
			}
		}

		if (!isStuck && stuckToolKeshid >= 3)
			sendHelp();
		else if (longnHP.containsKey(me().getID().getValue()) && !isStuck) {
			unSendHelp();
		} else if (!(me() instanceof PoliceForce)
				|| (((Human) me()).isBuriednessDefined() && ((Human) me())
						.getBuriedness() != 0)) {
			if (isStuck && modelPoliceForces.size() != 0)
				sendHelp();
		}
	}

	protected void checkBriedness() {
		if (me() instanceof Human) {
			Human me = (Human) me();
			if (me.getBuriedness() > 0 && modelAmbulanceTeams.size() != 0)
				sendHelpForAmb();
			if (longnAP.containsKey(me().getID().getValue())
					|| (wasItInLongnAP && counterForAmbUnHelp < 5)) {
				wasItInLongnAP = true;
				if (me.getBuriedness() == 0
						|| (me.isHPDefined() && me.getHP() == 0)) {
					counterForAmbHelp = 5;
					unSendHelpForAmb();
				}
			}
		}
	}

	protected void sendMyPos() {
		if (time - lastSaidPositionCycle >= cyclesBtweenSayingPos) {
			Human me = (Human) me();
			int pos = me.getPosition().getValue();
			byte[] byteArray = radar.code(Radar.HEADER_SAYMYPOS,
					allEntities.get(pos));
			sendSpeak(time, behtarinChannel1, byteArray);
			if (behtarinChannel1 != 0)
				sendSpeak(time, 0, byteArray);
			lastSaidPositionCycle = time;
		}
	}

	protected void hearSOsPos() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_SAYMYPOS && noCommi == false) {
							Area pos = (Area) allRanks.get(radar
									.decode(recivedSpeak));
							addAgentPositionToModel(entity, pos);
						}
					}
				}
			}
		}
	}

	List<EntityID> listMan;

	protected BitSet sendMyWay(BitSet bits, List<EntityID> listMan, int index) {
		int myIndex = index;
		int startSendWay = 0;
		startSendWay = Radar.calcWithBitSet(bits, Radar.HEADER_WAY,
				startSendWay, Radar.headerSize);
		startSendWay = Radar.calcWithBitSet(bits,
				allEntities.get(listMan.get(0).getValue()), startSendWay,
				powNum);
		startSendWay = Radar.calcWithBitSet(bits, myIndex, startSendWay, 4);
		for (int i = 0; i < myIndex - 1; i++) {
			Area a = (Area) model.getEntity(listMan.get(i));
			worldGraph.Area wa = a.worldGraphArea;
			ArrayList<Enterance> hameyeEntranceHayeMan = new ArrayList<Enterance>();
			hameyeEntranceHayeMan.addAll(wa.enterances);
			if (i > 0) {
				int lastOne = listMan.get(i - 1).getValue();
				for (int u = 0; u < hameyeEntranceHayeMan.size(); u++)
					if (hameyeEntranceHayeMan.get(u).neighbour.area.modelArea
							.getID().getValue() == lastOne) {
						hameyeEntranceHayeMan.remove(u);
						break;
					}
			}
			int powyeMan = Radar.findPow(hameyeEntranceHayeMan.size());
			if (powyeMan != 0) {
				Collections.sort(hameyeEntranceHayeMan, EnteranceComparator);
				int id = 0;
				for (int u = 0; u < hameyeEntranceHayeMan.size(); u++)
					if (hameyeEntranceHayeMan.get(u).neighbour.area.modelArea
							.getID().getValue() == listMan.get(i + 1)
							.getValue()) {
						id = u;
						break;
					}
				startSendWay = Radar.calcWithBitSet(bits, id, startSendWay,
						powyeMan);
			}
		}
		return bits;
	}

	protected void hearMyWay() {
		if (true)
			return;
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_WAY) {
							int id = cmd.getAgentID().getValue();
							if (id == me().getID().getValue())
								return;
							decodeForWay(recivedSpeak, entity);
						}
					}
				}
			}
		}
	}

	protected void hearFBWater() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						BitSet bits = new BitSet();
						bits = Radar.fromByteArray(recivedSpeak);
						if (header == Radar.HEADER_WAY
								&& (me() instanceof FireBrigade || me() instanceof FireStation)) {
							int startMessage = Radar.headerSize;
							Entity e = model.getEntity(cmd.getAgentID());
							((FireBrigade) e).Water = Radar
									.fromMabnaye2ToMabnaye10(bits,
											startMessage + 1, startMessage);
						}
					}
				}
			}
		}
	}

	public void decodeForWay(byte[] bit, Entity agentEntity) {
		BitSet bits = new BitSet();
		bits = Radar.fromByteArray(bit);
		int i = Radar.headerSize;
		Area a = (Area) allRanks.get(Radar.fromMabnaye2ToMabnaye10(bits, i
				+ powNum, i));
		int from = a.getID().getValue();
		i = i + powNum;
		int tedadRoad = Radar.fromMabnaye2ToMabnaye10(bits, i + 4, i);
		i = i + 4;
		worldGraph.Area wa = a.worldGraphArea;
		ArrayList<Enterance> entranceHa = null;
		int id = -1;
		int entrancePow = -1;
		Enterance e = null;
		Enterance eGhabli = null;
		int shomarande = 0;
		while (shomarande <= tedadRoad) {
			a.lastTimeSeen = time - 1;
			entranceHa = new ArrayList<Enterance>();
			entranceHa.addAll(wa.enterances);
			if (a.getID().getValue() != from && entranceHa.size() > 1)
				entranceHa.remove(eGhabli.neighbour);
			if (entranceHa.size() > 1) {
				Collections.sort(entranceHa, EnteranceComparator);
				entrancePow = Radar.findPow(entranceHa.size());
				id = Radar.fromMabnaye2ToMabnaye10(bits, i + entrancePow, i);
				i = i + entrancePow;
				e = entranceHa.get(id);
			} else
				e = entranceHa.get(0);
			if (shomarande != tedadRoad)
				makeConnectivities(eGhabli, e, false);
			else
				makeConnectivities(eGhabli, e, true);
			eGhabli = e;
			wa = e.neighbour.area;
			a = wa.modelArea;
			shomarande++;
		}
		addAgentPositionToModel(agentEntity, a);
	}

	protected void makeConnectivities(Enterance eGhabli, Enterance e,
			boolean isItTheLastOne) {
		if (eGhabli != null && !isItTheLastOne) {
			Enterance eg = eGhabli.neighbour;
			if (!isInChangeSet(eg.area.modelArea.getID())
					|| !isInChangeSet(eGhabli.area.modelArea.getID()))
				eGhabli.isItConnectedToNeighbour = eg.isItConnectedToNeighbour = true;
			if (!isInChangeSet(eg.area.modelArea.getID())) {
				if (!eg.internalEnterances.contains(e))
					eg.internalEnterances.add(e);
				if (!e.internalEnterances.contains(eg))
					e.internalEnterances.add(eg);
			}
		}
		if (isItTheLastOne) {
			Enterance eg = eGhabli.neighbour;
			if (!isInChangeSet(eg.area.modelArea.getID())
					|| !isInChangeSet(eGhabli.area.modelArea.getID()))
				eGhabli.isItConnectedToNeighbour = eg.isItConnectedToNeighbour = true;
		}
	}

	public Comparator<Enterance> EnteranceComparator = new Comparator<Enterance>() {
		public int compare(Enterance a1, Enterance a2) {
			if (a1.id > a2.id)
				return 1;
			if (a1.id < a2.id)
				return -1;
			return 0;
		}
	};

	protected void SendNoCommiForFire(HashSet<Building> buildings) {
		// TORO
		if (buildings.size() > 0) {
			BitSet bits = new BitSet();
			bits = radar
					.mapHeader(Radar.HEADER_BUILDING_OUT_OF_REACH_NEAR_ROADS);
			int start = Radar.headerSize;
			int fireness = 0;
			for (Building build : buildings) {
				// fireness = build.stFire;
				fireness = calculatedRF(build);
				// addBuildingToModel(build, fireness);
				int lts = 0;
				if (build.lastTimeSeen > 0)
					lts = build.lastTimeSeen;
				start = radar.buildingCode(bits,
						allEntities.get(build.getID().getValue()), fireness,
						lts, 9, start);
			}
			bits.set(start);
			byte[] byteArray = Radar.toByteArray(bits);
			sendSpeak(time, behtarinChannel1, byteArray);
		}
	}

	protected void sendEmptyBuildings(int num, boolean isChoosing) {
		if (lowCommi)
			return;
		BitSet bits = new BitSet();
		int start = 0;
		int check1 = 0;
		if (isChoosing)
			check1 = 1;
		start = Radar.calcWithBitSet(bits, Radar.HEADER_EMPTYBUILDINGS, start,
				Radar.headerSize);
		start = Radar.calcWithBitSet(bits, num, start, SBNum);
		start = Radar.calcWithBitSet(bits, check1, start, 1);
		int channel = 0;
		if (me() instanceof FireBrigade)
			channel = behtarinChannel2;
		else if (me() instanceof AmbulanceTeam)
			channel = behtarinChannel1;
		byte[] byteArray = Radar.toByteArray(bits);
		sendSpeak(time, channel, byteArray);
		if (channel != 0) {
			sendSpeak(time, 0, byteArray);
		}
	}

	HashSet<LittleZone> zoneFix = new HashSet<LittleZone>();
	HashSet<LittleZone> ambZoneFix = new HashSet<LittleZone>();

	protected void printLZ(LittleZone l) {
		String s = "";
		for (Building b : l.buildings) {
			s += b.getID().getValue();
			s += " ";
		}
		log("little zone:" + s);
	}

	protected void hearEmptyBuildings() {
		zoneFix = new HashSet<LittleZone>();
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						if (header == Radar.HEADER_EMPTYBUILDINGS) {
							Human him = (Human) model.getEntity(cmd
									.getAgentID());
							BitSet bs = Radar.fromByteArray(recivedSpeak);
							int ZN = Radar.fromMabnaye2ToMabnaye10(bs,
									Radar.headerSize + SBNum, Radar.headerSize);
							boolean isChoosing = false;
							if (Radar.fromMabnaye2ToMabnaye10(bs,
									Radar.headerSize + SBNum + 1,
									Radar.headerSize + SBNum) == 1)
								isChoosing = true;
							if (him instanceof AmbulanceTeam
									&& me() instanceof AmbulanceTeam) {
								AmbulanceTeam amb = (AmbulanceTeam) him;
								LittleZone lz = newZone.get(ZN);
								// lz.isEnd = isChoosing;
								if (isChoosing == false) {
									if (lz.ownersOfSearchZone.contains(amb
											.getID().getValue())) {
										if (lz.owner == amb.getID().getValue())
											lz.owner = -1;
										lz.ownersOfSearchZone
												.remove((Integer) amb.getID()
														.getValue());
									}
								}
								if (isChoosing == true) {
									for (LittleZone l : newZone)
										if (l != lz
												&& l.ownersOfSearchZone
														.contains(amb.getID()
																.getValue()))
											l.ownersOfSearchZone
													.remove((Integer) amb
															.getID().getValue());
									if (!lz.ownersOfSearchZone.contains(amb
											.getID().getValue())) {
										lz.ownersOfSearchZone.add(amb.getID()
												.getValue());
										ambZoneFix.add(lz);
									}
								}
							} else if (me() instanceof PoliceForce) {
								if (him instanceof AmbulanceTeam) {
									if (isChoosing)
										for (Building b : newZone.get(ZN).buildings) {
											b.lastTimeVisit = time - 1;
											b.lastTimeSeen = time - 1;
										}
								} else if (him instanceof FireBrigade) {
									if (isChoosing)
										for (Building b : newZone.get(ZN).buildings)
											b.lastTimeSeen = time - 1;
								}
							} else if (him instanceof FireBrigade
									&& me() instanceof FireBrigade) {
								FireBrigade fireB = (FireBrigade) him;
								LittleZone lz = searchZones.get(ZN);
								lz.isEnd = isChoosing;
								if (fireB.myLastSearchZone != null
										&& fireB.myLastSearchZone.owner == him
												.getID().getValue()) {
									fireB.myLastSearchZone.ownersOfSearchZone
											.remove((Integer) him.getID()
													.getValue());
									fireB.myLastSearchZone.owner = -1;
									zoneFix.add(fireB.myLastSearchZone);
								}
								if (!lz.ownersOfSearchZone
										.contains((Integer) him.getID()
												.getValue())) {
									lz.ownersOfSearchZone.add(him.getID()
											.getValue());
									zoneFix.add(lz);
									fireB.myLastSearchZone = searchZones
											.get(ZN);

								}
								// if (isChoosing == true) {
								// lz.owner = -1;
								// lz.ownersOfSearchZone = new
								// ArrayList<Integer>();
								// zoneFix.add(lz);
								// for (Building building : lz.buildings)
								// building.lastTimeSeen = time - 1;
								//
								// }
							}
						}
					}
				}
			}
			for (LittleZone littleZone : zoneFix)
				littleZone.fixOwner();
			for (LittleZone littleZone : ambZoneFix)
				littleZone.fixOwner();
		}
	}

	protected void updatingWayMemory() {
		sentBlockades = new ArrayList<Integer>();
		for (Area area : modelAreas) {
			area.worldGraphArea.blockades = new ArrayList<worldGraph.Blockade>();
			ArrayList<Enterance> enterances = area.worldGraphArea.enterances;
			for (int i = 0; i < enterances.size(); i++) {
				for (int j = 0; j < i; j++) {
					if (!enterances.get(i).internalEnterances
							.contains(enterances.get(j)))
						enterances.get(i).internalEnterances.add(enterances
								.get(j));
					if (!enterances.get(j).internalEnterances
							.contains(enterances.get(i)))
						enterances.get(j).internalEnterances.add(enterances
								.get(i));
					enterances.get(j).isItConnectedToNeighbour = true;
					enterances.get(j).updateAvaiablePoints();
				}
				enterances.get(i).isItConnectedToNeighbour = true;
				enterances.get(i).updateAvaiablePoints();
			}
		}
	}

	protected ArrayList<Road> findMainRoads(HashSet<Building> builds) {
		ArrayList<Road> mainRoads = new ArrayList<Road>();
		for (Building b : builds) {
			for (Enterance e : b.worldGraphArea.enterances) {
				if (e.neighbour.area.modelArea instanceof Road)
					for (Enterance ej : e.neighbour.area.enterances)
						if (ej.neighbour.area.modelArea instanceof Road) {
							Road r = (Road) ej.neighbour.area.modelArea;
							r.isItMain = true;
							mainRoads.add(r);
						}
			}
		}
		for (Road r : mainRoads) {
			ArrayList<Road> layer = new ArrayList<Road>();
			for (Road re : modelRoads) {
				re.lastArea = null;
				re.marked = false;
			}
			if (!r.marked) {
				layer.add(r);
				int shomar = 0;
				while (shomar < 5 && layer.size() > 0) {
					ArrayList<Road> newLayer = new ArrayList<Road>();
					for (Road rr : layer) {
						rr.marked = true;
						for (EntityID e : rr.getNeighbours()) {
							if (model.getEntity(e) instanceof Road) {
								Road newRoad = (Road) model.getEntity(e);
								if (!newRoad.marked) {
									if (newRoad.isItMain) {
										while (rr.lastArea != null) {
											rr.isItMain = true;
											rr = (Road) rr.lastArea;
										}
									}
									newRoad.marked = true;
									newRoad.lastArea = rr;
									newLayer.add(newRoad);
								}
							}
						}
					}
					layer = newLayer;
					shomar++;
				}
			}
		}
		return mainRoads;
	}

	protected void setIsAlive() {
		if (heard.size() > 0)
			for (Command cmd : heard) {
				Entity ent = model.getEntity(cmd.getAgentID());
				if (ent != null && ent instanceof AmbulanceTeam)
					((AmbulanceTeam) ent).isAlive = true;
			}
	}

	protected void fillingLongsForNoCommi() {
		for (Human a : modelAgents) {
			boolean isInBuilding = model.getEntity(a.getPosition()) instanceof Building;
			if (isInBuilding)
				longnAP.put(a.getID().getValue(), a.getPosition().getValue());
			if (!(a instanceof PoliceForce) || isInBuilding)
				longnHP.put(a.getID().getValue(), a.getPosition().getValue());
		}
	}

	boolean haveIHeardFromRadar = false;

	protected void subscribesChannelsCenters() {
		// selectChannel();
		int tedadChanneleMan = 0;
		if (me() instanceof Building)
			tedadChanneleMan = tedadChannelCenter;
		else
			tedadChanneleMan = tedadChannelAgent;
		if (tedadChanneleMan >= 2)
			sendSubscribe(time, behtarinChannel1, behtarinChannel2);
		else
			sendSubscribe(time, behtarinChannel1);
	}

	protected void radarBeforeDecide() throws CloneNotSupportedException {

		if (radar == null)
			radar = new Radar(powNum);
		radar.logger = logger;
		radar.sentBlockades = sentBlockades;
		counterForBusiness = 0;
		subscribesChannelsCenters();
		if (hearAGPosition == null) {
			hearAGPosition = new HashMap<Human, Area>();
			for (Human h : modelAgents)
				hearAGPosition.put(h, (Area) h.getPosition(model));
		}
		if (time == 1) {
			SBNum = Radar.findPow(Math.max(newZone.size(), searchZones.size()));
		}
		if (time == 1 && noCommi)
			fillingLongsForNoCommi();
		if (!noCommi) {
			setIsAlive();
			hearHelp();
			unHearHelp();
			hearHelpForAmb();
			unHearHelpForAmb();
			hearBuildingInformation();
			setSTFire();
			if (me() instanceof PoliceForce || me() instanceof PoliceOffice)
				hearPoliceMap();
			hearCVlInformation();
			hearEmptyBuildings();
			hearWay();
			hearNoWay();
			hearClearer();
			// hearCheck();
			hearMyWay();
			hearFBWater();
			hearSOsPos();
			hearAgPos();

			if (me() instanceof Human && time > 2) {
				checkRechablePolicesAvailability();
				// checkBriedness();
			}
			// if (me().getID().getValue() == centerForPolice)
			// centerForPolice();
			sendCheckForFires();
			if (me() instanceof FireBrigade) {
				((FireBrigadeAgent) this).shenide = false;
				((FireBrigadeAgent) this).hearFire();
			}
			sendBuildingInformation();
			if (!lowCommi) {
				// if (me() instanceof FireBrigade) {
				// Human me = (Human) me();
				// if (!me.isBuriednessDefined() || me.getBuriedness() == 0)
				// if (me().getID().getValue() % 2 == time % 2)
				// sendMyPos();
				// }
				if (me() instanceof Human) {
					Human me = (Human) me();
					if (!IDandPOS.containsKey(me.getID())
							|| IDandPOS.get(me.getID()).getValue() != me
									.getPosition().getValue())
						sendAgPos();
				}
			}
		} else {
			hearOnFireBuilds();
			sendOnFireBuilds();
			hearHelp();
			hearHelpForAmb();
			// hearBuildingInformation();
			setSTFire();
			hearCVlInformation();
			hearEmptyBuildings();
			hearMyWay();
			if (me() instanceof Human) {
				checkRechablePolicesAvailability();
				// checkBriedness();
			}
			// sendCVlInformationForNoCommi();
		}
	}

	protected void radarAfterDecide() {
		if (!noCommi)
			sendCVlInformation();
		if (time > 10) {
			if (time % 2 != 0)
				checkWays();
			if (lastSaidByteArray != null && lastSaidByteArray.length != 0) {
				sendSpeak(time, behtarinChannel1, lastSaidByteArray);
				if (behtarinChannel1 != 0)
					sendSpeak(time, 0, lastSaidByteArray);
				lastSaidByteArray = null;
			}
			sendClearer();

			for (byte[] b : lastByteArrays)
				if (b != null && b.length > 0) {
					sendSpeak(time, behtarinChannel1, b);
					if (behtarinChannel1 != 0)
						sendSpeak(time, 0, b);
				}
			sendClearerArea();
		}
	}

	public void littleZoneMaking(int kamineBuildings) {
		LittleZone[] littlezones = littleZoning(modelBuildings, 5);
		for (LittleZone little : littlezones)
			newZone.add(little);
		for (int j = 0; j < newZone.size(); j++)
			if (newZone.get(j).buildings.size() > kamineBuildings) {
				littlezones = littleZoning(newZone.get(j).buildings, 3);
				for (LittleZone lz : littlezones)
					if (lz.buildings.size() != 0)
						newZone.add(lz);
				newZone.remove(j);
				j--;
			}
		int id = 0;
		for (LittleZone lz : newZone) {
			lz.id = id;
			id++;
		}
	}

	public LittleZone[] littleZoning(ArrayList<Building> buildings, int zonenum) {
		LittleZone[] littlezones = new LittleZone[zonenum];
		for (int i = 0; i < littlezones.length; i++)
			littlezones[i] = new LittleZone();
		Point center = setcenter(buildings);
		for (Building build : buildings) {
			float theta = (new Line(center, new Point(build.getX(),
					build.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;
			int zoneNumber = (int) (theta / (360.0 / zonenum));
			littlezones[zoneNumber].buildings.add(build);
			build.zoneNumber = zoneNumber;
		}
		return littlezones;
	}

	public void isAnyNearBuilidngOnFire() {
		ArrayList<Area> layer = new ArrayList<Area>();
		for (Area area : modelAreas) {
			if (area instanceof Building
					&& ((Building) area).isFierynessDefined()
					&& ((Building) area).getFieryness() > 0)
				layer.add(area);
			area.check = false;
		}
		int shomar = 0;
		while (shomar < 10 && layer.size() > 0) {
			ArrayList<Area> newLayer = new ArrayList<Area>();
			for (Area aa : layer) {
				aa.check = true;
				for (EntityID e : aa.getNeighbours()) {
					Area ar = (Area) model.getEntity(e);
					if (!ar.equals(aa)
							&& !ar.check
							&& ((ar instanceof Building) && ((Building) ar)
									.isOnFire())) {
						ar.isNearFire = true;
						ar.check = true;
						newLayer.add(ar);
					}
				}
			}
			layer = newLayer;
			shomar++;
		}
	}

	public void randomWalkComputations() throws ActionCommandException {
		if (amIStucking == true /* && !(me() instanceof FireBrigade) */) {
			if (me() instanceof PoliceForce) {
				if (isSafeBlockadesNeeded) {
					log("RandomWalk executes for some problems 1!");
					randomWalk();
				} else {
					boolean a = false, b = false;
					Area myPos = (Area) model.getEntity(((Human) me())
							.getPosition());
					if (myPos.isBlockadesDefined()
							&& myPos.getBlockades().size() > 0)
						a = true;
					if (time > 3
							&& commandHistory.get(time - 1).getAction()
									.equals(StandardMessageURN.AK_CLEAR))
						if ((Blockade) model.getEntity(commandHistory
								.get(time - 1).lastClearedTarget) != null)
							b = true;
					if (a || b) {
						isSafeBlockadesNeededForRandomWalk = true;
					} else {
						log("RandomWalk executes for some problems 2!");
						randomWalk();
					}
				}
			} else if (!(me() instanceof AmbulanceTeam)) {
				log("RandomWalk executes for some problems 3!");
				randomWalk();
			}

		}
	}

	public void mikhadBemire() throws ActionCommandException {
		Human man = (Human) me();
		if (movingToRefuge
				&& man.isDamageDefined()
				&& man.getDamage() != 0
				&& reachableRefuges.size() > 0
				&& man.getPosition().getValue() == reachableRefuges.get(0)
						.getID().getValue())
			rest();
		if (movingToRefuge && reachableRefuges.size() > 0) {
			EntityID entID = reachableRefuges.get(0).getID();
			if (((Human) me()).getPosition().getValue() != entID.getValue()) {
				if (man instanceof PoliceForce) {
					((PoliceForceAgent) this)
							.fastClear(reachableRefuges.get(0));
				} else
					move(entID);
			} else
				movingToRefuge = false;
		}
		ArrayList<EntityID> dist = new ArrayList<EntityID>();
		if (man.isDamageDefined() && man.getDamage() > 0
				&& reachableRefuges.size() > 0) {
			boolean booli = true;
			if ((Human) me() instanceof PoliceForce)
				booli = false;
			dist = wg.getMinPathDijk(reachableRefuges.get(0).getID(), booli);
			if (dist != null && man.deadtime < dist.size() + 20 + time) {
				movingToRefuge = true;
				manlastDeadTime = man.deadtime;
				log("move e mikhad bemire");
				if (!booli)
					((PoliceForceAgent) this)
							.fastClear(reachableRefuges.get(0));
				else
					move(dist, true);
			}
		} else if (man.isDamageDefined() && man.getDamage() > 0
				&& man instanceof PoliceForce && reachableRefuges.size() > 0) {
			log("damage daram");
			EntityID refugeID = null;
			int minDis = Integer.MAX_VALUE;
			for (Refuge ref : modelRefuges) {
				if (minDis > ref.worldGraphArea.distanceFromSelf) {
					minDis = ref.worldGraphArea.distanceFromSelf;
					refugeID = ref.getID();
				}
			}
			ArrayList<EntityID> myPathToRefuge = wg.getMinPathDijk(refugeID,
					false);
			if (myPathToRefuge != null) {
				log("damage daram va mikhaham refuge baz konam");
				((PoliceForceAgent) this).fastClear(((Area) model
						.getEntity(refugeID)));
			}
		}
	}

	public void calcutingSayVajebBuildings() {
		HashSet<Building> buildHayeSayVajeb = new HashSet<Building>();
		ArrayList<Building> buildsKol = new ArrayList<Building>();
		for (Building building : modelBuildings)
			if (building.isOnFire())
				buildsKol.add(building);
		Collections.sort(buildsKol, lTSComparator);
		for (Building building : buildsKol) {
			buildHayeSayVajeb.add(building);
			if (buildHayeSayVajeb.size() >= 20)
				break;
		}
		if (buildHayeSayVajeb.size() != 0 && noCommi)
			SendNoCommiForFire(buildHayeSayVajeb);
	}

	public void calcutingStucks() {
		if (me() instanceof Human) {
			Human m = (Human) me();
			amIStucking = false;
			if (akharinStuckTime != time - 1)
				stuckToolKeshid = 0;
			if (Math.hypot(Math.abs(m.getX() - xLastMove),
					Math.abs(m.getY() - yLastMove)) > 500
					&& Math.hypot(Math.abs(m.getX() - xLastCycle),
							Math.abs(m.getY() - yLastCycle)) < 1000
					&& lastMoveCycle == time - 1 && time > 3) {
				stuckToolKeshid++;
				akharinStuckTime = time;

				if (stuckToolKeshid >= 2)
					amIStucking = true;
			}
			xLastCycle = m.getX();
			yLastCycle = m.getY();
		}
		if (counterForIsSafeBlock == 3) {
			isSafeBlockadesNeededForRandomWalk = false;
			counterForIsSafeBlock = 0;
		}
		if (isSafeBlockadesNeededForRandomWalk)
			counterForIsSafeBlock++;
		isSafeBlockadesNeeded = false;
	}

	public int time = -1;
	protected Collection<Command> heard = null;
	protected Map<Integer, ActionCommandException> commandHistory = new HashMap<Integer, ActionCommandException>();
	public ChangeSet changeSet = null;

	public int STUCKCYCLES = 4;
	public ArrayList<AmbulanceTeam> ambulances = new ArrayList<AmbulanceTeam>(); // for
	// ambulance
	public int[] positions = new int[STUCKCYCLES];

	public HashMap<Area, ArrayList<Building>> buildsOfFire = new HashMap<Area, ArrayList<Building>>();
	public ArrayList<Building> emptyB = new ArrayList<Building>();
	public int manlastDeadTime = 0;
	boolean amIStucking = false;
	protected CentreForAmbulance pg = null;
	protected CentreForFire kimpar = null;
	protected CenterForPolice safie = null;

	protected void centers() {
		if (me().getID().getValue() == centerForAmb) {
			if (pg == null)
				pg = new CentreForAmbulance(this);
			pg.decide();
		}
		if (me().getID().getValue() == centerForFire) {
			if (kimpar == null)
				kimpar = new CentreForFire(this);
			kimpar.decide();
		}
		if (me().getID().getValue() == centerForPolice && !lowCommi) {
			if (safie == null)
				safie = new CenterForPolice(this);
			safie.decide();
		}
	}

	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		try {
			cycleTimer.setTime(0);
			log("Think started");
			this.time = time;
			this.heard = heard;
			this.changeSet = changes;
			for (EntityID forroyechangset : changes.getChangedEntities()) {
				Entity entity = model.getEntity(forroyechangset);
				if (entity instanceof Area) {
					((Area) entity).lastTimeSeen = time;
					((Area) entity).realLastTimeSeen = time;
					if (entity instanceof Building) {
						Building build = (Building) model
								.getEntity(forroyechangset);
						hearRF(build, calculatedRF(build));
					}
				}
			}
			setSTFire();
			for (Building b : modelBuildings)
				b.shouldSend = false;
			if (time % timeForNewWayMemory == 0)
				updatingWayMemory();
			log("before world graph update");
			wg.logger = logger;
			wg.update(changes);
			log("after world graph update");
			if (me() instanceof Human) {
				wg.update(time, this, ((Human) me()).getPosition(),
						((Human) me()).getX(), ((Human) me()).getY());
				((Area) model.getEntity(((Human) me()).getPosition())).lastTimeVisit = time;
			} else
				wg.update(time, this, null, -1, -1);

			for (EntityID e : changeSet.getChangedEntities()) {
				Entity entity = model.getEntity(e);
				if (entity instanceof Human) {
					Human h = (Human) entity;
					// merge*********************************************************************
					h.setBury(h.getBuriedness());
					h.setHp(h.getHP(), time);
					h.setDmg(h.getDamage(), time);
					h.cycle(time);
					if (h.deadtime > 502)
						h.deadtime = 502;
					if (h.isBuriednessDefined() && h.getBuriedness() > 0)
						h.hasBuriedness = true;
					else
						h.hasBuriedness = false;
				}
			}
			logger.logTime(time);
			try {
				try {
					// calcutingSayVajebBuildings();
					log("radar before decide");
					radarBeforeDecide();
				} catch (Exception e) {
					e.printStackTrace();
					if (logger.getWriter() != null) {
						e.printStackTrace(logger.getWriter());
					}
				}
				if (me() instanceof Human)
					wg.updateReachableAreas(wg.myEnterances);
				try {
					centers();
				} catch (Exception e) {
					e.printStackTrace();
					e.printStackTrace(logger.getWriter());
				}
				calcutingStucks();
				randomWalkComputations();
				if (me() instanceof Human && !(me() instanceof PoliceForce))
					setMyZone(true);
				ActionCommandException ace = null;
				shouldBaseDecideForRandomWalk = true;
				try {
					if (me() instanceof Human) {
						if (me() instanceof PoliceForce)
							for (Area a : modelAreas)
								a.isNearFire = false;
						mikhadBemire();
						if (me() instanceof PoliceForce)
							isAnyNearBuilidngOnFire();
					}
					decide();
				} catch (ActionCommandException ac) {
					ace = ac;
					if (me() instanceof AmbulanceTeam)
						((AmbulanceTeamAgent) this).sendInfo();
				}
				if (ace == null)
					rest();
				throw ace;
			} catch (ActionCommandException ace) {
				log("Command: " + ace.getAction() + " Sent");
				commandHistory.put(time, ace);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (logger.getWriter() != null)
				e.printStackTrace(logger.getWriter());
			sendRest(time);
			commandHistory.put(time, new ActionCommandException(
					StandardMessageURN.AK_REST));
		}
		try {
			radarAfterDecide();
		} catch (Exception e) {
			e.printStackTrace();
			if (logger.getWriter() != null) {
				e.printStackTrace(logger.getWriter());
			}
		}
		logger.logEndOfCycle();
	}

	public Comparator<Building> lTSComparator = new Comparator<Building>() {
		public int compare(Building a, Building b) {
			if (a.lastTimeSeen > b.lastTimeSeen)
				return -1;
			if (a.lastTimeSeen < b.lastTimeSeen)
				return 1;
			return 0;
		}
	};

	protected void rest() throws ActionCommandException {
		sendRest(time);
		throw new ActionCommandException(StandardMessageURN.AK_REST);
	}

	protected void extinguish(EntityID target, int power)
			throws ActionCommandException {
		if (me() instanceof FireBrigade
				&& ((FireBrigade) me()).getWater() < power)
			power = ((FireBrigade) me()).getWater();
		for (Building building : ((Building) model.getEntity(target)).nearBuildings)
			if (!building.isFierynessDefined() || building.getFieryness() == 0)
				building.lastTimeSeen = LTSDORI;
		sendExtinguish(time, target, power);
		throw new ActionCommandException(StandardMessageURN.AK_EXTINGUISH);
	}

	protected void move(EntityID target) throws ActionCommandException {
		if (me() instanceof PoliceForce)
			move(target, false);
		else
			move(target, true);
	}

	protected void move(EntityID target, boolean checkBlockades)
			throws ActionCommandException {
		if (!(me() instanceof Human))
			return;
		Human m = (Human) me();
		ArrayList<EntityID> way = wg.getMinPath(m.getPosition(), target,
				checkBlockades);
		if (way.size() > 0
				&& way.get(0).getValue() == m.getPosition().getValue())
			way.remove(0);
		move(way, false);
	}

	int lastMoveCycle;

	int xLastMove = -1, yLastMove = -1;
	int xLastCycle = -1, yLastCycle = -1;

	protected boolean checkAL(List<EntityID> one, List<EntityID> two) {
		if (one.size() == 0 || two.size() == 0)
			return false;
		int sizemoon = 0;
		if (one.size() > two.size())
			sizemoon = two.size();
		else
			sizemoon = one.size();
		int tafazol1 = one.size() - sizemoon;
		int tafazol2 = two.size() - sizemoon;
		for (int i = sizemoon - 1; i >= 0; i--)
			if (one.get(i + tafazol1) != two.get(i + tafazol2))
				return false;
		return true;
	}

	protected void move(List<EntityID> path, boolean djkstraPath)
			throws ActionCommandException {
		if (path.size() == 0) {
			path.add(((Human) me()).getPosition());
		}
		int x = ((Area) model.getEntity(path.get(path.size() - 1))).getX();
		int y = ((Area) model.getEntity(path.get(path.size() - 1))).getY();
		move(path, x, y, djkstraPath);
	}

	public void sendKoreRah(List<EntityID> path) {
		Human me = (Human) me();
		if (listMan == null) {
			listMan = new ArrayList<EntityID>();
			for (EntityID eID : path)
				listMan.add(eID);
		}
		if (path != null && path.size() > 0) {
			if (time > 4) {
				if (listMan.contains(me.getPosition())
						&& (listMan.indexOf(me.getPosition()) >= 15 || checkAL(
								listMan, path) == false)
						&& listMan.indexOf(me.getPosition()) != 0) {
					BitSet bits = new BitSet();
					int a;
					if (listMan.indexOf(me.getPosition()) >= 15)
						a = 15;
					else
						a = listMan.indexOf(me.getPosition());
					bits = sendMyWay(bits, listMan, a);
					byte[] byteArray = Radar.toByteArray(bits);
					sendSpeak(time, behtarinChannel1, byteArray);
					if (behtarinChannel1 != 0)
						sendSpeak(time, 0, byteArray);
					listMan = new ArrayList<EntityID>();
					listMan.addAll(path);
					lastSaidPositionCycle = time;
				}
			}
		}
	}

	protected void sendFBWater() {
		if (me() instanceof FireBrigade && !lowCommi && !noCommi) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_WAY, 0,
					Radar.headerSize);
			if ((((FireBrigade) me()).getWater() + 1000) < ((FireBrigadeAgent) this).maxWater)
				start = Radar.calcWithBitSet(bits, 0, start, 1);
			else
				start = Radar.calcWithBitSet(bits, 1, start, 1);
			byte[] byteArray = Radar.toByteArray(bits);
			sendSpeak(time, behtarinChannel1, byteArray);
			if (behtarinChannel1 != 0)
				sendSpeak(time, 0, byteArray);
			((FireBrigade) me()).timeOfSendingWater = time;
		}
	}

	public ArrayList<EntityID> dijkstraCalculations(boolean djkstraPath,
			List<EntityID> path) {
		if (!djkstraPath) {
			try {
				if (path.size() > 1) {
					ArrayList<EntityID> epath = null;
					Area a = (Area) model.getEntity(path.get(path.size() - 1));
					if (a.getID().getValue() != lastTargetForDijk)
						isLastMoveBFS = true;
					lastTargetForDijk = a.getID().getValue();
					int thres = 30;
					if (!isLastMoveBFS)
						thres = 50;
					if (path.size() <= thres) {
						isLastMoveBFS = false;
						if (me() instanceof PoliceForce)
							epath = wg.getMinPathDijk(a.getID(), false, true);
						else if (a.worldGraphArea.isReachable)
							epath = wg.getMinPathDijk(a.getID(), true, true);
						if (epath != null && epath.size() > 0)
							path = epath;
					} else {
						isLastMoveBFS = true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (logger.getWriter() != null) {
					e.printStackTrace(logger.getWriter());
				}
			}
		}
		return (ArrayList<EntityID>) path;
	}

	EntityID lastMovePath;
	public boolean isLastMoveBFS = true;
	public int lastTargetForDijk = -1;

	protected void move(List<EntityID> path, int x, int y, boolean djkstraPath)
			throws ActionCommandException {
		// if (time % 10 == modelFireBrigades.indexOf(me()) % 10)
		if (me() instanceof FireBrigade
				&& ((time - ((FireBrigade) me()).timeOfSendingWater >= 10) || ((model
						.getEntity(((FireBrigade) me()).getPosition())) instanceof Refuge && (((FireBrigade) me())
						.getWater() + 1000) >= ((FireBrigadeAgent) this).maxWater)))
			sendFBWater();
		// sendKoreRah(path);
		ArrayList<EntityID> pd = dijkstraCalculations(djkstraPath, path);
		if (pd != null)
			path = pd;
		sendMove(time, path, x, y);
		xLastMove = x;
		yLastMove = y;
		lastMovePath = path.get(path.size() - 1);
		lastMoveCycle = time;
		throw new ActionCommandException(StandardMessageURN.AK_MOVE);
	}

	protected void clear(EntityID target) throws ActionCommandException {
		Area a = (Area) model.getEntity(((Human) me()).getPosition());
		if (a instanceof Building && ((Building) a).isOnFire())
			move(a.worldGraphArea.enterances.get(0).neighbour.area.modelArea
					.getID());
		sendClear(time, target);
		ActionCommandException ace = new ActionCommandException(
				StandardMessageURN.AK_CLEAR);
		ace.lastClearedTarget = ((Blockade) model.getEntity(target))
				.getPosition();
		throw ace;

	}

	protected void clearArea(int x, int y) throws ActionCommandException {
		Area a = (Area) model.getEntity(((Human) me()).getPosition());
		if (a instanceof Building && ((Building) a).isOnFire())
			move(a.worldGraphArea.enterances.get(0).neighbour.area.modelArea
					.getID());
		// Area targetArea = (Area) target;
		sendClearArea(time, x, y);
		ActionCommandException ace = new ActionCommandException(
				StandardMessageURN.AK_CLEAR_AREA);
		// TODO lastClearedTarget check beshe ke doros por mishe ya na!
		// ace.lastClearedTarget = target.getID();
		throw ace;

	}

	protected void rescue(EntityID target) throws ActionCommandException {
		lastRescuedTime = time;
		// lastRescuedObject = model.getEntity(target).getID();
		((AmbulanceTeam) model.getEntityByInt(me().getID().getValue())).target = target
				.getValue();
		((Human) model.getEntity(target)).owner = me().getID().getValue();
		sendRescue(time, target);
		throw new ActionCommandException(StandardMessageURN.AK_RESCUE);
	}

	protected void load(EntityID target) throws ActionCommandException {
		lastRescuedTime = time;
		((AmbulanceTeam) model.getEntityByInt(me().getID().getValue())).target = target
				.getValue();
		((Human) model.getEntity(target)).owner = me().getID().getValue();
		sendLoad(time, target);
		throw new ActionCommandException(StandardMessageURN.AK_LOAD);
	}

	protected void unLoad() throws ActionCommandException {
		((AmbulanceTeam) model.getEntityByInt(me().getID().getValue())).target = -1;
		sendUnload(time);
		throw new ActionCommandException(StandardMessageURN.AK_UNLOAD);
	}

	protected boolean isReachable(EntityID target) {
		StandardEntity se = model.getEntity(target);
		if (se == null || !(se instanceof Area))
			return false;
		Area area = (Area) se;
		return area.worldGraphArea.isReachable;
	}

	protected void log(String msg) {
		logger.log(msg);
	}

	public int getTime() {
		return time;
	}

	protected boolean isInChangeSet(EntityID entityID) {
		if (changeSet == null)
			return false;

		return changeSet.getChangedEntities().contains(entityID);
	}

	public StandardWorldModel getModel() {
		return model;
	}

	private static final int RANDOM_WALK_LENGTH = 50;
	private Map<EntityID, Set<EntityID>> neighbours;

	protected void randomWalk() throws ActionCommandException {
		log("random walk");
		List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
		Set<EntityID> seen = new HashSet<EntityID>();
		EntityID current = ((Human) me()).getPosition();
		for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
			result.add(current);
			seen.add(current);
			List<EntityID> toBeRemoved = new ArrayList<EntityID>();
			List<EntityID> possible = new ArrayList<EntityID>(
					neighbours.get(current));
			for (EntityID fired : possible) {
				if ((model.getEntity(fired)) instanceof Building
						&& ((Building) model.getEntity(fired))
								.isFierynessDefined()
						&& ((Building) model.getEntity(fired)).getFieryness() != 0
						|| !isReachable(fired))
					toBeRemoved.add(fired);
			}
			if (toBeRemoved.size() == possible.size()) {
				toBeRemoved = new ArrayList<EntityID>();
				for (EntityID fired : possible) {
					if ((model.getEntity(fired)) instanceof Building
							&& ((Building) model.getEntity(fired))
									.isFierynessDefined()
							&& ((Building) model.getEntity(fired))
									.getFieryness() != 0)
						toBeRemoved.add(fired);
				}
			}
			possible.removeAll(toBeRemoved);
			Collections.shuffle(possible, new Random());
			boolean found = false;
			for (EntityID next : possible) {
				if (seen.contains(next)) {
					continue;
				}
				current = next;
				found = true;
				break;
			}
			if (!found) {
				// We reached a dead-end.
				break;
			}
		}
		int x = ((Area) model.getEntity(result.get(result.size() - 1))).getX();
		int y = ((Area) model.getEntity(result.get(result.size() - 1))).getY();
		sendMove(time, result, x, y);
		xLastMove = x;
		yLastMove = y;
		lastMovePath = result.get(result.size() - 1);
		lastMoveCycle = time;
		throw new ActionCommandException(StandardMessageURN.AK_MOVE);
	}

	protected Zone searchZone = null;

	private ArrayList<Integer> IDs = new ArrayList<Integer>();

	int numberOfZones = 5;

	protected void setMyZone(boolean chooseMyself) {
		if (policeFirstPositions == null) {
			policeFirstPositions = new HashMap<Integer, Integer>();
			for (PoliceForce pf : modelPoliceForces)
				policeFirstPositions.put(pf.getID().getValue(), ((Human) pf)
						.getPosition().getValue());
		}
		lastIDs = new ArrayList<Integer>();
		lastIDs.addAll(IDs);
		IDs = new ArrayList<Integer>();
		if (this instanceof PoliceForceAgent) {
			for (PoliceForce policeForce : modelPoliceForces)
				if (!policeForce.hasBuriedness || noCommi)
					IDs.add(policeForce.getID().getValue());
			numberOfZones = Math.min(5, (IDs.size() + 1) / 2);
		}
		if (this instanceof FireBrigadeAgent) {
			for (FireBrigade fireBrigade : modelFireBrigades)
				if (!fireBrigade.hasBuriedness || noCommi)
					IDs.add(fireBrigade.getID().getValue());
			numberOfZones = Math.min(5, IDs.size());
		}
		if (this instanceof AmbulanceTeamAgent) {
			for (AmbulanceTeam ambulanceTeam : modelAmbulanceTeams)
				if (noCommi
						|| (!ambulanceTeam.hasBuriedness && (!ambulanceTeam
								.isHPDefined() || ambulanceTeam.getHP() != 0)))
					IDs.add(ambulanceTeam.getID().getValue());
			numberOfZones = Math.min(5, IDs.size());
		}

		if (!(me() instanceof PoliceForce) && lastIDs.size() == IDs.size())
			return;

		if (me() instanceof Human) {
			Human m = (Human) me();
			if (m.hasBuriedness || (m.isHPDefined() && m.getHP() == 0)) {
				mySubZone = null;
				return;
			}
		}
		// zoning the map
		zoning(numberOfZones);
		// my index =chandomin agent 0,1,2,...
		Collections.sort(IDs);
		int myIndex = IDs.indexOf(me().getID().getValue());

		// subZoning or-and policeZoning my zone...
		if (this instanceof PoliceForceAgent) {
			for (Zone z : zones)
				policeZoning(z.zoneNumber, z.policeZones.length);
			// setting my OWN police zone
			if (chooseMyself) {
				ArrayList<Integer> freePolices = new ArrayList<Integer>();
				freePolices.addAll(IDs);
				done: while (true)
					for (Zone z : zones)
						for (PoliceZone pz : z.policeZones) {
							if (freePolices.size() == 0)
								break done;
							int nearest = nearestPolice(pz.center.getX(),
									pz.center.getY(), freePolices, true);
							if (nearest == me().getID().getValue()) {
								myPoliceZone = pz;
								myZoneNumber = z.zoneNumber;
							}
							pz.policesDedicatedto.add(nearest);
							freePolices.remove(freePolices.indexOf(nearest));
						}

				for (Zone z : zones)
					for (PoliceZone pz : z.policeZones) {
						pz.setSubZonesForPolice(pz.policesDedicatedto.size());
					}
				for (Zone z : zones)
					for (PoliceZone pz : z.policeZones)
						subZoningForPolice(z.zoneNumber, pz.policeZoneNumber,
								pz.subZones.length);

				// setting my OWN subZone for polices
				int nearest = 0;
				freePolices.addAll(myPoliceZone.policesDedicatedto);
				for (SubZone sz : myPoliceZone.subZones) {
					nearest = nearestPolice(sz.center.getX(), sz.center.getY(),
							freePolices, true);
					if (nearest == me().getID().getValue()) {
						mySubZone = sz;
						mySubZoneNumber = sz.subZoneNumber;
					}
					freePolices.remove(freePolices.indexOf(nearest));
				}
			} else {
				// sizeOfNotManfiyeYekPFs = findIDsOfFreePolices(
				// policeDecisionHeard1).size();
				setPolicesDedicatedTo(IDs.size());

				for (Zone z : zones)
					for (PoliceZone pz : z.policeZones) {
						pz.setSubZonesForPolice(pz.dedicatedTo);
					}

				for (Zone z : zones)
					for (PoliceZone pz : z.policeZones)
						subZoningForPolice(z.zoneNumber, pz.policeZoneNumber,
								pz.subZones.length);

			}
		} else {
			for (Zone z : zones)
				subZoning(z.zoneNumber, z.subZones.length);
			// setting myOWN sub zone:
			int j = -1;
			done: while (true) {
				j++;
				for (int i = 0; i < numberOfZones; i++) {
					mySubZone = zones[i % numberOfZones].subZones[j];
					myZone1 = zones[i % numberOfZones];
					if (numberOfZones * (j) + (i) == (myIndex))
						break done;
				}
			}
		}
	}

	public void setPolicesDedicatedTo(int pfCount) {
		int counter = 0;
		done: while (true) {
			for (Zone z : zones)
				for (PoliceZone pz : z.policeZones) {
					if (counter == pfCount)
						break done;
					counter++;
					pz.dedicatedTo++;
				}
		}
	}

	private void setCenterOfMap() {
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (Area area : modelAreas) {
			if (area.getX() > maxX)
				maxX = area.getX();
			if (area.getX() < minX)
				minX = area.getX();
			if (area.getY() > maxY)
				maxY = area.getY();
			if (area.getY() < minY)
				minY = area.getY();
		}
		centerOfMap = new Point((minX + maxX) / 2, (minY + maxY) / 2);
	}

	Zone zones[];
	ArrayList<SubZone> subzones;

	public Point centerOfMap = null;

	private void zoning(int zonesCount) {
		zones = new Zone[zonesCount];
		// pCount = tedade hadeghal agent ha baraye 1 zone
		int agentsCount = IDs.size();
		int pCount = agentsCount / zonesCount;
		// setting subzones...
		if (this instanceof PoliceForceAgent) {
			// setting policeZones...
			pCount = Math.min(2, pCount);
			for (int i = 0; i < zones.length; i++) {
				zones[i] = new Zone(i);
				if (pCount == 2) {
					zones[i].setPoliceZones(2);
					zones[i].policeZones[0] = new PoliceZone(0);
					zones[i].policeZones[1] = new PoliceZone(1);
				} else if (i < agentsCount % zonesCount) {
					zones[i].setPoliceZones(pCount + 1);
					for (int j = 0; j <= pCount; j++)
						zones[i].policeZones[j] = new PoliceZone(j);
				} else {
					zones[i].setPoliceZones(pCount);
					for (int v = 0; v < pCount; v++)
						zones[i].policeZones[v] = new PoliceZone(v);
				}
			}
		} else {
			// setting subZones...
			for (int i = 0; i < zones.length; i++) {
				zones[i] = new Zone(i);
				if (i < agentsCount % zonesCount) {

					zones[i].setSubZones(pCount + 1);
					for (int j = 0; j <= pCount; j++)
						zones[i].subZones[j] = new SubZone(j);
				} else {
					zones[i].setSubZones(pCount);
					for (int v = 0; v < pCount; v++)
						zones[i].subZones[v] = new SubZone(v);

				}
			}
		}

		for (Area area : modelAreas) {
			float theta = (new Line(centerOfMap, new Point(area.getX(),
					area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;

			int zoneNumber = (int) (theta / (360.0 / zonesCount));
			area.worldGraphArea.ZoneNumberForPolice = zoneNumber;
			zones[zoneNumber].areas.add(area);
			if (area instanceof Refuge)
				zones[zoneNumber].refuges.add((Refuge) area);
			else if (area instanceof Building)
				zones[zoneNumber].buildings.add((Building) area);
			else if (area instanceof Road)
				zones[zoneNumber].roads.add((Road) area);
		}
		for (Zone z : zones) {
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			for (Area area : z.areas) {
				if (area.getX() > maxX)
					maxX = area.getX();
				if (area.getX() < minX)
					minX = area.getX();
				if (area.getY() > maxY)
					maxY = area.getY();
				if (area.getY() < minY)
					minY = area.getY();
			}
			z.center = new Point((minX + maxX) / 2, (minY + maxY) / 2);
		}

	}

	protected void subZoning(int zoneNumber, int subZonesCount) {
		for (Area area : zones[zoneNumber].areas) {
			float theta = (new Line(zones[zoneNumber].center, new Point(
					area.getX(), area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;

			int subZoneNumber = (int) (theta / (360.0 / subZonesCount));
			zones[zoneNumber].subZones[subZoneNumber].areas.add(area);
			if (area instanceof Refuge)
				zones[zoneNumber].subZones[subZoneNumber].refuges
						.add((Refuge) area);
			else if (area instanceof Building)
				zones[zoneNumber].subZones[subZoneNumber].buildings
						.add((Building) area);
			else if (area instanceof Road)
				zones[zoneNumber].subZones[subZoneNumber].roads
						.add((Road) area);

		}
		for (SubZone sz : zones[zoneNumber].subZones) {
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxY = Integer.MIN_VALUE;
			for (Area area : sz.areas) {
				if (area.getX() > maxX)
					maxX = area.getX();
				if (area.getX() < minX)
					minX = area.getX();
				if (area.getY() > maxY)
					maxY = area.getY();
				if (area.getY() < minY)
					minY = area.getY();
			}
			sz.center = new Point((minX + maxX) / 2, (minY + maxY) / 2);
		}
	}

	public void subZoningForPolice(int zoneNumber, int policeZoneNumber,
			int subZonesCount) {
		for (Area area : zones[zoneNumber].policeZones[policeZoneNumber].areas) {
			float theta = (new Line(
					zones[zoneNumber].policeZones[policeZoneNumber].center,
					new Point(area.getX(), area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;
			int subZoneNumber = (int) (theta / (360.0 / subZonesCount));
			area.worldGraphArea.subZoneNumberForPolice = subZoneNumber;
			zones[zoneNumber].policeZones[policeZoneNumber].subZones[subZoneNumber].areas
					.add(area);
			if (area instanceof Refuge)
				zones[zoneNumber].policeZones[policeZoneNumber].subZones[subZoneNumber].refuges
						.add((Refuge) area);
			else if (area instanceof Building)
				zones[zoneNumber].policeZones[policeZoneNumber].subZones[subZoneNumber].buildings
						.add((Building) area);
			else if (area instanceof Road)
				zones[zoneNumber].policeZones[policeZoneNumber].subZones[subZoneNumber].roads
						.add((Road) area);
			if (area instanceof Building) {
				Building b = (Building) area;
				SubZone sz = zones[zoneNumber].policeZones[policeZoneNumber].subZones[subZoneNumber];
				if (b.isFierynessDefined() && b.getFieryness() > 0
						&& !sz.isIgnited) {
					sz.isIgnited = true;
					sz.priority += 5;
				}
			}
		}
		for (SubZone sz : zones[zoneNumber].policeZones[policeZoneNumber].subZones) {
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxY = Integer.MIN_VALUE;
			for (Area area : sz.areas) {
				if (area.getX() > maxX)
					maxX = area.getX();
				if (area.getX() < minX)
					minX = area.getX();
				if (area.getY() > maxY)
					maxY = area.getY();
				if (area.getY() < minY)
					minY = area.getY();
			}
			sz.center = new Point((minX + maxX) / 2, (minY + maxY) / 2);
		}

	}

	public SubZone getSubzoneForAmb(int myID) {
		ArrayList<Integer> ambulances = new ArrayList<Integer>();
		for (AmbulanceTeam amb : modelAmbulanceTeams)
			ambulances.add(amb.getID().getValue());
		Collections.sort(ambulances);
		int myIndex = 0;
		for (int iD : ambulances) {
			if (myID == iD)
				break;
			myIndex++;
		}
		int j = -1, n = -1;
		while (true) {
			if (zones.length * (j) + (n) == (myIndex))
				break;
			j++;
			for (int i = 0; i < zones.length; i++) {
				n = i;
				if (numberOfZones * (j) + (n) == (myIndex))
					return zones[i % zones.length].subZones[j];
			}
		}
		return null;
	}

	public void policeZoning(int zoneNumber, int policeZonesCount) {
		for (Area area : zones[zoneNumber].areas) {
			float theta = (new Line(zones[zoneNumber].center, new Point(
					area.getX(), area.getY()))).getTheta()
					* Degree.RAD2DEG;
			if (theta < 0)
				theta += 360;
			int policeZoneNumber = (int) (theta / (360.0 / policeZonesCount));
			area.worldGraphArea.policeZoneNumber = policeZoneNumber;
			zones[zoneNumber].policeZones[policeZoneNumber].areas.add(area);
			if (area instanceof Refuge)
				zones[zoneNumber].policeZones[policeZoneNumber].refuges
						.add((Refuge) area);
			else if (area instanceof Building)
				zones[zoneNumber].policeZones[policeZoneNumber].buildings
						.add((Building) area);
			else if (area instanceof Road)
				zones[zoneNumber].policeZones[policeZoneNumber].roads
						.add((Road) area);
		}
		for (PoliceZone pz : zones[zoneNumber].policeZones) {
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxY = Integer.MIN_VALUE;
			for (Area area : pz.areas) {
				if (area.getX() > maxX)
					maxX = area.getX();
				if (area.getX() < minX)
					minX = area.getX();
				if (area.getY() > maxY)
					maxY = area.getY();
				if (area.getY() < minY)
					minY = area.getY();
			}
			pz.center = new Point((minX + maxX) / 2, (minY + maxY) / 2);
		}
	}

	public void findMySubzone(ArrayList<Integer> policeDesicion2) {
		int myIndex = -1;
		ArrayList<Integer> allPoliceIDs = new ArrayList<Integer>();
		for (PoliceForce p : modelPoliceForces)
			allPoliceIDs.add(p.getID().getValue());
		myIndex = allPoliceIDs.indexOf(me().getID().getValue());
		if (myIndex == -1 || policeDesicion2 == null
				|| policeDesicion2.size() == 0) {
			mySubZone = null;
			myPoliceZone = null;
			myZoneNumber = -1;
			return;
		}
		mySubZoneID = policeDesicion2.get(myIndex);
		int counter = -1;
		done: for (Zone z : zones)
			for (PoliceZone pz : z.policeZones)
				for (SubZone sz : pz.subZones) {
					counter++;
					if (counter == mySubZoneID) {
						mySubZone = sz;
						mySubZone.subZoneID = mySubZoneID;
						myPoliceZone = pz;
						myZoneNumber = z.zoneNumber;
						break done;
					}
				}
	}

	public boolean sameAL(ArrayList<Integer> decision1,
			ArrayList<Integer> decision2) {
		if (decision1 == null || decision1.size() == 0) {
			if (decision2 == null || decision2.size() == 0) {
				return true;
			}
			return false;
		}
		for (int i = 0; i < decision1.size(); i++) {
			if (!decision1.get(i).equals(decision2.get(i))) {
				return false;
			}
		}
		return true;
	}

	BitSet bitsForPolice = new BitSet();

	protected void sendPoliceAL() {
		if (time > 4) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_CENTER, 0,
					Radar.headerSize);
			bits.set(start, false);
			start++;
			for (PoliceForce pf : modelPoliceForces) {
				if (pf.hasBuriedness) {
					start = Radar.calcWithBitSet(bits, 1, start, 1);
				} else
					start = Radar.calcWithBitSet(bits, 0, start, 1);
			}
			if (!radar.checkBits(bits, bitsForPolice, modelPoliceForces.size())) {
				bitsForPolice = new BitSet();
				bitsForPolice = (BitSet) bits.clone();
				counterForPoliceMap = 0;
			}
			if (counterForPoliceMap < 3) {
				counterForPoliceMap++;
				byte[] byteArray = Radar.toByteArray(bits);
				sendSpeak(time, behtarinChannel1, byteArray);
				if (behtarinChannel1 != 0)
					sendSpeak(time, 0, byteArray);
			}
		}
	}

	ArrayList<Integer> policeStatus = new ArrayList<Integer>();

	protected void hearPoliceMap() {
		if (heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = radar.tellHeader(recivedSpeak);
						BitSet bits = Radar.fromByteArray(recivedSpeak);
						if (header == Radar.HEADER_CENTER
								&& bits.get(Radar.headerSize) == false) {
							policeStatus = new ArrayList<Integer>();
							int startMessage = Radar.headerSize + 1;
							int powNumer = Radar.findPow(modelAreas.size() + 2);
							for (int i = 0; i < modelPoliceForces.size(); i++) {
								int heard = Radar.fromMabnaye2ToMabnaye10(bits,
										startMessage + powNumer, startMessage);
								startMessage += powNumer;
								if (heard == 1)
									policeStatus.add(-1);
								else if (heard == 0) {
									policeStatus.add(0);
									modelPoliceForces.get(i).stuckTarget = null;
								} else {
									policeStatus.add(-1);
									modelPoliceForces.get(i).stuckTarget = (Area) allRanks
											.get(heard - 2);
								}
							}
						}
					}
				}
			}
		}
	}

	protected abstract void decide() throws ActionCommandException;
}
