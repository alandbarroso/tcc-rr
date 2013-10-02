package mrl.communication.messages.ambulanceMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by P.D.G.
 * User: Pooyad
 * Date: Nov 2, 2010
 * Time: 6:26:13 PM
 */

/**
 * This packet will sent, when any AT keep rescuing any civilian
 */
public class CurrentRescuingCivilianMessage extends Message {

    public CurrentRescuingCivilianMessage() {
        super();
    }

    public CurrentRescuingCivilianMessage(int civilianId, int currentTime) {
        this();
        setPropertyValue(PropertyName.HumanID, civilianId);
        setPropertyValue(PropertyName.TimeStep, currentTime);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 2));
        properties.add(new MessageProperty(PropertyName.TimeStep, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.CurrentRescuingCivilianMessage;
    }

    @Override
    public Priority getPriority() {
        return Priority.High;
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
        if (!(message instanceof CurrentRescuingCivilianMessage)) {
            return false;
        }
        return (getCivilianId() == ((CurrentRescuingCivilianMessage) message).getCivilianId());
    }


    public int getCivilianId() {
        return getPropertyValue(PropertyName.HumanID);
    }

    public int getCurrentTime() {
        return getPropertyValue(PropertyName.TimeStep);
    }
}