package agent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashSet;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;

public class CenterForPolice {

	public Agent<? extends StandardEntity> agent = null;
	protected int maxDistanceForMakingGroups = 50000;
	protected ArrayList<GroupForPolice> modelGroupsForPolices = new ArrayList<GroupForPolice>();
	protected ArrayList<PoliceForce> policesForMatchingGroups = new ArrayList<PoliceForce>();
	protected StandardWorldModel model = null;
	public int counterForPoliceMap = 0;
	public ArrayList<GroupForPolice> lastGroups = new ArrayList<GroupForPolice>();
	public int sizeOfPolices = -1;
	public boolean isGroupChanged = false;

	public CenterForPolice(Agent<? extends StandardEntity> age) {
		agent = age;
		model = agent.getModel();
	}

	protected void log(String msg) {
		agent.logger.log(msg);
	}

	private void setPolicesGroupNum(HungarianAlgorithm hu) {
		for (PoliceForce pf : policesForMatchingGroups) {
			pf.GroupNum = hu.getPoliceGroupNum(policesForMatchingGroups
					.indexOf(pf));
		}
	}

	private void selectPolicesForMatchingToGroups() {
		policesForMatchingGroups = new ArrayList<PoliceForce>();
		for (PoliceForce pf : agent.modelPoliceForces) {
			if (!agent.longnHP.containsKey(pf.getID().getValue())
					&& ((pf.isBuriednessDefined() && pf.getBuriedness() == 0) || !pf
							.isBuriednessDefined())) {
				policesForMatchingGroups.add(pf);
			}
		}
	}

	private void makingAndMatchingGroups() {
		isGroupChanged = false;
		modelGroupsForPolices.clear();
		for (FireBrigade fb : agent.modelFireBrigades)
			fb.pogNum = -1;
		for (AmbulanceTeam amb : agent.modelAmbulanceTeams)
			amb.pogNum = -1;
		for (PoliceForce pf : agent.modelPoliceForces)
			pf.pogNum = -1;
		for (int nhp : agent.longnHP.keySet())
			((Human) model.getEntityByInt(nhp)).mark = false;
		for (int nhP : agent.longnHP.keySet()) {
			Entity stuckAgent = model.getEntityByInt(nhP);
			if (((Human) stuckAgent).pogNum == -1) {
				setGroupOfStuckAgentForPolicesBybfs((Human) stuckAgent);
			}
		}
		for (Human hu : agent.modelAgents)
			hu.lastStuckGroup = null;
		for (GroupForPolice gp : modelGroupsForPolices)
			for (Human stuck : gp.StuckAgents)
				stuck.lastStuckGroup = gp;

		selectPolicesForMatchingToGroups();

		if (modelGroupsForPolices.size() != lastGroups.size()
				|| sizeOfPolices != policesForMatchingGroups.size()) {
			isGroupChanged = true;
		} else {
			done: for (int i = 0; i < modelGroupsForPolices.size(); i++) {
				for (Human hu : modelGroupsForPolices.get(i).StuckAgents)
					if (!lastGroups.get(i).StuckAgents.contains(hu)) {
						isGroupChanged = true;
						break done;
					}
				for (Human human : lastGroups.get(i).StuckAgents)
					if (!modelGroupsForPolices.get(i).StuckAgents
							.contains(human)) {
						isGroupChanged = true;
						break done;
					}
			}
		}
		if (isGroupChanged) {
			for (PoliceForce p : agent.modelPoliceForces)
				p.GroupNum = -1;
		}
		if (modelGroupsForPolices.size() > 0 && isGroupChanged
				&& policesForMatchingGroups.size() > 0) {
			HungarianAlgorithm matchingGroups = new HungarianAlgorithm();
			matchingGroups.time = agent.time;
			matchingGroups.matching(modelGroupsForPolices,
					policesForMatchingGroups, model);
			setPolicesGroupNum(matchingGroups);

		}
		lastGroups = (ArrayList<GroupForPolice>) modelGroupsForPolices.clone();
		sizeOfPolices = policesForMatchingGroups.size();

	}

	private void setStuckTarget() {
		for (PoliceForce p : agent.modelPoliceForces)
			p.stuckTarget = null;
		for (PoliceForce pf : policesForMatchingGroups) {
			if (pf.GroupNum != -1) {
				Area areaOfPf = (Area) pf.getPosition(model);
				double minDist = Integer.MAX_VALUE;
				Human nearestStuck = null;
				for (Human stuck : modelGroupsForPolices.get(pf.GroupNum).StuckAgents) {
					Area areaOfStuck = (Area) stuck.getPosition(model);
					if (minDist > Math.hypot(
							areaOfPf.getX() - areaOfStuck.getX(),
							areaOfPf.getY() - areaOfStuck.getY())) {
						minDist = Math.hypot(
								areaOfPf.getX() - areaOfStuck.getX(),
								areaOfPf.getY() - areaOfStuck.getY());
						nearestStuck = stuck;
					}
				}
				pf.stuckTarget = (Area) nearestStuck.getPosition(model);
			}

		}
	}

	private void setGroupOfStuckAgentForPolicesBybfs(Human hu) {

		HashSet<Human> layer = new HashSet<Human>();
		layer.add(hu);

		GroupForPolice pog = new GroupForPolice();
		pog.StuckAgents.add(hu);
		pog.pogNum = modelGroupsForPolices.size();
		hu.pogNum = modelGroupsForPolices.size();

		while (layer.size() != 0) {
			HashSet<Human> newlayer = new HashSet<Human>();

			for (Human stuckAgent : layer) {
				if (stuckAgent.mark)
					continue;
				stuckAgent.mark = true;
				for (int nhp : agent.longnHP.keySet()) {
					Entity nearStuckAgent = model.getEntityByInt(nhp);
					Area areaOFStuck = (Area) model.getEntity(stuckAgent
							.getPosition());
					Area areaOfNearStuck = (Area) model
							.getEntity(((Human) nearStuckAgent).getPosition());
					int dist = maxDistanceForMakingGroups;
					if (stuckAgent.lastStuckGroup != null
							&& stuckAgent.lastStuckGroup.StuckAgents
									.contains(nearStuckAgent)) {
						dist = maxDistanceForMakingGroups + 10000;
					}
					if (((Human) nearStuckAgent).pogNum == -1
							&& (Math.hypot(
									areaOFStuck.getX() - areaOfNearStuck.getX(),
									areaOFStuck.getY() - areaOfNearStuck.getY()) < dist || stuckAgent
									.getPosition().getValue() == ((Human) nearStuckAgent)
									.getPosition().getValue())) {
						newlayer.add((Human) nearStuckAgent);
						pog.StuckAgents.add((Human) nearStuckAgent);
						((Human) nearStuckAgent).pogNum = pog.pogNum;
					}
				}
			}

			layer = newlayer;
		}
		modelGroupsForPolices.add(pog);
		if (pog.StuckAgents.size() > 2)
			modelGroupsForPolices.add(pog);
	}

	protected void decide() {
		if (agent.time > 4) {
			makingAndMatchingGroups();
			setStuckTarget();
			sendPoliceAL();
		}

	}

	BitSet bitsForPolice = new BitSet();

	protected void sendPoliceAL() {
		log("in send");
		if (agent.time > 4 && !agent.lowCommi) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_CENTER, 0,
					Radar.headerSize);
			bits.set(start, false);
			start++;
			int powNumber = Radar.findPow(agent.modelAreas.size() + 2);
			for (PoliceForce pf : agent.modelPoliceForces) {
				if (pf.hasBuriedness) {
					start = Radar.calcWithBitSet(bits, 1, start, powNumber);
					log("buridness dare! :  " + pf.getID().getValue());
				} else if (pf.GroupNum == -1) {
					start = Radar.calcWithBitSet(bits, 0, start, powNumber);
					log("group nadare:  " + pf.getID().getValue());
				} else if (pf.GroupNum != -1) {
					int pos = agent.allEntities.get(pf.stuckTarget.getID()
							.getValue()) + 2;
					// int pos = agent.allEntities
					// .get(((Human) modelGroupsForPolices
					// .get(pf.GroupNum).StuckAgents.toArray()[0])
					// .getPosition().getValue()) + 2;
					log("group dare : "
							+ pf.getID().getValue()
							+ "  stuck : "
							+ ((Human) modelGroupsForPolices.get(pf.GroupNum).StuckAgents
									.toArray()[0]).getID().getValue()
							+ " posesh vagheie: "
							+ ((Human) modelGroupsForPolices.get(pf.GroupNum).StuckAgents
									.toArray()[0]).getPosition().getValue()
							+ "  mainStuckTarget:  "
							+ pf.stuckTarget.getID().getValue());
					start = Radar.calcWithBitSet(bits, pos, start, powNumber);
				}
			}
			// if (!agent.radar.checkBits(bits, bitsForPolice,
			// agent.modelPoliceForces.size())) {
			// bitsForPolice = new BitSet();
			// bitsForPolice = (BitSet) bits.clone();
			// counterForPoliceMap = 0;
			// }
			// if (counterForPoliceMap < 3) {
			// counterForPoliceMap++;
			if (!agent.radar.checkBits(bits, bitsForPolice, bits.length())) {
				log("vaghean daram send mikonam");
				bitsForPolice = bits;
				byte[] byteArray = Radar.toByteArray(bits);
				agent.sendSpeak(agent.time, agent.behtarinChannel1, byteArray);
			}
		}
	}

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_OFFICE);
	}

}
