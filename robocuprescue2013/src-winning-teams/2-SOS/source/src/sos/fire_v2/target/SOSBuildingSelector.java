package sos.fire_v2.target;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rescuecore2.misc.Pair;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.Building;
import sos.base.entities.Center;
import sos.base.entities.FireBrigade;
import sos.base.entities.GasStation;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.geom.ShapeInArea;
import sos.fire_v2.base.AbstractFireBrigadeAgent;
import sos.fire_v2.base.tools.BuildingBlock;
import sos.fire_v2.base.worldmodel.FireWorldModel;
import sos.search_v2.tools.cluster.MapClusterType;

public class SOSBuildingSelector extends SOSSelectTarget<Building> {

	public SOSBuildingSelector(@SuppressWarnings("rawtypes") SOSAgent agent, MapClusterType<FireBrigade> cluster) {
		super(agent, cluster);
	}

	@Override
	public void preCompute() {

	}

	@Override
	public Building getBestTarget(List<Building> validTarget) {
		double max = Integer.MIN_VALUE;
		Building best = null;

		for (Building e : validTarget) {
			if (e.priority() > max) {
				best = e;
				max = e.priority();
			}
		}
		log.info("get best Target " + best);

		return best;
	}

	@Override
	public void reset(List<Building> validTarget) {
		for (Building b : validTarget)
			b.resetPriority();
	}

	@Override
	public void setPriority(List<Building> validTarget) {
		EP_setPriorityForBuildingsInNewRoadSites(40, validTarget);
		for (Building b : validTarget) {

			EP_setPriorityForBuildingNotInMapSideBuildings(b, 1000000);

			if (b instanceof Center)
				EP_setPriorityForCenters(100000, (Center) b);
			if (b instanceof GasStation)
				EP_setPriorityForGasStation(100000, (GasStation) b);

			EP_setPriorityForDistance(b, -200);

			EP_setPriorityForBigBuilding(b);//Negative Score
			EP_setPriorityForUnBurnedIsLands(b, 1000);
			try {
				if (agent.getMapInfo().isBigMap() || agent.getMapInfo().isMediumMap())
					EP_setPriorityForSpread(10000, b.getEstimator(), b);
				else
					EP_setPriorityForSpread(1000, b.getEstimator(), b);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (b.virtualData[0].isBurning()) {
				E_setPriorityForEarlyIgnitedBuildings(b, 500);
				E_setPriorityForUnburnedNeighbours(b, 100);
			} else {
				P_setPreExtinguishProrityForLargBuildingsNearSmallFireBuilding(b, 300);
				P_setPriorityForBigBuilding(b);
				P_setPriorityForCriticalTempratureBuildings(b, 200);
			}
		}
	}

	@Override
	public List<Building> getValidTask(Object link) {
		SOSEstimatedFireZone site = (SOSEstimatedFireZone) link;
		ArrayList<Building> res = new ArrayList<Building>();

		log.logln("Target From " + site + site.getSize());
		log.logln("bs: " + site.getOuter());
		log.logln("ns: " + site.getSafeBuilding());

		res.addAll(site.getOuter());
		res.addAll(site.getSafeBuilding());
		if (!SOSConstant.IS_CHALLENGE_RUNNING)
			reset(res);
		filterNeutral(res);
		filterRefugesAndCenters(res);
		filterUnReachableForExitnguish(res);

		return res;
	}

	private boolean canExtinguish(Building building) {
		return (sos.tools.Utils.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY(), building.x(), building.y()) <= AbstractFireBrigadeAgent.maxDistance);
	}

	protected void filterUnReachableForExitnguish(ArrayList<Building> buildings) {
		log.log("filterUnReachableForExitnguish : \t");

		for (Iterator<Building> iterator = buildings.iterator(); iterator.hasNext();) {
			Building b = iterator.next();

			boolean reachable = isReachable(b.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea(), b);
			if (reachable)
				continue;
			reachable = isReachable(b.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea(), b);
			if (reachable)
				continue;
			log.log(b.getID().getValue() + " \t");
			b.addPriority(0, "Filter Unreachable 1");
			iterator.remove();

			//			if (b.getFireBuilding().getExtinguishableArea().isReallyUnReachableCustom()) {
			//				log.log(b.getID().getValue() + " \t");
			//				b.resetPriority();
			//				b.addPriority(0, "Filter Unreachable 1");
			//				iterator.remove();
			//			}
			//			if (b.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea().isEmpty() && b.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea().isEmpty()) {
			//				log.error("why it come here????" + b + " both ExtinguishableBuildings and ExtinguishableRoads are empty");
			//				b.resetPriority();
			//				b.addPriority(0, "Filter Unreachable 1");
			//				iterator.remove();
			//			}
		}
		log.logln("");
	}

	private boolean isReachable(ArrayList<ShapeInArea> shapes, Building building) {

		ArrayList<ShapeInArea> temp = new ArrayList<ShapeInArea>();
		for (ShapeInArea sh : shapes) {
			log.info("\t\t Shape" + sh + "   Area : " + sh.getArea());
			long cost = 0;
			if (sh.getArea() instanceof Building && ((Building) sh.getArea()).virtualData[0].isBurning())
			{
				log.info("\t\t\t burning building filter");
				continue;

			}
			if (((Human) agent.me()).getAreaPosition().equals(sh.getArea()) && canExtinguish(building)) {
				cost = 0;
			} else {
				temp.clear();
				temp.add(sh);
				cost = agent.move.getWeightTo(temp, StandardMove.class);
				if (cost >= MoveConstants.UNREACHABLE_COST) {
					log.info("\t\t\t Unreachable");
					continue;
				}
			}
			if (cost < MoveConstants.UNREACHABLE_COST)
				return true;
		}
		return false;

	}

	protected void filterRefugesAndCenters(ArrayList<Building> buildings) {
		for (Iterator<Building> iterator = buildings.iterator(); iterator.hasNext();) {
			Building building = iterator.next();
			if ((building instanceof Refuge || building instanceof Center) && !building.isBurning())
			{
				building.resetPriority();
				building.addPriority(0, "Filter Ref Center");
				iterator.remove();
			}
		}

	}

	protected void filterNeutral(ArrayList<Building> buildings) {
		for (Iterator<Building> iterator = buildings.iterator(); iterator.hasNext();) {
			Building building = iterator.next();
			if (building instanceof GasStation)
			{
				if (building.virtualData[0].getTemperature() < 5)
				{
					building.resetPriority();
					building.addPriority(0, "Filter Neutral");
					iterator.remove();
				}
				continue;
			}
			if (building.virtualData[0].getTemperature() < 20)
			{
				building.resetPriority();
				building.addPriority(0, "Filter Neutral");
				iterator.remove();
				continue;
			}
		}
	}

	protected void P_setPriorityForBigBuilding(Building b) {
		if (b.virtualData[0].getTemperature() > 30) {
			if (b.distance((FireBrigade) agent.me()) < AbstractFireBrigadeAgent.maxDistance)
				b.addPriority(b.getGroundArea() * 2, "PreEx Area 1");
			else
				b.addPriority(b.getGroundArea() / 4, "PreEx Area 2");

		}
		if (b.virtualData[0].getTemperature() > 30) {
			if (b.getGroundArea() > 2500) {
				if (b.distance((FireBrigade) agent.me()) < AbstractFireBrigadeAgent.maxDistance)
					b.addPriority(10000, "PreEx Area 3");
				else
					b.addPriority(1200, "PreEx Area 4");
			}
		}

	}

	protected void EX_E_setPriorityForFireNess(Building b, int i) {
		ScoreConstant constant = new ScoreConstant();
		switch (b.virtualData[0].getFieryness()) {
		case 0:
			b.addPriority(i * constant.fireness[0], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 1:
			b.addPriority(i * constant.fireness[1], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 2:
			b.addPriority(i * constant.fireness[2], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 3:
			b.addPriority(i * constant.fireness[3], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 4:
			b.addPriority(i * constant.fireness[4], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 5:
			b.addPriority(i * constant.fireness[5], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 6:
			b.addPriority(i * constant.fireness[6], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 7:
			b.addPriority(i * constant.fireness[7], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		case 8:
			b.addPriority(i * constant.fireness[8], "FireNess=" + b.virtualData[0].getFieryness());
			break;
		}
	}

	protected void E_setPriorityForUnburnedNeighbours(Building b, int i) {
		int num = 0;
		for (Building n : b.realNeighbors_Building()) {
			if (n.virtualData[0].getFieryness() == 0 || n.virtualData[0].getFieryness() == 4) {
				num++;
			}
		}
		b.addPriority(num * i, "UnBurned Neighbours");
	}

	protected void P_setPreExtinguishProrityForLargBuildingsNearSmallFireBuilding(Building b, int priority) {
		double num = 0;
		for (Building n : b.realNeighbors_Building()) {
			if (n.virtualData[0].isBurning()) {
				double d = b.getGroundArea() / n.getGroundArea();
				if (d >= 3d) {
					num += d / 3;
				}
			}
		}
		b.addPriority((int) (num * priority), "PRE_EX_ LARGE_BUILDING_NEAR_SMALL_FIRE_BUILDING");
	}

	protected void EP_setPriorityForDistance(Building b, int priority) {
		//TODO position
		//		ShapeInArea pos = ((FireBrigadeAgent) agent).positioning.getPosition(b);
		//		if (pos != null) {
		//			long cost = agent.move.getWeightTo(((FireBrigadeAgent) agent).positioning.getPosition(b).getArea(), StandardMove.class);
		//			b.addPriority(agent.move.getMovingTimeFrom(cost) * priority, "Distance");
		//		} else
		//		{
		//			b.addPriority(100 * priority, "Null position");
		//		}
		b.addPriority(agent.move.getMovingTimeFrom(b.getFireBuilding().getExtinguishableArea().getCostToCustom()) * priority, "Distance");

	}

	@SuppressWarnings("unchecked")
	protected void P_setPriorityForCriticalTempratureBuildings(Building n, int priority) {

		if (n.virtualData[0].getTemperature() > 30 && n.virtualData[0].getTemperature() < 50)
		{
			if (!n.isMapSide())
			{
				n.addPriority(priority, "CRITICAL_TEMPERATURE (over 30)");
				if (n.virtualData[0].getTemperature() > 40)
				{
					if (agent.getVisibleEntities(Building.class).contains(n))
					{
						if (n.distance((FireBrigade) agent.me()) < AbstractFireBrigadeAgent.maxDistance)
						{
							n.addPriority(priority, "CRITICAL_TEMPERATURE (over 40) And visible");
							if (n.getGroundArea() > 3000)
								n.addPriority(priority, "CRITICAL_TEMPERATURE (over 40 for big building) and visible");
						}
					}
				}
			}
		}
	}

	protected void EP_setPriorityForCenters(int priority, Center c) {
		if (agent.messageSystem.isUsefulCenter(c))
			if (c.virtualData[0].getTemperature() >= 10)
				c.addPriority(priority, "CENETER");
	}

	protected void EP_setPriorityForGasStation(int priority, GasStation c) {
		if (c.virtualData[0].getTemperature() >= 10 && (c.virtualData[0].getFieryness()==0||c.virtualData[0].getFieryness()==4))
			c.addPriority(priority, "GasStation");
	}

	protected void EP_setPriorityForBuildingNotInMapSideBuildings(Building b, int priority) {
		if (!b.isMapSide() || b.getFireBuilding().island().isFireNewInIsland()) {
			b.addPriority(priority, "MAP_SIDE");
		}
	}

	protected void EP_setPriorityForSpread(int priority, SOSEstimatedFireZone site, Building b) {
		if (b.virtualData[0].getFieryness() == 3 && agent.model().time() - b.updatedtime() < 6)
			return;

		int SPREAD_ANGLE=60;

//		if (((FireWorldModel) agent.model()).getInnerOfMap().contains(site.getCenterX(), site.getCenterY()))
//			SPREAD_ANGLE = 120;
//		else
//			SPREAD_ANGLE = 90;

		double x1, y1;
		Pair<Double, Double> spread = site.spread;
		x1 = spread.first();
		y1 = spread.second();

		double length = Math.sqrt(x1 * x1 + y1 * y1);

		priority = (int) (priority * length);

		double a3 = Tools.getAngleBetweenTwoVector(x1, y1, b.getX() - site.getCenterX(), b.getY() - site.getCenterY());
		int x = (int) (Math.abs(a3) / 30d);
		log.info("Building=" + b + "\t zone=" + site + "\tX=" + x + "\ta3=" + a3);
		int coef = 1;
		if (Tools.isBigFire(site))
			coef = 2;
		if (site.getAllBuildings().size() > 50)
		{
			coef = 30;
		}
		b.addPriority((coef * priority / (x + 1)), ("SPREAD X=" + x));

		if (a3 > 2 * SPREAD_ANGLE && site.getAllBuildings().size() > 50) {
			b.addPriority(-2*coef*priority, "FILTER_SPREAD");
		}
	}

	public boolean isBigMap() {
		if (agent.getMapInfo().isBigMap() || agent.getMapInfo().isMediumMap())
			return true;
		return false;
	}

	protected void EP_setPriorityForBuildingsInNewRoadSites(int priority, List<Building> buildings) {
		for (BuildingBlock bb : ((FireWorldModel) agent.model()).buildingBlocks()) {
			if (bb.isFireNewInBuildingBlock())
				for (Building b : buildings) {
					if (bb.insideCoverBuildings().contains(b))
						b.addPriority(priority, "new Road Site");
				}
		}
	}

	protected void EP_setPriorityForUnBurnedIsLands(Building b, int priority) {
		if (!b.getFireBuilding().island().isFireNewInIsland())
			return;
		double coef = 2;
		if (!b.getFireBuilding().island().insideCoverBuildings().contains(b))
			coef = 1.5;

		if (b.virtualData[0].getTemperature() < 20)
			priority /= 2;
		if (b.getFireBuilding().island().isImportant())
			b.addPriority((int) (coef * priority), "PRIORITY_FOR_UNBURNED_ISLAND");
		else
			b.addPriority((int) (coef * priority / 2), "PRIORITY_FOR_UNBURNED_ISLAND");

	}

	protected void E_setPriorityForEarlyIgnitedBuildings(Building b, int priority) {
		if (b.virtualData[0].isBurning() && b.virtualData[0].isExtinguishableInOneCycle(AbstractFireBrigadeAgent.maxPower)) {
			if (b.distance((FireBrigade) agent.me()) < AbstractFireBrigadeAgent.maxDistance)
				b.addPriority(priority, "EARLY IGNITED");
			else
				b.addPriority(priority / 5, "EARLY IGNITED");
		}
	}

	protected void computs() {
		((FireWorldModel) agent.model()).updateBuildingBlocks();
	}

	public static boolean isRighTurn(double x1, double y1, double x2, double y2) {
		double t = (x1 * y2) - (y1 * x2);
		return t < 0;
	}

	protected void EP_setPriorityForBigBuilding(Building b) {//TODO
		//		if (b.getGroundArea() > 3000) {
		//			if (b.virtualData[0].getTemperature() > 100)
		//				b.addPriority(-b.getGroundArea() * 2, "BigArea");
		//			if (b.virtualData[0].getTemperature() < 50)
		//				if (b.distance(model.owner().me()) < AbstractFireBrigadeAgent.maxDistance)
		//					b.addPriority(1300, "BigAreaMinTemp");
		//
		//		}
	}

	private void filterInConvexed(SOSEstimatedFireZone fireSite) {
		//		if (bs.size() > 10) {
		//			Shape convex;
		//			if (bs.size() < 25)
		//				convex = fireSite.getConvex().getScaleConvex(0.6f).getShape();
		//			else
		//				convex = fireSite.getConvex().getScaleConvex(0.8f).getShape();
		//			Building building;
		//			for (Iterator<Building> iterator = bs.iterator(); iterator.hasNext();) {
		//				building = iterator.next();
		//				if (convex.contains(building.getX(), building.getY()))
		//					iterator.remove();
		//			}
		//			for (Iterator<Building> iterator = ns.iterator(); iterator.hasNext();) {
		//				building = iterator.next();
		//				if (convex.contains(building.getX(), building.getY()))
		//					iterator.remove();
		//			}
		//
		//		}

	}

}
