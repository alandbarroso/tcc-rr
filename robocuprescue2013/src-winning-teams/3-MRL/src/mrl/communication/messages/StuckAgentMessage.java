package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 17, 2011
 * Time: 5:04:13 PM
 */
public class StuckAgentMessage extends Message {
    public StuckAgentMessage() {
        super();
    }

    public StuckAgentMessage(int positionIndex) {
        this();
        setPropertyValue(PropertyName.AreaIdIndex, positionIndex);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.AreaIdIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.StuckAgent;
    }

    @Override
    public Priority getPriority() {
        return Priority.VeryHigh;
    }

    @Override
    public Receivers getReceivers(Human self) {
        if (self instanceof AmbulanceTeam) {
            return Receivers.PFAndAT;
        } else if (self instanceof FireBrigade) {
            return Receivers.FBAndPF;
        }
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
        return (message instanceof StuckAgentMessage);
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.AreaIdIndex);
    }
}