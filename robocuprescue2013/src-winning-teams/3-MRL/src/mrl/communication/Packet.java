package mrl.communication;

import mrl.common.MRLConstants;
import mrl.communication.messages.CLHeader;
import mrl.communication.messages.Header;
import mrl.communication.property.Priority;
import mrl.communication.property.Receivers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 4:19:41 PM
 */
public class Packet extends ArrayList<Message> implements Comparable {
    private Header header;
    private Priority priority;
    private Receivers receivers;
    private int byteArraySize;
    private int sentNumber = 0;

    /**
     * add a header too each packet.
     *
     * @param header:        message id and message time step
     * @param priority:      priority of messages in this packet
     * @param receivers:     receivers this messages
     * @param byteArraySize: all size of this packet.
     */
    public Packet(Header header, Priority priority, Receivers receivers, int byteArraySize) {
        this.header = header;
        this.priority = priority;
        this.receivers = receivers;
        this.byteArraySize = byteArraySize + header.getMessageByteArraySize();
    }


    public void setByteArraySize(int byteArraySize) {
        this.byteArraySize = byteArraySize + header.getMessageByteArraySize();
    }

    private void setByteArraySize() {
        this.byteArraySize = (this.size() * header.getPacketType().getByteArraySize()) + header.getMessageByteArraySize();
    }

    public Header getHeader() {
        return header;
    }

    public Priority getPriority() {
        return priority;
    }

    public Receivers getReceivers() {
        return receivers;
    }

    public int getPacketByteArraySize() {
        return byteArraySize;
    }

    public void removeDuplicateMessages(Packet packet) {
        List<Message> toRemove = new ArrayList<Message>();
//        List<Message> toAdd = new ArrayList<Message>();
        for (Message thisMessage : this) {
            for (Message message : packet) {
                if (thisMessage.equals(message)) {
                    toRemove.add(message);
//                    toAdd.add(thisMessage);
                }
            }
        }
        packet.removeAll(toRemove);
//        packet.addAll(toAdd);
//        this.removeAll(toAdd);
        setByteArraySize();
    }

    public void updateHeader() {
        setByteArraySize();
        if (header instanceof CLHeader) {
            CLHeader clHeader = (CLHeader) header;
            clHeader.setMessageNumber(this.size());
        }
    }

    public void decreaseSentNumber() {
        sentNumber++;
    }

    public double getCLPacketValue() {
        CLHeader clHeader = (CLHeader) header;
        return dynamicFormula(clHeader);
//        return simpleValue(clHeader);
    }

    private double simpleValue(CLHeader clHeader) {
        double time = -(clHeader.getPacketInitialTTL() - clHeader.getTTL());
        double value = (priority.ordinal() * MRLConstants.SAY_PACKET_ALPHA);
        return time + value;
    }

    private double dynamicFormula(CLHeader clHeader) {
        double deltaT = (clHeader.getPacketInitialTTL() - clHeader.getTTL());
        double sigmaSent = (sentNumber * MRLConstants.SAY_PACKET_ALPHA1);
        double value1 = (1 - ((deltaT + sigmaSent) / MRLConstants.SAY_PACKET_TTL_MAX));
        double value2 = (priority.ordinal() * (MRLConstants.SAY_PACKET_N / MRLConstants.SAY_PACKET_TTL_MAX));
        return value1 + value2;
    }

    @Override
    public String toString() {
        String s;
        if (header instanceof CLHeader) {
            s = "HEADER: " + header.toString() + " sentNumber = " + sentNumber + " p = " + priority.ordinal() + " deltaT = " + (((CLHeader) header).getPacketInitialTTL() - ((CLHeader) header).getTTL()) + " packet value = " + getCLPacketValue() + "  [";
        } else {
            s = "HEADER: " + header.toString() + " sentNumber = " + sentNumber + " p = " + priority.ordinal();
        }
        for (Message message : this) {
            s += "   MSG:" + this.indexOf(message) + message.toString();
        }
        return s + " ]";
    }

    /**
     * Edited By Siavash
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {    // nozuli
        int b1 = priority.ordinal();
        int b2 = ((Packet) o).getPriority().ordinal();
        if (b1 == b2){
            return 0;
        } else if (b1 < b2){
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof Packet)) return false;
        if (!super.equals(o)) return false;

        Packet messages = (Packet) o;

        if (byteArraySize != messages.byteArraySize) return false;
        if (!header.equals(messages.header)) return false;
        if (priority != messages.priority) return false;
        if (receivers != messages.receivers) return false;

        if (this.containsAll(messages)) return true;

        return false;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + header.hashCode();
        result = 31 * result + priority.hashCode();
        result = 31 * result + (receivers != null ? receivers.hashCode() : 0);
        result = 31 * result + byteArraySize;
        result = 31 * result + sentNumber;
        return result;
    }
}
