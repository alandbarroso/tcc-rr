package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 17, 2011
 * Time: 6:22:48 PM
 */
public class CLStuckAgentMessage extends Message {
    public CLStuckAgentMessage() {
        super();
    }

    public CLStuckAgentMessage(int positionIndex, int agentIdIndex) {
        this();
        setPropertyValue(PropertyName.AreaIdIndex, positionIndex);
        setPropertyValue(PropertyName.AgentIdIndex, agentIdIndex);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.AreaIdIndex, 2));
        properties.add(new MessageProperty(PropertyName.AgentIdIndex, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.CLStuckAgent;
    }

    @Override
    public Priority getPriority() {
        return Priority.High;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.PoliceForce;
    }

    @Override
    public TypeOfSend getSendType() {
        return TypeOfSend.OnlyVoice;
    }

    @Override
    public int getInitialTTL() {
        return 63;
    }

    @Override
    public boolean equals(Message message) {
        if (message instanceof CLStuckAgentMessage) {
            return (getAgentIdIndex() == ((CLStuckAgentMessage) message).getAgentIdIndex());
        }
        return false;
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.AreaIdIndex);
    }

    public int getAgentIdIndex() {
        return getPropertyValue(PropertyName.AgentIdIndex);
    }
}
