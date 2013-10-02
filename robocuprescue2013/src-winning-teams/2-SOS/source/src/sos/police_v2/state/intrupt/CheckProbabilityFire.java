package sos.police_v2.state.intrupt;

import java.awt.Point;
import java.util.ArrayList;

import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.worldModel.SearchBuilding;

public class CheckProbabilityFire extends PoliceAbstractIntruptState {

	private ArrayList<Building> fireList;
	private ArrayList<Building> nearFireList;
	private Road target = null;
	private Road lastTarget = null;
	private PrecomputeState precomputeState;
	private int checkRang = 0;
	private int lastLock = -50;
	private int spentTime = 0;

	public CheckProbabilityFire(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		precomputeState = agent.getState(PrecomputeState.class);
		checkRang = (int) (model().getDiagonalOfMap() / 5);
		log.warn("check rang for fire property= " + checkRang);
	}

	@Override
	public boolean canMakeIntrupt() {
		target = null;
		if (model().time() > lastLock + 20)
			spentTime = 0;
		if (model().time() < lastLock + 50) {
			log.debug("still in lock state");
			return false;
		}
		if (precomputeState.isDone)
			return false;
		if (lastTarget != null)
			return true;
		fireList = agent.fireProbabilityChecker.getProbabilisticFieryBuilding();
		if (fireList.size() == 0) {
			return false;
		}
		//		setTarget();
		nearFireList = new ArrayList<Building>();
		for (Building select : fireList) {
			if (Point.distance(select.x(), select.y(), agent.me().getX(), agent.me().getY()) <= checkRang) {
				nearFireList.add(select);
			}
		}
		if (nearFireList.size() > 0)
			return true;
		return false;
	}

	private void setTarget() {
		nearFireList = new ArrayList<Building>();
		long avgX = 0;
		long avgY = 0;
		for (Building select : fireList) {
			if (Point.distance(select.x(), select.y(), agent.me().getX(), agent.me().getY()) <= checkRang) {
				nearFireList.add(select);
				avgX += select.getX();
				avgY += select.getY();
			}
		}
		if (nearFireList.size() == 0) {
			log.debug("nearFireList size is zero");
			return;
		}
		avgX = avgX / nearFireList.size();
		avgY = avgY / nearFireList.size();
		log.debug("near to me = " + nearFireList + "  avg=" + avgX + "----" + avgY);
		ArrayList<Road> roads = new ArrayList<Road>(model().getObjectsInRange((int) avgX, (int) avgY, checkRang / 2, Road.class));
		if (roads.size() == 0) {
			log.debug("roads size to select center road is zero");
			return;
		}
		Road best = roads.get(0);
		int dis = Integer.MAX_VALUE;
		for (Road road : roads) {
			int temp = (int) Point.distance(road.getX(), road.getY(), avgX, avgY);
			if (dis > temp) {
				best = road;
				dis = temp;
			}
		}
		log.debug("best is " + best);
		target = best;
		lastTarget = best;
	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void act() throws SOSActionException {
		spentTime++;
		log.debug("acting as checkProbabilityFire");
		if (spentTime > 10) {
			lastLock = model().time();
			spentTime = 0;
			log.debug("go to lock state");
			lastTarget = null;
			return;
		}
		//		nearFireList
		//		ArrayList<Building> res =agent.fireProbabilityChecker.getProbabilisticFieryBuilding();
		if (nearFireList.isEmpty())
			return;

		SearchBuilding best = agent.newSearch.getSearchWorld().getSearchBuilding(nearFireList.get(0));

		for (Building b : nearFireList) {
			SearchBuilding sb = agent.newSearch.getSearchWorld().getSearchBuilding(b);
			if (sb.isSpecialForFire() > best.isSpecialForFire())
				best = sb;
		}
		//		SearchTask task = agent.newSearch.fireSearchTask();
		SearchTask task = new SearchTask(best.getRealBuilding().fireSearchBuilding().sensibleAreasOfAreas());
		handleTask(task);
		//		if (target != null)
		//			makeReachableTo(target);
		//		if (lastTarget != null)
		//			makeReachableTo(lastTarget);
		//		lastTarget = null;
	}

	public void handleTask(SearchTask task) throws SOSActionException {
		log.debug("Handeling task " + task);
		if (task == null) {
			return;
		} else {
			moveToShape(task.getArea());
		}
	}
}
