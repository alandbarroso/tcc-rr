package mrl.communication.messages.ambulanceMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * User: pooyad
 * Date: 4/2/11
 * Time: 5:15 PM
 */
public class AmbulanceCivilianTaskMessage extends Message {

    public AmbulanceCivilianTaskMessage() {
        super();
    }

    public AmbulanceCivilianTaskMessage(int agentID, int victimID) {
        this();
        setPropertyValue(PropertyName.AgentID, agentID);
        setPropertyValue(PropertyName.HumanID, victimID);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.AgentID, 1));
        properties.add(new MessageProperty(PropertyName.HumanID, 1));
        return properties;

    }

    @Override
    public MessageType getType() {
        return MessageType.AmbulanceCivilianTask;
    }

    @Override
    public Priority getPriority() {
        return Priority.VeryHigh;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.AmbulanceTeam;
    }

    @Override
    public TypeOfSend getSendType() {
        return TypeOfSend.OnlyRadio;
    }

    @Override
    public int getInitialTTL() {
        return 0;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof AmbulanceCivilianTaskMessage)) {
            return false;
        }
        return (getAgentID() == ((AmbulanceCivilianTaskMessage) message).getAgentID());
    }

    public int getAgentID() {
        return getPropertyValue(PropertyName.AgentID);
    }

    public int getVictimID() {
        return getPropertyValue(PropertyName.HumanID);
    }

}
