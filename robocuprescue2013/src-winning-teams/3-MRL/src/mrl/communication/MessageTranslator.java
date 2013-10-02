package mrl.communication;

import mrl.communication.messages.CLHeader;
import mrl.communication.messages.Header;
import mrl.communication.property.MessageProperty;
import rescuecore2.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 3:30:47 PM
 */
public class MessageTranslator {

    public static byte[] getByteArray(Packet packet) {
        int size = 0;
        size += packet.getPacketByteArraySize();
        byte[] bytes = new byte[size];
        System.arraycopy(packet.getHeader().getHeaderByteArray(), 0, bytes, 0, packet.getHeader().getMessageByteArraySize());
        int index = packet.getHeader().getMessageByteArraySize();
        for (Message message : packet) {
            byte[] msg = getMessageByteList(message);
            System.arraycopy(msg, 0, bytes, index, msg.length);
            index += msg.length;
        }
        return bytes;
    }

    private static byte[] getMessageByteList(Message message) {

        byte[] bytes = new byte[message.getMessageByteArraySize()];
        int index = 0;
        for (MessageProperty property : message.getProperties()) {
            int byteNeeded = property.getByteNeeded();
            int value = property.getValue();

            while (byteNeeded > 0) {
                byte v = (byte) (value & 0xff);
                bytes[index++] = v;
                byteNeeded--;
                value = value >> 8;
            }
        }
        return bytes;
    }

    /**
     * this method yek message be size 1 ro baraye scan channel ijad mikone.
     * albate faghat type-e message ro mide.
     *
     * @param messageT is a type of message
     * @return message byte array
     */
    public static byte[] getSimpleByteArray(MessageType messageT) {
        byte[] packetByteArray = new byte[1];

        int typeValue = messageT.ordinal();
        packetByteArray[0] = (byte) (typeValue & 0xff);

        return packetByteArray;
    }

    public static Packet getPacket(byte[] packetByteArray) {

        Packet packet;
        int index = 0;
        Header header = getHeader(packetByteArray);
        index += header.getMessageByteArraySize();
        MessageType messType = header.getPacketType();

        if (messType == null) {
            Logger.error(" message type is null: MessageTranslator.getMessage() type:" + messType);
            return null;
        }

        packet = new Packet(header, messType.getPriority(), messType.getReceivers(null), packetByteArray.length);

        while (packetByteArray.length > index) {
            Message message;
            message = messType.newInstance();

            if (message == null) {
                Logger.error(" message newInstance is null: MessageTranslator.getMessage()");
                continue;
            }
            packet.add(getMessage(packetByteArray, index, message));
            index += message.getMessageByteArraySize();

        }

        return packet;
    }

    public static List<Packet> getSayPacket(byte[] packetByteArray) {

        List<Packet> receivedPackets = new ArrayList<Packet>();

        Packet packet;
        int index;
        int packetIndex = 0;
        int remainedPacketSize = packetByteArray.length;
        byte[] received = packetByteArray;

        while (received.length > 0) {

            CLHeader header = getCLHeader(received);
            if(header==null){
                break;
            }
            index = header.getMessageByteArraySize();
            MessageType messType = header.getPacketType();

            if (messType == null) {
                Logger.error("ERROR : message type is null: MessageTranslator.getSayPacket() header:" + header);
               //TODO: recover it
                //printBytes(received, header, new NullPointerException(), "MessageTranslator.getSayPacket");
                return new ArrayList<Packet>();
            }
//            String s = "";
//            for (byte b : received) {
//                s += String.valueOf(b) + ", ";
//            }
//            System.err.println(world.getPlatoonAgent().getDebugString()+" --------RECEIVE:" + " byte Array = " + s);

            packet = new Packet(header, messType.getPriority(), messType.getReceivers(null), header.getMessageNumber() * header.getPacketType().getByteArraySize());

            for (int i = 0; i < header.getMessageNumber(); i++) {
                Message message;
                message = messType.newInstance();

                if (message == null) {
                    Logger.error(" message newInstance is null: MessageTranslator.getMessage()");
                    continue;
                }
                packet.add(getMessage(received, index, message));
                index += message.getMessageByteArraySize();

            }
            receivedPackets.add(packet);
            packetIndex += index;
            remainedPacketSize -= index;
            if (remainedPacketSize <= 0) {
                received = new byte[0];
            } else {
                received = new byte[remainedPacketSize];
                System.arraycopy(packetByteArray, packetIndex, received, 0, remainedPacketSize);
            }
        }
        return receivedPackets;
    }

    /**
     * methodi ke header-e packet ro dorost mikone.
     *
     * @param messageByteArray: byte array-e daryaft shode.
     * @return packet header.
     */
    public static Header getHeader(byte[] messageByteArray) {
        Header header;

        int cycle = Header.getTime(messageByteArray);
        int type = Header.getType(messageByteArray);

        header = new Header(cycle, type);

        return header;
    }

    public static CLHeader getCLHeader(byte[] messageByteArray) {
        CLHeader header;
        try {
            int type = CLHeader.getPacketType(messageByteArray);
            int number = CLHeader.getMessageNumber(messageByteArray);
            int ttl = CLHeader.getTTL(messageByteArray);
            header = new CLHeader(type, number, ttl);
        }catch (Exception ex){
            System.out.println("Exception:  CLHeader Exception.....");
            return null;
        }


        return header;
    }

    /**
     * this method yek message az noee ke behesh dade shode dorost mikone.
     *
     * @param messageByteArray: byte array-e daryaft shode.
     * @param index:            index-e shoroo-e in message.
     * @param message:          yek message khali. ke gharare inja por beshe.
     * @return complete message.
     */
    private static Message getMessage(byte[] messageByteArray, int index, Message message) {

        for (MessageProperty property : message.getProperties()) {
            int byteNeeded = property.getByteNeeded();
            try {
                int value = getValue(messageByteArray, index, byteNeeded);
                message.setPropertyValue(property.getPropertyName(), value);
            } catch (Exception e) {
                //TODO: recover it
//                printBytes(messageByteArray, message, e, "MessageTranslator.getMessage");
            }
            index += byteNeeded;
        }

        return message;
    }

    public static int getValue(byte[] bytes, int firstIndex, int byteNeeded) throws Exception {
        byte[] tempBytes = new byte[byteNeeded];

        System.arraycopy(bytes, firstIndex, tempBytes, 0, byteNeeded);
        return getValue(tempBytes);
    }

    public static Integer getValue(byte[] bytes) {
        int value = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            byte b = bytes[i];
            value = value << 8;
            int v = b & 0xff;
            value += v;
        }
        return value;
    }

    private static void printBytes(byte[] bytes, Object message, Exception e, String method) {
        String s = "";
        s += "EXCEPTION in " + method + ": " + e.toString() + "   message = " + message.toString() + "     byteArray =  ";
        for (byte b : bytes) {
            s += b + ", ";
        }
        System.err.println("\n" + s + " ---------");
    }

//    public static void main(String[] args) {
////        byte[] b = {4, 40, 19, 4, 20, 2, -1, 18, -58, 2, 3, 20, 2, -1, 18, -58, 2, 3, 24, 6, -1, -1, 1, 3, 24, 6, -1, -1, 2};
//        byte[] b = {-124, 74, 8, 68, 105};

//        CLHeader header = new CLHeader(MessageType.BurningBuilding.ordinal(), 1, 18);
//        BurningBuildingMessage message = new BurningBuildingMessage(1017, 2, 208);
//        Packet pak = new Packet(header, message.getPriority(), message.getReceivers(null), message.getMessageByteArraySize());
//        pak.add(message);
//        b = getByteArray(pak);
//        List<Packet> p = getSayPacket(b);
//        System.out.println(p);
//    }
}
