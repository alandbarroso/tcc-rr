package sos.ambulance_v2.decision;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.decision.states.CenterAssignedTask;
import sos.ambulance_v2.decision.states.CivilianSearchState;
import sos.ambulance_v2.decision.states.DeadState;
import sos.ambulance_v2.decision.states.FireSearchState;
import sos.ambulance_v2.decision.states.HelpState;
import sos.ambulance_v2.decision.states.IAmHurtState;
import sos.ambulance_v2.decision.states.IAmStuckState;
import sos.ambulance_v2.decision.states.LowCommunicationStartegyState;
import sos.ambulance_v2.decision.states.RunAwayFromGassStationState;
import sos.ambulance_v2.decision.states.SelfTaskAssigningState;
import sos.ambulance_v2.decision.states.SpecialSelfTaskAssigningState;
import sos.tools.decisionMaker.implementations.stateBased.SOSStateBasedDecisionMaker;
import sos.tools.decisionMaker.implementations.stateBased.StateFeedbackFactory;

public class AmbulanceDecisionMaker extends SOSStateBasedDecisionMaker {

	private final AmbulanceTeamAgent agent;
	private AmbulanceDecision ambDecision;

	public AmbulanceDecisionMaker(AmbulanceTeamAgent agent, StateFeedbackFactory feedbackFactory) {
		super(agent, feedbackFactory, AmbulanceInformationModel.class);
		this.agent = agent;
	}

	@Override
	public void initiateStates() {
		getThinkStates().add(new DeadState(infoModel));
		getThinkStates().add(new IAmHurtState(getInfoModel()));
		getThinkStates().add(new IAmStuckState(getInfoModel()));
		getThinkStates().add(new RunAwayFromGassStationState(getInfoModel()));
		getThinkStates().add(new CenterAssignedTask(getInfoModel()));
		getThinkStates().add(new SpecialSelfTaskAssigningState(getInfoModel()));
		getThinkStates().add(new LowCommunicationStartegyState(getInfoModel()));
		getThinkStates().add(new SelfTaskAssigningState(getInfoModel()));
		getThinkStates().add(new FireSearchState(getInfoModel()));
		getThinkStates().add(new CivilianSearchState(getInfoModel()));
		getThinkStates().add(new HelpState(getInfoModel()));
//		getThinkStates().add(new ferociousState(getInfoModel()));
	}

	public AmbulanceInformationModel getInfoModel() {
		return (AmbulanceInformationModel) infoModel;
	}
}