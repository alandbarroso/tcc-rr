package mrl.communication.messages;


import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 5, 2011
 * Time: 11:30:14 AM
 */
public class ChannelScannerMessage extends Message {

    public ChannelScannerMessage() {
        super();
    }

    public ChannelScannerMessage(int channel, int agentInThis, int repeatForOne, int repeatForTwo, int repeatForThree, int repeatForFour) {
        this();
        setPropertyValue(PropertyName.ChannelIdAndAgentTypeInThisChannel, getSingleValue(channel, agentInThis));
        setPropertyValue(PropertyName.repeatForPriorityOneAndTwo, getSingleValue(repeatForOne, repeatForTwo));
        setPropertyValue(PropertyName.repeatForPriorityThreeAndFour, getSingleValue(repeatForThree, repeatForFour));
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.ChannelIdAndAgentTypeInThisChannel, 1));
        properties.add(new MessageProperty(PropertyName.repeatForPriorityOneAndTwo, 1));
        properties.add(new MessageProperty(PropertyName.repeatForPriorityThreeAndFour, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.ChannelScannerMessage;
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

    private static int getSingleValue(int value1, int value2) {
        int value;

        value = (value1 << 4);
        value = (value | value2);

        return value;
    }

    private static int getFirstValue(int value) {
        return (value >> 4);
    }

    private static int getSecondValue(int value) {
        return (value & 15);
    }

    public int getChannelId() {
        return getFirstValue(getPropertyValue(PropertyName.ChannelIdAndAgentTypeInThisChannel));
    }

    public int getAgentInThisChannel() {
        return getSecondValue(getPropertyValue(PropertyName.ChannelIdAndAgentTypeInThisChannel));
    }

    public int getRepeatForPriorityOne() {
        return getFirstValue(getPropertyValue(PropertyName.repeatForPriorityOneAndTwo));
    }

    public int getRepeatForPriorityTwo() {
        return getSecondValue(getPropertyValue(PropertyName.repeatForPriorityOneAndTwo));
    }

    public int getRepeatForPriorityThree() {
        return getFirstValue(getPropertyValue(PropertyName.repeatForPriorityThreeAndFour));
    }

    public int getRepeatForPriorityFour() {
        return getSecondValue(getPropertyValue(PropertyName.repeatForPriorityThreeAndFour));
    }

}
