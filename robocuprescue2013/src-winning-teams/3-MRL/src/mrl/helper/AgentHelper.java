package mrl.helper;

import mrl.helper.info.AgentInfo;
import mrl.helper.info.Types.ActionType;
import mrl.helper.info.Types.AgentState;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Area;

public class AgentHelper implements IHelper {
    AgentInfo agentInfo;
    MrlPlatoonAgent platoonAgent;
    MrlWorld world;

    public AgentHelper(MrlWorld world) {
        this.world = world;
    }

    @Override
    public void init() {
        agentInfo = new AgentInfo();
        agentInfo.setAction(ActionType.NO_ACTION);
        agentInfo.setTarget(null);
        agentInfo.setState(AgentState.STANDING);
        platoonAgent = world.getPlatoonAgent();

    }

    @Override
    public void update() {
        Area destination;
        if (platoonAgent != null) {
            if (platoonAgent.getPathPlanner().getLastMovePlan().isEmpty()) {
                agentInfo.setDestination(null);
            } else {
                destination = world.getEntity(platoonAgent.getPathPlanner().getLastMovePlan().get(platoonAgent.getPathPlanner().getLastMovePlan().size() - 1), Area.class);
                agentInfo.setDestination(destination);
            }
        }
    }

    public AgentInfo getAgentInfo() {
        return agentInfo;
    }
}
