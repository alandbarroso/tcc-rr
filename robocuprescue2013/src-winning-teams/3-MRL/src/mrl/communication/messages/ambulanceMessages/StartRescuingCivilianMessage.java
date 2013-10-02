package mrl.communication.messages.ambulanceMessages;


import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 21, 2010
 * Time: 8:28:51 PM
 */


/**
 * This packet will sent, when any AT Starts rescuing any civilian
 */
public class StartRescuingCivilianMessage extends Message {

    public StartRescuingCivilianMessage() {
        super();
    }

    public StartRescuingCivilianMessage(int civilianId, int startTime) {
        this();
        setPropertyValue(PropertyName.HumanID, civilianId);
        setPropertyValue(PropertyName.TimeStep, startTime);
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
        return MessageType.StartRescuingCivilianMessage;
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
        return 0;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof StartRescuingCivilianMessage)) {
            return false;
        }
        return (getCivilianId() == ((StartRescuingCivilianMessage) message).getCivilianId());
    }

    public int getCivilianId() {
        return getPropertyValue(PropertyName.HumanID);
    }

    public int getStartTime() {
        return getPropertyValue(PropertyName.TimeStep);
    }
}