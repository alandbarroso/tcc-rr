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
 * Date: Oct 23, 2010
 * Time: 6:39:42 PM
 */
public class ValueFunctionMessage extends Message {

    public ValueFunctionMessage() {
        super();
    }

    public ValueFunctionMessage(int time, int valueFunction) {
        this();
        setPropertyValue(PropertyName.TimeStep, time);
        setPropertyValue(PropertyName.ValueFunction, valueFunction);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.TimeStep, 2));
        properties.add(new MessageProperty(PropertyName.ValueFunction, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.ValueFunctionMessage;
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
        return TypeOfSend.OnlyRadio;
    }

    @Override
    public int getInitialTTL() {
        return 0;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof ValueFunctionMessage)) {
            return false;
        }
        return (getTime() == ((ValueFunctionMessage) message).getTime());
    }

    public int getTime() {
        return getPropertyValue(PropertyName.TimeStep);
    }

    public int getValueFunction() {
        return getPropertyValue(PropertyName.ValueFunction);
    }
}
