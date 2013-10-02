package sos.fire_v2.position;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JTextArea;

import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.move.Move;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.AbstractFireBrigadeAgent;
import sos.fire_v2.base.worldmodel.FireWorldModel;

public class Positioing {

	private FireWorldModel world;
	private FireBrigadeAgent me;
	private Move move;
	private SOSLoggerSystem log;
	public JTextArea datas = new JTextArea();
	/**
	 * moteghayere sabet baraye inke age update time male chand cycel ghabl bashe ta bere bebine bezane ya bezane
	 */
	public static int maxUnupdatedToEx = 6;

	public Positioing(FireBrigadeAgent fireBrigadeAgent, FireWorldModel model) {
		this.me = fireBrigadeAgent;
		this.world = model;
		move = me.move;
		log = new SOSLoggerSystem(world.me(), "Positioning/", true, OutputType.File, true);
		fireBrigadeAgent.sosLogger.addToAllLogType(log);
		datas.setFont(new Font("arial", Font.PLAIN, 20));
	}

	public ShapeInArea getPosition(Building building) {
		if (isNoComm() || isLowComm())
		{
			ShapeInArea sh = getVisibilityPosition(building);
			if (sh == null) {
				return getGreedyPosition(building);
			}
			return sh;
		}
		datas.append("\n-----------------------------\nTime = " + me.model().time());
		log.info("Position for " + building);

		if (isUpdate(building)) {
			log.info("building is update" + building);
			datas.append("\n Building is update");
			return getGreedyPosition(building);
		}

		int dupd = me.model().time() - building.updatedtime();

		long visibilityCost = move.getWeightToLowProcess(building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea(), StandardMove.class);
		long greedyCost = building.getFireBuilding().getExtinguishableArea().getCostToCustom();

		int greedyTime = move.getMovingTimeFrom(greedyCost);
		int visibilityTime = move.getMovingTimeFrom(visibilityCost);

		datas.append("\nVisibility Cost = " + visibilityCost);
		datas.append("\nVisibility time = " + visibilityTime);
		datas.append("\nGreedy Cost = " + greedyCost);
		datas.append("\nGreedy Time = " + greedyTime);
		datas.append("\ndUpd = " + dupd);

		log.info("Visibility Cost = " + visibilityCost);
		log.info("Visibility time = " + visibilityTime);
		log.info("Greedy Cost = " + greedyCost);
		log.info("Greedy Time = " + greedyTime);
		log.info("dUpd = " + dupd);

		if (visibilityCost < Move.UNREACHABLE_COST) {
			log.info("visible is reachable");
			try {
				if (visibilityCost / greedyCost > dupd) {
					log.info("visibility high cost greedy");
					return getGreedyPosition(building);
				}
			} catch (Exception e) {
				log.info("Exception " + e.getMessage());
				if (visibilityTime - greedyTime > 5) {
					log.info("move time greedy");
					return getGreedyPosition(building);
				}
			}
			log.info("visibility is good");
			return getVisibilityPosition(building);
		}
		log.info("visible is not reachable");

		return getGreedyPosition(building);

	}

	public void newPsitioning(Building building) throws SOSActionException {
		datas.append("\n-----------------------------\nTime = " + me.model().time());
		log.info("Position for " + building);

		if (isUpdate(building)) {
			log.info("building is update" + building);
			datas.append("\n Building is update");
			greedyPositioning(building);
			return;
		}

		if (isNoComm() || isLowComm())
		{
			visiblityPositioning(building);
			return;
		}

		int dupd = me.model().time() - building.updatedtime();

		long visibilityCost = move.getWeightToLowProcess(building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea(), StandardMove.class);
		long greedyCost = building.getFireBuilding().getExtinguishableArea().getCostToCustom();

		int greedyTime = move.getMovingTimeFrom(greedyCost);
		int visibilityTime = move.getMovingTimeFrom(visibilityCost);

		datas.append("\nVisibility Cost = " + visibilityCost);
		datas.append("\nVisibility time = " + visibilityTime);
		datas.append("\nGreedy Cost = " + greedyCost);
		datas.append("\nGreedy Time = " + greedyTime);
		datas.append("\ndUpd = " + dupd);

		log.info("Visibility Cost = " + visibilityCost);
		log.info("Visibility time = " + visibilityTime);
		log.info("Greedy Cost = " + greedyCost);
		log.info("Greedy Time = " + greedyTime);
		log.info("dUpd = " + dupd);

		if (visibilityCost < Move.UNREACHABLE_COST) {
			log.info("visible is reachable");
			try {
				if (visibilityTime / greedyTime > dupd) {
					log.info("visibility high cost greedy");
					greedyPositioning(building);
					return;
				}
			} catch (SOSActionException e) {
				throw e;
			} catch (Exception e) {
				log.info("Exception " + e.getMessage());
				visiblityPositioning(building);
				return;
			}
			log.info("visibility is good");
			visiblityPositioning(building);
			return;
		}
		log.info("visible is not reachable");
		greedyPositioning(building);
		return;
	}

	private boolean isLowComm() {
		return me.messageSystem.type == Type.LowComunication;
	}

	private boolean isNoComm() {
		return me.messageSystem.type == Type.NoComunication;
	}

	private ShapeInArea getVisibilityPosition(Building building) {
		log.info("Visibility Position");

		ArrayList<ShapeInArea> inRoad = building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea();
		ArrayList<ShapeInArea> temp = new ArrayList<ShapeInArea>();
		long minRoad = Integer.MAX_VALUE;
		ShapeInArea bestRoad = null;
		log.info("Shape in Area " + inRoad.size());
		for (ShapeInArea sh : inRoad) {
			if (!(sh.getArea() instanceof Road))
				continue;
			log.info("\t\t Shape" + sh + "   Area : " + sh.getArea());

			temp.clear();
			temp.add(sh);
			long cost = move.getWeightTo(temp, StandardMove.class);
			if (cost >= MoveConstants.UNREACHABLE_COST) {
				log.info("\t\t\t Unreachable");
				continue;
			}
			if (cost < minRoad) {
				minRoad = cost;
				bestRoad = sh;
			}
		}

		long minBuilding = Integer.MAX_VALUE;
		ShapeInArea bestBuilding = null;
		for (ShapeInArea sh : inRoad) {
			if (sh.getArea() instanceof Road)
				continue;
			if (((Building) sh.getArea()).virtualData[0].isBurning()) {
				log.info("\t\t\t Building is Burning");
				continue;
			}

			log.info("\t\t Shape" + sh + "   Area : " + sh.getArea());
			temp.clear();
			temp.add(sh);
			long cost = move.getWeightTo(temp, StandardMove.class);
			if (cost >= MoveConstants.UNREACHABLE_COST) {
				log.info("\t\t\t Unreachable");
				continue;
			}
			if (cost < minBuilding) {
				minBuilding = cost;
				bestBuilding = sh;
			}

		}
		if (bestRoad != null) {
			if (bestBuilding == null)
				return bestRoad;
			return minRoad <= 1.5 * minBuilding ? bestRoad : bestBuilding;
		}
		return bestBuilding;
	}

	private ShapeInArea getGreedyPosition(Building building) {

		Area loc = me.me().getAreaPosition();
		if (loc instanceof Road) {
			if (canExtinguish(building))
				return new ShapeInArea(loc.getApexList(), loc);
		} else {
			Building b = (Building) loc;
			if (!b.isBurning() && canExtinguish(building))
				return new ShapeInArea(loc.getApexList(), loc);
		}

		ArrayList<ShapeInArea> inRoad = building.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea();
		ArrayList<ShapeInArea> temp = new ArrayList<ShapeInArea>();
		long minRoad = Integer.MAX_VALUE;
		ShapeInArea bestRoad = null;
		log.info("Greedy Position");

		log.info("Shape in Road " + inRoad.size());
		for (ShapeInArea sh : inRoad) {
			log.info("\t\t Shape" + sh + "   Area : " + sh.getArea());
			long cost = 0;

			if (me.me().getAreaPosition().equals(sh.getArea()) && canExtinguish(building)) {
				cost = 0;
			} else {
				temp.clear();
				temp.add(sh);
				cost = move.getWeightTo(temp, StandardMove.class);
				if (cost >= MoveConstants.UNREACHABLE_COST) {
					log.info("\t\t\t Unreachable");
					continue;
				}
			}
			if (cost < minRoad) {
				minRoad = cost;
				bestRoad = sh;
			}
		}

		ArrayList<ShapeInArea> inBuilding = building.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea();
		log.info("Shape in Building " + inRoad.size());

		long minBuilding = Integer.MAX_VALUE;
		ShapeInArea bestBuilding = null;
		for (ShapeInArea sh : inBuilding) {
			log.info("\t\t Shape" + sh + "   Area : " + sh.getArea());

			if (((Building) sh.getArea()).virtualData[0].isBurning()) {
				log.info("\t\t\t Building is Burning");
				continue;
			}

			long cost = 0;
			if (me.me().getAreaPosition().equals(sh.getArea()) && canExtinguish(building))
			{
				cost = 0;
			} else {
				temp.clear();
				temp.add(sh);
				cost = move.getWeightTo(temp, StandardMove.class);
				if (cost >= MoveConstants.UNREACHABLE_COST) {
					log.info("\t\t\t Unreachable");
					continue;
				}
			}
			if (cost < minBuilding) {
				minBuilding = cost;
				bestBuilding = sh;
			}

		}
		log.info("Best Road " + bestRoad + "   " + minRoad + "   best building " + bestBuilding + "  " + minBuilding);

		if (bestRoad != null) {
			if (bestBuilding == null)
				return bestRoad;
			return minRoad <= 1.5 * minBuilding ? bestRoad : bestBuilding;
		}
		return bestBuilding;
	}

	private void position(Building building) throws SOSActionException {

		datas.append("\n-----------------------------\nTime = " + me.model().time());

		if (building.updatedtime() <= 1)
			visiblityPositioning(building);

		if (isUpdate(building)) {
			datas.append("\n Building is update");
			greedyPositioning(building);
		}

		int dupd = me.model().time() - building.updatedtime();

		long visibilityCost = move.getWeightToLowProcess(building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea(), StandardMove.class);

		long greedyCost = building.getFireBuilding().getExtinguishableArea().getCostToCustom();

		int visibilityTime = move.getMovingTimeFrom(visibilityCost);

		int greedyTime = move.getMovingTimeFrom(greedyCost);

		if (isUpdate(building)) {
			datas.append("\n Building is update");
			greedyPositioning(building);
		}

		if (isUpdate(building)) {
			datas.append("\n Building is update");
			greedyPositioning(building);
		}

		//		if(greedyTime)
		//		if(visibilityTime/gre

		//				move.getWeightToLowProcess(building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea(), StandardMove.class);

		//		else {
		//			datas.append("\n Building is not update");
		//			visiblityPositioning(building);
		//		}
	}

	Random rn = new Random(20);

	private boolean isUpdate(Building building) {
		if (me.model().time() - building.updatedtime() < Math.min(me.model().time(), 5))
			return true;
		return false;
	}

	private void visiblityPositioning(Building building) throws SOSActionException {
		if (getVisibleBuilding().contains(building) && canExtinguish(building))
			return;

		if (move.getWeightToLowProcess(building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea(), StandardMove.class) < MoveConstants.UNREACHABLE_COST) {
			log.info("buildinh has reachabe shape for ex");
			datas.append("buildinh has reachabe shape for ex");
			move.moveToShape(building.getFireBuilding().getExtinguishableArea().getExtinguishableSensibleArea(), StandardMove.class);//TODO building
		}
		log.info("building hasnt reachable shape ::> greedy");

		greedyPositioning(building);
	}

	private boolean canExtinguish(Building building) {
		return (sos.tools.Utils.distance(me.me().getX(), me.me().getY(), building.x(), building.y()) <= AbstractFireBrigadeAgent.maxDistance);
	}

	public void greedyPositioning(Building building) throws SOSActionException {
		if (!canExtinguish(building)) {//TODO shayad bere Building
			//				move.moveToShape(building.fireSearchBuilding().sensibleAreasOfAreas(), StandardMove.class);
			if (building.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea().size() > 0)
			{
				if (move.getWeightTo(building.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea(), StandardMove.class) < MoveConstants.UNREACHABLE_COST)
					move.moveToShape(building.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea(), StandardMove.class);
				else if (move.getWeightTo(building.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea(), StandardMove.class) < MoveConstants.UNREACHABLE_COST)
					move.moveToShape(building.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea(), StandardMove.class);
				move.moveToShape(building.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea(), StandardMove.class);
			} else {
				move.moveToShape(building.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea(), StandardMove.class);

			}
		}
	}

	private void add(String string) {
		datas.append(string + "\n");
	}

	private ArrayList<Building> getVisibleBuilding() {
		return me.getVisibleEntities(Building.class);
	}

}