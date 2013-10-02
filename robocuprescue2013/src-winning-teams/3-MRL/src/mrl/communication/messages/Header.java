package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 4:32:51 PM
 */
public class Header extends Message {

    public Header() {
        super();
    }

    /**
     * @param cycle:        time
     * @param messagesType: noe message haye in packet.
     */
    public Header(int cycle, int messagesType) {
        this();
        setPropertyValue(PropertyName.PacketCycle, cycle);
        setPropertyValue(PropertyName.PacketType, messagesType);
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.PacketCycle, 1));
        properties.add(new MessageProperty(PropertyName.PacketType, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.PacketHeader;
    }

    @Override
    public Priority getPriority() {
        return Priority.High;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.All;
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
        if (!(message instanceof Header)) {
            return false;
        }
        return (getPacketType() == ((Header) message).getPacketType() && getPacketCycle(0) == ((Header) message).getPacketCycle(0));
    }

    public int getPacketCycle(int time) {
        return getPropertyValue(PropertyName.PacketCycle);
    }

    public MessageType getPacketType() {
        return MessageType.getMessageType(getPropertyValue(PropertyName.PacketType));
    }

    public byte[] getHeaderByteArray() {
        byte[] headerByteArray = new byte[2];
        int time = getPacketCycle(0);
        int type = getPacketType().ordinal();

        headerByteArray[1] = (byte) (type << 2);
        headerByteArray[0] = (byte) (time & 0xff);
        headerByteArray[1] = (byte) (headerByteArray[1] | (time >> 8));

        return headerByteArray;
    }

    public static int getTime(byte[] bytes) {
        int time;
        int value0 = bytes[0] & 0xff;
        int value1 = bytes[1] & 0xff;
        time = (value1 & 0x03);
        time = (time << 8);
        time = (time | value0);
        return time;
    }

    public static int getType(byte[] bytes) {
        int type;
        int value1 = bytes[1] & 0xff;

        type = (value1 >> 2);
        return type;
    }

    @Override
    public String toString() {
        return "cycle:" + getPacketCycle(0) + " type:" + getPacketType();
    }
}
