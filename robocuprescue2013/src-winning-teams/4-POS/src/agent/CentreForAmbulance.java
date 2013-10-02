package agent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.Entity;
import agent.Agent.MapType;

public class CentreForAmbulance {

	public Agent<? extends StandardEntity> agent = null;
	protected ArrayList<Human> humans = new ArrayList<Human>();
	protected ArrayList<AmbulanceTeam> allAmbs = new ArrayList<AmbulanceTeam>();
	protected ArrayList<Human> owners = new ArrayList<Human>();
	protected ArrayList<AmbulanceTeam> PgAmb = new ArrayList<AmbulanceTeam>();
	protected BitSet message = new BitSet();
	protected ArrayList<AmbulanceTeam> freeAmbs = new ArrayList<AmbulanceTeam>();
	protected boolean naroTuAfter = false;
	private boolean targetInRoad = false;
	private HashMap<AmbulanceTeam, Integer> lastSetTime = new HashMap<AmbulanceTeam, Integer>();
	private HashMap<AmbulanceTeam, Area> lastSetPos = new HashMap<AmbulanceTeam, Area>();
	private HashMap<AmbulanceTeam, HashMap<Area, Integer>> unreachableAreas = new HashMap<AmbulanceTeam, HashMap<Area, Integer>>();
	private static final int SAFE_TIME = 1;
	private int numOfAmb = 1;
	private int generateNum = 10000;

	public CentreForAmbulance(Agent<? extends StandardEntity> age) {
		agent = age;
	}

	public Comparator<Entity> newComparator = new Comparator<Entity>() {
		public int compare(Entity a1, Entity a2) {
			int m = 2;
			if (agent.map == MapType.Berlin || agent.map == MapType.Istanbul
					|| agent.map == MapType.Paris)
				m = 1;
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
			if (f1 > f2)
				return 1;
			if (f2 > f1)
				return -1;
			return 0;
		}
	};

	public boolean isInRefuge(Human h) {
		if (model.getEntityByInt(h.getPosition().getValue()) instanceof Refuge)
			return true;
		return false;
	}

	public boolean isOnFire(Entity building) {
		if (building instanceof Building
				&& ((Building) building).isFierynessDefined()
				&& ((Building) building).getFieryness() != 0
				&& ((Building) building).getFieryness() != 5)
			return true;
		return false;
	}

	public boolean goodEnough(Human h) {
		if ((h.hasBuriedness || h instanceof Civilian)
				&& h.isPositionDefined()
				&& !isOnFire(model.getEntity(h.getPosition()))
				&& h.deadtime > time
				&& (model.getEntity(h.getPosition()) instanceof Building || (model
						.getEntity(h.getPosition()) instanceof Road
						&& h.isDamageDefined() && h.getDamage() != 0))
				&& !(isInRefuge(h))) {
			if (h.deadtime > time) {
				return true;
			}
		}
		return false;
	}

	protected void sort() {
		humans = new ArrayList<Human>();
		for (Human hu : agent.modelAgents) {
			if (goodEnough(hu))
				humans.add(hu);
		}
		for (Entity ent : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			Civilian cv = (Civilian) ent;
			if (goodEnough(cv))
				humans.add(cv);
		}

		Collections.sort(humans, new Comparator<Human>() {
			@Override
			public int compare(Human arg0, Human arg1) {
				if (arg0 instanceof Civilian && !(arg1 instanceof Civilian))
					return 1;
				if (!(arg0 instanceof Civilian) && arg1 instanceof Civilian)
					return -1;
				int Dead0 = arg0.deadtime;
				int Dead1 = arg1.deadtime;
				if (Dead0 > Dead1)
					if (Dead1 > time)
						return 1;
					else
						return -1;
				if (Dead1 > Dead0)
					if (Dead0 > time)
						return -1;
					else
						return 1;
				return 0;
			}
		});

	}

	private ArrayList<TaskAssign> allTasks = new ArrayList<TaskAssign>();

	private void setTimeAndPos(AmbulanceTeam amb) {
		lastSetTime.put(amb, agent.time);
		lastSetPos.put(amb, (Area) amb.getPosition(model));
		amb.isFree = true;
		amb.PgAim = null;
	}

	boolean firstTime = true;

	private void setOwner() {
		log("in setOwner");
		checkAreaReachablity();
		checkHumansOnAreas();

		for (AmbulanceTeam amb : allAmbs) {

			if (amb.feedback && !amb.isReachable && amb.lastSetAim != null
					&& amb.setAimTime + SAFE_TIME < agent.time) {

				unreachableAreas.get(amb).put(
						(Area) amb.lastSetAim.getPosition(model), agent.time);
				log("chert1" + amb);
				setTimeAndPos(amb);
				for (int i = 0; i < allTasks.size(); i++) {
					if (allTasks.get(i).ambs.contains(amb)) {
						for (AmbulanceTeam a : allTasks.get(i).ambs)
							if (a.lastSetAim != null
									&& a.lastSetAim.equals(amb.lastSetAim)) {
								log("chert2" + amb);
								setTimeAndPos(a);
							}
						TaskAssign removed = allTasks.remove(i);
						if (removed.human == amb.lastSetAim)
							break;
						i--;
					}
				}

			}

			if (amb.feedback && amb.isMozakhraf && amb.lastSetAim != null
					&& amb.setAimTime + SAFE_TIME < agent.time) {
				for (TaskAssign t : allTasks) {
					if (amb.lastSetAim != null
							&& t.human.equals(amb.lastSetAim)
							&& t.ambs.contains(amb)) {
						t.ambs.remove(amb);
						log("cherte" + amb);
						setTimeAndPos(amb);
					}
				}
			}
		}

		// TODO: tofe exception
		// ---------------------------------------------------------
		for (TaskAssign t : allTasks) {
			if (t.human.getPosition(model) instanceof Refuge) {
				for (AmbulanceTeam amb : t.ambs) {
					log("tof" + amb);
					setTimeAndPos(amb);
				}
				t.ambs.clear();
			}
		}

		for (AmbulanceTeam amb : allAmbs)
			if (amb.PgAim != null
					&& amb.PgAim.getPosition(model) instanceof Refuge) {
				log("##tof");
				setTimeAndPos(amb);
			}
		// ---------------------------------------------------------

		for (AmbulanceTeam amb : allAmbs) {
			if (!lastSetTime.containsKey(amb)) {
				setTimeAndPos(amb);

			} else if (lastSetTime.get(amb) <= agent.time) {
				if (amb.isFree) {
					setTimeAndPos(amb);
				} else {
					lastSetTime.put(amb, lastSetTime.get(amb) + 3);
					for (int i = 0; i < allTasks.size(); i++) {
						if (allTasks.get(i).ambs.contains(amb)) {
							int index = allTasks.get(i).ambs.indexOf(amb);
							if (allTasks.get(i).time.get(index) == agent.time)
								allTasks.get(i).time.set(index,
										allTasks.get(i).time.get(index) + 3);
							if (allTasks.get(i).time.get(index) < lastSetTime
									.get(amb)) {
								for (AmbulanceTeam a : allTasks.get(i).ambs)
									if (a.lastSetAim != null
											&& a.lastSetAim.equals(allTasks
													.get(i).human)) {
										setTimeAndPos(a);
									}
								allTasks.remove(i);
								i--;
							}
						}
					}
				}
			}

		}

		// taske ba'adish
		for (AmbulanceTeam amb : allAmbs) {
			if (lastSetTime.get(amb) == agent.time) {
				for (TaskAssign t : allTasks) {
					if (t.ambs.contains(amb)) {
						int a = t.ambs.indexOf(amb);
						if (t.time.get(a) > agent.time) {
							log("1111111111111");
							setTarget(amb, t.human, t.time.get(a));
							break;
						}
					}
				}
			}
		}

		ArrayList<Human> taskHumans = new ArrayList<Human>();
		for (Human h : humans) {
			if ((h.getPosition(model) instanceof Building || (h
					.getPosition(model) instanceof Road && h.isDamageDefined() && h
					.getDamage() != 0))
					&& !(h.getPosition(model) instanceof Refuge)) {
				taskHumans.add(h);
			}
		}

		for (TaskAssign t : allTasks) {
			for (Human h : taskHumans)
				if (t.human.equals(h)) {
					taskHumans.remove(h);
					break;
				}
		}
		for (Human h : taskHumans)
			log("human " + h + " b " + h.getBuriedness() + " d " + h.deadtime);

		for (Human h : taskHumans)
			h.updateDistance(model, agent.wg);

		HashMap<AmbulanceTeam, Integer> lastSetTimeCopy = (HashMap<AmbulanceTeam, Integer>) lastSetTime
				.clone();
		HashMap<AmbulanceTeam, Area> lastSetPosCopy = (HashMap<AmbulanceTeam, Area>) lastSetPos
				.clone();

		done: for (AmbulanceTeam amb : lastSetPos.keySet()) {
			for (int i = allTasks.size() - 1; i >= 0; i--) {
				if (allTasks.get(i).ambs.contains(amb)) {
					int a = allTasks.get(i).ambs.indexOf(amb);
					if (allTasks.get(i).time.get(a) > agent.time) {
						lastSetTimeCopy.put(amb, allTasks.get(i).time.get(a));
						log(" cast " + allTasks.get(i).human.getPosition()
								+ "  " + agent.time + " "
								+ amb.getID().getValue());
						if (allTasks.get(i).human.getPosition(model) instanceof Area)
							lastSetPosCopy.put(amb,
									(Area) allTasks.get(i).human
											.getPosition(model));
						// TODO: zoodtar refuge beshe age gharare beshe
						continue done;
					}
				}
			}
		}

		State bestState = null;
		numOfAmb = allAmbs.size();
		for (int i = 0; i < generateNum; i++) {
			State state = new State(model);
			state.time = agent.time;
			state.lastSetPos = (HashMap<AmbulanceTeam, Area>) lastSetPosCopy
					.clone();
			state.lastSetTime = (HashMap<AmbulanceTeam, Integer>) lastSetTimeCopy
					.clone();
			state.generateRandom(numOfAmb, taskHumans, unreachableAreas);
			if (bestState == null
					|| state.getFitness() > bestState.getFitness())
				bestState = state;
		}
		if (bestState.tasks.size() == 0)
			log("bugggggg " + agent.time);
		for (TaskAssign t : bestState.tasks) {
			for (int i = 0; i < t.ambs.size(); i++) {
				if (lastSetTime.get(t.ambs.get(i)) == agent.time) {
					setTarget(t.ambs.get(i), t.human, t.time.get(i));
					if (!allTasks.contains(t)) {
						allTasks.add(t);
					}
				}
			}
		}
	}

	private void setTarget(AmbulanceTeam amb, Human h, int time) {
		if (amb.getID().getValue() == 978)
			log("dozd");
		h.PgOwners.add(amb);
		amb.PgAim = h;
		amb.lastSetAim = h;
		amb.setAimTime = agent.time;
		lastSetTime.put(amb, time);
		lastSetPos.put(amb, (Area) h.getPosition(model));
		amb.isFree = false;
	}

	public void checkHumansOnAreas() {
		for (Area area : agent.modelAreas)
			area.checkForAmbForBFS = false;
		for (AmbulanceTeam amb : agent.modelAmbulanceTeams)
			((Area) model.getEntity(amb.getPosition())).checkForAmbForBFS = true;
		for (Refuge refuge : agent.modelRefuges)
			refuge.checkForAmbForBFS = true;
		for (FireBrigade fb : agent.modelFireBrigades)
			if (fb.hasBuriedness)
				((Area) model.getEntity(fb.getPosition())).checkForAmbForBFS = true;
		for (PoliceForce pf : agent.modelPoliceForces)
			if (pf.hasBuriedness)
				((Area) pf.getPosition(model)).checkForAmbForBFS = true;
		for (Entity cibil : model.getEntitiesOfType(StandardEntityURN.CIVILIAN))
			if (cibil instanceof Civilian
					&& ((Civilian) cibil).getPosition(model) instanceof Building)
				((Area) ((Civilian) cibil).getPosition(model)).checkForAmbForBFS = true;
	}

	protected int changeToMabnaye2(BitSet bits, int num, int start,
			int messagesize) {
		ArrayList<Integer> baghiMandeHa = new ArrayList<Integer>();
		ArrayList<Integer> barMabnaye2 = new ArrayList<Integer>();
		int maghsom = num;
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
		// log("tafazol " + tafazol);
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

	private HashMap<AmbulanceTeam, Integer> lastAssigned = new HashMap<AmbulanceTeam, Integer>();

	protected String bitsetToString(BitSet b) {
		String s = "";
		for (int i = 0; i < b.size(); i++)
			if (b.get(i))
				s += "1";
			else
				s += "0";
		return s;
	}

	protected void setmessage(BitSet bits) {
		int start = Radar.headerSize + 1 + 1;
		int messageSize = agent.EntityBeInt.size();
		for (AmbulanceTeam a : allAmbs)
			if (a.PgAim != null && a.PgAim.getPosition(model) instanceof Road) {
				targetInRoad = true;
				messageSize = agent.allEntities.size();
				break;
			}
		log("messageSize " + messageSize);
		for (AmbulanceTeam Amb : allAmbs) {
			log("A:: " + Amb);
			log("bits" + bitsetToString(bits));
			log("start " + start);
			if (!lastAssigned.containsKey(Amb)) {
				lastAssigned.put(Amb, 0);
			}
			if (Amb.PgAim == null) {
				Amb.isFree = true;
				start = changeToMabnaye2(bits, 0, start, messageSize);
				lastAssigned.put(Amb, 0);
				log("AMB:::" + Amb + "  free " + agent.time);
			} else {
				Amb.isFree = false;
				log("AMB:::" + Amb + " pgaim " + Amb.PgAim.getPosition()
						+ " darsad " + Amb.PgAim.getID().getValue()
						% agent.cvRecognizer);
				if (!(Amb.PgAim.getPosition(model) instanceof AmbulanceTeam)) {
					if (targetInRoad) {
						log("daram minevisam:"
								+ agent.allEntities.get(Amb.PgAim.getPosition()
										.getValue()));
						lastAssigned.put(Amb, agent.allEntities.get(Amb.PgAim
								.getPosition().getValue()));
					} else {
						lastAssigned.put(Amb,
								agent.EntityBeInt.get(Amb.PgAim.getPosition()));
						log("daram minevisam:"
								+ agent.EntityBeInt
										.get(Amb.PgAim.getPosition()));
					}
				}
				start = changeToMabnaye2(bits, lastAssigned.get(Amb), start,
						messageSize);
				start = changeToMabnaye2(bits, Amb.PgAim.getID().getValue()
						% agent.cvRecognizer, start, agent.cvRecognizer - 1);
			}
		}
	}

	protected void hearInfo() {
		if (agent.heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : agent.heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = agent.radar.tellHeader(recivedSpeak);
						BitSet bits = new BitSet();
						bits = Radar.fromByteArray(recivedSpeak);
						// String s = "";
						// for (int i = 0; i < bits.length(); i++) {
						// if (bits.get(i))
						// s += "1";
						// else
						// s += "0";
						// }
						if (header == Radar.HEADER_AMB) {
							if (bits.get(Radar.headerSize) == true) {
								if (entity instanceof AmbulanceTeam) {
									((AmbulanceTeam) entity).isReachable = true;
									log("reachable " + entity);
								}
							} else {
								if (entity instanceof AmbulanceTeam) {
									((AmbulanceTeam) entity).isReachable = false;
									log("!!reachable " + entity);
								}
							}

							if (entity instanceof AmbulanceTeam) {
								((AmbulanceTeam) entity).feedback = true;
								if (bits.get(Radar.headerSize + 1) == false) {
									((AmbulanceTeam) entity).isMozakhraf = false;
									log("!!chert: " + entity);
								} else {
									((AmbulanceTeam) entity).isMozakhraf = true;
									log("cherte  :" + entity);
								}
							}

						}
						// String w = "";
						// for (int i = 0; i < bits.size(); i++) {
						// if (bits.get(i))
						// w += "1";
						// else
						// w += "0";
						// }
						// log("info                 " + w);
					}
				}
			}
		}
	}

	protected void hearAmbulance() {
		if (agent.heard.size() > 0) {
			byte[] recivedSpeak = null;
			for (Command cmd : agent.heard) {
				Entity entity = model.getEntityByInt(cmd.getAgentID()
						.getValue());
				if (cmd instanceof AKSpeak && entity != null
						&& !(entity instanceof Civilian)) {
					AKSpeak as = (AKSpeak) cmd;
					recivedSpeak = as.getContent();
					if (recivedSpeak.length > 0) {
						int header = agent.radar.tellHeader(recivedSpeak);
						BitSet bits = new BitSet();
						bits = Radar.fromByteArray(recivedSpeak);
						// String s = "";
						// for (int i = 0; i < bits.length(); i++) {
						// if (bits.get(i))
						// s += "1";
						// else
						// s += "0";
						// }
						if (header == Radar.HEADER_AMB
								&& bits.get(Radar.headerSize) == true) {
							if (bits.get(Radar.headerSize + 1) == true) {
								if (entity instanceof AmbulanceTeam) {
									((AmbulanceTeam) entity).isReachable = true;
									log("Reachable: " + entity);
								}
							} else {
								if (entity instanceof AmbulanceTeam) {
									((AmbulanceTeam) entity).isReachable = false;
									log("!Reachable: "
											+ entity.getID().getValue());
								}
							}
						}
						// String w = "";
						// for (int i = 0; i < bits.size(); i++) {
						// if (bits.get(i))
						// w += "1";
						// else
						// w += "0";
						// }
						// log(w);
					}
				}
			}
		}
	}

	protected void sendAmbAL() {
		if (agent.time > 2) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_CENTER, 0,
					Radar.headerSize);
			start = Radar.calcWithBitSet(bits, 1, start, 1);
			setmessage(bits);
			if (targetInRoad)
				start = Radar.calcWithBitSet(bits, 1, start, 1);
			else
				start = Radar.calcWithBitSet(bits, 0, start, 1);
			byte[] byteArray = Radar.toByteArray(bits);
			String w = "";
			for (int i = 0; i < bits.size(); i++) {
				if (bits.get(i))
					w += "1";
				else
					w += "0";
			}
			log(w);
			agent.sendSpeak(time, agent.behtarinChannel1, byteArray);
			if (agent.behtarinChannel2 != 0)
				agent.sendSpeak(time, agent.behtarinChannel2, byteArray);
		}
	}

	protected int time = -1;
	protected StandardWorldModel model = null;

	protected void log(String msg) {
		agent.logger.log(msg);
	}

	private void checkAreaReachablity() {
		for (AmbulanceTeam amb : allAmbs) {
			HashMap<Area, Integer> map = unreachableAreas.get(amb);
			ArrayList<Area> removeMap = new ArrayList<Area>();
			for (Area arr : map.keySet()) {
				if (agent.time - map.get(arr) > 10) {
					removeMap.add(arr);
				}
			}
			for (Area arr : removeMap)
				map.remove(arr);
		}
	}

	protected void decide() {
		if (agent.noCommi || agent.lowCommi)
			return;
		time = agent.time;
		model = agent.getModel();
		log("going to decide :)");
		if (agent.lowCommi)
			for (AmbulanceTeam amb : agent.modelAmbulanceTeams)
				amb.isAlive = true;

		for (AmbulanceTeam amb : agent.modelAmbulanceTeams)
			if (amb.isAlive && !allAmbs.contains(amb)) {
				allAmbs.add(amb);
				unreachableAreas.put(amb, new HashMap<Area, Integer>());
			}

		log("SIZE: " + allAmbs.size());

		clearFeedbacks();

		hearInfo();
		sort();
		setOwner();
		printTasks();
		sendAmbAL();
	}

	private void printTasks() {
		// log("alltasks.size: " + allTasks.size());
		//
		// for (AmbulanceTeam amb : allAmbs)
		// log("amb " + amb + " isReach " + amb.isReachable + " isfree "
		// + amb.isFree + " lastsett " + lastSetTime.get(amb));
		// for (TaskAssign t : allTasks)
		// log(" t: " + t.human + "    " + t.human.getPosition() + "    "
		// + t.ambs + " " + agent.time);
	}

	private void clearFeedbacks() {
		targetInRoad = false;
		for (AmbulanceTeam amb : agent.modelAmbulanceTeams) {
			amb.feedback = false;
			amb.isReachable = true;
			amb.isMozakhraf = false;
		}
	}

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_CENTRE);
	}

}
