package sos.police_v2.state.intrupt;

import java.awt.Point;

import sos.base.entities.AmbulanceTeam;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class ReachableHumanIntruptState extends PoliceAbstractIntruptState {

	private Human agentThatMakeIntrupt;

	public ReachableHumanIntruptState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {

	}

	@Override
	public boolean canMakeIntrupt() {
		//		ArrayList<PoliceForce> sensedpolice = agent.getVisibleEntities(PoliceForce.class);

		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.debug("I'm SpecialPoliceTask can't make ReachableHuman Intrupt...");
				return false;
			}
			//			for (PoliceForce policeForce : sensedpolice)
			//				if (policeForce.getID().getValue() < police.getRealEntity().getID().getValue())
			//					continue;

		}
		for (Human hum : agent.getVisibleEntities(Human.class)) {
			if (hum instanceof FireBrigade || hum instanceof AmbulanceTeam) {
				if (Point.distance(agent.me().getX(), agent.me().getY(), hum.getX(), hum.getY()) > agent.clearDistance)
					continue;
				if (!isReachableTo(hum)) {
					log.debug("I'm not reachable to " + hum + ". making intrrupt..");
					agentThatMakeIntrupt = hum;
					return true;
				}
				log.debug("I'm reachable to " + hum + "...");
			}
		}

		agentThatMakeIntrupt = null;
		return false;
	}

	@Override
	public void act() throws SOSActionException {
		log.info("acting as " + this.getClass().getSimpleName());
		makeReachableTo(agentThatMakeIntrupt);

	}

}
