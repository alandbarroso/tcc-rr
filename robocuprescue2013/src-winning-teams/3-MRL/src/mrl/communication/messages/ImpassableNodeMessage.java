package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created Mostafa Shabani.
 * Date: Mar 15, 2011
 * Time: 11:08:45 PM
 */
public class ImpassableNodeMessage extends Message {
    public ImpassableNodeMessage() {
        super();
    }

    public ImpassableNodeMessage(int nodeId) {
        this();
        setPropertyValue(PropertyName.NodeId, nodeId);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.NodeId, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.ImpassableNode;
    }

    @Override
    public Priority getPriority() {
        return Priority.Medium;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.ATAndFB;
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
        if (!(message instanceof ImpassableNodeMessage)) {
            return false;
        }
        return (getNodeId() == ((ImpassableNodeMessage) message).getNodeId());
    }

    public int getNodeId() {
        return getPropertyValue(PropertyName.NodeId);
    }
}
