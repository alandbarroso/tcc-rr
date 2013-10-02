package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import mrl.platoon.State;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 17, 2011
 * Time: 4:54:50 PM
 */
public class AgentInfoMessage extends Message {
    public AgentInfoMessage() {
        super();
    }

    public AgentInfoMessage(int positionIndex, State state) {
        this();
        setPropertyValue(PropertyName.StateAndIndex, getIndexAndState(positionIndex, state));
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.StateAndIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.AgentInfo;
    }

    @Override
    public Priority getPriority() {
        return Priority.VeryHigh;
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
        return true;
    }

    private int getIndexAndState(int index, State state) {
        int value;
        int f = state.ordinal();
        value = (f << 14);
        value = (value | index);

        return value;

    }

    public int getAreaIdIndex() {
        int value = getPropertyValue(PropertyName.StateAndIndex);

        return (value & 0x3fff);
    }

    public State getState() {
        int value = getPropertyValue(PropertyName.StateAndIndex);
        value = (value >> 14);
        return State.getState(value);
    }
}
