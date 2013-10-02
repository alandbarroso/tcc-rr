package mrl.communication.messages.ambulanceMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PDG.
 * Date: Jan 21, 2011
 * Time: 7:27:10 PM
 */
public class AmbulanceAgentBidMessage extends Message {
    public AmbulanceAgentBidMessage() {
        super();
    }

    public AmbulanceAgentBidMessage(int humanID, int bidValue) {
        this();
        setPropertyValue(PropertyName.HumanID, humanID);
        setPropertyValue(PropertyName.bidVAlue, bidValue);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 1));
        properties.add(new MessageProperty(PropertyName.bidVAlue, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.AmbulanceCivilianBid;
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
        if (!(message instanceof AmbulanceAgentBidMessage)) {
            return false;
        }
        return (getVictimAgentId() == ((AmbulanceAgentBidMessage) message).getVictimAgentId());
    }

    public int getVictimAgentId() {
        return getPropertyValue(PropertyName.HumanID);
    }
    public int getCAOP() {
        return getPropertyValue(PropertyName.bidVAlue);

    }
}

