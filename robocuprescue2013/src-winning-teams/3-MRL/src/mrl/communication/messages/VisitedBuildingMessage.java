package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 5:33:13 PM
 */
public class VisitedBuildingMessage extends Message {

    public VisitedBuildingMessage() {
        super();
    }

    public VisitedBuildingMessage(int buildingIdIndex) {
        this();
        setPropertyValue(PropertyName.AreaIdIndex, buildingIdIndex);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.AreaIdIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.VisitedBuilding;
    }

    @Override
    public Priority getPriority() {
        return Priority.Medium;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.All;
    }

    @Override
    public TypeOfSend getSendType() {
        return TypeOfSend.VoiceAndRadio;
    }

    @Override
    public int getInitialTTL() {
        return 63;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof VisitedBuildingMessage)) {
            return false;
        }
        return (getAreaIdIndex() == ((VisitedBuildingMessage) message).getAreaIdIndex());
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.AreaIdIndex);
    }
}
