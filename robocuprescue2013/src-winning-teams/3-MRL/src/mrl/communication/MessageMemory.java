package mrl.communication;

import javolution.util.FastMap;
import mrl.common.MRLConstants;
import mrl.common.comparator.ConstantComparators;
import mrl.communication.messages.CLHeader;
import mrl.communication.messages.Header;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 20, 2011
 * Time: 7:06:49 PM
 */
public class MessageMemory implements MRLConstants {

    private Map<EntityID, List<Header>> receivedPacketsCheckList;
//    private Map<Integer, List<Packet>> previousPackets;
        private Map<Integer, SortedSet<Packet>> previousPackets;

    //    private Map<MessageType, List<Packet>> sayPacketsMap;
    private Map<EntityID, ArrayList<Integer>> receivedSayPacketsCheckList;
//    private List<Packet> sayPacketsList;
    private SortedSet<Packet> sayPacketsList;

    private List<Packet> previousRendezvousSays;
    private int lastRendezvousTime;

    public MessageMemory() {
        receivedPacketsCheckList = new FastMap<EntityID, List<Header>>();
        previousPackets = new FastMap<Integer, SortedSet<Packet>>();
//        sayPacketsMap = new FastMap<MessageType, List<Packet>>();
        receivedSayPacketsCheckList = new FastMap<EntityID, ArrayList<Integer>>();
        sayPacketsList = new TreeSet<Packet>(ConstantComparators.PACKET_CL_VALUE_COMPARATOR);
        previousRendezvousSays = new ArrayList<Packet>();
    }

    public void addPacketToMemory(int time, Packet packet) {
//        List<Packet> packets = previousPackets.get(time);
        SortedSet<Packet> packets = previousPackets.get(time);

        if (packets == null) {
            packets = new TreeSet<Packet>();
        }
        packets.add(packet);
        previousPackets.put(time, packets);
    }

    public void updatePreviousPackets(int time) {
        previousPackets.remove(time - MESSAGE_VALIDATE_THRESHOLD);
    }

    public SortedSet<Packet> getPackets(int time) {
        return previousPackets.get(time);
    }
//
//    public List<Packet> getPackets(int time) {
//        return previousPackets.get(time);
//    }

    public void addReceivedPacketToCheckList(EntityID id, Header header) {
        List<Header> headerList = receivedPacketsCheckList.get(id);

        if (headerList == null) {
            headerList = new ArrayList<Header>();
        }

        headerList.add(header);
        receivedPacketsCheckList.put(id, headerList);
    }

    public boolean isRepetitivePacket(EntityID id, Header header) {
        List<Header> headers = receivedPacketsCheckList.get(id);
        if (headers == null) {
            return false;
        }
        for (Header aHeader : headers) {
            if (aHeader.equals(header)) {
                return true;
            }
        }
        return false;
    }

    public void updateCheckList(int time) {
        for (List<Header> thisAgentHeaders : receivedPacketsCheckList.values()) {
            List<Header> toRemove = new ArrayList<Header>();

            for (Header header : thisAgentHeaders) {
                if (header.getPacketCycle(time) < (time - MESSAGE_VALIDATE_THRESHOLD)) {
                    toRemove.add(header);
                }
            }
            thisAgentHeaders.removeAll(toRemove);
        }
    }

    public List<Packet> getSayPackets(int time, boolean onRendezvous) {
        updateSayPackets(time);
        List<Packet> toSay = new ArrayList<Packet>();
        toSay.addAll(sayPacketsList);

        if (onRendezvous) {
            toSay.removeAll(previousRendezvousSays);
        }
        return toSay;
    }

    private void updateSayPackets(int time) {
        List<Packet> toRemove = new ArrayList<Packet>();
        CLHeader header;
        for (Packet packet : sayPacketsList) {
            header = (CLHeader) packet.getHeader();
            if (!header.increaseTTL()) {
                toRemove.add(packet);
            }
        }
        sayPacketsList.removeAll(toRemove);
//        Collections.sort(sayPacketsList, ConstantComparators.PACKET_CL_VALUE_COMPARATOR);
    }

    public void addPacketToSayList(Packet newPacket, int bandWidth, int time) {
        List<Packet> toRemove = new ArrayList<Packet>();

        for (Packet packet : sayPacketsList) {
            if (packet.getHeader().getPacketType().equals(newPacket.getHeader().getPacketType())) {
                if (newPacket.getHeader().getPacketCycle(time) >= packet.getHeader().getPacketCycle(time)) {
                    updatePackets(newPacket, packet);
                } else {
                    updatePackets(packet, newPacket);
                }
                newPacket.removeDuplicateMessages(packet);
                if (packet.isEmpty()) {
                    toRemove.add(packet);
                }
            }
        }
        sayPacketsList.removeAll(toRemove);
        if (!newPacket.isEmpty()) {
            sayPacketsList.addAll(splitPacket(newPacket, bandWidth));
        }
    }

    private List<Packet> splitPacket(Packet packet, int bandWidth) {
        List<Packet> splitPackets = new ArrayList<Packet>();
        int size = packet.size();
        int packetBSize = packet.getPacketByteArraySize();
        int messageSize = packet.getHeader().getPacketType().getByteArraySize();
        if (bandWidth > messageSize * 4) {
            bandWidth /= 2;
        }
        int messagePerPacket = (bandWidth - CLHeader.MESSAGE_SIZE) / messageSize;

        if (messagePerPacket > 9) {
            messagePerPacket = 9;
        }
        if (packetBSize < bandWidth || messagePerPacket <= 1 || bandWidth == -1) {
            splitPackets.add(packet);
            return splitPackets;
        }

        while (size > 0) {
            Packet sp = new Packet(packet.getHeader(), packet.getPriority(), packet.getReceivers(), 0);
            int counter = messagePerPacket;
            for (Message message : packet) {
                if (counter-- <= 0) {
                    break;
                }
                sp.add(message);
                size--;
            }
            packet.removeAll(sp);
            splitPackets.add(sp);
        }

        return splitPackets;
    }

    public void updatePackets(Packet packet1, Packet packet2) {
        List<Message> toRemove = new ArrayList<Message>();

        for (Message message1 : packet1) {

            for (Message message2 : packet2) {
                if (message1.equals(message2)) {
                    toRemove.add(message2);
                }
            }
            if (!toRemove.isEmpty()) {
                packet2.removeAll(toRemove);
            }
        }
    }

    public void addReceivedSayPacketToCheckList(EntityID id, int time) {
        ArrayList<Integer> timeList = receivedSayPacketsCheckList.get(id);

        if (timeList == null) {
            timeList = new ArrayList<Integer>();
        } else {
            List<Integer> toRemove = new ArrayList<Integer>();

            for (Integer t : timeList) {
                if (t < (time - SAY_VALIDATE_THRESHOLD)) {
                    toRemove.add(t);
                }
            }
            timeList.removeAll(toRemove);
        }

        timeList.add(time);
        receivedSayPacketsCheckList.put(id, timeList);
    }

    public boolean isRepetitiveSayPacket(EntityID id, int time) {
        ArrayList<Integer> timeList = receivedSayPacketsCheckList.get(id);
        return timeList != null && timeList.contains(time);
    }

    public void addRendezvousSaysPacket(Packet packet, int time) {
        if (lastRendezvousTime + 1 == time) {
            previousRendezvousSays.add(packet);
            lastRendezvousTime = time;
        } else {
            previousRendezvousSays.clear();
        }
    }
}
