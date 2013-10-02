package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Erfan Jazeb Nikoo
 */
public class EmptyBuildingMessage extends Message {

    public EmptyBuildingMessage() {
        super();
    }

    public EmptyBuildingMessage(int buildingID) {
        this();
        setPropertyValue(PropertyName.EmptyBuildingID, buildingID);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.EmptyBuildingID, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.EmptyBuilding;
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
        return TypeOfSend.OnlyVoice;
    }

    @Override
    public int getInitialTTL() {
        return 63;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof EmptyBuildingMessage)) {
            return false;
        }
        return (getAreaIdIndex() == ((EmptyBuildingMessage) message).getAreaIdIndex());
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.EmptyBuildingID);
    }
}