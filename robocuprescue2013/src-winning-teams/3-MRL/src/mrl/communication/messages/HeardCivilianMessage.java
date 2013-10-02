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
 * Time: 5:24:36 PM
 */
public class HeardCivilianMessage extends Message {
    public HeardCivilianMessage() {
        super();
    }

    public HeardCivilianMessage(int id, int locationX, int locationY) {
        this();
        setPropertyValue(PropertyName.HumanID, id);
        setPropertyValue(PropertyName.LocationX, locationX);
        setPropertyValue(PropertyName.LocationY, locationY);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 4));
        properties.add(new MessageProperty(PropertyName.LocationX, 3));
        properties.add(new MessageProperty(PropertyName.LocationY, 3));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.HeardCivilian;
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
        return TypeOfSend.OnlyRadio;
    }

    @Override
    public int getInitialTTL() {
        return 0;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof HeardCivilianMessage)) {
            return false;
        }
        return (getCivilianId() == ((HeardCivilianMessage) message).getCivilianId());
    }

    public int getCivilianId() {
        return getPropertyValue(PropertyName.HumanID);
    }

    public int getLocationX() {
        return getPropertyValue(PropertyName.LocationX);
    }

    public int getLocationY() {
        return getPropertyValue(PropertyName.LocationY);
    }
}
