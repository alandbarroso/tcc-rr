package mrl.communication.messages.ambulanceMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;


/**
 * User: PooyaDG
 * Date: Jan 23, 2011
 * Time: 11:08:25 AM
 */
public class LoaderMessage extends Message {

    public LoaderMessage() {
        super();
    }

    public LoaderMessage(int civilianID) {
        this();
        setPropertyValue(PropertyName.HumanID, civilianID);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.LoaderMessage;
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
        if (!(message instanceof LoaderMessage)) {
            return false;
        }
        return (getCivilianId() == ((LoaderMessage) message).getCivilianId());
    }

    public int getCivilianId() {
        return getPropertyValue(PropertyName.HumanID);
    }

}
