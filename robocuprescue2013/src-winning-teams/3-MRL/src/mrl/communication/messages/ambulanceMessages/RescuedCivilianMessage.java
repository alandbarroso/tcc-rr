package mrl.communication.messages.ambulanceMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 17, 2011
 * Time: 5:56:39 PM
 */
public class RescuedCivilianMessage extends Message {
    public RescuedCivilianMessage() {
        super();
    }

    public RescuedCivilianMessage(int civilianId) {
        this();
        setPropertyValue(PropertyName.HumanID, civilianId);
//        setPropertyValue(PropertyName.HealthPoint, hP);
//        setPropertyValue(PropertyName.TotalATsInThisRescue, totalATsInThisRescue);
//        setPropertyValue(PropertyName.TotalRescueTime, totalRescueTime);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 1));
//        properties.add(new MessageProperty(PropertyName.HealthPoint, 2));
//        properties.add(new MessageProperty(PropertyName.TotalATsInThisRescue, 1));
//        properties.add(new MessageProperty(PropertyName.TotalRescueTime, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.RescuedCivilian;
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
        return TypeOfSend.VoiceAndRadio;
    }

    @Override
    public int getInitialTTL() {
        return 20;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof RescuedCivilianMessage)) {
            return false;
        }
        return (getCivilianId() == ((RescuedCivilianMessage) message).getCivilianId());
    }

    public int getCivilianId() {
        return getPropertyValue(PropertyName.HumanID);
    }

//    public int getHP() {
//        return getPropertyValue(PropertyName.HealthPoint);
//    }

//    public int getTotalATsInThisRescue() {
//        return getPropertyValue(PropertyName.TotalATsInThisRescue);
//    }
//
//    public int getTotalRescueTime() {
//        return getPropertyValue(PropertyName.TotalRescueTime);
//    }
}
