package mrl.communication.messages.fireBrigadeMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad Salehi
 * Date: 1/5/12
 * Time: 5:23 PM
 */
public class FBGoToClusterMessage extends Message {
    public FBGoToClusterMessage() {
        super();
    }

    public FBGoToClusterMessage(int buildingIdIndex) {
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
        return MessageType.GoToCluster;
    }

    @Override
    public Priority getPriority() {
        return Priority.High;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.FireBrigade;
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
        if (!(message instanceof FBGoToClusterMessage)) {
            return false;
        }
        return (getAreaIdIndex() == ((FBGoToClusterMessage) message).getAreaIdIndex());
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.AreaIdIndex);
    }
}
