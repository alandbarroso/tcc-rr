package agent;

import geometry.Degree;
import geometry.Line;
import geometry.Point;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import worldGraph.Enterance;

public class FireStationAgent extends Agent<FireStation> {
	@Override
	protected void postConnect() {
		// TODO Auto-generated method stub
		super.postConnect();
//		visionRoadsAndBuildings();
	}

	public ArrayList<Point> getVisionPoints(Area area, Building building,
			int minMax) {
		return getVisionPoints(model, area, building, maxDistanceExtingiush,
				minMax);
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
						&& !hasIntersect)
					visionPoints.add(buildingToRoad.getSecondPoint());
			}
		}

		return visionPoints;
	}

	public ArrayList<FireZone> modelZones = null;
	private FireZone zone = null;

	public void zoning() {
		modelZones = new ArrayList<FireZone>();
		for (Building isCheckingbuilding : modelBuildings) {
			isCheckingbuilding.zoneNumber = -1;
			isCheckingbuilding.haveZone = false;
		}
		for (Building isCheckingbuilding : ignitedBuildings) {
			if (!isCheckingbuilding.haveZone) {
				zone = new FireZone(modelZones.size(), me().getID().getValue());
				checkingNeighboursForZoning(isCheckingbuilding);
				modelZones.add(zone);
			}
		}
	}

	private void checkingNeighboursForZoning(Building isChecking) {
		isChecking.zoneNumber = zone.zoneNum;
		zone.buildings.add(isChecking);
		if (isChecking.isOnFire())
			zone.ignitedBuilds.add(isChecking);
		isChecking.haveZone = true;
		for (Building neighbour : isChecking.nearBuildings)
			if (((neighbour.isFierynessDefined() && neighbour.getFieryness() != 0) || neighbour
					.isOnFire()) && !neighbour.haveZone)
				checkingNeighboursForZoning(neighbour);
	}

	private ArrayList<Building> ignitedBuildings = new ArrayList<Building>();

	private void initialBuildings() {
		ignitedBuildings.clear();
		for (Building build : modelBuildings) {
			if (build.isOnFire()) {
				ignitedBuildings.add(build);
			}
		}
	}

	private void setZoneSurface() {
		for (FireZone fireZone : modelZones) {
			for (Building building : fireZone.ignitedBuilds)
				fireZone.valueForWorkwerNum += building.getGroundArea()
						/ averageOfSurfaces;
		}
	}

	private void setWorkersNum() { // TODO : dorost shavad!!!
		for (FireZone fireZone : modelZones) {
			// int f=0;
			if (fireZone.valueForWorkwerNum > 17)
				fireZone.workersNum = 15;
			if (fireZone.valueForWorkwerNum <= 17)
				fireZone.workersNum = 9;
			if (fireZone.valueForWorkwerNum <= 12)
				fireZone.workersNum = 6;
			if (fireZone.valueForWorkwerNum <= 7)
				fireZone.workersNum = 3;
			if (map == MapType.Berlin || map == MapType.Istanbul
					|| map == MapType.Paris)
				fireZone.workersNum *= 2;
			// if(w!=fireZone.lastworkersnum)
			// fireZone.workersNum=w;
		}
	}

	private int valueForFireZone(double value) {
		int[] arr = { 3, 6, 10, 15, 25 };
		for (int i = 0; i < arr.length; i++)
			if (value <= arr[i])
				return i;

		return arr.length;
	}

	public Comparator<FireZone> zoneComparator = new Comparator<FireZone>() {
		public int compare(FireZone a, FireZone b) {
			if (a.value > b.value)
				return 1;
			if (a.value < b.value)
				return -1;
			return 0;
		}
	};

	public boolean bigMap = true;

	private int distToFire(int v) {
		int[] arr = { 10, 20, 30, 40, 50, 60 };
		for (int i = 0; i < arr.length; i++)
			if (v <= arr[i])
				return i;

		return arr.length;
	}

	public static final double w1 = 100, w2 = 5, w3 = -2.5, w4 = -5;

	private void setValueForBuildings(Building build, FireBrigade fb) {
		build.value = w1
				* (build.getGroundArea() * build.getFloors() / maxVolumeOfAllBuildings)
				+ w2
				* wg.getMinPath(fb.getPosition(), build.getID(), true).size()
				+ w4 * build.volume;
	}

	private int valueForNotIgnited(int v) {
		int[] arr = { 0, 1, 5, 10, 15, 20, 30, 40, 70 };
		for (int i = 0; i < arr.length; i++)
			if (v <= arr[i])
				return i;

		return arr.length;
	}

	protected void setZoneValue() {
		for (FireZone fireZone : modelZones) {
			fireZone.value = 0;
			for (Building building : fireZone.buildings)
				fireZone.value += building.getGroundArea() / averageOfSurfaces;
			fireZone.value = valueForFireZone(fireZone.value);
			if (fireZone.ignitedBuilds.size() < 10)
				fireZone.isInhibitor = true;
		}
	}

	private void setAim() {
		for (FireBrigade fb : modelFireBrigades) {
			if (isOk(fb)) {
				double minValue = Double.MAX_VALUE;
				Building minBuil = null;
				if (fb.myZone != null) {
					if (fb.aim != null && fb.aim.isOnFire()) {
						log("aim ghabli   :" + fb.aim.getID().getValue()
								+ "  :  " + fb.getID().getValue());
						continue;
					}
					if (fb.aim == null || !fb.aim.isOnFire()) {
						for (Building build : modelFireGroup.get(fb.fgNum).visionBuildings) {
							if (build.zoneNumber == fb.myZone.zoneNum) {
								setValueForBuildings(build, fb);
								if (build.value < minValue) {
									minValue = build.value;
									minBuil = build;
								}
							}
						}
						if (minValue < Double.MAX_VALUE) {
							fb.aim = minBuil;
							log("aim new   :" + fb.aim.getID().getValue()
									+ "  :  " + fb.getID().getValue());
						}
					}
				}
			}
		}
		for (GroupForFire fg : modelFireGroup)
			log("fg  :" + fg.fireAgents.size());
	}

	protected void checkAim() {
		log("in checkaim");
		for (FireBrigade fireBrigade : modelFireBrigades) {
			if (isOk(fireBrigade)) {
				if (fireBrigade.aim != null
						&& (fireBrigade.aim.getFieryness() == 8 || !fireBrigade.aim
								.isOnFire())) {
					fireBrigade.aim = null;
					log("fb.aim null khamushe:  " + 65243);
				}

				if (fireBrigade.aim != null
						&& (modelFireGroup.get(fireBrigade.fgNum).visionBuildings
								.size() != 0 && !modelFireGroup
								.get(fireBrigade.fgNum).visionBuildings
								.contains(fireBrigade.aim))) {
					log("aim nullide");
					fireBrigade.aim = null;
				}
				if (fireBrigade.aim == null && fireBrigade.myZone != null) {
					boolean isIn = false;
					for (Building build : fireBrigade.myZone.buildings) {
						// log("workers:  " + fireBrigade.myZone.workersNum);
						if (build.isOnFire()) {
							isIn = true;
							fireBrigade.myZone = modelZones
									.get(build.zoneNumber);
							break;
						}
					}
					if (!isIn) {
						log("fb.myzone null");
						fireBrigade.myZone = null;
					}
				}
			}
		}
	}

	protected void setVisonBuilding() {
		for (FireZone fireZone : modelZones) {
			for (Building build : fireZone.aroundBuildings) {
				for (Road road : build.getVisionRoads().keySet()) {
					if (road.fgNum != -1) {
						fireZone.fireGroups.add(modelFireGroup
								.get(road.fgNum));
						modelFireGroup.get(road.fgNum).visionBuildings
								.add(build);
					}
				}
			}
		}
	}

	protected void setmyZoneForFireBrigades() {
		for (FireBrigade fireBrigade : modelFireBrigades) {
			if (isOk(fireBrigade)) {
				if (fireBrigade.myZone != null) {
					if (fireBrigade.myZone.buildings.get(0).zoneNumber != -1) {
						fireBrigade.myZone = modelZones
								.get(fireBrigade.myZone.buildings.get(0).zoneNumber);
						log("myzone ghabli:	"
								+ fireBrigade.myZone.buildings.get(0) + "  :  "
								+ fireBrigade.getID().getValue());
						//break;
						continue;
					} else
						fireBrigade.myZone = null;
				}
				boolean eshterak = false;
				if (fireBrigade.myZone != null)
					for (Building b : modelFireGroup.get(fireBrigade.fgNum).visionBuildings) {
						if (fireBrigade.myZone.buildings.contains(b))
							eshterak = true;
					}
				if (!eshterak)
					fireBrigade.myZone = null;

			}
		}

		for (FireBrigade fireBrigade : modelFireBrigades) {
			if (fireBrigade.myZone != null) {
				if (fireBrigade.myZone.workersNum > 0)
					fireBrigade.myZone.workersNum -= 1;
				else
					fireBrigade.myZone = null;
			}
		}
		Collections.sort(modelZones, zoneComparator);
		for (FireZone fireZone : modelZones) {
			done: for (GroupForFire fireGroup : fireZone.fireGroups) {
				for (FireBrigade fireBrigade : fireGroup.fireAgents) {
					if ((fireBrigade.aim == null && fireBrigade.myZone == null)
							&& fireZone.workersNum > 0) {
						fireBrigade.myZone = fireZone;
						fireZone.workersNum -= 1;
						log("myzone jadid:	"
								+ fireBrigade.myZone.buildings.get(0) + "  :  "
								+ fireBrigade.getID().getValue());
					}
					if (fireZone.workersNum <= 0)
						break done;
				}
			}
		}
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

	private boolean isOk(FireBrigade fireB) {
		if (fireB.hasBuriedness || fireB.Water == 0)
			return false;
		else
			return true;
	}

	private GroupForFire bfsForSetFireGroups(FireBrigade fireB) {
		// log("in bfs");
		GroupForFire fireGroup = new GroupForFire();
		fireGroup.num = modelFireGroup.size();

		Area areaOfFireB = ((Area) model.getEntity(fireB.getPosition()));
		fireGroup.fireAgents.addAll(areaOfFireB.worldGraphArea.fB);

		if (areaOfFireB instanceof Building) {
			fireGroup.buildings.add((Building) areaOfFireB);
		} else {
			fireGroup.roads.add((Road) areaOfFireB);
		}

		HashSet<Enterance> layer = new HashSet<Enterance>();
		layer.addAll(areaOfFireB.worldGraphArea.enterances);

		while (layer.size() > 0) {
			HashSet<Enterance> newLayer = new HashSet<Enterance>();
			for (Enterance enterance : layer) {
				if (enterance.mark)
					continue;
				enterance.mark = true;
				Enterance neighbourE = enterance.neighbour;
				if (neighbourE.isItConnectedToNeighbour) {
					Area areaOfNeighbourE = neighbourE.area.modelArea;
					if (areaOfNeighbourE instanceof Building) {
						fireGroup.buildings.add((Building) areaOfNeighbourE);
					}
					if (areaOfNeighbourE instanceof Road) {
						fireGroup.roads.add((Road) areaOfNeighbourE);
					}
					fireGroup.fireAgents.addAll(neighbourE.area.fB);
					for (Enterance internal : neighbourE.internalEnterances) {
						if (!internal.mark)
							newLayer.add(internal);
					}
					enterance.neighbour.mark = true;
				}
			}
			layer = newLayer;
		}
		for (FireBrigade f : fireGroup.fireAgents)
			f.fgNum = fireGroup.num;
		for (Building build : fireGroup.buildings)
			build.fgNum = fireGroup.num;
		for (Road road : fireGroup.roads)
			road.fgNum = fireGroup.num;
		return fireGroup;
	}

	ArrayList<GroupForFire> modelFireGroup = new ArrayList<GroupForFire>();

	private void setFireGroup() {
		wg.clearAreas();

		for (FireBrigade fb : modelFireBrigades)
			fb.fgNum = -1;
		for (Area a : modelAreas)
			a.fgNum = -1;
		modelFireGroup = new ArrayList<GroupForFire>();

		for (Area ar : modelAreas)
			ar.worldGraphArea.fB.clear();
		for (FireBrigade fire : modelFireBrigades) {
			if (!isOk(fire))
				continue;
			worldGraph.Area area = (((Area) model.getEntityByInt(fire
					.getPosition().getValue())).worldGraphArea);
			area.fB.add(fire);
		}

		for (FireBrigade fire : modelFireBrigades)
			if (fire.fgNum == -1 && isOk(fire)) {
				modelFireGroup.add(bfsForSetFireGroups(fire));
			}

	}

	// protected void decide2() throws ActionCommandException {
	// initialBuildings();
	// // log("ignited:   " + IgnitedBuildings.size());
	// zoning();
	// setFireGroup();
	// for (FireZone zone : modelZones) {
	// zone.setAroundBuilding();
	// }
	// String s = "";
	// for (FireZone zone : modelZones) {
	// for (Building build : zone.buildings) {
	// s += build.getID() + " ";
	// }
	// s += "; ";
	// }
	// log("zones:   " + s);
	// setZoneCenter();
	// setZoneSurface();
	// setWorkersNum();
	// for (FireZone z : modelZones) {
	// log(z.buildings.get(0) + "  workersNum:  " + z.workersNum);
	// }
	// setZoneValue();
	// checkAim();
	// //selectMyZone();
	// //for (FireBrigade fb : modelFireBrigades)
	// //selectAim(fb);
	//
	// }

	protected void setAroundForFireZone() {
		for (FireZone fz : modelZones)
			fz.setAroundBuildings(bigMap);
	}

	protected void decide() throws ActionCommandException {
//		if (time > 10) {
//			if (modelFireStations.indexOf(me()) == 0) {
//				initialBuildings();
//				zoning();
//				setAroundForFireZone();
//				setFireGroup();
//				if (time == 120) {
//					String d = "";
//					for (GroupForFire g : modelFireGroup) {
//						for (Road r : g.roads)
//							d += r.getID().getValue() + " ";
//						for (Building b : g.buildings)
//							d += b.getID().getValue() + " ";
//						d += " ; ";
//					}
//					log("modelfiregroup: " + d);
//				}
//				setVisonBuilding();
//
//				String s = "";
//				for (FireZone zone : modelZones) {
//					for (Building build : zone.buildings) {
//						s += build.getID() + " ";
//					}
//					s += "; ";
//				}
//				log("zones:   " + s);
//				setZoneCenter();
//				setZoneSurface();
//				setWorkersNum();
//
//				for (FireZone z : modelZones) {
//					log(z.buildings.get(0) + "  workersNum:  " + z.workersNum);
//				}
//				setZoneValue();
//				checkAim();
//				setmyZoneForFireBrigades();
//				for(FireBrigade fb :modelFireBrigades)
//					log("myzone       "+fb.myZone);
//				setAim();
//				sendFireAL();
//				if (map == MapType.Kobe || map == MapType.VC)
//					bigMap = false;
//			}
//			
//		}
	}

	protected void sendFireAL() {
		if (time > 2) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_FIRECENTER, 0,
					Radar.headerSize);
			setmessage(bits);
			byte[] byteArray = Radar.toByteArray(bits);
			String w = "";
			for (int i = 0; i < bits.size(); i++) {
				if (bits.get(i))
					w += "1";
				else
					w += "0";
			}
			// log(w);
			sendSpeak(time, behtarinChannel1, byteArray);
		}
	}

	protected void setmessage(BitSet bits) {
		int start = radar.headerSize;
		for (FireBrigade fire : modelFireBrigades) {
			if (fire.aim == null) {
				start = changeToMabnaye2(bits, 0, start);
				log("aim before send in setmessage: " + null);
			} else {
				log("aim before send in setmessage: "
						+ fire.aim.getID().getValue()
						+ " kasi ke khahad shenid: " + fire.getID().getValue());
				start = changeToMabnaye2(bits,
						EntityBeInt.get(fire.aim.getID()), start);
				int lasttimeseen = time - fire.aim.lastTimeSeen;
				if (lasttimeseen > 32)
					lasttimeseen = 32;
				start = radar.calcWithBitSet(bits, lasttimeseen, start, 5);
			}

		}

	}

	protected int changeToMabnaye2(BitSet bits, int num, int start) {
		ArrayList<Integer> baghiMandeHa = new ArrayList<Integer>();
		ArrayList<Integer> barMabnaye2 = new ArrayList<Integer>();
		int maghsom = num;
		int messagesize = EntityBeInt.size();
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


	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_STATION);
	}
}
