package mrl.helper.info;

import mrl.communication.Message;
import mrl.helper.info.Types.ActionType;
import mrl.helper.info.Types.AgentState;
import mrl.helper.info.Types.TargetType;
import rescuecore2.standard.entities.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Mahdi Taherian
 */
public class AgentInfo {
    private AgentState state;
    private TargetType targetType;
    private ActionType action;
    private StandardEntity target;
    private Area destination;
    private Set<Message> messagesSent;
    private Set<Message> messagesReceived;

    public void setState(AgentState state) {
        this.state = state;
        messagesReceived = new HashSet<Message>();
        messagesSent = new HashSet<Message>();
    }

    public void setTarget(StandardEntity target) {
        if (target == null) {
            targetType = TargetType.UNDEFINED;
        } else if (target instanceof Civilian) {
            targetType = TargetType.CIVILIAN;
        } else if (target instanceof Human) {
            targetType = TargetType.AGENT;
        } else if (target instanceof Building) {
            targetType = TargetType.BUILDING;
        } else if (target instanceof Road) {
            targetType = TargetType.ROAD;
        } else if (target instanceof Blockade) {
            targetType = TargetType.BLOCKADE;
        }
        this.target = target;
    }

    public void setDestination(Area destination) {
        this.destination = destination;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public void addMessageSent(Message message){
        messagesSent.add(message);
    }

    public void addMessageReceived(Message message){
        messagesReceived.add(message);
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public AgentState getState() {
        return state;
    }

    public ActionType getAction() {
        return action;
    }

    public Area getDestination() {
        return destination;
    }

    public Set<Message> getMessagesSent() {
        return messagesSent;
    }

    public Set<Message> getMessagesReceived() {
        return messagesReceived;
    }

    public StandardEntity getTarget() {
        return target;
    }
}

