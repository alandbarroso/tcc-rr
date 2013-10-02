package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.MessageProperty;
import mrl.communication.property.PropertyName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 4:32:51 PM
 */
public class CLHeader extends Header {
    public static int MESSAGE_SIZE = 3;

    public CLHeader() {
        super();
    }

    /**
     * @param messagesType   type of messages in this packet.
     * @param messagesNumber number of messages in this packet.
     * @param ttl            time to live for this packet.
     */
    public CLHeader(int messagesType, int messagesNumber, int ttl) {
        this();
        setPropertyValue(PropertyName.PacketTypeAndHMessageNumber, getPacketTypeHMessageNumber(messagesType, messagesNumber));
        setPropertyValue(PropertyName.LMessageNumberAndTTL, getLMessageNumberAndTTL(messagesNumber, ttl));
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        // MessageType    6b
        // Message Number 4b
        // TTL            6b
        properties.add(new MessageProperty(PropertyName.PacketTypeAndHMessageNumber, 1));
        properties.add(new MessageProperty(PropertyName.LMessageNumberAndTTL, 1));
        return properties;
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
        return (getPacketType() == ((Header) message).getPacketType());
    }
    public int getPacketCycle(int time) {
        return time - (getPacketInitialTTL() - getTTL());
    }

    public MessageType getPacketType() {
        return MessageType.getMessageType(getPropertyValue(PropertyName.PacketTypeAndHMessageNumber) >> 2);
    }

    public int getMessageNumber() {
        int value;

        value = (getPropertyValue(PropertyName.LMessageNumberAndTTL) >> 6);
        value = value | ((getPropertyValue(PropertyName.PacketTypeAndHMessageNumber) & 0x03) << 2);
        return value;
    }

    public int getTTL() {
        return (getPropertyValue(PropertyName.LMessageNumberAndTTL) & 0x3f);
    }

    public int getPacketInitialTTL() {
        return getPacketType().newInstance().getInitialTTL();
    }

    public void setMessageNumber(int number) {
        int type = getPacketType().ordinal();
        int ttl = getTTL();

        setPropertyValue(PropertyName.PacketTypeAndHMessageNumber, getPacketTypeHMessageNumber(type, number));
        setPropertyValue(PropertyName.LMessageNumberAndTTL, getLMessageNumberAndTTL(number, ttl));
    }

    public byte[] getHeaderByteArray() {
        byte[] headerByteArray = new byte[2];
        int type = getPacketType().ordinal();
        int number = getMessageNumber();
        int ttl = getTTL();

        headerByteArray[0] = (byte) (getPacketTypeHMessageNumber(type, number));
        headerByteArray[1] = (byte) (getLMessageNumberAndTTL(number, ttl));

        return headerByteArray;
    }

    private static int getPacketTypeHMessageNumber(int messagesType, int messagesNumber) {
        int value;
        value = messagesType << 2;
        value |= (messagesNumber & 0x0c) >> 2;
        return value;
    }

    private static int getLMessageNumberAndTTL(int messagesNumber, int ttl) {
        int value;
        value = (messagesNumber & 0x03) << 6;
        value |= ttl;
        return value;
    }

    public static int getPacketType(byte[] bytes) {
        int value = bytes[0] & 0xff;
        value = (value >> 2);

        return value;
    }

    public static int getMessageNumber(byte[] bytes) {
        int v1 = bytes[0] & 0xff;
        int v2 = bytes[1] & 0xff;
        int value;
        value = (v1 & 0x03) << 2;
        value = value | (v2 >> 6);

        return value;
    }

    public static int getTTL(byte[] bytes) {
        int value = bytes[1] & 0xff;
        value = (value & 0x3f);

        return value;
    }

    public boolean increaseTTL() {
        int v = getPropertyValue(PropertyName.LMessageNumberAndTTL);
        int ttl = v & 0x3f;
        v = v & 0xc0;
        ttl--;
        if (ttl > 0) {
            v = v | ttl;
            setPropertyValue(PropertyName.LMessageNumberAndTTL, v);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return " type:" + getPacketType() + " number:" + getMessageNumber() + " ttl:" + getTTL();
    }


//---------------------------------------   TEST   -------------------------------------------------------------------------------
//
//    public static byte[] getHeaderByteArray0(int type, int number, int ttl) {
//        byte[] headerByteArray = new byte[3];
//
//        headerByteArray[0] = (byte) (getPacketTypeHMessageNumber(type, number));
//        headerByteArray[1] = (byte) (getLMessageNumberAndTTL(number, ttl));
//
//        return headerByteArray;
//    }
//
//    public static int increaseTTL0(int v) {
//        int ttl = v & 0x3f;
//        v = v & 0xc0;
//        ttl--;
//        if (ttl > 0) {
//            v = v | ttl;
//            return v;
//        }
//        return v;
//    }
//
//    public static int getPacketType0(int v) {
//        return v >> 2;
//    }
//
//    public static int getMessageNumber0(int v1, int v2) {
//        int value;
//        value = v2 >> 6;
//        value = value | ((v1 & 0x03) << 2);
//        return value;
//    }
//
//    public static int getTTL0(int v) {
//        return (v & 0x3f);
//    }
//
//    public static void main(String[] args) {
//        // ttl 0 ~ 63
//        // type 0 ~ 63
//        // no 0 ~ 16
//        byte[] bytes;
//        for (int n = 0; n < 16; n++) {
//            for (int tp = 0; tp < 64; tp++) {
//                bytes = getHeaderByteArray0(tp, n, 63);
//                int ttl = getTTL(bytes);
//
//                int v1 = getPacketTypeHMessageNumber(tp,n);
//                int v2 = getLMessageNumberAndTTL(n, ttl);
//
//                if (getPacketType(bytes) != tp || getPacketType0(v1) != tp) {
//                    getHeaderByteArray0(tp, n, 63);
//                    getPacketType(bytes);
//                    getPacketTypeHMessageNumber(tp,n);
//                    getLMessageNumberAndTTL(n, ttl);
//                    getPacketType0(v1);
//                    throw new RuntimeException("incorrect TYPE: tp = " + tp + " n = " + n + " ttl = " + ttl);
//                }
//                if (getMessageNumber(bytes) != n || getMessageNumber0(v1, v2) != n) {
//                    getHeaderByteArray0(tp, n, 63);
//                    getMessageNumber(bytes);
//                    getPacketTypeHMessageNumber(tp,n);
//                    getLMessageNumberAndTTL(n, ttl);
//                    getMessageNumber0(v1, v2);
//                    throw new RuntimeException("incorrect MESSAGE NUMBER: tp = " + tp + " n = " + n + " ttl = " + ttl);
//                }
//                if (getTTL(bytes) != ttl || getTTL0(v2) != ttl) {
//                    getHeaderByteArray0(tp, n, 63);
//                    getTTL(bytes);
//                    getPacketTypeHMessageNumber(tp,n);
//                    getLMessageNumberAndTTL(n, ttl);
//                    getTTL0(v2);
//                    throw new RuntimeException("incorrect TTL: tp = " + tp + " n = " + n + " ttl = " + ttl);
//                }
//                while (ttl > 0) {
//                    ttl = increaseTTL0(ttl);
//                }
//            }
//        }
//    }

}