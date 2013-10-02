package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 17, 2011
 * Time: 6:00:14 PM
 */
public class ClearedPathMessage extends Message {
    public ClearedPathMessage() {
        super();
    }

    public ClearedPathMessage(int pathIdIndex) {
        this();
        setPropertyValue(PropertyName.PathIdIndex, pathIdIndex);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.PathIdIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.ClearedPath;
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
        if (!(message instanceof ClearedPathMessage)) {
            return false;
        }
        return (getPathIdIndex() == ((ClearedPathMessage) message).getPathIdIndex());
    }

    public int getPathIdIndex() {
        return getPropertyValue(PropertyName.PathIdIndex);
    }
}
