package sos.ambulance_v2.decision.states;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.geometry.Point2D;
import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.base.util.information_stacker.CycleInformations;
import sos.base.util.information_stacker.act.MoveAction;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/*
 * @author reyhaneh
 */
public class IAmStuckState extends SOSIState {
	private AmbulanceTeamAgent ambulance =null;
	
	public IAmStuckState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {
		if (infoModel.getModel().time() < 5)
			return null;
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ I'm Stock $$$$$$$$$$$$$$$$$$$$$$$$$");
		List<Point2D> positions = new ArrayList<Point2D>();
		for (int i = 1; i <= 5; i++) {
			CycleInformations act = infoModel.getAgent().informationStacker.getInformations(i);
			if (!(act.getAct() instanceof MoveAction)){
				infoModel.getLog().debug(act +" "+i+" cycle ago was not move! ==>is not stuck");
				return null;
			}

			positions.add(act.getPositionPair().second());
		}
		int numberOfNearPosition = 0;
		for (int i = 0; i < positions.size(); i++) {
			Point2D point2d = positions.get(i);
			int mindistanceToOther = getDistaneToOther(point2d, positions);
			if( mindistanceToOther>4000&&i==0)
				return null;
			if ( mindistanceToOther< 1000)
				numberOfNearPosition++;
		}
		if(numberOfNearPosition>=3){
			infoModel.getLog().warn(" have 3 cycle position near together! ==>is stuck");
			if((infoModel.getATEntity().getWork() != null && infoModel.getATEntity().getWork().getTarget() != null)){
				Human target = infoModel.getATEntity().getWork().getTarget();
				target.getRescueInfo().setIgnoredUntil(IgnoreReason.StuckInUnreachableMode,infoModel.getTime()+10);
				AmbulanceUtils.rejectTarget( target, infoModel.getATEntity(), ambulance);
			}
			else if(ambulance.oldVirtualTarget != null){
				int index = ambulance.oldVirtualTarget.getPosition().getAreaIndex();
				ambulance.lowComAmbDecision.ignoreBuildingUntil[index]=(short)(ambulance.time() +15);
				ambulance.oldVirtualTarget = null;
			}
				
			return null;
		}
		infoModel.getLog().debug(" Not stuck");
		return null;
	}

	private int getDistaneToOther(Point2D point2d, List<Point2D> positions) {
		int result = Integer.MAX_VALUE;
		for (Point2D point : positions) {
			if (point == point2d)
				continue;
			result = Math.min(result, (int) point.distance(point2d));
		}
		return result;
	}

	@Override
	public void giveFeedbacks(List feedbacks) {

	}

	@Override
	public void skipped() {

	}

	@Override
	public void overTaken() {

	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {

	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {

	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

}