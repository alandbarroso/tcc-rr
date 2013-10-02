package mrl.mosCommunication.message;

//import Manager.LogManager;
import mrl.common.BenchMark.messaging.*;
import mrl.common.MRLConstants;
import mrl.helper.CivilianHelper;
import mrl.helper.HumanHelper;
import mrl.helper.PropertyHelper;
import mrl.helper.RoadHelper;
import mrl.mosCommunication.entities.*;
import mrl.mosCommunication.message.property.ChannelCondition;
import mrl.mosCommunication.message.property.SendType;
import mrl.mosCommunication.message.type.AbstractMessage;
import mrl.mosCommunication.message.type.MessageTypes;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.graph.Node;
import mrl.world.routing.path.Path;
import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import rescuecore2.misc.Handy;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mostafa
 * Date: 1/21/11
 * Time: 10:47 PM
 */
public class MessageManager {

    public static final String COMMUNICATIONS_KEY = "comms";
    public static final String CHANNELS_KEY = "channels";
    public static final String NOISE_KEY = "noise";
    public static final String USE_KEY = "use";
    public static final String PROBABILITY_KEY = "p";
    public static final String COUNT_KEY = "count";

    HashMap<Integer, MessageEntities> receivedMEs = new HashMap<Integer, MessageEntities>();
    HashMap<Integer, MessageEntities> sentMEs = new HashMap<Integer, MessageEntities>();
    HashMap<Integer, Integer> receivedMessageSize = new HashMap<Integer, Integer>();
    protected Set<AbstractMessage> messages = new HashSet<AbstractMessage>();
    //    protected List<AbstractMessage> emergencyMessages = new ArrayList<AbstractMessage>();
//    protected List<AbstractMessage> sayMessages = new ArrayList<AbstractMessage>();
    public static int channels;
    protected static int[] bandwidths;
    protected ChannelCondition[] channelConditions;
    StandardAgent agent;
    MrlWorld world;
    int receivedBytes;
    private int time;
    Config config;
    int voiceChannelBandwidth;
    int clearSayMessagesCounter = 5;
    IDConverter idConverter = new IDConverter();
    Map<MessageTypes, Integer> channelMap;
    private PropertyHelper propertyHelper;
    private HumanHelper humanHelper;
    private CivilianHelper civilianHelper;
    private RoadHelper roadHelper;
    private MessageFactory messageFactory;
    private boolean isThreadStarted;
//    protected SendMessageLogManager sendMessageLogManager;
//    protected ReceiveMessageLogManager receiveMessageLogManager;
//    protected DetailedMessageLogManager detailedMessageLogManager;
//    private List<LogManager> logManagers = Collections.synchronizedList(new ArrayList<LogManager>());
//    private MrlMessagingLogThread logThread;
    int maxBandwidthChannel;

    public MessageManager(MrlWorld world, Config config) {
        this.agent = world.getSelf();
        this.world = world;
        this.config = config;
        channels = config.getIntValue("comms.channels.count");
        bandwidths = new int[channels];
        channelConditions = new ChannelCondition[channels];
        bandwidths[0] = config.getIntValue("comms.channels.0.messages.size");
        for (int i = 1; i < channels; i++) {
            bandwidths[i] = config.getIntValue("comms.channels." + i + ".bandwidth");
            if (bandwidths[i] > maxBandwidthChannel) {
                maxBandwidthChannel = i;
            }
        }
        setChannelConditions();
        voiceChannelBandwidth = config.getIntValue("comms.channels.0.messages.size");
        idConverter.convertAll(this.world);
        setChannelMap();
        propertyHelper = world.getHelper(PropertyHelper.class);
        humanHelper = world.getHelper(HumanHelper.class);
        civilianHelper = world.getHelper(CivilianHelper.class);
        roadHelper = world.getHelper(RoadHelper.class);
        messageFactory = new MessageFactory(this, world);

        setWorldCommunicationCondition();

       /* logThread = null;
        if (MRLConstants.MYSQL_DEBUG_MESSAGING) {
            isThreadStarted = false;
            sendMessageLogManager = new SendMessageLogManager(world);
            receiveMessageLogManager = new ReceiveMessageLogManager(world);
            detailedMessageLogManager = new DetailedMessageLogManager(world);
//            logManagers.add(sendMessageLogManager);
//            logManagers.add(receiveMessageLogManager);
//            logManagers.add(detailedMessageLogManager);
        }*/
    }

    private void setWorldCommunicationCondition() {
        if (channels == 1) {
            world.setCommunicationLess(true);
            return;
        }
        switch (channelConditions[maxBandwidthChannel]) {
            case Low:
                world.setCommunicationLow(true);
                break;
            case Medium:
                world.setCommunicationMedium(true);
                break;
            case High:
                world.setCommunicationHigh(true);
                break;
        }
    }

    private void setChannelConditions() {
        for (int i = 0; i < channels; i++) {
            if (bandwidths[i] <= 128) {
                channelConditions[i] = ChannelCondition.Low;
            } else if (bandwidths[i] <= 1024) {
                channelConditions[i] = ChannelCondition.Medium;
            } else {
                channelConditions[i] = ChannelCondition.High;
            }
        }
    }

    public void initializePlatoonMessages() {
        removeSentMessages();
        messageFactory.createPlatoonMessages(world, (MrlPlatoonAgent) agent);
    }

    public void initializeCenterMessages() {
        removeSentMessages();
        messageFactory.createCenterMessages(world);
    }

    /**
     * remove all messages with sayTTL, speakTTL & emergencyTTL equal to 0.
     */
    private void removeSentMessages() {
        List<AbstractMessage> toDelete = new ArrayList<AbstractMessage>();
        for (AbstractMessage message : messages) {
            if (message.getSendTypes().contains(SendType.Say) && message.getSayTTL() >= 0) {
                continue;
            }
            if (message.getSendTypes().contains(SendType.Speak) && message.getSpeakTTL() >= 0) {
                continue;
            }
            if (message.getSendTypes().contains(SendType.Emergency) && message.getEmergencyTTL() >= 0) {
                continue;
            }
            toDelete.add(message);
        }
        messages.removeAll(toDelete);
    }

    public void addMessage(AbstractMessage message) {
        messages.remove(message);
        messages.add(message);
    }

/*    private boolean isTimeToLog() {
        return MRLConstants.MYSQL_DEBUG_MESSAGING && logThread != null && !logThread.isAlive() *//*&& world.getTime() % 5 == 0*//*;
    }*/

    public void sendMessages() {
        if (messages.isEmpty()) {
            return;
        }
        for (AbstractMessage msg : messages) {
            msg.setSender(agent.getID());
            int channel = getChannel(msg.getMessageType());
            if (channels == 1) {
                msg.setSpeakTTL(0);
            }
            ChannelCondition channelCondition = null;
            if (channels >= channel) {
                channelCondition = channelConditions[channel];
            }
//
//            if (msg instanceof CivilianSeenMessage) {
//                CivilianSeenMessage civilianSeenMessage = (CivilianSeenMessage) msg;
//                int value = civilianSeenMessage.getPropertyValues().get(PropertyTypes.HumanID);
//                if (value == 560110533) {
//                    System.out.println("ieow;uil;jh");
//                }
//            }

            if (msg.shouldSend(world, SendType.Speak, channelCondition)) {
                try {
                    MessageCreator MCSpeak = new MessageCreator(bandwidths[channel]);
                    MCSpeak.create(Arrays.asList(msg), /*sendMessageLogManager, detailedMessageLogManager,*/ time, SendType.Speak, TransmitType.SEND);
                    if (agent instanceof MrlCentre) {
                        ((MrlCentre) agent).sendMessage(channel, MCSpeak.getMessage().getBytes());
                    }
                    if (agent instanceof MrlPlatoonAgent) {
                        ((MrlPlatoonAgent) agent).sendMessage(channel, MCSpeak.getMessage().getBytes());
                    }
                } catch (Exception ignored) {
                    System.out.println("send message error");
                }
            }
            if (msg.shouldSend(world, SendType.Say, channelConditions[0])) {
                try {
                    MessageCreator MCSay = new MessageCreator(voiceChannelBandwidth);
                    MCSay.create(Arrays.asList(msg),/* sendMessageLogManager, detailedMessageLogManager,*/ time, SendType.Say, TransmitType.SEND);
                    if (agent instanceof MrlCentre) {
                        ((MrlCentre) agent).sendMessage(0, MCSay.getMessage().getBytes());
                    }
                    if (agent instanceof MrlPlatoonAgent) {
                        ((MrlPlatoonAgent) agent).sendMessage(0, MCSay.getMessage().getBytes());
                    }
                } catch (Exception ignored) {
                    System.out.println("send say message error");
                }
            }
        }
//        if (!isThreadStarted || isTimeToLog()) {
//            logThread = new MrlMessagingLogThread(logManagers);
//            logThread.start();
//            isThreadStarted = true;
//        }

    }

    public void sendEmergencyMessages() {
        if (messages.isEmpty())
            return;
//        if (bandwidths.length <= 1)
//            return;
        for (AbstractMessage msg : messages) {
            msg.setSender(agent.getID());
            int channel = getChannel(msg.getMessageType());
            if (channels == 1) {
                msg.setEmergencyTTL(0);
            }
//            if (msg instanceof CivilianSeenMessage) {
//                CivilianSeenMessage civilianSeenMessage = (CivilianSeenMessage) msg;
//                int value = civilianSeenMessage.getPropertyValues().get(PropertyTypes.HumanID);
//                if (value == 560110533) {
//                    System.out.println("ieow;uil;jh");
//                }
//            }
            ChannelCondition channelCondition = null;
            if (channels >= channel) {
                channelCondition = channelConditions[channel];
            }
            if (msg.shouldSend(world, SendType.Emergency, channelCondition)) {
                try {
                    MessageCreator MCSpeak = new MessageCreator(bandwidths[channel]);
                    MCSpeak.create(Arrays.asList(msg), /*sendMessageLogManager, detailedMessageLogManager,*/ time, SendType.Emergency, TransmitType.SEND);
                    if (agent instanceof MrlCentre) {
                        ((MrlCentre) agent).sendMessage(channel, MCSpeak.getMessage().getBytes());
                    }
                    if (agent instanceof MrlPlatoonAgent) {
                        ((MrlPlatoonAgent) agent).sendMessage(channel, MCSpeak.getMessage().getBytes());
                    }
                } catch (Exception ignored) {
                    System.out.println("send emergency message error");
                }
            }
        }
    }

    public void receive(int time, Collection<Command> heard) {
        this.time = time;
        MessageEntities messageEntities = new MessageEntities();
        Collection<MessageEntity> victimMessageEntities = new ArrayList<MessageEntity>();
        int size = 0;
        for (Command next : heard) {
            if (next.getAgentID().equals(agent.getID()))
                continue;
            if (next instanceof AKSpeak && ((AKSpeak) next).getChannel() == 0) {// say messages
                AKSpeak speak = (AKSpeak) next;
                Collection<EntityID> platoonIDs = Handy.objectsToIDs(world.getEntitiesOfType(StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.FIRE_BRIGADE));
                if (!platoonIDs.contains(speak.getAgentID())) {//Civilian message
                    processCivilianCommand(speak);
                } else { //agent message
                    SendType sendType = SendType.Say;
                    MessageParser mp = new MessageParser(new Message(((AKSpeak) next).getContent()), next.getAgentID());
                    MessageEntities tmp = mp.readMessage(sendType, world.getTime());
                    repeatSayMessages(tmp.getMessages());
                    if (MRLConstants.MYSQL_DEBUG_MESSAGING) {
//                        for (AbstractMessage message : tmp.getMessages()) {
//                            MessageLogProperty logProperty = new MessageLogProperty(message, world.getTime(), sendType, TransmitType.RECEIVE);
//                            receiveMessageLogManager.appendProperty(logProperty);
//                            detailedMessageLogManager.appendProperty(logProperty);
//                        }
                    }
                    messageEntities.merge(tmp);
                    processMessageEntities(tmp.getMessageEntities());
                }
            }
            if (next instanceof AKSpeak && ((AKSpeak) next).getChannel() != 0 && !next.getAgentID().equals(agent.getID())) {// speak messages
                SendType sendType = SendType.Speak;
                size += ((AKSpeak) next).getContent().length;
                MessageParser mp = new MessageParser(new Message(((AKSpeak) next).getContent()), next.getAgentID());
                MessageEntities tmp = mp.readMessage(sendType, world.getTime());
                if (MRLConstants.MYSQL_DEBUG_MESSAGING) {
//                    for (AbstractMessage message : tmp.getMessages()) {
//                        MessageLogProperty logProperty = new MessageLogProperty(message, world.getTime(), sendType, TransmitType.RECEIVE);
//                        receiveMessageLogManager.appendProperty(logProperty);
//                        detailedMessageLogManager.appendProperty(logProperty);
//                    }
                }
                messageEntities.merge(tmp);
                processMessageEntities(tmp.getMessageEntities());
            }
        }
        processMessageEntities(victimMessageEntities);
        receivedMessageSize.put(time, size);
        receivedMEs.clear();
        receivedMEs.put(time, messageEntities);
    }

    private void repeatSayMessages(Collection<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            if (message.getSayTTL() > 0) {
                boolean isContain = false;
                for (AbstractMessage msg : this.messages) {
                    if (msg.equals(message)) {
                        if (msg.getSayTTL() > message.getSayTTL()) {
                            isContain = true;
                            break;
                        }
                    }
                }
                if (!isContain) {
                    message.setSendable(true);
                    this.addMessage(message);
                }
            }
        }
    }

    private void processMessageEntities(Collection<MessageEntity> messageEntities) {
        for (MessageEntity messageEntity : messageEntities) {

            // do process this messageEntity
            switch (messageEntity.getMessageEntityType()) {
                case NOP:
                    break;
                case BurningBuilding:
                    processBurningBuilding((BurningBuilding) messageEntity);
                    break;
                case AgentInfo:
                    processAgentInfo((AgentInfo) messageEntity);
                    break;
                case BuriedAgent:
                    processBuriedAgent((BuriedAgent) messageEntity);
                    break;
                case ClearedRoad:
                    processClearedRoad((ClearedRoad) messageEntity);
                    break;
                case EmptyBuilding:
                    processEmptyBuilding((EmptyBuilding) messageEntity);
                    break;
                case ExtinguishedBuilding:
                    processExtinguishBuilding((ExtinguishedBuilding) messageEntity);
                    break;
                case FullBuilding:
                    processVictimContainBuilding((FullBuilding) messageEntity);
                    break;
                case HeardCivilian:
                    processHeardCivilianMessage((HeardCivilian) messageEntity);
                    break;
                case CivilianSeen:
                    processCivilianSeenMessage((CivilianSeen) messageEntity);
                    break;
                case ClearedPath:
                    processClearedPathMessage((ClearedPath) messageEntity);
                    break;
                case WarmBuilding:
                    processWarmBuildingMessage((WarmBuilding) messageEntity);
                    break;
                default:
                    if (agent instanceof MrlPlatoonAgent) {
                        ((MrlPlatoonAgent) agent).processMessage(messageEntity);
                    }
                    if (agent instanceof MrlCentre) {
                        ((MrlCentre) agent).processMessage(messageEntity);
                    }
                    break;
            }
        }
    }

    private void processWarmBuildingMessage(WarmBuilding warmBuilding) {
        throw new NotImplementedException();
    }

    private void processClearedPathMessage(ClearedPath clearedPath) {
        int time = clearedPath.getSendTime();
        Path path = world.getPath(clearedPath.getPathID());
        if (path != null) {
            for (Road road : path) {
                if (propertyHelper.getPropertyTime(road.getBlockadesProperty()) < time) {
                    road.setBlockades(new ArrayList<EntityID>());
                    propertyHelper.setPropertyTime(road.getBlockadesProperty(), time);
                    roadHelper.setRoadPassable(road.getID(), true);
                    for (Node node : world.getPlatoonAgent().getPathPlanner().getGraph().getAreaNodes(road.getID())) {
                        node.setPassable(true, time);
                    }
                }
            }
        }
    }

    private void processCivilianSeenMessage(CivilianSeen civilianSeen) {
        int time = civilianSeen.getSendTime();
        EntityID positionID;
        Civilian civilian;
        civilian = (Civilian) world.getEntity(civilianSeen.getCivilianID());
        boolean shouldUpdate = false;
        boolean isFirstTime = false;

        if (civilian == null) {
            civilian = new Civilian(civilianSeen.getCivilianID());
            world.addNewCivilian(civilian);
            shouldUpdate = true;
        } else if (propertyHelper.getEntityLastUpdateTime(civilian) < time) {
            shouldUpdate = true;
        }
        if (!civilian.isBuriednessDefined()) {
            isFirstTime = true;
        }

        positionID = civilianSeen.getBuildingID();
        if (shouldUpdate) {


            //updating Civilian position map
            EntityID prevPosition = world.getCivilianPositionMap().get(civilian.getID());
            EntityID currentPosition = civilian.getPosition();
            if (prevPosition != null && world.getEntity(prevPosition) instanceof Building) {

                if (!prevPosition.equals(currentPosition)) {
                    world.getMrlBuilding(prevPosition).getHumans().remove(civilian);
                    if (world.getEntity(currentPosition) instanceof Building) {
                        world.getMrlBuilding(currentPosition).getHumans().add(civilian);
                    }
                }
            } else if (world.getEntity(currentPosition) instanceof Building) {
                world.getMrlBuilding(currentPosition).getHumans().add(civilian);
            }
            world.getCivilianPositionMap().put(civilian.getID(), civilian.getPosition());

            Pair<Integer, Integer> location = world.getEntity(positionID).getLocation(world);
            civilian.setX(location.first());
            propertyHelper.setPropertyTime(civilian.getXProperty(), time);
            civilian.setY(location.second());
            propertyHelper.setPropertyTime(civilian.getYProperty(), time);
            civilian.setPosition(positionID);
            propertyHelper.setPropertyTime(civilian.getPositionProperty(), time);
            civilian.setHP(civilianSeen.getHp());
            propertyHelper.setPropertyTime(civilian.getHPProperty(), time);
            civilian.setDamage(civilianSeen.getDamage());
            propertyHelper.setPropertyTime(civilian.getDamageProperty(), time);
            civilian.setBuriedness(civilianSeen.getBuriedness());
            propertyHelper.setPropertyTime(civilian.getBuriednessProperty(), time);
            humanHelper.setFromSense(civilian.getID(), false);
            //Informations

            if (isFirstTime) {
                humanHelper.setFirstHP(civilian.getID(), civilian.getHP());
                humanHelper.setFirstDamage(civilian.getID(), civilian.getDamage());
                humanHelper.setFirstBuriedness(civilian.getID(), civilian.getBuriedness());
            }
            humanHelper.setTimeToRefuge(civilian.getID(), civilianSeen.getTimeToRefuge());
        }
    }

//    private void processVisitedBuilding(VisitedBuilding visitedBuilding) {
//        EntityID id = visitedBuilding.getBuildingID();
//        world.setBuildingVisited(id);
//    }

    private void processVictimContainBuilding(FullBuilding fullBuilding) {
        //System.out.println(fullBuilding.toString());
    }

//    private void processStuckAgent(StuckAgent stuckAgent) {
//        Human human = (Human) world.getEntity(stuckAgent.getSender());
//        int time = world.getTime();
//        if (propertyHelper.getPropertyTime(human.getPositionProperty()) < time) {
//            human.setPosition(stuckAgent.getPositionID());
//            propertyHelper.setPropertyTime(human.getPositionProperty(), time);
//        }
//        humanHelper.setLockedByBlockade(human.getID(), true);
//    }

    private void processExtinguishBuilding(ExtinguishedBuilding extinguishedBuilding) {
//        int time = packet.getHeader().getPacketCycle(world.getTime());
        int time = extinguishedBuilding.getSendTime();
        Building building = (Building) world.getEntity(extinguishedBuilding.getBuildingID());

        if (propertyHelper.getPropertyTime(building.getFierynessProperty()) < time) {
            building.setFieryness(extinguishedBuilding.getFieryness());
            propertyHelper.setPropertyTime(building.getFierynessProperty(), time);

//                if ((platoonAgent instanceof MrlFireBrigade)) {
//                    MrlFireBrigadeWorld w = (MrlFireBrigadeWorld) world;
            MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
            mrlBuilding.setWasEverWatered(true);
//                    mrlBuilding.setEnergy(Math.max(0, mrlBuilding.getIgnitionPoint() - 20) * mrlBuilding.getCapacity());
            mrlBuilding.setEnergy(0);
//                    world.printData("extinguishBuilding:" + building+" f:"+extinguishedBuildingMessage.getFieriness());
//                }
        }
    }

    private void processEmptyBuilding(EmptyBuilding emptyBuilding) {
        EntityID id = emptyBuilding.getBuildingID();
        world.getEmptyBuildings().add(id);
    }

    private void processClearedRoad(ClearedRoad clearedRoad) {
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);
        Road road = (Road) world.getEntity(clearedRoad.getRoadID());
        road.setBlockades(new ArrayList<EntityID>());
        roadHelper.setRoadPassable(road.getID(), true);
    }

    private void processBurningBuilding(BurningBuilding burningBuilding) {
        Building building;
        int time = burningBuilding.getSendTime();

        building = (Building) world.getEntity(burningBuilding.getID());
        if (propertyHelper.getPropertyTime(building.getFierynessProperty()) < time) {
            if (building.isFierynessDefined() && building.getFieryness() == 8 && burningBuilding.getFieryness() != 8) {
                if (MRLConstants.LAUNCH_VIEWER) {
                    System.out.println("aaaa");
                }
            }
            if (building.getID().getValue() == 25393) {
                world.printData("BurningBuilding\tSender=" + burningBuilding.getSender().getValue() + " Real Fire=" + (building.isFierynessDefined() ? building.getFieryness() : 0) + " message fire: " + burningBuilding.getFieryness());
            }
            building.setFieryness(burningBuilding.getFieryness());
            propertyHelper.setPropertyTime(building.getFierynessProperty(), time);
            building.setTemperature(burningBuilding.getTemperature());
            propertyHelper.setPropertyTime(building.getTemperatureProperty(), time);
//                if ((platoonAgent instanceof MrlFireBrigade)) {
//                    MrlFireBrigadeWorld w = (MrlFireBrigadeWorld) world;
            MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
            switch (building.getFieryness()) {
                case 0:
                    mrlBuilding.setFuel(mrlBuilding.getInitialFuel());
                    break;
                case 1:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.75));
                    } else if (mrlBuilding.getFuel() == mrlBuilding.getInitialFuel()) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.90));
                    }
                    break;

                case 2:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.33
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.50));
                    }
                    break;

                case 3:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.01
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.33) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.15));
                    }
                    break;

                case 8:
                    mrlBuilding.setFuel(0);
                    break;
            }
            mrlBuilding.setEnergy(building.getTemperature() * mrlBuilding.getCapacity());
//                    world.printData("burningBuilding:" + building+" f:"+burningBuildingMessage.getFieriness()+" temp:"+burningBuildingMessage.getTemperature());
//                }
            //updating burning buildings set
            if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                world.getBurningBuildings().add(building.getID());
                mrlBuilding.setIgnitionTime(world.getTime());
            } else {
                world.getBurningBuildings().remove(building.getID());
            }


        }
    }

    private void processBuriedAgent(BuriedAgent buriedAgent) {
        Human human = (Human) world.getEntity(buriedAgent.getSender());
        int time = buriedAgent.getSendTime();
        if (propertyHelper.getEntityLastUpdateTime(human) <= time) {
            human.setHP(buriedAgent.getHp());
            propertyHelper.setPropertyTime(human.getHPProperty(), time);
            human.setDamage(buriedAgent.getDamage());
            propertyHelper.setPropertyTime(human.getDamageProperty(), time);
            human.setBuriedness(buriedAgent.getBuriedness());
            propertyHelper.setPropertyTime(human.getBuriednessProperty(), time);
            human.setPosition(buriedAgent.getBuildingID());
            propertyHelper.setPropertyTime(human.getPositionProperty(), time);
            if (human.getDamage() == 0) {
                human.setDamage(6); //todo....
            }
            if (humanHelper.getFirstHP(human.getID()) < 0) {
                humanHelper.setFirstHP(human.getID(), human.getHP());
                humanHelper.setFirstDamage(human.getID(), human.getDamage());
                humanHelper.setFirstBuriedness(human.getID(), human.getBuriedness());
            }
            if (human.getHP() > 0 && human.getBuriedness() > 0 && !world.getBuriedAgents().contains(human.getID())) {
                world.getBuriedAgents().add(human.getID());
                humanHelper.setAgentSate(human.getID(), State.BURIED);
            }

            if (human.getHP() != 0 && human.getBuriedness() == 0) {
                world.getBuriedAgents().remove(human.getID());
                humanHelper.setAgentSate(human.getID(), State.WORKING);
            } else if (human.getBuriedness() == 0) {
                world.getBuriedAgents().remove(human.getID());
                humanHelper.setAgentSate(human.getID(), State.DEAD);

            }

            if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
                if (world.getSelfHuman() instanceof AmbulanceTeam) {
                    System.out.println(">>> " + world.getTime() + " " + world.getSelf().getID() + " ID:" + human.getID() + " DMG:" + human.getDamage());
                }
            }
        }
    }

    private void processAgentInfo(AgentInfo agentInfo) {
        Human human = (Human) world.getEntity(agentInfo.getSender());
        int time = agentInfo.getSendTime();

        if (propertyHelper.getPropertyTime(human.getPositionProperty()) < time) {
            human.setPosition(agentInfo.getPositionID());
            propertyHelper.setPropertyTime(human.getPositionProperty(), time);
            humanHelper.setAgentSate(human.getID(), agentInfo.getState());
        }
    }

    private void processHeardCivilianMessage(HeardCivilian heardCivilian) {
        EntityID id = heardCivilian.getCivilianID();
        Civilian civilian = (Civilian) world.getEntity(id);
        if (civilian == null) {
            civilian = new Civilian(id);
            world.addNewCivilian(civilian);
        }
        civilianHelper.addHeardCivilianPosition(civilian.getID(), heardCivilian.getLocationX(), heardCivilian.getLocationY());
    }

    public void processCivilianCommand(AKSpeak speak) {
        Civilian civilian = (Civilian) world.getEntity(speak.getAgentID());
        if (civilian == null) {
            civilian = new Civilian(speak.getAgentID());
            world.addNewCivilian(civilian);
        }
        if (!civilian.isPositionDefined()) {
            world.addHeardCivilian(civilian.getID());
        }

    }

    public MessageEntities getAllReceivedMessages() {
        MessageEntities res = new MessageEntities();
        for (int i = 1; i <= time; i++) {
            res.merge(receivedMEs.get(i));
        }
        return res;
    }

    public MessageEntities getReceivedMessagesAtTime(int time) {
        return receivedMEs.get(time);
    }

    public int getReceivedBytesInTime(int time) {
        return receivedMessageSize.get(time);
    }

    public int getAllReceivedBytes() {
        receivedBytes = 0;
        for (int i = 1; i <= time; i++) {
            receivedBytes += receivedMessageSize.get(i);
        }
        return receivedBytes;
    }

    public int getChannel(MessageTypes messageType) {
        Integer no;

        no = channelMap.get(messageType);
        if (no == null) {
            return 1;
        } else {
            return no;
        }
    }

    private void setChannelMap() {
        int n;
        Map<MessageTypes, Integer> map;

        // Channel
        n = getChannelCount();
        map = new HashMap<MessageTypes, Integer>();
        for (MessageTypes messageType : MessageTypes.values()) {
            map.put(messageType, maxBandwidthChannel);
        }

        channelMap = map;
    }

    private int getChannelCount() {
        String key = COMMUNICATIONS_KEY + "." + CHANNELS_KEY + "." + COUNT_KEY;
        return config.getIntValue(key);
    }

    public ChannelCondition[] getChannelConditions() {
        return channelConditions;
    }

    public int getMaxBandwidthChannel() {
        return maxBandwidthChannel;
    }
}
