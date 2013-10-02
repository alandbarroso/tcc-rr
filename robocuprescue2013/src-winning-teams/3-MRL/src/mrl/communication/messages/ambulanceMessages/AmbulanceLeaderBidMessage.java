package mrl.communication.messages.ambulanceMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * User: pooyad
 * Date: 5/2/11
 * Time: 6:17 PM
 */
public class AmbulanceLeaderBidMessage extends Message {


    public AmbulanceLeaderBidMessage() {
        super();
    }

    public AmbulanceLeaderBidMessage(int victimID) {
        this();
        setPropertyValue(PropertyName.HumanID, victimID);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.AmbulanceLeaderBid;
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
        return TypeOfSend.OnlyRadio;
    }

    @Override
    public int getInitialTTL() {
        return 0;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof AmbulanceLeaderBidMessage)) {
            return false;
        }
        return (getVictimId() == ((AmbulanceLeaderBidMessage) message).getVictimId());
    }

    public int getVictimId() {
        return getPropertyValue(PropertyName.HumanID);
    }
}
