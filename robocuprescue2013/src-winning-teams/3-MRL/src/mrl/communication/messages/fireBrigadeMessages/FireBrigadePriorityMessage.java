package mrl.communication.messages.fireBrigadeMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Vahid Hoosahngi
 * Date: 5/15/11
 * Time: 4:30 PM
 */
public class FireBrigadePriorityMessage extends Message {
    public FireBrigadePriorityMessage() {
        super();
    }

    public FireBrigadePriorityMessage(int zoneId, int civCount, int civID) {
        this();
        setPropertyValue(PropertyName.ZoneId, zoneId);
        setPropertyValue(PropertyName.CivilianCount, civCount);
        setPropertyValue(PropertyName.CivilianID, civID);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.ZoneId, 1));
        properties.add(new MessageProperty(PropertyName.CivilianCount, 1));
        properties.add(new MessageProperty(PropertyName.CivilianID, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.FireBrigadePriority;
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
        if (!(message instanceof FireBrigadePriorityMessage)) {
            return false;
        }
        return (getZoneId() == ((FireBrigadePriorityMessage) message).getZoneId());
    }

    public int getZoneId() {
        return getPropertyValue(PropertyName.ZoneId);
    }

    public int getCivilianCount() {
        return getPropertyValue(PropertyName.CivilianCount);
    }

    public int getCivilianID() {
        return getPropertyValue(PropertyName.CivilianID);
    }

}
