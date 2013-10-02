package agent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashSet;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
//import firesimulator.world.FireBrigade;

public class PoliceOfficeAgent extends Agent<PoliceOffice> {

	int maxDistanceForMakingGroups = 3000;
	ArrayList<GroupForPolice> modelGroupsForPolices = new ArrayList<GroupForPolice>();
	ArrayList<PoliceForce> policesForMatchingGroups = new ArrayList<PoliceForce>();

	//TODO: in tabe bayad behtar she! 
	private void selectPolicesForMatchingToGroups() {
		if (modelPoliceForces.size() > modelGroupsForPolices.size()) {
			int sizeOfPolicesForMatchingGroups = modelGroupsForPolices.size()  ;
			for (int i = 0; i < sizeOfPolicesForMatchingGroups; i++) {
				if (!longnHP.containsKey(modelPoliceForces.get(i).getID()
						.getValue())
						&& ((modelPoliceForces.get(i).isBuriednessDefined()
						&& modelPoliceForces.get(i).getBuriedness() == 0) || !modelPoliceForces.get(i).isBuriednessDefined())) {
					policesForMatchingGroups.add(modelPoliceForces.get(i));
				}
			}
		} else
			policesForMatchingGroups = modelPoliceForces;
	}

	private void makingAndMatchingGroups() {

		modelGroupsForPolices.clear();
		for(FireBrigade fb : modelFireBrigades)
			fb.pogNum = -1;
		for(AmbulanceTeam amb : modelAmbulanceTeams)
			amb.pogNum = -1;
		
		String b = "";
		for (int nhP : longnHP.keySet()) {
			Entity stuckAgent = model.getEntityByInt(nhP);
			b += ((Human) stuckAgent).getID().getValue() + " ; ";
			if (((Human) stuckAgent).pogNum == -1) {
				setGroupOfStuckAgentForPolicesBybfs((Human) stuckAgent);
			}
		}

		log("kole gir kardeha.id: " + b);
		String s = "";
		for (GroupForPolice gp : modelGroupsForPolices) {
			for (Human hu : gp.StuckAgents)
				s += hu.getPosition().getValue() + " ";
			s += " ; ";
		}
		log("nearStuckAgents.pos: " + s);
		String z = "";
		for (GroupForPolice gp : modelGroupsForPolices) {
			for (Human hu : gp.StuckAgents)
				z += hu.getID().getValue() + " ";
			z += " ; ";
		}
		log("nearStuckAgents.id: " + z + "  size: "
				+ modelGroupsForPolices.size());

		if (modelGroupsForPolices.size() > 0) {
			selectPolicesForMatchingToGroups();
			HungarianAlgorithm selectGroups = new HungarianAlgorithm();
			selectGroups.matching(modelGroupsForPolices,
					policesForMatchingGroups,model);
			String p = "";
			for (int i = 0; i < selectGroups.joined.length; i++) {
				p += selectGroups.joined[i] + " ";
			}
			log("joined: " + p);
			String q = "";
			for(PoliceForce o : policesForMatchingGroups)
				q += o.getID().getValue() + " ";
			String r = "";
			for(GroupForPolice gp : modelGroupsForPolices)
				r += ((Human) gp.StuckAgents.toArray()[0]).getID().getValue() + " ";
			log("modelpolices: " + q );
			log("modelg: " + r);
				
			
		}

	}

	private void setGroupOfStuckAgentForPolicesBybfs(Human agent) {

		HashSet<Human> layer = new HashSet<Human>();
		layer.add(agent);

		GroupForPolice pog = new GroupForPolice();
		pog.StuckAgents.add(agent);
		pog.pogNum = modelGroupsForPolices.size();
		agent.pogNum = modelGroupsForPolices.size();

		while (layer.size() != 0) {
			HashSet<Human> newlayer = new HashSet<Human>();

			for (Human stuckAgent : layer) {
				if (stuckAgent.mark)
					continue;
				stuckAgent.mark = true;
				for (int nhp : longnHP.keySet()) {
					Entity nearStuckAgent = model.getEntityByInt(nhp);
					Area areaOFStuck = (Area) model.getEntity(stuckAgent.getPosition());
					Area areaOfNearStuck = (Area) model.getEntity(((Human)nearStuckAgent).getPosition());
					if (!nearStuckAgent.equals(stuckAgent)
							&& ((Human) nearStuckAgent).pogNum == -1
							&& Math.hypot(areaOFStuck.getX()
									- areaOfNearStuck.getX(),
									areaOFStuck.getY()
											- areaOfNearStuck.getY()) < maxDistanceForMakingGroups) {
						newlayer.add((Human) nearStuckAgent);
						pog.StuckAgents.add((Human) nearStuckAgent);
						((Human) nearStuckAgent).pogNum = pog.pogNum;
					}
				}
			}

			layer = newlayer;
		}
		modelGroupsForPolices.add(pog);
	}

	protected void decide() throws ActionCommandException {

		//makingAndMatchingGroups();
	}

	protected void sendGroupsForPolices() {
		if (time > 2) {
			BitSet bits = new BitSet();
			int start = Radar.calcWithBitSet(bits, Radar.HEADER_FIRECENTER, 0,
					Radar.headerSize);
			start = Radar.calcWithBitSet(bits, 1, start, 1);
			setmessage(bits);
			byte[] byteArray = Radar.toByteArray(bits);
			sendSpeak(time, behtarinChannel1, byteArray);
		}
	}

	protected void setmessage(BitSet bits) {
		int start = radar.headerSize + 1;
		for (GroupForPolice gp : modelGroupsForPolices) {
			if (model.getEntity(((Human) gp.StuckAgents.toArray()[0])
					.getPosition()) instanceof Road)
				// Entity poseitionOfStuckAgent =
				// model.getEntity(((Human)gp.StuckAgents.toArray()[0]).getPosition());
				start = changeToMabnaye2(bits, EntityBeInt.get(((Road) model
						.getEntity(((Human) gp.StuckAgents.toArray()[0])
								.getPosition())).nearBuildings.get(0).getID()),
						start);
			if (model.getEntity(((Human) gp.StuckAgents.toArray()[0])
					.getPosition()) instanceof Building)
				start = changeToMabnaye2(bits,
						EntityBeInt.get(((Human) gp.StuckAgents.toArray()[0])
								.getPosition()), start);
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

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_OFFICE);
	}
}
