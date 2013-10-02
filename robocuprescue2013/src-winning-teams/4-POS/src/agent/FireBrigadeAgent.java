package agent;

import geometry.Mathematic;
import geometry.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import worldGraph.Blockade;
import worldGraph.Enterance;

public class FireBrigadeAgent extends Agent<FireBrigade> {
	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	public int maxWater = -1;
	private int maxPower = -1;
	private Building extTarget;
	private Building myBuilding;
	private FireZone myZone = null;
	public Point centralPointOfMap = new Point();
	public boolean scannerError = false;
	public boolean bigMap = true;
	public boolean salam = false;
	public boolean IamOnfire = false;
	public Random random;
	private ArrayList<FireZone> zoneChanging;
	protected int waitingForRefuge = 0;
	protected Building lastBuilding = null;


	protected void postConnect() {
		super.postConnect();
		random = new Random(me().getID().getValue());
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);
		fb2012.maxPower = config.getIntValue(MAX_POWER_KEY);
		fb2012.maxWater = config.getIntValue(MAX_WATER_KEY);
		System.out.println("PreCompuation of Fire Started");
		// visionRoadsAndBuildings();
		fb2012.agent = this;
		littleZoneMaking(15);
		searchZones = new ArrayList<LittleZone>();
		searchZones.addAll(newZone);

		System.out.println("PreCompuation of Fire Finished");
	}

	public void scanner() {
		try {
			Scanner scanner;
			try {
				scanner = new Scanner(new File("./Configs/My-File.txt"));
				if (modelBuildings.size() == 1426 && modelRoads.size() == 3385)
					scanner = new Scanner(new File("./Configs/In-Berlin.txt"));
				else if (modelBuildings.size() == 1244
						&& modelRoads.size() == 3337)
					scanner = new Scanner(new File("./Configs/In-Istanbul.txt"));
				else if (modelBuildings.size() == 736
						&& modelRoads.size() == 1515)
					scanner = new Scanner(new File("./Configs/In-Kobe.txt"));
				else if (modelBuildings.size() == 1618
						&& modelRoads.size() == 3025)
					scanner = new Scanner(new File("./Configs/In-Paris.txt"));
				else if (modelBuildings.size() == 1263
						&& modelRoads.size() == 1954)
					scanner = new Scanner(new File("./Configs/In-VC.txt"));
				salam = true;
				ArrayList<Area> hamunModelAreas = (ArrayList<Area>) modelAreas
						.clone();
				Collections.sort(hamunModelAreas, areaSorter);
				while (scanner.hasNext()) {
					int n = scanner.nextInt();
					ArrayList<Building> builOfZone = new ArrayList<Building>();
					for (int i = 0; i < n; i++) {
						int id = scanner.nextInt();
						if (hamunModelAreas.get(id) instanceof Building) {
							Building b = (Building) hamunModelAreas.get(id);
							builOfZone.add(b);
						}
					}
					LittleZone littleZone = new LittleZone();
					littleZone.buildings.addAll(builOfZone);
					if (littleZone.buildings.size() != 0) {
						littleZone.id = searchZones.size();
						searchZones.add(littleZone);
					}
				}
			} catch (FileNotFoundException e) {
				salam = false;
				e.printStackTrace();
			}
		} catch (Exception e) {
			salam = false;
			e.printStackTrace();
			if (logger.getWriter() != null) {
				System.out.println("me : " + me().getID().getValue());
				e.printStackTrace(logger.getWriter());
			}
		}
	}

	private ArrayList<LittleZone> reachableLittleZone = new ArrayList<LittleZone>();

	private LittleZone selectRandomZoneForSearch() {
		reachableLittleZone = new ArrayList<LittleZone>();
		for (LittleZone little : newZone)
			for (Building building : little.buildings) {
				if (buildingReachability(building) && little.owners.size() == 0) {
					reachableLittleZone.add(little);
					break;
				}
			}
		if (reachableLittleZone.size() == 0)
			return null;
		LittleZone lz = reachableLittleZone.get(random
				.nextInt(reachableLittleZone.size()));
		lz.searchBuildings = (ArrayList<Building>) lz.buildings.clone();
		// sendEmptyBuildings(lz.id, true);
		return lz;
	}

	private Building findtheNearestLTS() {
		ArrayList<Building> top5 = new ArrayList<Building>();
		int minlts = Integer.MAX_VALUE;
		Building tarSearch = null;

		for (Building build : modelBuildings) {
			if (minlts > build.lastTimeSeen && buildingReachability(build))
				minlts = build.lastTimeSeen;
		}
		ArrayList<Building> minltsbuilding = new ArrayList<Building>();
		for (Building building : modelBuildings) {
			if (building.lastTimeSeen == minlts
					&& buildingReachability(building))
				minltsbuilding.add(building);
		}

		for (int i = 0; i < 5; i++) {
			double minDis = Double.MAX_VALUE;
			if (minltsbuilding.size() == 0)
				break;
			for (Building building : minltsbuilding) {
				if (Math.hypot(building.getX() - me().getX(), building.getY()
						- me().getY()) < minDis) {
					minDis = Math.hypot(building.getX() - me().getX(),
							building.getY() - me().getY());
					tarSearch = building;
				}
			}
			if (!top5.contains(tarSearch))
				top5.add(tarSearch);
			minltsbuilding.remove(tarSearch);
		}

		tarSearch = top5.get(random.nextInt(top5.size()));
		return tarSearch;
	}

	boolean isSearching = false;
	LittleZone randZone = null;
	Boolean hasTemp = false;
	Building btemp = null;
	Building minbuild = null;
	ArrayList<Building> copyOfNearBuilding = new ArrayList<Building>();
	ArrayList<Building> allBtemp = new ArrayList<Building>();
	boolean allNotNull = false;

	public void checkNearBuildingsForSearch() {
		for (EntityID id : changeSet.getChangedEntities()) {
			if (model.getEntity(id) instanceof Building
					&& copyOfNearBuilding.contains(model.getEntity(id))) {
				copyOfNearBuilding.remove(model.getEntity(id));
			}
		}
		for (Building build : btemp.nearBuildings) {
			if (!buildingReachability(build)) {
				copyOfNearBuilding.remove(build);
			}
		}
	}

	public void tempSearch() throws ActionCommandException {
		if (noCommi && ExtinguishInNocom) {
			hasTemp = false;
			btemp = null;
			lastZone = null;
			ExtinguishInNocom = false;
		}
		chooseBtemp();
		setFireBrigadeForSearch();
		if (hasTemp && !noCommi) {
			log("searchByTem");
			checkNearBuildingsForSearch();
			double mindis = Double.MAX_VALUE;
			if (minbuild != null && copyOfNearBuilding.contains(minbuild)) {
				moveToFire(minbuild);
			}
			if (copyOfNearBuilding.size() != 0) {
				for (Building build : copyOfNearBuilding) {
					if (Math.hypot(build.getX() - me().getX(), build.getY()
							- me().getY()) < mindis) {
						mindis = Math.hypot(build.getX() - me().getX(),
								build.getY() - me().getY());
						minbuild = build;
					}
				}
				moveToFire(minbuild);
			} else {
				for (Building build : btemp.nearBuildings) {
					if (build.isTemperatureDefined()
							&& build.getTemperature() != 0
							&& !allBtemp.contains(build)) {
						btemp = build;
						copyOfNearBuilding = (ArrayList<Building>) build.nearBuildings
								.clone();
						allBtemp.add(build);
						endSearch();
					}
				}

			}
			hasTemp = false;
			btemp = null;
			lastZone = null;
			minbuild = null;

		}

	}

	public void chooseBtemp() {
		if (btemp == null && !noCommi) {
			for (EntityID id : changeSet.getChangedEntities()) {
				if (model.getEntity(id) instanceof Building) {
					Building build = (Building) model.getEntity(id);
					if (build.isTemperatureDefined()
							&& build.getTemperature() != 0
							&& !allBtemp.contains(build)) {
						btemp = build;
						allBtemp.add(build);
						hasTemp = true;
						copyOfNearBuilding = (ArrayList<Building>) btemp.nearBuildings
								.clone();
						log("btemp:" + btemp.getID().getValue());
						break;
					}
				}
			}
		}

	}

	public double setDistFromzoneCentreToAround(FireZone fz) {
		double maxDist = -1;
		Point po = null;
		for (Building b : fz.aroundBuildings) {
			for (Point p : b.worldGraphArea.points) {
				double dist = Math.hypot(p.getX() - fz.centerX, p.getY()
						- fz.centerY);
				if (maxDist < dist) {
					maxDist = dist;
					po = p;
				}
			}
		}
		log(" doortarin point : " + po);
		return maxDist;
	}

	public void setFireBrigadeForSearch() {
		log("dare entekahb mikone");
		HashSet<FireBrigade> nearFireBrigades = new HashSet<FireBrigade>();
		double zoneCentreToAround = setDistFromzoneCentreToAround(lastZone) + 30000;
		for (Human h : hearAGPosition.keySet()) {
			if (h instanceof FireBrigade && !h.hasBuriedness) {
				if (Math.hypot(h.getX() - lastZone.centerX, h.getY()
						- lastZone.centerY) < zoneCentreToAround) {
					nearFireBrigades.add((FireBrigade) h);
				}
			}
		}
		for (int i = 0; i < 2; i++) {
			int id = Integer.MAX_VALUE;
			FireBrigade fire = null;
			for (FireBrigade f : nearFireBrigades) {
				if (f.getID().getValue() < id) {
					id = f.getID().getValue();
					fire = f;
				}
			}
			log("fire " + fire);
			if (me().equals(fire)) {
				IamSearching = true;
				break;
			} else
				nearFireBrigades.remove(fire);
		}
	}

	public Building lastLtsAndNearest = null;

	private void endSearch() throws ActionCommandException {

		log("endSearch");
		if(reachableAreas.size() <= 1)
			randomWalk();
		// tempSearch();
		if (!hasTemp) {
			if (!isSearching || randZone == null) {
				randZone = selectRandomZoneForSearch();
				isSearching = true;
			}
			if (randZone != null) {
				for (int i = 0; i < randZone.searchBuildings.size(); i++) {
					if (isInChangeSet(randZone.searchBuildings.get(i).getID())
							|| !buildingReachability(randZone.searchBuildings
									.get(i))) {
						randZone.searchBuildings.remove(i);
						i--;
					}
				}
				if (randZone != null && randZone.searchBuildings.size() != 0) {
					if (!randZone.searchBuildings.contains(lastTar3)) {
						newRndomSearch(randZone);
						printLittleZone(randZone);
						moveToFire(lastTar3);
					} else {
						moveToFire(lastTar3);
					}
				} else {
					if (lastBuilding != null) {
						if (lastBuilding.lastTimeSeen < time
								&& isReachable(lastBuilding.getID()))
							moveToFire(lastBuilding);
						else
							lastBuilding = null;
					}
					Building ltsAndNearest = null;
					if (lastLtsAndNearest == null)
						ltsAndNearest = findtheNearestLTS();
					if (lastLtsAndNearest != null
							&& !isInChangeSet(lastLtsAndNearest.getID()))
						moveToFire(ltsAndNearest);
					else {
						log("endSearch()/!hasTemp/2");
						lastLtsAndNearest = null;
						log("XXXX : " + ltsAndNearest.getID());
						moveToFire(ltsAndNearest);
					}
				}
			}
		}
	}

	public ArrayList<Point> getVisionPoints(Area area, Building building,
			int minMax) {
		return getVisionPoints(model, area, building, maxDistanceExtingiush,
				minMax);
	}

	public EntityID target = null;
	boolean goingToRefuge = false;

		private void checkWater() throws ActionCommandException {
		Area targetRefuge = null;
		log("CHECK WATER");
		log("my water?~~~~~~" + me().getWater());
		if (me().getWater() == 0)
			goingToRefuge = true;
		if (me().getWater() >= maxWater) {
			goingToRefuge = false;
		}
		if (goingToRefuge) {
			log("going to refuge:" + goingToRefuge);
			if (reachableRefuges != null && reachableRefuges.size() != 0) {
				log("baiad beram b refuge!");
				targetRefuge = reachableRefuges.get(0);
			} else if (reachableHydrants != null
					&& reachableHydrants.size() != 0) {
				log("ghablesh size!:"+reachableHydrants.size());
				for (int i = 0; i < reachableHydrants.size(); i++) {
					for (FireBrigade f : modelFireBrigades) {
						log("ki ?"+f.getID().getValue() + "  jaash?"+f.getPosition().getValue());
						if (f.getPosition().getValue() == reachableHydrants
								.get(i).getID().getValue() && f.getID().getValue() != me().getID()
										.getValue()
								&& me().getID().getValue() > f.getID().getValue()) {
							log("remove kardam!:D"+reachableHydrants.get(i));
							reachableHydrants.remove(i);
							i--;
							break;
						}
					}
				}
				if (reachableHydrants.size() != 0) {
					log("size ?"+reachableHydrants.size());
					log("reachable refuge nadaram ama hydrant hast");
					targetRefuge = reachableHydrants.get(0);
					log("mikham bram b hydrant!");
				}
			}
			if (targetRefuge == null)
				endSearch();
			move(targetRefuge.getID());
		}
	}
	public static final double w1 = 100, w2 = 5, w3 = -2.5, w4 = -5;

	public ArrayList<Building> reachableAndIgnitedBuildings = new ArrayList<Building>();
	public int counter = 10;

	public boolean buildingReachability(Building building) {
		if (building.worldGraphArea.buildingReachabilityCalculated)
			return building.worldGraphArea.buildingReachability;

		building.worldGraphArea.buildingReachabilityCalculated = true;
		if (isInChangeSet(building.getID()) && canExtinguish(building)
				&& !IamOnfire && building.hasGoodVisionPoint()) {
			building.worldGraphArea.buildingReachability = true;
			return true;
		}
		if (building.getVisionRoads().size() == 0
				&& building.visionBuildings.size() == 0) {
			building.worldGraphArea.buildingReachability = false;
			return false;
		}
		if (building.worldGraphArea.isReachable && !IamOnfire
				&& building.hasGoodVisionPoint()) {
			building.worldGraphArea.buildingReachability = true;
			return true;
		}
		for (Road road : building.getVisionRoads().keySet())
			if (road.worldGraphArea.isReachable) {
				for (Point point : building.getVisionRoads().get(road)) {
					boolean i = true;
					for (Blockade blockade : road.worldGraphArea.blockades)
						if (blockade.isInShape(point)) {
							i = false;
							break;
						}
					if (i == true) {
						for (Enterance e : road.worldGraphArea.enterances) {
							if (e.isReachable) {
								for (Point p : e.avaialablePoints) {
									if (e.area.isThereWay(point, p, 500)) {
										building.worldGraphArea.buildingReachability = true;
										return true;
									}
								}
							}
						}

						// building.worldGraphArea.buildingReachability = true;
						// return true;
					}
				}
			}
		for (Building b : building.visionBuildings.keySet())
			if (b.worldGraphArea.isReachable && b.stFire == 0) {
				building.worldGraphArea.buildingReachability = true;
				return true;
			}
		building.worldGraphArea.buildingReachability = false;
		return false;
	}

	private void moveAndExtinguish(Building fireTarget)
			throws ActionCommandException {
		if (fireTarget != null) {
			beforeExt(fireTarget);
			log("extTarget : " + fireTarget.getID().getValue()
					+ " reachable?  " + buildingReachability(fireTarget)
					+ " lts: " + fireTarget.lastTimeSeen);
			if (isInChangeSet(fireTarget.getID()) && canExtinguish(fireTarget)
					&& !IamOnfire) {
				extinguish(fireTarget.getID(), maxPower);
			}
			log("moveAndExtinguish()");
			moveToFire(fireTarget);
		}
	}

	public int countReachableBuilds = 0;

	public void simulator(Building building) {
		for (Building building2 : building.nearBuildings)
			if (!building2.isFierynessDefined()
					|| building2.getFieryness() == 0) {
				building.nearForSimulator.add(building2);
			}
	}

	public void setVolumeSimulator(Building buil) {
		buil.volume = 0;
		for (Building hf : buil.nearForSimulator) {
			buil.volume += hf.getFloors() * hf.getGroundArea()
					/ maxVolumeOfAllBuildings;
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
					if (map == MapType.Berlin || map == MapType.Paris
							|| map == MapType.Istanbul) {
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

	int timeSearch = 0;
	FireZone exzone = null;
	ArrayList<FireZone> hibitra = new ArrayList<FireZone>();
	FireBrigadeAgent2012 fb2012 = new FireBrigadeAgent2012();

	private void NoHear() throws ActionCommandException {
		log("noHear");
		if (me().aim != null && me().aim.stFire > 0
				&& buildingReachability(me().aim)
				&& allArounds.contains(me().aim)) {
			log("first if");

			moveAndExtinguish(me().aim);
		}
		// TODO in yaroo miad ye bar toye yoho aim entekhab mikone
		if (me().aim != null) {
			Building mindisb = null;
			int mindis = Integer.MAX_VALUE;
			if (me().aim.zoneNumber != -1) {
				log("bakhsh aval");
				for (Building build : modelZones.get(me().aim.zoneNumber).aroundBuildings) {
					if (buildingReachability(build)) {
						int dist = getDistToFire(build);
						if (dist < mindis) {
							mindis = dist;
							mindisb = build;
						}
					}
				}
			} else {
				if (allArounds.size() != 0) {
					log("bakhsh dovom");
					for (Building build : allArounds) {
						if (buildingReachability(build)) {
							int dist = getDistToFire(build);
							if (dist < mindis) {
								mindis = dist;
								mindisb = build;
							}
						}
					}
				}
			}
			if (mindisb != null) {
				log("if sevom: mindisb :" + mindisb.getID().getValue());
				moveAndExtinguish(mindisb);
			}

		}

		if (modelZones.size() != 0) {
			fb2012.decide();
		}
		fb2012.checkEmergencySituation();
		endSearch();
	}

	public void hearFire() {
		log("hearfire");
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

						if (header == Radar.HEADER_FIRECENTER) {
							shenide = true;
							int startMessage = Radar.headerSize;
							int powNumber = Radar.findPow(EntityBeInt.size());
							for (int i = 0; i < modelFireBrigades.size(); i++) {
								int target = Radar.fromMabnaye2ToMabnaye10(
										bits, startMessage + powNumber,
										startMessage);
								if (target == 0) {
									startMessage += powNumber;
									// log("aim in hearfire nulle?: "
									// + null
									// + " kasi ke shenide: "
									// + modelFireBrigades.get(i).getID()
									// .getValue() + " shenide: "
									// + shenide);
								} else {
									Entity aim2 = model.getEntity(IntBeEntity
											.get(target));
									modelFireBrigades.get(i).aim = (Building) aim2;
									int lastTimeSeen = Radar
											.fromMabnaye2ToMabnaye10(bits,
													startMessage + powNumber
															+ 5, startMessage
															+ powNumber);
									modelFireBrigades.get(i).aim.heardlastTimeSeen = cmd
											.getTime() - lastTimeSeen;
									if (((Building) aim2).lastTimeSeen == time)
										((Building) aim2).shouldSend = true;
									if (((Building) aim2).lastTimeSeen < cmd
											.getTime() - lastTimeSeen
											&& modelFireBrigades.get(i).getID()
													.getValue() == me().getID()
													.getValue()) {
										((Building) aim2).stFire = 1;
										((Building) aim2).lastTimeSeen = cmd
												.getTime() - lastTimeSeen;
										((Building) aim2).lastFireness = 1;
										longnBP.put(aim2.getID().getValue(), 1);
									}
									startMessage += powNumber + 5;
									// log("aim in hearfire: "
									// + modelFireBrigades.get(i).aim
									// + " kasi ke shenide "
									// + modelFireBrigades.get(i).getID()
									// .getValue());
									// log("lasttimeseen: " + lastTimeSeen);
								}
							}
						}
					}
				}
			}
		}
	}

	public void beforeExt(Building target) {
		log("in before ext");
		isSearching = false;
		btemp = null;
		minbuild = null;
		hasTemp = false;
		allBtemp = new ArrayList<Building>();
		log("target " + target.getID().getValue() + " target.z : "
				+ target.zoneNumber);
		lastZone = modelZones.get(target.zoneNumber);
		copyOfZone = (ArrayList<Building>) lastZone.buildings.clone();
		for (Building b : lastZone.buildings)
			for (Building bu : b.nearBuildings)
				if (!copyOfZone.contains(bu))
					copyOfZone.add(bu);
		String cop = "";
		for (Building b : copyOfZone)
			cop += b.getID().getValue() + " ";
		log("copy : " + cop);
	}

	public void setIamOnFire() {
		IamOnfire = false;
		Entity myPos = me().getPosition(model);
		if (myPos instanceof Building && ((Building) myPos).stFire > 0) {
			IamOnfire = true;
		}
		ArrayList<Building> isReachableAndNotIgnited = new ArrayList<Building>();
		for (Building building : modelBuildings) {
			if (building.worldGraphArea.isReachable && building.stFire == 0)
				isReachableAndNotIgnited.add(building);
		}
		if (reachableRoads.size() == 0 && isReachableAndNotIgnited.size() == 0)
			IamOnfire = false;
	}

	public void printChangeSet(FireBrigade f) {
		String ch = "";
		for (EntityID id : changeSet.getChangedEntities()) {
			if (model.getEntity(id) instanceof Building) {
				Building build = (Building) model.getEntity(id);
				ch += build.getID().getValue() + " ";
			}
		}
		log("printChangeSet: " + ch);
	}

	public void printZones() {
		// String z = "";
		// String mk = "";
		// for (FireZone f : modelZones) {
		// for (Building b : f.buildings) {
		// z += b.getID().getValue() + " ";
		// if (buildingReachability(b))
		// mk += b.getID().getValue() + " ";
		// }
		// z += "; ";
		// mk += "; ";
		// }
		// log("printZone: " + z);
		// log("onaE ke mitoonam beBinam" + mk);
	}

	public void printLittleZone(LittleZone lz) {
		String s = "";
		for (Building b : lz.buildings)
			s += b.getID().getValue() + " ";
		s += ";";
		log("printLittleZone: " + s);
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

	public Building chooseMinLtsAndNearest(ArrayList<Building> AB) {
		double mindis = Double.MAX_VALUE;
		Building mindisb = null;
		if (AB.size() != 0) {
			double minlts = Double.MAX_VALUE;
			for (Building b : AB) {
				if (b.lastTimeSeen < minlts) {
					minlts = b.lastTimeSeen;
				}
			}
			ArrayList<Building> minltsBuildings = new ArrayList<Building>();
			for (Building b : AB) {
				if (b.lastTimeSeen == minlts) {
					minltsBuildings.add(b);
				}
			}
			for (Building b : minltsBuildings) {
				if (Math.hypot(b.getX() - me().getX(), b.getY() - me().getY()) < mindis) {
					mindis = Math.hypot(b.getX() - me().getX(), b.getY()
							- me().getY());
					mindisb = b;
				}
			}

		}
		return mindisb;
	}

	public Building searchAroundZone() {
		for (EntityID id : changeSet.getChangedEntities()) {
			if (model.getEntity(id) instanceof Building
					&& copyOfZone.contains(model.getEntity(id))) {
				copyOfZone.remove(model.getEntity(id));
			}
		}
		if (copyOfZone.size() != 0) {
			HashSet<Building> notReachable = new HashSet<Building>();
			for (Building b : copyOfZone) {
				if (!buildingReachability(b)) {
					notReachable.add(b);
				}
			}
			copyOfZone.removeAll(notReachable);
		}
		return chooseMinLtsAndNearest(copyOfZone);
	}

	public void checkIamSearching() {
		for (EntityID id : changeSet.getChangedEntities())
			if (model.getEntity(id) instanceof Building) {
				Building b = (Building) model.getEntity(id);
				if (b.stFire > 0)
					IamSearching = false;
			}

	}

	boolean shenide = false;

	public FireZone lastZone = null;
	public ArrayList<Building> copyOfZone = new ArrayList<Building>();
	public boolean ExtinguishInNocom = false;
	public HashSet<Building> allArounds = new HashSet<Building>();
	public boolean IamSearching = false;
	public Building lastSearchB = null;

	protected void decide() throws ActionCommandException {
		// /XXX in chizi ke rajebe pak kardane wold graph area ha toye 2012 hast
		// ro ma nemikhaim?
		log("decide Fire");
		log("me.aim : " + me().aim);
		log("my position: " + me().getPosition().getValue());
		printChangeSet(me());
		setIamOnFire();
		if (map == MapType.Kobe || map == MapType.VC)
			bigMap = false;
		initialBuildings();
		checkWater();
		if (me().hasBuriedness) {
			log("Buridness daram");
			rest();
		}
		zoning();
		setZoneCenter();
		printZones();
		allArounds = new HashSet<Building>();
		for (FireZone z : modelZones) {
			z.setAroundBuildings(bigMap);
			allArounds.addAll(z.aroundBuildings);
		}
		if (lastZone != null && lastZone.buildings.get(0).zoneNumber == -1) {
			if (!IamSearching)
				setFireBrigadeForSearch();
			checkIamSearching();
			if (IamSearching) {
				log("man daram search mikonam");
				Building searchB = null;
				searchB = searchAroundZone();
				if (lastSearchB == null) {
					lastSearchB = searchB;
				}
				log(" Buidinge ke mkham search konam  : " + searchB);
				if (lastSearchB != null && copyOfZone.contains(lastSearchB))
					moveToFire(lastSearchB);
				else if (searchB != null) {
					lastSearchB = null;
					log("move to searchB");
					moveToFire(searchB);

				} else {
					log("searcham tamoom shod");
					IamSearching = false;
					lastSearchB = null;
					lastZone = null;
				}
			}
		} else
			IamSearching = false;
		if (!IamSearching) {
			if (!shenide
					|| me().aim == null
					|| me().aim.stFire == 0
					|| !buildingReachability(me().aim)
					|| (!allArounds.contains(me().aim) && me().aim.heardlastTimeSeen < me().aim.lastTimeSeen)) {
				log("toye ife nohear");
				NoHear();
			} else {
				log("AIM DARAM");
				moveAndExtinguish(me().aim);
			}
		}

	}

	private HashSet<FireBrigade> bFSForFire() {
		HashSet<Enterance> laye = new HashSet<Enterance>();
		HashSet<FireBrigade> list = new HashSet<FireBrigade>();
		wg.clearAreas();
		list.add(me());
		for (Enterance enterance : wg.myEnterances) {
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

	private Comparator<FireBrigade> idComparator = new Comparator<FireBrigade>() {
		public int compare(FireBrigade a, FireBrigade b) {
			if (a.getID().getValue() > b.getID().getValue())
				return 1;
			if (a.getID().getValue() < b.getID().getValue())
				return -1;
			return 0;
		}
	};

	private Comparator<FireZone> zoneComparator = new Comparator<FireZone>() {
		public int compare(FireZone a, FireZone b) {
			if (a.value > b.value)
				return 1;
			if (a.value < b.value)
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
		if (me().hasBuriedness)
			return;
		for (FireBrigade brigade : bFSForFire())
			if (!brigade.hasBuriedness)
				fireBrigades.add(brigade);
		myZone = null;
		for (FireZone fireZone : modelZones) {
			fireZone.value = 0;
			fireZone.assignedWorkerNum = 0;
			for (Building building : fireZone.buildings)
				fireZone.value += building.getGroundArea() / averageOfSurfaces;
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
					if (me().getID().getValue() == f.getID().getValue()) {
						myZone = sortedZone.get(g);
						if (myZone != null && myZone.emergency
								&& !isCalledByZoneChange)
							unemploymentSituation(myZone);
						if (myZone.isInhibitor)
							selectNearestHibitor = true;
					}
					if (f.getID().getValue() != me().getID().getValue()
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
				for (int i = 0; i < me().distToFire.size(); i++)
					if (me().distToFire.get(i) < minDist
							&& sortedZone.get(i).isInhibitor) {
						minDist = me().distToFire.get(i);
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
					if (me().getID().getValue() == f.getID().getValue()) {
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
				for (int i = 0; i < me().distToFire.size(); i++)
					if (me().distToFire.get(i) < minDist
							&& sortedZone.get(i).isInhibitor) {
						minDist = me().distToFire.get(i);
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
			dist = Math.hypot(me().getX() - z.centerX, me().getY() - z.centerY);
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

	private int getDistToFire(Building building) {
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
						if (me().getPosition().getValue() == road.getID()
								.getValue())
							return 1;
						minDist = road.worldGraphArea.distanceFromSelf;
						break done;
					}
				}
		}

		for (Building buil : building.visionBuildings.keySet()) {
			if (buil.worldGraphArea.isReachable) {
				if (me().getPosition().getValue() == buil.getID().getValue())
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

	public void moveToFire(Building target) throws ActionCommandException {
		wg.clearTargets();
		log("JaE ke daram moveToFire mizanam  " + target.getID().getValue());
		for (Road road : target.getVisionRoads().keySet()) {
			boolean tf = true;
			if (road.worldGraphArea.isReachable)
				for (int j = 0; j < target.getVisionRoads().get(road).size(); j++) {
					if (tf == false)
						break;
					boolean i = true;
					for (Blockade blockade : road.worldGraphArea.blockades)
						if (blockade.isInShape(target.getVisionRoads()
								.get(road).get(j))) {
							i = false;
							break;
						}

					double dist = Math.hypot(me().getX()
							- target.getVisionRoads().get(road).get(j).getX(),
							me().getY()
									- target.getVisionRoads().get(road).get(j)
											.getY());

					if (dist < 400 && !isInChangeSet(target.getID())) {
						System.out.println("MaxDist: " + MAX_DISTANCE_VIEW);
						System.out.println("Dist: "
								+ Math.hypot(me().getX() - target.getX(), me()
										.getY() - target.getY()));
						System.err.println("Time: " + time + " "
								+ target.getID().getValue() + " "
								+ road.getID().getValue() + " Agent: "
								+ me().getID().getValue());
						target.getVisionRoads().get(road).remove(j);
						if (target.getVisionRoads().get(road).size() == 0)
							target.getVisionRoads().remove(road);
						target.worldGraphArea.buildingReachabilityCalculated = false;
						decide();
					}

					if (i == true) {
						Point poi = target.getVisionRoads().get(road).get(j);
						for (Enterance e : road.worldGraphArea.enterances) {
							if (tf == false)
								break;
							if (e.isReachable) {
								for (Point p : e.avaialablePoints) {
									if (e.area.isThereWay(poi, p, 500)) {
										if (me().getPosition().getValue() == road
												.getID().getValue()) {
											ArrayList<EntityID> path = new ArrayList<EntityID>();
											path.add(road.getID());
											move(path, target.getVisionRoads()
													.get(road).get(j).getX(),
													target.getVisionRoads()
															.get(road).get(j)
															.getY(), false);
										}
										road.worldGraphArea
												.setAreaAsTarget(true);
										tf = false;
										break;
									}
								}
							}
						}
						// if (me().getPosition().getValue() == road.getID()
						// .getValue()) {
						// ArrayList<EntityID> path = new ArrayList<EntityID>();
						// path.add(road.getID());
						// move(path, target.getVisionRoads().get(road).get(j)
						// .getX(), target.getVisionRoads().get(road)
						// .get(j).getY(), false);
						// }
						// road.worldGraphArea.setAreaAsTarget(true);
						// break;
					}
				}
		}
		for (Building building : target.visionBuildings.keySet()) {
			if (building.worldGraphArea.isReachable && building.stFire == 0) {
				if (me().getPosition().getValue() == building.getID()
						.getValue()) {
					ArrayList<EntityID> path = new ArrayList<EntityID>();
					path.add(building.getID());
					move(path, target.visionBuildings.get(building).getX(),
							target.visionBuildings.get(building).getY(), false);

				}
				building.worldGraphArea.setAreaAsTarget(true);
			}
		}

		int x = -1, y = -1;
		ArrayList<EntityID> path = wg.getMinPath(wg.myEnterances, true);
		if (path.size() == 0 && target.worldGraphArea.isReachable) {
			move(target.getID());
		}
		if (path.size() == 0) {
			// System.err.println("BAD SiDtuation");
			log("rest in move to fire");
			rest();
		}
		for (Road r : target.getVisionRoads().keySet()) {
			if (path.get(path.size() - 1).getValue() == r.getID().getValue()) {
				for (int j = 0; j < target.getVisionRoads().get(r).size(); j++) {
					boolean i = true;
					for (Blockade blockade : r.worldGraphArea.blockades)
						if (blockade.isInShape(target.getVisionRoads().get(r)
								.get(j))) {
							i = false;
							break;
						}
					if (i == true) {
						x = target.getVisionRoads().get(r).get(j).getX();
						y = target.getVisionRoads().get(r).get(j).getY();
						move(path, x, y, false);
					}
				}
			}
		}
		for (Building b : target.visionBuildings.keySet())
			if (b.stFire == 0
					&& path.get(path.size() - 1).getValue() == b.getID()
							.getValue()) {
				x = target.visionBuildings.get(b).getX();
				y = target.visionBuildings.get(b).getY();
				move(path, x, y, false);
			}

	}

	private boolean canExtinguish(Building target) {
		return Mathematic.getDistance(target.getX(), target.getY(),
				me().getX(), me().getY()) < maxDistanceExtingiush;
	}

	private Building lastTar3 = null;

	private void newRndomSearch(LittleZone randZone)
			throws ActionCommandException {
		int minlts1 = Integer.MAX_VALUE;
		double mindist = Double.MAX_VALUE;
		ArrayList<Building> minLastTimeSeen = new ArrayList<Building>();
		Building tar3 = null;
		for (Building building : randZone.searchBuildings)
			if (building.lastTimeSeen <= minlts1)
				minlts1 = building.lastTimeSeen;
		for (Building building : randZone.searchBuildings)
			if (building.lastTimeSeen == minlts1)
				minLastTimeSeen.add(building);
		for (Building building : minLastTimeSeen) {
			if (Math.hypot(building.getX() - me().getX(), building.getY()
					- me().getY()) <= mindist) {
				mindist = Math.hypot(building.getX() - me().getX(),
						building.getY() - me().getY());
				tar3 = building;
			}
		}
		if (tar3 != null)
			lastTar3 = tar3;
	}

	private ArrayList<Building> ignitedBuildings = new ArrayList<Building>();

	// XXX notIgnitedNearBuilding ro pak konam

	private void initialBuildings() {
		ignitedBuildings.clear();
		reachableAndIgnitedBuildings = new ArrayList<Building>();
		for (Building build : modelBuildings) {
			if (build.stFire > 0) {
				ignitedBuildings.add(build);

			}
		}
		for (Building build : ignitedBuildings)
			if (buildingReachability(build))
				reachableAndIgnitedBuildings.add(build);
	}

	private Building tar1 = null;

	private void unemploymentSituation(FireZone FZ)
			throws ActionCommandException {
		log(" unemployment ");
		checkWater();
		Building buil1 = null;
		int maxLast = 0;
		for (StandardEntity e : model.getObjectsInRange(me().getX(), me()
				.getY(), maxDistanceExtingiush))
			if (e instanceof Building && ((Building) e).stFire > 0
					&& canExtinguish((Building) e)
					&& !buildingReachability((Building) e)
					&& ((Building) e).lastTimeSeen > maxLast
					&& ((Building) e).lastTimeSeen + 10 >= time) {
				maxLast = ((Building) e).lastTimeSeen;
				buil1 = ((Building) e);
			}
		if (buil1 != null) {
			extinguish(buil1.getID(), maxPower);
		}
		int maxheard = 0;
		Building tar = null;
		for (Building building : FZ.buildings)
			if (unreachableAndIgnitedBuildings.contains(building)
					&& building.lastTimeSeen > maxheard) {
				maxheard = building.lastTimeSeen;
				tar = building;
			}
		if (tar1 == null || tar1.stFire == 0 || !FZ.buildings.contains(tar1))
			tar1 = tar;
		if (tar1 != null) {
			wg.clearTargets();
			for (Area area : tar1.nearAreas50000)
				if (area instanceof Road
						&& Math.hypot(area.getX() - tar1.getX(), area.getY()
								- tar1.getY()) < maxDistanceExtingiush)
					area.worldGraphArea.setAreaAsTarget(true);
			ArrayList<EntityID> path = wg.getMinPath(wg.myEnterances, true);
			if (path.size() != 0) {
				move(path, false);
			}
		}
	}

	public ArrayList<FireZone> modelZones = null;
	private FireZone zone = null;
	ArrayList<Building> unreachableAndIgnitedBuildings = new ArrayList<Building>();

	public void zoning() {
		modelZones = new ArrayList<FireZone>();
		for (Building isCheckingbuilding : modelBuildings) {
			isCheckingbuilding.zoneNumber = -1;
			isCheckingbuilding.haveZone = false;
		}

		unreachableAndIgnitedBuildings = new ArrayList<Building>();
		for (Building building : modelBuildings)
			if (building.stFire > 0 && !buildingReachability(building)
					&& building.lastTimeSeen + 10 >= time) {
				for (Area area : building.nearAreas50000) {
					if (area instanceof Road && area.worldGraphArea.isReachable) {
						unreachableAndIgnitedBuildings.add(building);
						break;
					}
				}
			}

		for (Building isCheckingbuilding : reachableAndIgnitedBuildings) {
			if (!isCheckingbuilding.haveZone) {
				zone = new FireZone(modelZones.size(), me().getID().getValue());
				checkingNeighboursForZoning(isCheckingbuilding);
				modelZones.add(zone);
			}
		}

		for (Building isCheckingBuilding : unreachableAndIgnitedBuildings) {
			if (!isCheckingBuilding.haveZone) {
				zone = new FireZone(modelZones.size(), me().getID().getValue());
				zone.emergency = true;
				checkingNeighboursForZoning(isCheckingBuilding);
				modelZones.add(zone);
			}
		}
	}

	private void checkingNeighboursForZoning(Building isChecking) {
		isChecking.zoneNumber = zone.zoneNum;
		zone.buildings.add(isChecking);
		if (isChecking.stFire > 0)
			zone.ignitedBuilds.add(isChecking);
		isChecking.haveZone = true;
		for (Building neighbour : isChecking.nearBuildings)
			if (((neighbour.isFierynessDefined() && (neighbour.getFieryness() > 0)) || neighbour.stFire > 0)
					&& !neighbour.haveZone) {
				checkingNeighboursForZoning(neighbour);
			}
	}

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}
}
