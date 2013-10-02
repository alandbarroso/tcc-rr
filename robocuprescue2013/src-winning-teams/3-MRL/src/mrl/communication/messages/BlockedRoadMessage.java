package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 29, 2011
 * Time: 2:36:38 PM
 */
public class BlockedRoadMessage extends Message {
    public BlockedRoadMessage() {
        super();
    }

    public BlockedRoadMessage(int areaIndexId) {
        this();
        setPropertyValue(PropertyName.AreaIdIndex, areaIndexId);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.AreaIdIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.Block;
    }

    @Override
    public Priority getPriority() {
        return Priority.Medium;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.PoliceForce;
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
        if (!(message instanceof BlockedRoadMessage)) {
            return false;
        }
        return (getAreaIdIndex() == ((BlockedRoadMessage) message).getAreaIdIndex());
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.AreaIdIndex);
    }

}
