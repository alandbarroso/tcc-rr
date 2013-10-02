package sos.police_v2.state;

import java.util.ArrayList;
import java.util.HashSet;

import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.move.MoveConstants;
import sos.base.move.types.PoliceReachablityMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class UpdateClusterFireState extends PoliceAbstractState {
	ArrayList<Road> clusterRoad;
	HashSet<Building> clusterBuilding;
	ArrayList<SOSEstimatedFireZone> fireZonesInCluster;
	public Building target = null;
	public HashSet<Building> doneBuilding;
	public ArrayList<Building> listOfTargets;
	public ArrayList<Building> allOuter;

	public UpdateClusterFireState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		clusterBuilding = model().searchWorldModel.getClusterData().getBuildings();
		fireZonesInCluster = new ArrayList<SOSEstimatedFireZone>();
		doneBuilding = new HashSet<Building>();
		listOfTargets = new ArrayList<Building>();
		allOuter = new ArrayList<Building>();
	}

	@Override
	public void act() throws SOSActionException {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.info(" i am special police so dont send act in updateClusteState");
				return;
			}
		}
		if (target == null) {
			updateZonesInMyCluster();
			updateAllOuter();
			updateListOfTargetsBuilding();
			target = setTarget();
			//			if (target != null)
			//				log.warn(" target to update fire in cluster is " + target);
			//			else
			//				log.warn("fireZonesInCluster = " + fireZonesInCluster.size() + "  listOfTargets" + listOfTargets.size());
		}
		if (target != null) {
			updateAllOuter();
			if (isDone(target)) {
				doneBuilding.add(target);
				updateZonesInMyCluster();
				updateAllOuter();
				updateListOfTargetsBuilding();
				target = setTarget();
			}
		}
		if (target != null)
			moveTO(target);

	}

	private Building setTarget() {
		if (listOfTargets.size() == 0)
			return null;
		long distance = Long.MAX_VALUE;
		Building temp = listOfTargets.get(0);
		for (Building b : listOfTargets) {
			ArrayList<ShapeInArea> areas = b.fireSearchBuilding().sensibleAreasOfAreas();
			long weight = agent.move.getWeightToLowProcess(areas, PoliceReachablityMove.class);
			if (weight < distance) {
				temp = b;
				distance = weight;
			}
		}
		return temp;
	}

	private void updateAllOuter() {
		allOuter.clear();
		for (SOSEstimatedFireZone fireZone : fireZonesInCluster)
			allOuter.addAll(fireZone.getOuter());
	}

	private void updateListOfTargetsBuilding() {
		listOfTargets.clear();
		for (Building building : allOuter) {
			if (!clusterBuilding.contains(building))
				continue;
			if (doneBuilding.contains(building))
				continue;
			if (isDone(building)) {
				doneBuilding.add(building);
				continue;
			}
			listOfTargets.add(building);
		}

	}

	private void updateZonesInMyCluster() {
		for (Building select : clusterBuilding) {
			if (select.getFieryness() > 0 && select.getFieryness() < 8) {
				if (select.getEstimator() != null)
					if (!select.getEstimator().isDisable()) {
						if (!fireZonesInCluster.contains(select.getEstimator())) {
							fireZonesInCluster.add(select.getEstimator());
							continue;
						}
					}
			}
		}
	}

	public void moveTO(Building b) throws SOSActionException {
		ArrayList<ShapeInArea> areas = b.fireSearchBuilding().sensibleAreasOfAreas();
		moveToShape(areas);
	}

	public boolean isDone(Building b) {
		if (agent.me().getAreaPosition().equals(b)) {
			log.debug(b + " update is done because me position is equal this");
			return true;
		}
		if (agent.getVisibleEntities(Building.class).contains(b)) {
			log.debug(b + " update is done because its in visible Entities");
			return true;
		}
		ArrayList<ShapeInArea> areas = b.fireSearchBuilding().sensibleAreasOfAreas();
		long weight = agent.move.getWeightToLowProcess(areas, PoliceReachablityMove.class);
		if (weight < MoveConstants.UNREACHABLE_COST) {
			log.debug(b + " update is done because the cost to move to building is lower than 1 cycle unreachable move");
			return true;
		}
		for (ShapeInArea shapeInArea : areas) {
			if (shapeInArea.contains(agent.me().getPositionPoint().toGeomPoint())) {
				log.debug(b + " update is done because agent is in sensible areas of it");
				return true;
			}
		}
		if (!allOuter.contains(b)) {
			log.debug(b + " update is done because its not outer now");
			return true;
		}
		return false;

	}

}