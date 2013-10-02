package mrl.communication.messages.policeMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;


/**
 * User: pooyad
 * Date: Mar 17, 2011
 * Time: 7:52:53 PM
 */
public class PoliceBidMessage extends Message {

    public PoliceBidMessage() {
        super();
    }

    public PoliceBidMessage(int pathID, int value,int importance) {
        this();
        setPropertyValue(PropertyName.PathIdIndex, pathID);
        setPropertyValue(PropertyName.Value, value);
        setPropertyValue(PropertyName.Importance, importance);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.PathIdIndex, 2));
        properties.add(new MessageProperty(PropertyName.Value, 1));
        properties.add(new MessageProperty(PropertyName.Importance, 1));

        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.PoliceBid;
    }

    @Override
    public Priority getPriority() {
        return Priority.VeryHigh;
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
        if (!(message instanceof PoliceBidMessage)) {
            return false;
        }
        return (getPathIdIndex() == ((PoliceBidMessage) message).getPathIdIndex());
    }

    public int getPathIdIndex() {
        return getPropertyValue(PropertyName.PathIdIndex);
    }

    public int getValue() {
        return getPropertyValue(PropertyName.Value);
    }

    public int getImportance() {
        return getPropertyValue(PropertyName.Importance);
    }

}
