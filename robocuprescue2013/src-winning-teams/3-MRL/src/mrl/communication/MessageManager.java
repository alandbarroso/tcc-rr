package mrl.communication;

import mrl.ambulance.MrlAmbulanceCentre;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.common.MRLConstants;
import mrl.common.comparator.ConstantComparators;
import mrl.communication.channels.Channel;
import mrl.communication.channels.ChannelScanner;
import mrl.communication.channels.Channels;
import mrl.communication.messages.CLHeader;
import mrl.communication.messages.ChannelScannerMessage;
import mrl.communication.messages.Header;
import mrl.communication.property.Priority;
import mrl.communication.property.Receivers;
import mrl.communication.property.TypeOfSend;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireStation;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceOffice;
import mrl.world.MrlWorld;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.messages.AKSpeak;

import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 5, 2011
 * Time: 2:31:14 PM
 */
public class MessageManager implements MRLConstants {
    private MrlWorld world;
    private MrlPlatoonAgent platoonAgent;
    private MrlCentre centre;
    private Channels channels;
    private ChannelScanner scanner;
    private boolean amIScanner;
    private int finishScanTime;
    private int ignoreCommandTime;
    private int scannerMaxChannel;
    private Channel scanPeriodChannel;
    private int voiceMessageRepeat;
    private MessageMemory messageMemory;
    private MessageFactory messageFactory;
    private ProcessMessageHelper processMessageHelper;
    private EnumMap<MessageType, List<Message>> messageQueueMap;
    private EnumMap<MessageType, List<Message>> sayMessageQueueMap;
    private int ignoreTime;
    private boolean amINewConnect = true;
    private List<Pair<Packet, Set<Channel>>> shufflePacketMap = new ArrayList<Pair<Packet, Set<Channel>>>();
    //    private Map<Packet, Collection<Channel>> shufflePacketMap = new HashMap<Packet, Collection<Channel>>();
    private int myOwnBW;

    public MessageManager(MrlWorld world) {
        this.world = world;
        if (world.getSelf() instanceof MrlPlatoonAgent) {
            this.platoonAgent = (MrlPlatoonAgent) world.getSelf();
            ignoreTime = world.getIgnoreCommandTime();
        } else {
            this.centre = (MrlCentre) world.getSelf();
            ignoreTime = centre.getIgnoreCommandTime();
        }
        this.messageMemory = new MessageMemory();
        this.messageFactory = new MessageFactory(this, world);
        this.channels = new Channels(world);
        this.processMessageHelper = new ProcessMessageHelper(world, platoonAgent, messageMemory, this,channels);
        setScannerAgent();
        messageQueueMap = new EnumMap<MessageType, List<Message>>(MessageType.class);
        sayMessageQueueMap = new EnumMap<MessageType, List<Message>>(MessageType.class);
        voiceMessageRepeat = Math.min(channels.getVoiceChannelMessagesMax(), VOICE_CHANNEL_MESSAGE_REPEAT);
    }

    /**
     * dar in method agenti ke scanner ast moshakhas mishavad.
     * agar centre dashte bashim koochektarin ID anha entekhab mishavad,
     * vagar na kochektarin platoon.
     * ye channel baraye baghie agent ha ke scanner nistand set mishe.
     */
    private void setScannerAgent() {
        List<Channel> chs = new ArrayList<Channel>();

        for (Channel c : channels) {
            if (c.getType().equalsIgnoreCase("radio")) {
                scanPeriodChannel = c;
                break;
            }
        }
        if (scanPeriodChannel == null || (channels.getPlatoonMaxChannel() == 0 && channels.getCentreMaxChannel() == 0)) {
            amIScanner = false;
            world.setCommunicationLess(true);
            return;
        }

        scanPeriodChannel.setPrimaryChannel(world.getAgents().size());
        chs.add(scanPeriodChannel);

        channels.setATChannels(chs);
        channels.setFBChannels(chs);
        channels.setPFChannels(chs);

        StandardEntity agent = null;
        if (!world.getCentres().isEmpty() && channels.getCentreMaxChannel() != 0) {
            agent = Collections.min(world.getCentres(), ConstantComparators.ID_COMPARATOR);
        } else if (channels.getPlatoonMaxChannel() != 0) {
            agent = Collections.min(world.getPlatoonAgents(), ConstantComparators.ID_COMPARATOR);
        }

        if (agent == null) {
            world.setCommunicationLess(true);
            return;
        }

        amIScanner = agent.getID().equals(world.getSelf().getID());
        scanner = new ChannelScanner();

        if (agent instanceof Human) {
            scannerMaxChannel = channels.getPlatoonMaxChannel();
            finishScanTime = (int) (Math.ceil((float) (channels.size() - 1) / (float) scannerMaxChannel));
        } else {
            scannerMaxChannel = channels.getCentreMaxChannel();
            finishScanTime = (int) (Math.ceil((float) (channels.size() - 1) / (float) scannerMaxChannel));
        }


        if (centre == null) {
            ignoreCommandTime = world.getIgnoreCommandTime();
        } else {
            ignoreCommandTime = centre.getIgnoreCommandTime();
        }
        finishScanTime += ignoreCommandTime;

        if (DEBUG_MESSAGING) {
            System.out.println("");
            world.printData(" amIScanner = " + amIScanner + " finishScanTime = " + finishScanTime + " scannerMaxChannel = " + scannerMaxChannel);
//            Debbuge.fileAppending(" amIScanner = " + amIScanner + " finishScanTime = " + finishScanTime + " scannerMaxChannel = " + scannerMaxChannel);
            if (amIScanner) {
                String text;
                System.out.println(" -----------------------------------------");
                System.out.print("AmbulanceTeam channels: ");
                text = "AmbulanceTeam channels: ";
                for (Channel c : channels.getATChannels()) {
                    System.out.print("  " + c.getId());
                    text += "  " + c.getId();
                }
                System.out.println("");
                System.out.print("PoliceForce channels: ");
                text += "\n" + "PoliceForce channels: ";
                for (Channel c : channels.getPFChannels()) {
                    System.out.print("  " + c.getId());
                    text += "  " + c.getId();
                }
                System.out.println("");
                System.out.print("FireBrigade channels: ");
                text += "\n" + "FireBrigade channels: ";
                for (Channel c : channels.getFBChannels()) {
                    System.out.print("  " + c.getId());
                    text += "  " + c.getId();
                }
                System.out.println("");
                System.out.println(" -----------------------------------------");

//                Debbuge.fileAppending(text);
                channels.printChannelsInfo();
            }
        }
    }

    public void scanChannels(Collection<Command> commands) {
        if (finishScanTime < world.getTime() || world.isCommunicationLess() || channels.isScanBreak()) {
            return;
        }

        if (amINewConnect && world.getTime() != ignoreTime) {
            channels.setScanBreak();
            channels.subscribe(world);

        } else {
            amINewConnect = false;
        }

        if (centre != null) {
            scanner.sendMessageForScanChannel(channels, scannerMaxChannel, centre, world.getAgents().size(), amIScanner);
        } else {
            scanner.sendMessageForScanChannel(channels, scannerMaxChannel, platoonAgent, world.getAgents().size(), amIScanner);
        }

        if (!amIScanner) {
            return;
        }

        if (!channels.isScanFinished()) {
            scanner.scanChannel(channels, commands, scannerMaxChannel, world.getAgents().size());
        }
        if (channels.isScanFinished()) {
            channels.subscribe(world);
            sendScannerMessage();
        }
    }

    /**
     * vaghti scan kardane channel ha tamoom shod ettelaate channel ra be baghie message midahad.
     */
    private void sendScannerMessage() {
        Header header = new Header(world.getTime(), MessageType.ChannelScannerMessage.ordinal());
        Packet packet = new Packet(header, Priority.VeryHigh, Receivers.All, 0);
        int messageSize = 0;

        for (Channel channel : channels) {
            Receivers receivers = null;
            if (channels.getATChannels().contains(channel)) {
                receivers = Receivers.AmbulanceTeam;
            }
            if (channels.getFBChannels().contains(channel)) {
                if (receivers == null) {
                    receivers = Receivers.FireBrigade;
                } else {
                    receivers = Receivers.ATAndFB;
                }
            }
            if (channels.getPFChannels().contains(channel)) {
                if (receivers == null) {
                    receivers = Receivers.PoliceForce;
                } else if (receivers == Receivers.AmbulanceTeam) {
                    receivers = Receivers.PFAndAT;
                } else if (receivers == Receivers.FireBrigade) {
                    receivers = Receivers.FBAndPF;
                } else {
                    receivers = Receivers.All;
                }
            }
            if (receivers != null) {
                ChannelScannerMessage message = new ChannelScannerMessage(channel.getId(), receivers.ordinal(), channel.getRepeatCont(Priority.Low),
                        channel.getRepeatCont(Priority.Medium), channel.getRepeatCont(Priority.High), channel.getRepeatCont(Priority.VeryHigh));
                packet.add(message);
                messageSize += message.getMessageByteArraySize();
            }
        }

        packet.setByteArraySize(messageSize);

        int repeatCount = scanPeriodChannel.getBandwidth() / packet.getPacketByteArraySize();
        if (DEBUG_MESSAGING) {
            String text = world.getTime() + " " + world.getSelf() + " send " + repeatCount + " scanner message: " + packet.toString();
            System.out.println(text);
//            Debbuge.fileAppending(text);
        }
        for (int i = 0; i < repeatCount; i++) {
            sendMessage(scanPeriodChannel.getId(), MessageTranslator.getByteArray(packet));
        }
    }

    private void sendMessage(int channel, byte[] bytes) {
        if (centre != null) {
            centre.sendMessage(channel, bytes);
        } else {
            platoonAgent.sendMessage(channel, bytes);
        }
    }

    private void sendSay(byte[] bytes) {
//        if (centre != null) {
//            centre.sendMessage(Channels.VOICE_CHANNEL, bytes);
//        } else {
        platoonAgent.sendMessage(Channels.VOICE_CHANNEL, bytes);
//        }
    }

    /**
     * ezafe kardane yek message be list baraye ersal da in cycle.
     * albate check mikone ke in message barqaye ersal rooye channel-e voice ast ya radio.
     * message be ye list az hamoon no' ezafe mikone.
     *
     * @param message:     payami ke bayad ezafe beshe.
     * @param messageType: no'e message.
     */
    public void addMessageToQueue(Message message, MessageType messageType) {
        // radio messages
        if (!message.getSendType().equals(TypeOfSend.OnlyVoice)) {
            List<Message> list = messageQueueMap.get(messageType);
            if (list != null) {
                list.add(message);
            } else {
                list = new ArrayList<Message>();
                list.add(message);
            }
            messageQueueMap.put(messageType, list);
        }
        // say messages
        if (!message.getSendType().equals(TypeOfSend.OnlyRadio)) {
            List<Message> sayMessages = sayMessageQueueMap.get(messageType);
            if (sayMessages != null) {
                sayMessages.add(message);
            } else {
                sayMessages = new ArrayList<Message>();
                sayMessages.add(message);
            }
            sayMessageQueueMap.put(messageType, sayMessages);
        }
    }

    private List<Packet> getPacketList(int time) {
        List<Packet> packetList = new ArrayList<Packet>();
//        SortedSet<Packet> packetList = new TreeSet<Packet>();


        for (MessageType type : messageQueueMap.keySet()) {
            Header header = new Header(time, type.ordinal());
            Packet packet = new Packet(header, type.getPriority(), type.getReceivers(world.getSelfHuman()), (type.getByteArraySize() * messageQueueMap.get(type).size()));
            packet.addAll(messageQueueMap.get(type));
            messageQueueMap.get(type).clear();
            packetList.add(packet);
        }

        return packetList;
    }

    private List<Packet> getSayPacketList() {
        List<Packet> packetList = new ArrayList<Packet>();
        List<Message> messageList;

        for (MessageType type : sayMessageQueueMap.keySet()) {
            messageList = sayMessageQueueMap.get(type);
            CLHeader header = new CLHeader(type.ordinal(), messageList.size(), type.newInstance().getInitialTTL());
            Packet packet = new Packet(header, type.getPriority(), type.getReceivers(null), (type.getByteArraySize() * messageList.size()));
            packet.addAll(messageList);
            messageList.clear();
            if (!packet.isEmpty()) {
                packetList.add(packet);
            }
        }

        return packetList;
    }

    public void sendMessages(boolean onRendezvous) {
        int time = world.getTime();

        if (finishScanTime == time || time == ignoreCommandTime) {
            // next cycle we received channel scanner message.
            // this cycle scanner scanning first channel.

            if (!amIScanner) {
                List<Channel> ch = new ArrayList<Channel>();
                ch.add(scanPeriodChannel);
                channels.sendSubscribeToHear(world.getSelf(), ch);
            }

            return;
        }

        if (centre == null) {
            messageFactory.createMessages(world, platoonAgent);
        }

        if (!world.isCommunicationLess()) {
            if (centre != null) {
                messageFactory.createMessages(world);
            }
            for (Channel channel : channels) {
                channel.resetRemainedBandWidth();
            }
            List<Packet> thisCyclePackets = getPacketList(world.getTime());

//            SortedSet<Packet> thisCyclePackets = getPacketList(world.getTime());

            Collections.sort(thisCyclePackets);

            sendMessagesFromQueue(time, thisCyclePackets);
            sendMessageFromMemory(time);

        }

        if (centre == null) {
            List<Packet> thisCycleSayPackets = getSayPacketList();
            sendSayMessages(thisCycleSayPackets, onRendezvous);
        }

    }

    private void sendMessagesFromQueue(int time, SortedSet<Packet> packetQueue) {

        Packet notSentPacket;
        shufflePacketMap.clear();

        for (Packet packet : packetQueue) {
            notSentPacket = sendPacket(packet);
            if (notSentPacket != null && !notSentPacket.isEmpty()) {
                messageMemory.addPacketToMemory(time, notSentPacket);
            }
        }
        sendShufflePacketMap();
    }

    private void sendMessagesFromQueue(int time, List<Packet> packetQueue) {

        Packet notSentPacket;
        shufflePacketMap.clear();

        for (Packet packet : packetQueue) {
            notSentPacket = sendPacket(packet);
            if (notSentPacket != null && !notSentPacket.isEmpty()) {
                messageMemory.addPacketToMemory(time, notSentPacket);
            }
        }
        sendShufflePacketMap();
    }

    private void sendMessageFromMemory(int time) {

        messageMemory.updatePreviousPackets(time);

//        List<Packet> packetQueue;
        SortedSet<Packet> packetQueue;
        Packet notSentPacket;
        for (int i = 1; i <= MESSAGE_VALIDATE_THRESHOLD; i++) {

            packetQueue = messageMemory.getPackets(time - i);
            if (packetQueue == null) {
                return;
            }
//            Collections.sort(packetQueue);
            shufflePacketMap.clear();

            for (Packet packet : packetQueue) {
                notSentPacket = sendPacket(packet);
                if (notSentPacket != null && !notSentPacket.isEmpty()) {
                    messageMemory.addPacketToMemory(time, notSentPacket);
                }
            }
            sendShufflePacketMap();
        }
    }

    private void sendShufflePacketMap() {

//        Collections.shuffle(shufflePacketMap);
        for (Pair<Packet, Set<Channel>> ah : shufflePacketMap) {
            for (Channel channel : ah.second()) {
                send(ah.first(), channel);
            }
        }
    }

    private Packet sendPacket(Packet packet) {
        if (packet.isEmpty()) {
            return null;
        }

        int minChannelsBW = Integer.MAX_VALUE;
        int minPacketSize = packet.getHeader().getMessageByteArraySize() + 2;
        Set<Channel> channelsToSend = new HashSet<Channel>();

        //AT
        if (packet.getReceivers().equals(Receivers.All)
                || packet.getReceivers().equals(Receivers.ATAndFB)
                || packet.getReceivers().equals(Receivers.PFAndAT)
                || packet.getReceivers().equals(Receivers.AmbulanceTeam)) {
            int maxBW = minPacketSize;
            Channel selected = null;
            for (Channel channel : channels.getATChannels()) {
                if (channel.getRemainedBandWidth() > maxBW) {
                    selected = channel;
                    maxBW = channel.getRemainedBandWidth();
                }
            }
            if (selected != null) {
                channelsToSend.add(selected);
                if (minChannelsBW > selected.getRemainedBandWidth()) {
                    minChannelsBW = selected.getRemainedBandWidth();
                }
            }

        }
        // FB
        if (packet.getReceivers().equals(Receivers.All)
                || packet.getReceivers().equals(Receivers.ATAndFB)
                || packet.getReceivers().equals(Receivers.FBAndPF)
                || packet.getReceivers().equals(Receivers.FireBrigade)) {
            int maxBW = minPacketSize;
            Channel selected = null;
            for (Channel channel : channels.getFBChannels()) {
                if (channel.getRemainedBandWidth() > maxBW) {
                    selected = channel;
                    maxBW = channel.getRemainedBandWidth();
                }
            }
            if (selected != null) {
                channelsToSend.add(selected);
                if (minChannelsBW > selected.getRemainedBandWidth()) {
                    minChannelsBW = selected.getRemainedBandWidth();
                }
            }

        }
        //PF
        if (packet.getReceivers().equals(Receivers.All)
                || packet.getReceivers().equals(Receivers.FBAndPF)
                || packet.getReceivers().equals(Receivers.PFAndAT)
                || packet.getReceivers().equals(Receivers.PoliceForce)) {
            int maxBW = minPacketSize;
            Channel selected = null;
            for (Channel channel : channels.getPFChannels()) {
                if (channel.getRemainedBandWidth() > maxBW) {
                    selected = channel;
                    maxBW = channel.getRemainedBandWidth();
                }
            }
            if (selected != null) {
                channelsToSend.add(selected);
                if (minChannelsBW > selected.getRemainedBandWidth()) {
                    minChannelsBW = selected.getRemainedBandWidth();
                }
            }

        }

        if (minChannelsBW != Integer.MAX_VALUE) {
            Packet remainedMessages = null;
            int numberOfMessages = packet.size();
            int aMessageSize = packet.get(0).getMessageByteArraySize();
            int numberOfMessageCanSend = (int) Math.floor(minChannelsBW / aMessageSize);

            if (numberOfMessageCanSend < numberOfMessages) {
                remainedMessages = new Packet(packet.getHeader(), packet.getPriority(), packet.getReceivers(), (aMessageSize * (numberOfMessages - numberOfMessageCanSend)));
                for (int i = numberOfMessageCanSend; i < numberOfMessages; i++) {
                    remainedMessages.add(packet.get(i));
                    packet.setByteArraySize(packet.getPacketByteArraySize() - aMessageSize - 2);
                }
                packet.removeAll(remainedMessages);
            }

            if (packet.isEmpty()) {
                return remainedMessages;
            }

            if (DEBUG_MESSAGING) {
                String s = "";
                for (Channel c : channelsToSend) {
                    s += c.getId() + ",";
                }
                if (centre != null) {
                    String text = "SEND: " + centre.getDebugString() + " CHANNELS[" + s + "] " + packet.toString();
                    System.out.println(text);
//                    Debbuge.fileAppending(text);
                } else {
                    String text = "SEND: " + platoonAgent.getDebugString() + " CHANNELS[" + s + "] " + packet.toString();
                    System.out.println(text);
//                    Debbuge.fileAppending(text);
                }
            }

//            for (Channel ch : channelsToSend) {
//                send(packet, ch);
//            }
            shufflePacketMap.add(new Pair<Packet, Set<Channel>>(packet, channelsToSend));
//            shufflePacketMap.put(packet, channelsToSend);
            return remainedMessages;
        }
        return packet;
    }

    private void send(Packet packet, Channel channel) {
        if (packet.isEmpty()) {
            return;
        }
        byte[] packetByteArray = MessageTranslator.getByteArray(packet);
        for (int i = 0; i < channel.getRepeatCont(packet.getPriority()); i++) {
            sendMessage(channel.getId(), packetByteArray);
            channel.decreaseRemainedBW(packetByteArray.length);
            if (channel.getRemainedBandWidth() < packetByteArray.length) {
                break;
            }
        }
    }

    private void sendSayMessages(List<Packet> thisCyclePackets, boolean onRendezvous) {
        int mySayChannelBW = channels.getVoiceChannelBandwidth();
//        int agentCanHearMe = 0;
//        int nearCivilians = 0;

        // mohasebe inke che meghdar BW be man mirese.
//        for (StandardEntity entity : world.getObjectsInRange(world.getSelfLocation().first(), world.getSelfLocation().second(), channels.getVoiceChannelRange())) {
//            if (!(entity instanceof Area)) {
//                agentCanHearMe++;
//                if (DEBUG_SAY) {
//                    world.printData(" agent can here me : "+entity +"     dist = "+world.getDistance(entity.getID(), world.getSelf().getID()));
//                }
//                if (entity instanceof Civilian) {
//                    nearCivilians++;
//                }
//            }
//        }
//        if (agentCanHearMe <= 1) {
//            if (DEBUG_SAY) {
//                world.printData("  no body can here me.......");
//            }
//            return;
//        }
//        agentCanHearMe -= nearCivilians;
//        mySayChannelBW = (mySayChannelBW / (voiceMessageRepeat * ((agentCanHearMe)) - (nearCivilians * 10)));// 10 is message size from civilians

        mySayChannelBW = (mySayChannelBW / (3));

        for (Packet packet : thisCyclePackets) {
            messageMemory.addPacketToSayList(packet, mySayChannelBW, world.getTime());
        }

        List<Packet> packetList = new ArrayList<Packet>();
        packetList.addAll(messageMemory.getSayPackets(world.getTime(), onRendezvous));

        byte[] temp = new byte[mySayChannelBW];
        int index = 0;
        byte[] msg;
        int size;

        for (Packet packet : packetList) {
            if (!packet.isEmpty()) {
                packet.updateHeader();
                size = packet.getPacketByteArraySize();
                if (mySayChannelBW >= size) {
                    if (DEBUG_SAY) {
                        if (centre != null) {
                            System.out.println("SEND SAY:" + centre.getDebugString() + packet.toString());
                        } else {
                            System.out.println("SEND SAY:" + platoonAgent.getDebugString() + packet.toString());
                        }
                    }
                    mySayChannelBW -= size;
                    msg = MessageTranslator.getByteArray(packet);
                    System.arraycopy(msg, 0, temp, index, size);
                    index += msg.length;
                    packet.decreaseSentNumber();
                    if (onRendezvous) {
                        messageMemory.addRendezvousSaysPacket(packet, world.getTime());
                    }
                }
            }
        }

        byte[] toSend = new byte[index];
        System.arraycopy(temp, 0, toSend, 0, index);
//        String s = "";
//        for (byte b : toSend) {
//            s += String.valueOf(b) + ", ";
//        }
//        System.out.println(world.getPlatoonAgent().getDebugString()+" --------SEND:" + " byte Array = " + s);
        if (toSend.length > 0) {
//            for (int i = 0; i < voiceMessageRepeat; i++) {
            sendSay(toSend);
//            }
        }
    }

    public void processHearing(Collection<Command> heard) {
        messageMemory.updateCheckList(world.getTime());
        boolean isBreakScan = true;

        for (Command command : heard) {
            AKSpeak speak = (AKSpeak) command;
            if (shouldBeProcess(speak)) {

                isBreakScan = false;
                Packet receivedPacket = MessageTranslator.getPacket(speak.getContent());

                if (DEBUG_MESSAGING) {
                    if (centre != null) {
                        System.out.println("RECEIVE:" + centre.getDebugString() + " FROM: (" + speak.getAgentID().getValue() + ")  Channel: " + speak.getChannel() + "  " + receivedPacket.toString());
//                        String text = "RECEIVE:" + centre.getDebugString() + " FROM: (" + speak.getAgentID().getValue() + ")  Channel: " + speak.getChannel() + "  " + receivedPacket.toString();
//                        try {
//                            Debbuge.fileAppending(text);
//                        } catch (Exception e) {
//
//                        }
                    } else {
                        System.out.println("RECEIVE:" + platoonAgent.getDebugString() + " FROM: (" + speak.getAgentID().getValue() + ")  Channel: " + speak.getChannel() + "  " + receivedPacket.toString());
//                        String text = "RECEIVE:" + platoonAgent.getDebugString() + " FROM: (" + speak.getAgentID().getValue() + ")  Channel: " + speak.getChannel() + "  " + receivedPacket.toString();
//                        try {
//                            Debbuge.fileAppending(text);
//                        } catch (Exception e) {
//
//                        }
                    }
                }
                if (receivedPacket != null && !processMessageHelper.processMessage(receivedPacket, speak.getAgentID())) {
                    if (centre != null) {
                        centre.processMessage(receivedPacket, speak.getAgentID());
                    } else {
//                        platoonAgent.processMessage(receivedPacket, speak.getAgentID());
                    }
                }
            }
        }

        if (finishScanTime + 1 == world.getTime() && isBreakScan) {
            channels.setScanBreak();
            channels.subscribe(world);
        }
    }

    private boolean shouldBeProcess(AKSpeak speak) {
        if (speak.getAgentID().equals(world.getSelf().getID())
                || speak.getContent().length < 2) {
            return false;
        }

        if (speak.getChannel() == Channels.VOICE_CHANNEL) {
            if (isCivilianCommand(speak)) {
                processMessageHelper.processCivilianCommand(speak);
            } else if (!messageMemory.isRepetitiveSayPacket(speak.getAgentID(), speak.getTime())) {
                processSayMessages(speak);
            }
            return false;
        }

        Header header = MessageTranslator.getHeader(speak.getContent());

        return !messageMemory.isRepetitivePacket(speak.getAgentID(), header);
    }

    private void processSayMessages(AKSpeak speak) {
        byte[] received = speak.getContent();
        List<Packet> receivedPackets = MessageTranslator.getSayPacket(received);
        for (Packet packet : receivedPackets) {
            if (DEBUG_SAY) {
                if (centre != null) {
                    System.err.println("RECEIVE SAY:" + centre.getDebugString() + " FROM: (" + speak.getAgentID().getValue() + ")  Channel: " + speak.getChannel() + "  " + packet.toString());
                } else {
                    System.err.println("RECEIVE SAY:" + platoonAgent.getDebugString() + " FROM: (" + speak.getAgentID().getValue() + ")  Channel: " + speak.getChannel() + "  " + packet.toString());
                }
            }
            if (!processMessageHelper.processMessage(packet, speak.getAgentID())) {
                if (centre != null) {
                    centre.processMessage(packet, speak.getAgentID());
                } else {
//                    platoonAgent.processMessage(packet, speak.getAgentID());
                }
            }

            messageMemory.addPacketToSayList(packet, -1, world.getTime());
            messageMemory.addReceivedSayPacketToCheckList(speak.getAgentID(), packet.getHeader().getPacketCycle(world.getTime()));
        }
    }

    private boolean isCivilianCommand(AKSpeak command) {
        StandardEntity entity = world.getEntity(command.getAgentID());
        return entity == null || entity instanceof Civilian;
    }

    public void setMyOwnBW() {
        List<Channel> myChannels = null;
        myOwnBW = 0;
        if ((world.getSelf() instanceof MrlAmbulanceTeam) || (world.getSelf() instanceof MrlAmbulanceCentre)) {
            myChannels = channels.getATChannels();
        } else if ((world.getSelf() instanceof MrlFireBrigade) || (world.getSelf() instanceof MrlFireStation)) {
            myChannels = channels.getFBChannels();
        } else if ((world.getSelf() instanceof MrlPoliceForce) || (world.getSelf() instanceof MrlPoliceOffice)) {
            myChannels = channels.getPFChannels();
        }
        if (myChannels != null) {
            for (Channel channel : myChannels) {
                myOwnBW += channel.getRemainedBandWidth();
            }
        }

        if (myOwnBW < 22) {
//            world.setCommunicationLimited(true);
        }
    }

    public int getMyOwnBW() {
        return myOwnBW;
    }

    public int getMaxID() {
        return processMessageHelper.maxID;
    }

}
