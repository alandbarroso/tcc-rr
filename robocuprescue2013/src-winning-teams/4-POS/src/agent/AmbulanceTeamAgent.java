package agent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Random;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import worldGraph.Enterance;

public class AmbulanceTeamAgent extends Agent<AmbulanceTeam> {
	protected ArrayList<Human> nowTarAgent = new ArrayList<Human>();
	protected ArrayList<Human> nowTarCv = new ArrayList<Human>();
	public ArrayList<Human> done = new ArrayList<Human>();
	public ArrayList<Human> humans = new ArrayList<Human>();
	protected ArrayList<Building> endedBuildings = new ArrayList<Building>();

	protected EntityID target = null;
	protected EntityID aimPosition = null;
	protected EntityID lastPosition = null;
	protected EntityID myTargetPosition = null;
	protected EntityID lastHeard = null;
	public LittleZone myLZone = null;
	public LittleZone littlezones[];
	public LittleZone nearestlz = new LittleZone();
	private Civilian civilianOnBoard = null;
	public Human myTarget = null;

	private int targetCounter = 0;
	private int aimIndex = -1;
	private int myIndex = -1;
	private int cvSayingDist = 30000;
	protected boolean isChert = false;
	private boolean isHeard = false;
	private boolean shouldListen = true;
	private Building tar = null;

	public boolean goodEnough(Area area, int index) {
		return ((area instanceof Building || area instanceof Road)
				&& !(area instanceof Refuge) && isReachable(area.getID())
				&& !isOnFire(area) && !area.isEmpty[index]);
	}

	// TODO: sakhtari ke feedbacks ro chandbar say kone

	public boolean goodEnough(Human h) {
		return goodEnough(h, true);
	}

	public boolean goodEnough(Human h, boolean shouldCheckIsEmpty) {
		// TODO: irancello ina!!!
		if (!noCommi && !lowCommi)
			h.irancell = modelAmbulanceTeams.size();
		int saverNum = h.irancell;
		if (time > 200 && modelAmbulanceTeams.size() > 2)
			saverNum = modelAmbulanceTeams.size() - 2;

		if ((h.hasBuriedness || h instanceof Civilian)
				&& h.isPositionDefined()
				&& isReachable(h.getPosition())
				&& !isOnFire(model.getEntity(h.getPosition()))
				&& h.deadtime > time
				&& (model.getEntity(h.getPosition()) instanceof Building || (model
						.getEntity(h.getPosition()) instanceof Road
						&& h.isDamageDefined() && h.getDamage() != 0))
				&& (!shouldCheckIsEmpty || !((Area) h.getPosition(model)).isEmpty[h
						.getID().getValue() % cvRecognizer]) && !isInRefuge(h)
				&& !(done.contains(h))) {
			h.ge = true;
			if (h.deadtime > time + (h.getBuriedness() / (saverNum))) {
				h.ge = false;
				return true;
			}
		}
		return false;
	}

	public boolean isInRefuge(Human h) {
		if (model.getEntityByInt(h.getPosition().getValue()) instanceof Refuge)
			return true;
		return false;
	}

	public boolean isOnFire(Entity building) {
		if (building instanceof Building
				&& ((Building) building).isFierynessDefined()
				&& ((Building) building).getFieryness() > 0)
			return true;
		return false;
	}

	private void setCivilianOnBoard() throws ActionCommandException {
		log("inCVonBoard");
		civilianOnBoard = null;
		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			Civilian civilian = (Civilian) next;
			if (civilian.isPositionDefined()
					&& civilian.getPosition().equals(getID())) {
				civilianOnBoard = civilian;
				if (civilian.getHP() != 0)
					Rescue(civilianOnBoard);
				else {
					if (civilian.deadCounter > 3) {
						me().feedback = true;
						isChert = true;
						unLoad();
					} else
						civilian.deadCounter++;
				}
				break;
			}
		}
	}

	protected void postConnect() {
		super.postConnect();
		random = new Random(me().getID().getValue());
		littleZoneMaking(10);
	}

	public void setEmpty(EntityID b) {
		log("set empty");
		Civilian c = null;
		int counter[] = new int[cvRecognizer];
		Human buildhumans[] = new Human[cvRecognizer];
		Area build = (Area) me().getPosition(model);
		for (int i = 0; i < cvRecognizer; i++)
			build.isEmpty[i] = true;

		for (StandardEntity ent : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			c = (Civilian) ent;
			if (c.isPositionDefined()
					&& c.getPosition(model).getID().getValue() == build.getID()
							.getValue() && goodEnough(c, false)) {
				int id = c.getID().getValue() % cvRecognizer;
				build.isEmpty[id] = false;
				counter[id]++;
				buildhumans[id] = c;
			}
		}

		for (Human h : modelAgents)
			if (h.isPositionDefined()
					&& h.getPosition().getValue() == build.getID().getValue()
					&& h.hasBuriedness && h.getBuriedness() > 0
					&& goodEnough(h, false)) {
				int id = h.getID().getValue() % cvRecognizer;
				build.isEmpty[id] = false;
				counter[id]++;
				buildhumans[id] = h;
			}

		for (int i = 0; i < counter.length; i++)
			if (counter[i] == 1) {
				ArrayList<AmbulanceTeam> y = new ArrayList<AmbulanceTeam>();
				for (AmbulanceTeam a : modelAmbulanceTeams) {
					if (a.isPositionDefined()
							&& a.getPosition().getValue() == build.getID()
									.getValue()
							&& me().assignIndex.get(a).equals(i))
						y.add(a);

				}
				int z = 0;
				if (y.contains(me()))
					z = y.indexOf(me()) + 1;
				else
					z = y.size() + 1;

				if (z != 1 && buildhumans[i].getBuriedness() < z)
					build.isEmpty[i] = true;
			}
	}

	public void Rescue(Human human) throws ActionCommandException {
		if (amIStucking) {
			log("RANDOM WALK");
			randomWalk();
		}
		log("start of rescue");
		if (human == null) {
			return;
		}

		if (myLZone != null
				&& myLZone.ownersOfSearchZone.contains(me().getID().getValue())
				&& myLZone.isRealLittleZone) {
			sendEmptyBuildings(myLZone.id, false);
			if (myLZone.owner == me().getID().getValue())
				myLZone.owner = -1;
			myLZone.ownersOfSearchZone
					.remove((Integer) me().getID().getValue());
			myLZone = null;
		}

		if (civilianOnBoard == null) {
			if (human.isPositionDefined()
					&& me().getPosition().getValue() != human.getPosition()
							.getValue()) {
				move(human.getPosition());
			}
			if (!isInChangeSet(human.getID())) {
				done.add(human);
				log("be done add shod" + human.getID().getValue()
						+ "  define budan e position"
						+ human.isPositionDefined());
				myTarget = null;
				log("dar changeset nabud!!!");
				if (human instanceof Civilian) {
					human.undefinePosition();
					sendingAUndifinededPositionCV((Civilian) human);
				}
				return;
			}
			if (human.getBuriedness() > 0) {
				if (!(human instanceof Civilian) && human.getBuriedness() == 1) {
					me().feedback = true;
					isChert = true;
				}

				rescue(human.getID());
			}

			if (!(done.contains(human)))
				done.add(human);

			if (!(human instanceof Civilian)) {
				myTarget = null;
				myTargetPosition = null;
				myIndex = -1;
				log("chon target agent e ");
				return;
			}

			int id = me().getID().getValue();
			if (noCommi || isInsetHuman || lowCommi) {
				for (Human h : modelAmbulanceTeams)
					if (h.isPositionDefined()
							&& h.getPosition().equals(human.getPosition())) {
						id = h.getID().getValue();
						break;
					}
			} else
				for (Human a : modelAmbulanceTeams) {
					log(me().assignIndex.get((AmbulanceTeam) a) + "");
					if (a.isPositionDefined()
							&& a.getPosition().equals(human.getPosition())
							&& me().assignIndex.get((AmbulanceTeam) a) == myIndex) {
						id = a.getID().getValue();
						log("id " + id);
						break;
					}
				}

			if (me().getID().getValue() == id) {
				log("man load mikonam");
				load(human.getID());
			} else {

				myTarget = null;
				myTargetPosition = null;
				lastPosition = me().getPosition();
				myIndex = -1;
				log("yeki dg  bayad loadesh kone!!" + id);
				me().feedback = true;
				isChert = true;
			}
		} else {
			EntityID refugeID = null;
			int minDis = Integer.MAX_VALUE;
			for (Refuge ref : reachableRefuges) {
				log("REFUGE " + ref);
				if (minDis > ref.worldGraphArea.distanceFromSelf) {
					minDis = ref.worldGraphArea.distanceFromSelf;
					refugeID = ref.getID();
				}
			}
			if (refugeID == null) {
				if (modelRefuges.size() > 0)
					newSearch();
				else if (reachableRoads.size() > 0)
					refugeID = reachableRoads.get(0).getID();
				else if (reachableBuildings.size() > 0) {
					if (reachableBuildings.contains(me().getPosition()))
						refugeID = me().getPosition();
				}
				if (refugeID == null) {
					log("refuge id==null e");
					// me().isFree = false;
					rest();
				}
			}
			if (me().getPosition().getValue() != refugeID.getValue()) {
				// me().isFree = false;
				move(refugeID);
			} else {
				myTarget = null;
				log("unload shod");
				me().feedback = true;
				isChert = true;
				unLoad();
			}
		}
	}

	public Comparator<Entity> newComparator = new Comparator<Entity>() {
		public int compare(Entity a1, Entity a2) {
			double m = 1.0;
			if (map == MapType.Kobe || map == MapType.VC)
				m = 2.0;
			if (lowCommi || noCommi)
				m = 0.25;
			Human hu1 = (Human) a1;
			Human hu2 = (Human) a2;
			double d1 = (((Area) model.getEntity(hu1.getPosition())).worldGraphArea.distanceFromSelf);
			double d2 = (((Area) model.getEntity(hu2.getPosition())).worldGraphArea.distanceFromSelf);
			double f1 = ((hu1.deadtime) * m + d1);
			double f2 = ((hu2.deadtime) * m + d2);
			if (hu1 instanceof Civilian && hu2 instanceof Civilian) {
				if (hu1.deadtime > time && hu2.deadtime <= time)
					return 1;
				if (hu2.deadtime > time && hu1.deadtime <= time)
					return -1;
			}

			if (f1 == f2) {
				f1 = hu1.getID().getValue();
				f2 = hu2.getID().getValue();
			}

			if (f1 > f2)
				return 1;
			if (f2 > f1)
				return -1;
			return 0;

		}
	};

	protected void sendInfo() {
		if (time > 2 && me().feedback) {
			log("reach: " + me().isReachable + "  CHERT: " + isChert);
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_AMB, 0,
					Radar.headerSize);
			if (me().isReachable)
				start = Radar.calcWithBitSet(bits, 1, start, 1);
			else
				start = Radar.calcWithBitSet(bits, 0, start, 1);
			if (isChert)
				start = Radar.calcWithBitSet(bits, 1, start, 1);
			else
				start = Radar.calcWithBitSet(bits, 0, start, 1);

			byte[] byteArray = Radar.toByteArray(bits);
			sendSpeak(time, behtarinChannel1, byteArray);
		}
	}

	protected int readmessage(ArrayList<Integer> targetID) {
		int pow = 1;
		int targetPosition = 0;
		for (int i = 0; i < targetID.size(); i++) {
			targetPosition += pow * targetID.get(i);
			pow *= 2;
		}
		return targetPosition;
	}

	int lastHeardTime = 0;

	protected void hearAmbulance() {
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
						if (header == Radar.HEADER_CENTER
								&& bits.get(Radar.headerSize) == true) {
							int messagesize = EntityBeInt.size();
							boolean b = false;
							if (bits.get(Radar.headerSize + 1) == true) {
								messagesize = allEntities.size();
								b = true;
							}
							int powNumber = 0;
							while (messagesize > 0) {
								messagesize /= 2;
								powNumber++;
							}
							log("ponum  " + powNumber);
							isHeard = true;
							int startMess = Radar.headerSize + 1 + 1;
							for (Human h : modelAmbulanceTeams) {
								ArrayList<Integer> tarID = new ArrayList<Integer>();
								ArrayList<Integer> tar = new ArrayList<Integer>();
								for (int j = startMess + powNumber - 1; j >= startMess; j--) {
									if (bits.get(j))
										tarID.add(1);
									else
										tarID.add(0);
								}
								int position = readmessage(tarID);
								startMess += powNumber;
								int re = -1;
								if (position != 0) {
									for (int i = startMess + 5 - 1; i >= startMess; i--) {
										if (bits.get(i))
											tar.add(1);
										else
											tar.add(0);
									}
									startMess += 5;
									re = readmessage(tar);
									log("por shod:a " + h + " re " + re);
									me().assignIndex.put((AmbulanceTeam) h, re);
								} else
									me().assignIndex.put((AmbulanceTeam) h, -1);

								if (h.getID().getValue() == me().getID()
										.getValue()) {
									lastHeardTime = time;
									log("shenidam");
									if (position == 0) {
										aimPosition = null;
									} else {
										if (b)
											aimPosition = allRanks
													.get(position).getID();
										else
											aimPosition = IntBeEntity
													.get(position);

									}
									aimIndex = re;
								}
							}
						}
					}
				}
			}
		}
	}

	boolean isInsetHuman = false;

	protected void setHuman() throws ActionCommandException {
		log("in PGSETTTTTTTTTTTT");
		// aim cherto mozakhrafe hala khodam ye chi peyda mikonam ha ha ha!
		log("lastheard " + lastHeardTime + "  time: " + time);

		nowTarAgent = new ArrayList<Human>();
		nowTarCv = new ArrayList<Human>();
		// age az ghabl myTarget daram hamuno edame bedam
		if (myTarget != null && goodEnough(myTarget))
			Rescue(myTarget);
		myTarget = null;
		log("my target null bud");
		// ye yaruye dg baraye rescue peyda konam
		for (Human h : modelAgents)
			if (goodEnough(h) && isReachable(h.getPosition()))
				nowTarAgent.add(h);
		for (Entity ent : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			Civilian cv = (Civilian) ent;
			if (goodEnough(cv) && isReachable(cv.getPosition())
					&& model.getEntity(cv.getPosition()) instanceof Area)
				nowTarCv.add(cv);
		}
		Collections.sort(nowTarAgent, newComparator);
		Collections.sort(nowTarCv, newComparator);
		if (nowTarAgent.size() != 0) {
			for (Human hu : nowTarAgent) {
				myTarget = hu;
				myTargetPosition = hu.getPosition();
				myIndex = hu.getID().getValue() % cvRecognizer;
				isInsetHuman = true;
				log("mytarget entekhabshod1: " + myTarget);
				Rescue(myTarget);
			}
		} else if (nowTarCv.size() != 0) {
			for (Human hu : nowTarCv) {
				myTarget = hu;
				myTargetPosition = hu.getPosition();
				myIndex = hu.getID().getValue() % cvRecognizer;
				isInsetHuman = true;
				log("mytarget entekhabshod2: " + myTarget);
				Rescue(myTarget);
			}
		}
		log("man kheili kharam ke miram search");
		newSearch();
	}

	protected void decide() throws ActionCommandException {
		if (time < 3)
			rest();
		isHeard = false;
		aimPosition = null;
		isInsetHuman = false;
		me().feedback = false;
		me().isReachable = true;
		isChert = false;
		for (AmbulanceTeam a : modelAmbulanceTeams)
			if (me().assignIndex.get(a) == null)
				me().assignIndex.put(a, -1);
		hearAmbulance();

		for (AmbulanceTeam amb : me().assignIndex.keySet())
			log("amb " + amb + "  " + me().assignIndex.get(amb));

		if (model.getEntity(me().getPosition()) instanceof Building
				|| model.getEntity(me().getPosition()) instanceof Road)
			setEmpty(me().getPosition());

		if (aimPosition != null)
			log("aimPosition: " + aimPosition.getValue() + " darsad "
					+ aimIndex);

		setCivilianOnBoard();

		if (me().hasBuriedness) {
			myTarget = null;
			myTargetPosition = null;
			myIndex = -1;
			if (isHeard && aimPosition != null) {
				me().feedback = true;
				me().isReachable = false;
			}
			rest();
		}

		if (myTarget != null && !goodEnough(myTarget)) {
			log("myTarget != null && !goodEnough(myTarget)");
			myTarget = null;
			myTargetPosition = null;
			myIndex = -1;
		}
		log("myIndex " + myIndex);
		if (myTargetPosition != null
				&& !goodEnough((Area) model.getEntity(myTargetPosition),
						myIndex)) {
			log("myTPos != null && !goodEnough(myTPos)");
			myTarget = null;
			myTargetPosition = null;
			myIndex = -1;
		}

		if (noCommi || lowCommi) {
			setHuman();
		}

		if (lastHeardTime + 20 < time || !shouldListen) {
			shouldListen = false;
			setHuman();
			isInsetHuman = true;
		}

		if (!isHeard) {
			log("lastheard " + lastHeardTime + " time " + time + " mytarget "
					+ myTarget + " mytarpos " + myTargetPosition);
			if (myTarget != null || myTargetPosition != null) {
				normalWork();
				log("my target daram");
			}
		}

		else {
			log("isHeard");
			if (aimPosition != null && !isReachable(aimPosition)) {
				log("aimPosition != null && !isReachable(aimPosition)");
				me().feedback = true;
				me().isReachable = false;
				aimPosition = null;
			}
			if (aimPosition != null
					&& !goodEnough((Area) model.getEntity(aimPosition),
							aimIndex)) {
				log("!goodEnough((Aim)");
				me().feedback = true;
				isChert = true;
				aimPosition = null;
			} else {
				myTargetPosition = aimPosition;
				myIndex = aimIndex;
			}
		}

		normalWork();
	}

	private void normalWork() throws ActionCommandException {
		if (myTargetPosition != null) {
			if (!me().getPosition().equals(myTargetPosition)) {
				log("move be myTarget");
				move(myTargetPosition);
			}

			ArrayList<Human> atTargetPosCv = new ArrayList<Human>();
			ArrayList<Human> atTargetPosAgent = new ArrayList<Human>();
			for (Human se : modelAgents) {
				Human h = (Human) se;
				if (h.isPositionDefined() && h.isBuriednessDefined()
						&& h.getPosition().equals(myTargetPosition)
						&& h.getBuriedness() > 0 && goodEnough(h)
						&& h.getID().getValue() % cvRecognizer == myIndex) {
					atTargetPosAgent.add(h);
				}
			}
			for (StandardEntity se : model
					.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
				Human h = (Human) se;
				if (h.isPositionDefined()
						&& h.getPosition().equals(myTargetPosition)
						&& goodEnough(h)
						&& h.getID().getValue() % cvRecognizer == myIndex)
					atTargetPosCv.add(h);
			}
			if (atTargetPosAgent.size() != 0) {
				Collections.sort(atTargetPosAgent, newComparator);
				for (Human h : atTargetPosAgent)
					myTarget = h;
			} else if (atTargetPosCv.size() != 0) {
				Collections.sort(atTargetPosCv, newComparator);
				for (Human h : atTargetPosCv)
					myTarget = h;
			}
			log("rescue myTarget");
			Rescue(myTarget);
		}
		newSearch();
	}

	private void newSearch() throws ActionCommandException {
		log("SEARCH");
		if (noCommi && myLZone != null && myLZone.isRealLittleZone) {
			log("too nocommi say!");
			log("index : " + newZone.indexOf(myLZone));
			// myLZone.owner = me().getID().getValue();
			sendEmptyBuildings(myLZone.id, true);
			log("goftam! little zone e?" + myLZone.id);
		}

		// me().isFree = true;
		// me().feedback = true;
		if (amIStucking) {
			log("RANDOM WALK");
			randomWalk();
		}
		log("nmiai birooooooooon?!");
		if (myLZone != null) {
			for (int i = 0; i < myLZone.searchBuildings.size(); i++) {
				if (!goodBuilding(myLZone.searchBuildings.get(i))) {
					myLZone.searchBuildings.remove(i);
					i--;
				}
			}
		}

		if (myLZone == null
				|| myLZone.searchBuildings.size() == 0
				|| (myLZone.owner != me().getID().getValue() && myLZone.owner != -1)) {
			log("if e aval");
			tar = null;
			// if (myLZone != null && myLZone.owner == me().getID().getValue()
			// && myLZone.buildings.size() == seenBuildings)
			// sendEmptyBuildings(myLZone.id, true);
			ArrayList<LittleZone> allZ = new ArrayList<LittleZone>();
			for (LittleZone littleZone : newZone)
				for (Building building : littleZone.buildings)
					if (goodBuilding(building)) {
						allZ.add(littleZone);
						break;
					}
			ArrayList<LittleZone> allUnChosenlittleZones = new ArrayList<LittleZone>();
			ArrayList<LittleZone> allChosenlittleZones = new ArrayList<LittleZone>();
			for (LittleZone littleZone : allZ)
				if (littleZone.owner == -1)
					allUnChosenlittleZones.add(littleZone);
				else
					allChosenlittleZones.add(littleZone);
			myLZone = null;
			if (allUnChosenlittleZones.size() != 0) {
				myLZone = selectZoneForSearch(allUnChosenlittleZones);
			} else if (allChosenlittleZones.size() != 0) {
				log("going to selectzone");
				myLZone = selectZoneForSearch(allChosenlittleZones);
			}
			if (myLZone != null) {
				myLZone.owner = me().getID().getValue();
				if (!noCommi && myLZone.isRealLittleZone) {
					log("inja");
					log("index : " + newZone.indexOf(myLZone));
					sendEmptyBuildings(myLZone.id, true);
					log("goftam! little zone e?" + myLZone.id);
				}
			}
			if (myLZone == null) {
				log("my zone null , i'm going to search");
				search();
			}
			// sendEmptyBuildings(myLZone.id, true);
		}
		log("haa? che shode ast to raa?" + myLZone.id + " owner ? "
				+ myLZone.owner);
		moveToSearch();
		search();
	}

	// private int distToZoneSearch(int v) {
	// int[] arr = { 10, 20, 30, 40, 50, 60 };
	// for (int i = 0; i < arr.length; i++)
	// if (v <= arr[i])
	// return i;
	//
	// return arr.length;
	// }
	private boolean reduceLtv() {
		boolean tof = false;
		EntityID myPosID = me().getPosition();
		if (heard.size() > 0) {
			for (Command cmd : heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& (entity instanceof Civilian)) {
					tof = true;
					for (StandardEntity e : model.getObjectsInRange(myPosID,
							cvSayingDist)) {
						if (e instanceof Building) {
							if (((Building) e).lastTimeVisit < 0) {
								double dist = Math.hypot(((Building) e).getX()
										- me().getX(), ((Building) e).getY()
										- me().getY());
								if (dist < 30000) {
									((Building) e).lastTimeVisit--;
								}
							}
						}
					}

				}
			}
		}
		return tof;
	}

	private LittleZone dorObarLZ() {
		LittleZone doram = new LittleZone();
		EntityID myPosID = me().getPosition();
		doram.isRealLittleZone = false;
		for (StandardEntity e : model.getObjectsInRange(myPosID, cvSayingDist)) {
			if (e instanceof Building) {
				if (((Building) e).lastTimeVisit < 0) {
					double dist = Math.hypot(((Building) e).getX()
							- me().getX(), ((Building) e).getY() - me().getY());
					if (dist < 30000 && goodBuilding((Building) e)) {
						doram.searchBuildings.add((Building) e);
					}
				}
			}
		}
		return doram;
	}

	private boolean goodBuilding(Building build) {
		return ((!build.isFierynessDefined() || (!build.isOnFire() && build
				.getFieryness() != 8)) && build.lastTimeVisit < 0 && build.worldGraphArea.isReachable);
	}

	private LittleZone selectZoneForSearch(ArrayList<LittleZone> allZ) {
		if (reduceLtv()) {
			return dorObarLZ();
		}
		ArrayList<LittleZone> reachableLittles = new ArrayList<LittleZone>();
		for (LittleZone little : allZ) {
			little.centrePoint();
			reachableLittles.add(little);
		}
		Collections.sort(reachableLittles, new Comparator<LittleZone>() {
			public int compare(LittleZone o1, LittleZone o2) {
				double dist1 = Math.hypot(o1.centreX - me().getX(), o1.centreY
						- me().getY());
				double dist2 = Math.hypot(o2.centreX - me().getX(), o2.centreY
						- me().getY());
				if (dist1 > dist2)
					return 1;
				else if (dist1 < dist2)
					return -1;
				return 0;
			}
		});

		int x = random.nextInt(Math.min(5, reachableLittles.size()));
		LittleZone lz = reachableLittles.get(x);
		lz.searchBuildings = new ArrayList<Building>();
		for (Building b : lz.buildings) {
			if (goodBuilding(b)) {
				lz.searchBuildings.add(b);
			}
		}

		return lz;
	}

	private void moveToSearch() throws ActionCommandException {
		int minltv1 = Integer.MAX_VALUE;
		int mindist = Integer.MAX_VALUE;
		ArrayList<Building> minLastTimeVisit = new ArrayList<Building>();
		Building tar3 = null;

		for (Building building : myLZone.searchBuildings) {
			// log("shoghl e dolati");
			if (building.lastTimeVisit <= minltv1)
				minltv1 = building.lastTimeVisit;
		}
		for (Building building : myLZone.searchBuildings)
			if (building.lastTimeVisit == minltv1)
				minLastTimeVisit.add(building);
		for (Building building : minLastTimeVisit) {
			if (building.worldGraphArea.distanceFromSelf <= mindist) {
				mindist = building.worldGraphArea.distanceFromSelf;
				tar3 = building;
			}
		}

		if (tar == null || !goodBuilding(tar)
				|| (tar3 != null && tar3.lastTimeVisit < tar.lastTimeVisit)) {
			tar = tar3;
		}

		if (tar != null) {
			move(tar.getID());
		}
	}

	private void search() throws ActionCommandException {
		if (amIStucking || reachableAreas.size() <= 1) {
			log("RANDOM WALK");
			randomWalk();
		}
		log("I am searching");
		// me().isFree = true;
		if (target != null
				&& (me().getPosition().equals(target) || !isReachable(target) || (model
						.getEntity(target) instanceof Building && ((Building) model
						.getEntity(target)).isOnFire())))
			target = null;
		if (target == null) {
			int minTime = Integer.MAX_VALUE;
			for (Building building : reachableBuildings)
				if (!building.isOnFire() && building.lastTimeVisit < minTime) {
					minTime = building.lastTimeVisit;
					if (minTime == -2)
						break;
				}
			target = findTarget(minTime);
			if (target != null)
				targetCounter++;
		}

		if (target == null) {
			int minTime = Integer.MAX_VALUE;
			for (Area area : reachableAreas)
				if (area.lastTimeVisit < minTime) {
					minTime = area.lastTimeVisit;
					target = area.getID();
					if (minTime == -1)
						break;
				}
		}
		if (targetCounter >= 5 && reachableBuildings.size() < 10) {
			targetCounter = 0;
			int minTime = Integer.MAX_VALUE;
			for (rescuecore2.standard.entities.Road road : reachableRoads)
				if (road.lastTimeVisit < minTime) {
					minTime = road.lastTimeVisit;
					target = road.getID();
					if (minTime == -1)
						break;
				}
		}

		if (target == null)
			randomWalk();
		else
			move(target);
	}

	private EntityID findTarget(int minVisitTime) {
		wg.clearAreas();

		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		ArrayList<Enterance> start = wg.myEnterances;

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
					if (neighbour.area.modelArea.lastTimeVisit <= minVisitTime
							&& (neighbour.area.modelArea instanceof Building || neighbour.area.modelArea instanceof Refuge)
							&& !((Building) neighbour.area.modelArea)
									.isOnFire())
						return neighbour.area.modelArea.getID();

					neighbour.mark = true;
					for (Enterance internal : neighbour.internalEnterances)
						if (!internal.mark) {
							if (internal.area.modelArea.lastTimeVisit <= minVisitTime
									&& (internal.area.modelArea instanceof Building || internal.area.modelArea instanceof Refuge)
									&& !((Building) internal.area.modelArea)
											.isOnFire())
								return internal.area.modelArea.getID();

							internal.mark = true;
							newLayer.add(internal);
						}
				}
			}

			layer = newLayer;
		}

		return null;
	}

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	}
}
