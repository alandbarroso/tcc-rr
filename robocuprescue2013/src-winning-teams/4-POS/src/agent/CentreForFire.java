package agent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import worldGraph.Enterance;
import agent.Agent.MapType;

public class CentreForFire {
	public Agent<? extends StandardEntity> agent = null;
	public static final int MAX_DISTANCE_VIEW = 30000;
	protected StandardWorldModel model = null;

	public CentreForFire(Agent<? extends StandardEntity> age) {
		agent = age;
		model = agent.getModel();

		if (!agent.isVisionsCalculated) {
			agent.visionRoadsAndBuildings();
			agent.isVisionsCalculated = true;
		}
	}

	protected void log(String msg) {
		agent.logger.log(msg);
	}

	public ArrayList<FireZone> modelZones = null;
	private FireZone zone = null;

	public void zoning() {
		modelZones = new ArrayList<FireZone>();
		for (Building isCheckingbuilding : agent.modelBuildings) {
			isCheckingbuilding.zoneNumber = -1;
			isCheckingbuilding.haveZone = false;
		}

		for (Building isCheckingbuilding : ignitedBuildings) {
			if (!isCheckingbuilding.haveZone) {
				zone = new FireZone(modelZones.size(), agent.getID().getValue());
				checkingNeighboursForZoning(isCheckingbuilding);
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

		for (Building neighbour : isChecking.nearBuildings) {
			if (((neighbour.isFierynessDefined() && neighbour.getFieryness() != 0) || neighbour.stFire > 0)
					&& !neighbour.haveZone) {
				checkingNeighboursForZoning(neighbour);
				continue;
			}
		}
	}

	private ArrayList<Building> ignitedBuildings = new ArrayList<Building>();

	private void initialBuildings() {
		ignitedBuildings.clear();
		for (Building build : agent.modelBuildings) {
			if (build.stFire > 0) {
				ignitedBuildings.add(build);
			}
		}
	}

	private void setZoneSurface() {
		for (FireZone fireZone : modelZones)
			for (Building building : fireZone.ignitedBuilds)
				fireZone.valueForWorkwerNum += building.getGroundArea()
						/ agent.averageOfSurfaces;
	}

	private void setWorkersNum() {
		for (FireZone fireZone : modelZones) {
			boolean b = true;
			for (Building bu : fireZone.buildings)
				if (bu.stFire > 0 && !bu.isSimulatedForFire) {
					b = false;
					break;
				}

			if (b) {
				fireZone.workersNum = 1;
			} else {
				if (fireZone.valueForWorkwerNum > 17)
					fireZone.workersNum = 30;
				if (fireZone.valueForWorkwerNum <= 17)
					fireZone.workersNum = 10;
				if (fireZone.valueForWorkwerNum <= 7)
					fireZone.workersNum = 5;
			}
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

	private void setSimulatedValue(Building build) {
		int areaForAround = 30000;
		if (bigMap)
			areaForAround = 60000;

		build.simulatedValue = 0;
		for (Building b : agent.modelBuildings)
			if (b.stFire == 0
					&& Math.hypot(build.getX() - b.getX(),
							build.getY() - b.getY()) < areaForAround)
				build.simulatedValue++;
	}

	protected void setZoneValue() {
		for (FireZone fireZone : modelZones) {
			fireZone.value = valueForFireZone(fireZone.valueForWorkwerNum);
			if (fireZone.ignitedBuilds.size() < 10)
				fireZone.isInhibitor = true;
		}
	}

	private void setAim() {
		if (modelZones.size() == 0)
			return;

		for (FireZone fz : modelZones)
			for (Building b : fz.aroundBuildings)
				setSimulatedValue(b);

		HashSet<FireBrigade> fireAgents = new HashSet<FireBrigade>();
		for (FireBrigade fb : agent.modelFireBrigades)
			if (isOk(fb) && fb.myZone != null && fb.aim == null) {
				fireAgents.add(fb);
			}
		for (FireBrigade fb : fireAgents) {
			for (Building b : fb.myZone.aroundBuildings)
				b.toBeAssigned = 3;
		}

		while (true) {
			double minDis = Double.MAX_VALUE;
			Building minBuil = null;
			FireBrigade fireb = null;
			for (FireBrigade fb : fireAgents)
				for (Building b : fb.myZone.aroundBuildings)
					if (b.assigned < b.toBeAssigned) {
						double i = Math.hypot(fb.getX() - b.getX(), fb.getY()
								- b.getY());
						if (i < minDis) {
							minDis = i;
							minBuil = b;
							fireb = fb;
						}
					}

			if (minBuil == null) {
				if (fireAgents.size() > 0) {
					for (FireBrigade fb : fireAgents)
						for (Building b : fb.myZone.aroundBuildings)
							b.toBeAssigned = 100;
					continue;
				}
				break;
			}

			fireb.aim = minBuil;
			log("fireB : " + fireb.getID().getValue() + " fireB.aim : "
					+ fireb.aim.getID().getValue());
			minBuil.assigned++;
			fireAgents.remove(fireb);
		}
	}

	protected void checkAim() {
		for (FireBrigade fireBrigade : agent.modelFireBrigades) {
			if (fireBrigade.aim == null)
				continue;

			if (isOk(fireBrigade)) {
				if ((fireBrigade.aim.isFierynessDefined() && fireBrigade.aim
						.getFieryness() == 8) || fireBrigade.aim.stFire == 0)
					setAimNull(fireBrigade);
				else if (!modelFireGroup.get(fireBrigade.fgNum).visionBuildings
						.contains(fireBrigade.aim))
					setAimNull(fireBrigade);
				else if (!modelZones.get(fireBrigade.aim.zoneNumber).aroundBuildings
						.contains(fireBrigade.aim)
						&& !modelZones.get(fireBrigade.aim.zoneNumber).aroundBuildingsWithoutSim
								.contains(fireBrigade.aim))
					setAimNull(fireBrigade);
			} else
				setAimNull(fireBrigade);
		}
	}

	public void setAimNull(FireBrigade fb) {
		if (fb.aim != null) {
			fb.aim.assigned--;
			if (fb.aim.assigned < 0)
				System.err.println("BUG! " +  agent.time + " " + fb + " " + fb.aim);
		}

		fb.aim = null;
	}

	public void printBuildings(ArrayList<Building> ab) {
		String s = "";
		for (Building b : ab)
			s += b.getID().getValue() + " ";
		log("Buildings" + s);
	}

	// /XXX in code bug dare khoda...!!! aslan az in copy che estefadeE mish ?
	// /XXX chera roye around buildinga chera buildinga na?
	protected void setVisionBuilding() {
		for (FireZone fireZone : modelZones) {
			for (Building build : fireZone.aroundBuildings) {
				for (Road road : build.getVisionRoads().keySet())
					if (road.fgNum != -1) {
						fireZone.fireGroups.add(modelFireGroup.get(road.fgNum));
						// ArrayList<Building> copy = new ArrayList<Building>();
						// copy.addAll(modelFireGroup.get(road.fgNum).buildings);
						modelFireGroup.get(road.fgNum).visionBuildings
								.add(build);
					}

				for (Building b : build.visionBuildings.keySet()) {
					if (b.fgNum != -1) {
						fireZone.fireGroups.add(modelFireGroup.get(b.fgNum));
						modelFireGroup.get(b.fgNum).visionBuildings.add(build);
					}
				}
			}
			// for (Building build : fireZone.aroundBuildingsWithoutSim)
			// for (Road road : build.getVisionRoads().keySet())
			// if (road.fgNum != -1) {
			// fireZone.selectedFireGroup.add(modelFireGroup
			// .get(road.fgNum));
			// modelFireGroup.get(road.fgNum).visionBuildings
			// .add(build);
			// }
		}
	}

	public boolean isIncommon(ArrayList<Building> b1, HashSet<Building> b2) {
		for (Building b : b1)
			if (b2.contains(b))
				return true;
		return false;
	}

	public void updateFireBrigadesZones() {
		log("updateZone ");
		for (FireBrigade fireBrigade : agent.modelFireBrigades) {
			if (isOk(fireBrigade)
					&& fireBrigade.myZone != null
					&& isIncommon(
							fireBrigade.myZone.buildings,
							modelFireGroup.get(fireBrigade.fgNum).visionBuildings)
					&& fireBrigade.myZone.buildings.get(0).zoneNumber != -1) {
				fireBrigade.myZone = modelZones
						.get(fireBrigade.myZone.buildings.get(0).zoneNumber);
				// log("zonesh hamoon ghablias" +
				// fireBrigade.getID().getValue());
				// String mz = "";
				// for (Building b : fireBrigade.myZone.buildings)
				// mz += b.getID().getValue() + " ";
				// log("zonesh " + mz);

			} else {
				fireBrigade.myZone = null;
				// log("zonesh null shod");
			}
		}
	}

	protected void setZoneForFireBrigades() {
		ArrayList<FireZone> hibitors = new ArrayList<FireZone>();
		ArrayList<FireZone> nothibitor = new ArrayList<FireZone>();
		for (FireZone fz : modelZones)
			if (fz.isInhibitor)
				hibitors.add(fz);
			else
				nothibitor.add(fz);

		setFGNumsForFireZones();
		setFireBrigadeForzone(hibitors, true);
		setFireBrigadeForzone(nothibitor, true);
		setFireBrigadeForzone(nothibitor, false);
		setFireBrigadeForzone(hibitors, false);
	}

	private void setFGNumsForFireZones() {
		for (FireZone fz : modelZones) {
			HashSet<GroupForFire> fgOfZone = new HashSet<GroupForFire>();
			for (Building b : fz.buildings)
				if (b.fgNum > -1)
					fgOfZone.add(modelFireGroup.get(b.fgNum));

			for (GroupForFire gf : fgOfZone)
				fz.fireAgents.addAll(gf.fireAgents);
		}
	}

	private void setFireBrigadeForzone(ArrayList<FireZone> zones,
			boolean checkWorkersNum) {
		boolean finished = false;

		while (!finished) {
			finished = true;

			double mindis = Double.MAX_VALUE;
			FireBrigade f = null;
			FireZone minFireZone = null;
			for (FireZone fz : zones)
				if (fz.workersNum > 0 || !checkWorkersNum)
					for (FireBrigade fb : fz.fireAgents) {
						// for (FireBrigade fb : agent.modelFireBrigades) {
						if (isOk(fb) && fb.myZone == null) {
							double dist = Math.hypot(fz.centerX - fb.getX(),
									fz.centerY - fb.getY());
							if (mindis > dist) {
								mindis = dist;
								f = fb;
								minFireZone = fz;
							}
						}
					}
			if (f != null) {
				f.myZone = minFireZone;
				minFireZone.workersNum--;
				finished = false;
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
		return (!fireB.hasBuriedness && fireB.Water > 0);
	}

	private GroupForFire bfsForSetFireGroups(FireBrigade fireB) {
		GroupForFire fireGroup = new GroupForFire();
		fireGroup.num = modelFireGroup.size();

		Area areaOfFireB = ((Area) model.getEntity(fireB.getPosition()));
		fireGroup.fireAgents.addAll(areaOfFireB.worldGraphArea.fB);

		if (areaOfFireB instanceof Building)
			fireGroup.buildings.add((Building) areaOfFireB);
		else
			fireGroup.roads.add((Road) areaOfFireB);

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
					if (areaOfNeighbourE instanceof Building)
						fireGroup.buildings.add((Building) areaOfNeighbourE);
					if (areaOfNeighbourE instanceof Road)
						fireGroup.roads.add((Road) areaOfNeighbourE);

					fireGroup.fireAgents.addAll(neighbourE.area.fB);
					for (Enterance internal : neighbourE.internalEnterances)
						if (!internal.mark)
							newLayer.add(internal);

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
		agent.wg.clearAreas();

		for (FireBrigade fb : agent.modelFireBrigades)
			fb.fgNum = -1;

		for (Area a : agent.modelAreas) {
			a.fgNum = -1;
			a.worldGraphArea.fB.clear();
		}

		modelFireGroup = new ArrayList<GroupForFire>();
		for (FireBrigade fire : agent.modelFireBrigades) {
			if (!isOk(fire))
				continue;
			worldGraph.Area area = (((Area) model.getEntityByInt(fire
					.getPosition().getValue())).worldGraphArea);
			area.fB.add(fire);
		}

		for (FireBrigade fire : agent.modelFireBrigades) {
			if (fire.fgNum == -1 && isOk(fire)) {
				modelFireGroup.add(bfsForSetFireGroups(fire));
			}
		}
	}

	protected void setAroundForFireZone() {
		for (FireZone fz : modelZones) {
			fz.setAroundBuildings(bigMap);
//			fz.setAroundBuildingWithoutSim();
		}
	}

	private ArrayList<FireZone> zoneChanging = null;

	public void printZones() {
		// String z = "";
		// for (FireZone f : modelZones) {
		// for (Building b : f.buildings) {
		// z += b.getID().getValue() + " ";
		// }
		// z += "; ";
		// }
		// log("printZone: " + z);
		// z = "";
		// for (FireZone f : modelZones) {
		// for (Building b : f.aroundBuildings) {
		// z += b.getID().getValue() + " ";
		// }
		// z += "; ";
		// }
		// log("printAroundZone: " + z);

	}

	public void printFireGroups() {
		String fg = "";
		for (GroupForFire gf : modelFireGroup) {
			for (Building bu : gf.buildings)
				fg += bu.getID().getValue() + " ";
			for (Road ro : gf.roads)
				fg += ro.getID().getValue() + " ";
			fg += "; ";
		}
		log("fireGroups : " + fg);
		fg = "";
		for (GroupForFire gf : modelFireGroup) {
			for (Building bu : gf.visionBuildings)
				fg += bu.getID().getValue() + " ";
			if (gf.visionBuildings.size() != 0)
				fg += " ; ";
		}
		log("fireGroups.V : " + fg);
	}

	String sim = "";

	private void simulate() {
		log("inSimulate");

		for (Building b : agent.modelBuildings) {
			b.haveOwner = false;
			if (b.stFire > 0 && agent.time - b.timeOfStartOfFire > 10
			 && !b.isSimulatedForFire) {
//				sim += b.getID().getValue() + " ; ";
				for (Building bu : b.nearBuildings) {
					if ((bu.lastTimeSeen < 0 || agent.time - bu.lastTimeSeen > 20 * (bu.counterForSimulate + 1))
							&& !bu.isNotIgnitable()
							&& !bu.isSimulatedForFire
							&& bu.stFire == 0
							&& (!bu.isFierynessDefined() || bu.getFieryness() != 8)) {
						bu.stFire = 1;
						bu.lastTimeSeen = agent.time;
						bu.isSimulatedForFire = true;
						// bu.isSimulsted = true;
						b.isSimulatedForFire = true;
						bu.counterForSimulate++;
						sim += bu.getID().getValue() + " ";
//						log("be ezaye : " + b + " simulate shode  :" + bu
//								+ "count : " + bu.counterForSimulate
//								+ " simulatedIsOnfire :"
//								+ bu.isSimulatedForFire);
					}
				}
			}
		}
	}

	public void printFireBrigadesOFZone() {
		for (FireZone fz : modelZones) {
			String z = "";
			String a = "";
			for (Building b : fz.buildings)
				z += b.getID().getValue() + " ";
			for (FireBrigade f : fz.fireAgents)
				a += f.getID().getValue() + " ";

			log("zone :" + z + "\n"
					+ "              fireBrigadehaE ke rah daran : " + a);
		}
	}

	public void conflictBetweenFireBrigadeAndZone() {
		for (FireBrigade f : agent.modelFireBrigades) {
			for (FireZone z : f.zonesIcanExt) {
				if (!z.fireAgents.contains(f))
					log("vay   vay ");
			}

		}
	}

	public void printZoneForFireBrigade() {
		log("aval");
		int counter = 0;
		for (FireBrigade f : agent.modelFireBrigades) {
			if (f.fgNum != -1) {
				for (Building b : modelFireGroup.get(f.fgNum).buildings) {
					if (b.zoneNumber != -1)
						f.zonesIcanExt.add(modelZones.get(b.zoneNumber));
				}
//				String m = "";
//				for (FireZone z : f.zonesIcanExt) {
//					for (Building b : z.buildings) {
//						m += b.getID().getValue() + " ";
//						counter++;
//					}
//					m += "; ";
//
//				}
				// log("fireBrigade : " + f.getID().getValue() + " zones:   " +
				// m);
			}
		}
		log("caunt : " + counter);
	}

	protected void decide() {
		log("Center for Fire");
		// XXX toye setZone on moshkele rah nadastan be hameye Building haie
		// zone
		// XXX chera ma nemigim onaE ke cv daran ro zood tar khamoosh kone?
		// XXX to khone haE ke amb hast alaki ab narizan :WWW
		if (agent.time > 10) {
			for (FireBrigade f : agent.modelFireBrigades) {
				if (f.Water < 1)
					log("ab in kamtar az yeke!!  " + f.getID().getValue()
							+ "   " + f.Water);
			}
			if (agent.map == MapType.Kobe || agent.map == MapType.VC)
				bigMap = false;

			if (bigMap && !agent.lowCommi) {
				simulate();
			}

			initialBuildings();

			// Zone...
			zoneChanging = modelZones;
			zoning();
			setAroundForFireZone();
			zoneMatching();
			setZoneCenter();
			setZoneSurface();
			setWorkersNum();
			setZoneValue();
			// Finish
			printZones();
			// FireGroup...
			setFireGroup();
			setVisionBuilding();
			// Task Assigon
			// Set zone
			if (zoneChange())
				for (FireBrigade f : agent.modelFireBrigades)
					f.myZone = null;
			else
				updateFireBrigadesZones();

			for (FireBrigade f : agent.modelFireBrigades)
				if (f.myZone == null)
					setAimNull(f);
			setZoneForFireBrigades();
			// Set Aim
			checkAim();

			setAim();

			// Send data
			sendFireAL();
		}
	}

	public void zoneMatched(FireZone asli, FireZone farE) {
		for (Building build : farE.buildings)
			build.zoneNumber = asli.zoneNum;
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
							if (e instanceof Building) {
								Building b = (Building) e;
								if (b.stFire > 0 && b.haveZone
										&& b.zoneNumber != building.zoneNumber) {
									toBeRemoved = modelZones.get(b.zoneNumber);
									zoneMatched(fireZone, toBeRemoved);
									topZone = fireZone;
									break done;
								}
							}

				if (toBeRemoved == null)
					break;

				topZone.setAroundBuildings(1, topZone.ignitedBuilds);
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

	public boolean zoneChange() {
		// if (zoneChanging.size() == 0 || modelZones == null)
		if (zoneChanging == null || zoneChanging.size() == 0
				|| modelZones == null)
			return true;

		if (zoneChanging.size() != modelZones.size())
			return true;

		return false;
	}

	protected void sendFireAL() {
		if (agent.time > 2) {
			BitSet bits = new BitSet();
			Radar.calcWithBitSet(bits, Radar.HEADER_FIRECENTER, 0,
					Radar.headerSize);
			setmessage(bits);
			byte[] byteArray = Radar.toByteArray(bits);
			agent.sendSpeak(agent.time, agent.behtarinChannel1, byteArray);
			if(agent.behtarinChannel2 != 0)
				agent.sendSpeak(agent.time, agent.behtarinChannel2, byteArray);
		}
	}

	protected void setmessage(BitSet bits) {
		int start = Radar.headerSize;
		for (FireBrigade fire : agent.modelFireBrigades) {
			if (fire.aim == null) {
				start = changeToMabnaye2(bits, 0, start);
			} else {
				start = changeToMabnaye2(bits,
						agent.EntityBeInt.get(fire.aim.getID()), start);

				int lasttimeseen = agent.time - fire.aim.lastTimeSeen;
				if (lasttimeseen >= 32)
					lasttimeseen = 31;
				start = Radar.calcWithBitSet(bits, lasttimeseen, start, 5);
			}
		}
	}

	protected int changeToMabnaye2(BitSet bits, int num, int start) {
		ArrayList<Integer> baghiMandeHa = new ArrayList<Integer>();
		ArrayList<Integer> barMabnaye2 = new ArrayList<Integer>();
		int maghsom = num;
		int messagesize = agent.EntityBeInt.size();
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

}
