package mrl.communication;

import mrl.ambulance.MrlAmbulanceCentre;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.common.MRLConstants;
import mrl.communication.channels.Channel;
import mrl.communication.channels.Channels;
import mrl.communication.messages.*;
import mrl.communication.property.Priority;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireStation;
import mrl.helper.CivilianHelper;
import mrl.helper.HumanHelper;
import mrl.helper.PropertyHelper;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceOffice;
import mrl.world.IndexSort;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.graph.Node;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 1/5/12
 * Time: 11:34 AM
 */
public class ProcessMessageHelper implements MRLConstants {
    private MrlWorld world;
    private IndexSort indexSort;
    private MrlPlatoonAgent platoonAgent;
    private MessageManager messageManager;
    private MessageMemory messageMemory;
    private HumanHelper humanHelper;
    private CivilianHelper civilianHelper;
    private RoadHelper roadHelper;
    private PropertyHelper propertyHelper;
    private Channels channels;
    public int maxID;

    public ProcessMessageHelper(MrlWorld world, MrlPlatoonAgent platoonAgent, MessageMemory messageMemory, MessageManager messageManager, Channels channels) {
        this.world = world;
        this.indexSort = world.getIndexes();
        this.platoonAgent = platoonAgent;
        this.messageManager = messageManager;
        this.messageMemory = messageMemory;
        humanHelper = world.getHelper(HumanHelper.class);
        civilianHelper = world.getHelper(CivilianHelper.class);
        roadHelper = world.getHelper(RoadHelper.class);
        propertyHelper = world.getHelper(PropertyHelper.class);
//        this.channels = new Channels(world);
        this.channels = channels;
        maxID = world.maxID;
    }

    public boolean processMessage(Packet packet, EntityID sender) {

        Header header;
        MessageType messageType;

        header = packet.getHeader();

        // add message header and sender id in memory for check repetitive message.
        messageMemory.addReceivedPacketToCheckList(sender, header);

        messageType = header.getPacketType();

        if (messageType.equals(MessageType.AgentInfo)) {
            try {
                processAgentInfoMessage(packet, sender);
            } catch (Exception ex) {
//                System.out.println("exception in AgentInfo Message");
            }
            return true;

        } else if (messageType.equals(MessageType.BuriedAgent)) {
            try {

                ProcessBuriedAgentMessage(packet, sender);
            } catch (Exception ex) {
//                System.out.println("exception in BuriedAgent Message");
            }

            return true;

        } else if (messageType.equals(MessageType.BurningBuilding)) {
            try {

                processBurningBuildingMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in BurningBuilding");
            }

            return true;

        } else if (messageType.equals(MessageType.ChannelScannerMessage)) {
            try {

                ProcessChannelScannerMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in ChannelScannerMessage");
            }

            return true;

        } else if (messageType.equals(MessageType.CivilianSeen)) {
            try {

                processCivilianSeenMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in CivilianSeen");
            }

            return true;

        } else if (messageType.equals(MessageType.CLBuriedAgent)) {
            try {

                processCLBuriedAgentMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in CLBuriedAgent");
            }

            return true;

        } else if (messageType.equals(MessageType.ClearedPath) && platoonAgent != null) {
            try {

                processClearedPathMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in ClearedPath");
            }

            return true;

        } else if (messageType.equals(MessageType.CLStuckAgent)) {
            try {

//                processCLStuckAgentMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in CLStuckAgent");
            }

            return true;

        } else if (messageType.equals(MessageType.ExtinguishedBuilding)) {
            try {

                processExtinguishedBuildingMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in ExtinguishedBuilding");
            }

            return true;

        } else if (messageType.equals(MessageType.HeardCivilian)) {
            try {

                processHeardCivilianMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in HeardCivilian");
            }

            return true;

        } else if (messageType.equals(MessageType.ImpassableNode) && platoonAgent != null) {
            try {

                processImpassableNodeMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in ImpassableNode");
            }

            return true;

        } else if (messageType.equals(MessageType.StuckAgent)) {
            try {

//                processStuckAgentMessage(packet, sender);
            } catch (Exception ex) {
//                System.out.println("exception in StuckAgent");
            }

            return true;

        } else if (messageType.equals(MessageType.VisitedBuilding)) {
            try {

                processVisitedBuildingMessage(packet, sender);
            } catch (Exception ex) {
//                System.out.println("exception in VisitedBuilding");
            }

            return true;
        } else if (messageType.equals(MessageType.FullBuilding)) {
            try {

                processFullBuildingMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in FullBuilding");
            }

            return true;
        } else if (messageType.equals(MessageType.EmptyBuilding)) {
            try {

                processEmptyBuildingMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in EmptyBuilding");
            }

            return true;
        } else if (messageType.equals(MessageType.ClearedRoad)) {
            try {

                processClearedRoadMessage(packet);
            } catch (Exception ex) {
//                System.out.println("exception in ClearedRoad");
            }

        }

        return false;
    }

    private void processFullBuildingMessage(Packet packet) {
        CLFullBuildingMessage clFullBuildingMessage;
        int time = world.getTime();
        EntityID positionID;

        for (Message message : packet) {
            Civilian civilian;
            clFullBuildingMessage = (CLFullBuildingMessage) message;
            civilian = (Civilian) world.getEntity(new EntityID(clFullBuildingMessage.getCivilianId() + maxID));
            boolean shouldUpdate = false;
            boolean isFirstTime = false;

            if (civilian == null) {
                civilian = new Civilian(new EntityID((clFullBuildingMessage.getCivilianId() + maxID)));
                world.addNewCivilian(civilian);
                world.getFullBuildings().add(civilian.getID());
                shouldUpdate = true;
            } else if (propertyHelper.getEntityLastUpdateTime(civilian) < time) {
                shouldUpdate = true;
            }
            if (!civilian.isBuriednessDefined()) {
                isFirstTime = true;
            }

            positionID = indexSort.getAreaID(clFullBuildingMessage.getAreaIdIndex());
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

                civilian.setPosition(positionID);
                propertyHelper.setPropertyTime(civilian.getPositionProperty(), time);
                civilian.setHP(clFullBuildingMessage.getHealthPoint());
                propertyHelper.setPropertyTime(civilian.getHPProperty(), time);
                civilian.setDamage(clFullBuildingMessage.getDamage());
                propertyHelper.setPropertyTime(civilian.getDamageProperty(), time);
                civilian.setBuriedness(clFullBuildingMessage.getBuriedness());
                propertyHelper.setPropertyTime(civilian.getBuriednessProperty(), time);
                humanHelper.setFromSense(civilian.getID(), false);

                if (isFirstTime) {
                    humanHelper.setFirstHP(civilian.getID(), civilian.getHP());
                    humanHelper.setFirstDamage(civilian.getID(), civilian.getDamage());
                    humanHelper.setFirstBuriedness(civilian.getID(), civilian.getBuriedness());
                }
            }
        }
    }

    private void processEmptyBuildingMessage(Packet packet) {
        EmptyBuildingMessage emptyBuildingMessage;
        EntityID id;

        for (Message message : packet) {
            emptyBuildingMessage = (EmptyBuildingMessage) message;
            id = indexSort.getAreaID(emptyBuildingMessage.getAreaIdIndex());
//            world.getUnvisitedBuildings().remove(id);
//            world.getMrlBuilding(id).setVisited();
            world.getEmptyBuildings().add(id);

        }
    }

    private void processVisitedBuildingMessage(Packet
                                                       packet, EntityID sender) {
        VisitedBuildingMessage visitedBuildingMessage;
        EntityID id;

        for (Message message : packet) {
            visitedBuildingMessage = (VisitedBuildingMessage) message;
            id = indexSort.getAreaID(visitedBuildingMessage.getAreaIdIndex());
//            world.getUnvisitedBuildings().remove(id);
//            world.getVisitedBuildings().add(id);
            world.setBuildingVisited(id);
//            world.getMrlBuilding(id).setVisited();
            //System.err.println("<<<<<<" + world.getTime() + " " + world.getSelf().getID() + " Received:" + (EntityID) id + " from:" +sender);
        }
    }

    private void processStuckAgentMessage(Packet packet, EntityID sender) {

        StuckAgentMessage stuckAgentMessage;
        Human human = (Human) world.getEntity(sender);
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
//            if (sender.equals(MrlPlatoonAgent.CHECK_ID2)) {
//                    System.err.print("");
//            }
            stuckAgentMessage = (StuckAgentMessage) message;
            if (stuckAgentMessage.getAreaIdIndex() != 65535) {
                if (propertyHelper.getPropertyTime(human.getPositionProperty()) < time) {
                    human.setPosition(indexSort.getAreaID(stuckAgentMessage.getAreaIdIndex()));
                    propertyHelper.setPropertyTime(human.getPositionProperty(), time);
                }
                humanHelper.setLockedByBlockade(human.getID(), true);
            } else {
                humanHelper.setLockedByBlockade(human.getID(), false);
            }
        }
    }

    private void processImpassableNodeMessage(Packet packet) {
        ImpassableNodeMessage impassableNodeMessage;
        Graph graph = world.getPlatoonAgent().getPathPlanner().getGraph();
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
            impassableNodeMessage = (ImpassableNodeMessage) message;
            graph.getNode(new EntityID(impassableNodeMessage.getNodeId())).setPassable(false, time);
        }

    }

    private void processHeardCivilianMessage(Packet packet) {
        HeardCivilianMessage heardCivilianMessage;

        for (Message message : packet) {
            heardCivilianMessage = (HeardCivilianMessage) message;
            int id = heardCivilianMessage.getCivilianId() + maxID;
            Civilian civilian = (Civilian) world.getEntity(new EntityID(id));
            if (civilian == null) {
                civilian = new Civilian(new EntityID(id));
                world.addNewCivilian(civilian);
            }
            civilianHelper.addHeardCivilianPosition(civilian.getID(), heardCivilianMessage.getLocationX(), heardCivilianMessage.getLocationY());
        }
    }

    private void processExtinguishedBuildingMessage(Packet packet) {
        ExtinguishedBuildingMessage extinguishedBuildingMessage;
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
            extinguishedBuildingMessage = (ExtinguishedBuildingMessage) message;
            Building building = (Building) world.getEntity(indexSort.getAreaID(extinguishedBuildingMessage.getAreaIdIndex()));

            if (propertyHelper.getPropertyTime(building.getFierynessProperty()) < time) {
                building.setFieryness(extinguishedBuildingMessage.getFieriness());
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
    }

    private void processCLStuckAgentMessage(Packet packet) {
        CLStuckAgentMessage clStuckAgentMessage;
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
            clStuckAgentMessage = (CLStuckAgentMessage) message;
            Human human = (Human) world.getEntity(indexSort.getAgentID(clStuckAgentMessage.getAgentIdIndex()));
            if (clStuckAgentMessage.getAreaIdIndex() != 65535) {
                if (propertyHelper.getPropertyTime(human.getPositionProperty()) < time) {
                    human.setPosition(indexSort.getAreaID(clStuckAgentMessage.getAreaIdIndex()));
                    propertyHelper.setPropertyTime(human.getPositionProperty(), time);
                }
                humanHelper.setLockedByBlockade(human.getID(), true);
            } else {
                humanHelper.setLockedByBlockade(human.getID(), false);
            }
        }
    }

    private void processClearedPathMessage(Packet packet) {
        ClearedPathMessage clearedPathMessage;
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
            clearedPathMessage = (ClearedPathMessage) message;
            Path path = world.getPath(indexSort.getPathID(clearedPathMessage.getPathIdIndex()));
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
    }

    private void processCLBuriedAgentMessage(Packet packet) {
        CLBuriedAgentMessage cLBuriedAgentMessage;
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
            cLBuriedAgentMessage = (CLBuriedAgentMessage) message;
            Human human = (Human) world.getEntity(indexSort.getAgentID(cLBuriedAgentMessage.getAgentIdIndex()));
            if (propertyHelper.getEntityLastUpdateTime(human) < time) {
                human.setHP(cLBuriedAgentMessage.getHealthPoint());
                propertyHelper.setPropertyTime(human.getHPProperty(), time);
                human.setDamage(cLBuriedAgentMessage.getDamage());
                propertyHelper.setPropertyTime(human.getDamageProperty(), time);
                human.setBuriedness(cLBuriedAgentMessage.getBuriedness());
                propertyHelper.setPropertyTime(human.getBuriednessProperty(), time);
                human.setPosition(indexSort.getAreaID(cLBuriedAgentMessage.getAreaIdIndex()));
                propertyHelper.setPropertyTime(human.getPositionProperty(), time);
                if (human.getBuriedness() > 0 && human.getDamage() == 0) {
                    human.setDamage(6); //todo....
                }
                if (humanHelper.getFirstHP(human.getID()) < 0) {
                    humanHelper.setFirstHP(human.getID(), human.getHP());
                    humanHelper.setFirstDamage(human.getID(), human.getDamage());
                    humanHelper.setFirstBuriedness(human.getID(), human.getBuriedness());

                }
                if (human.getHP() > 0 && human.getBuriedness() > 0 && !world.getBuriedAgents().contains(human.getID())) {
                    world.getBuriedAgents().add(human.getID());
                }
                if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
                    if (world.getSelfHuman() instanceof AmbulanceTeam) {
                        System.out.println(">>> " + world.getTime() + " " + world.getSelf().getID() + " ID:" + human.getID() + " DMG:" + human.getDamage());
                    }
                }
            }
        }
    }

    private void processCivilianSeenMessage(Packet packet) {
        CivilianSeenMessage civilianSeenMessage;
//        int time = packet.getHeader().getPacketCycle(world.getTime()); // todo: bayad chekc beshe ke kodoom time doroste.    AGHAYE GOHARDANI
        int time = world.getTime();
        EntityID positionID;

        for (Message message : packet) {
            Civilian civilian;
            civilianSeenMessage = (CivilianSeenMessage) message;
//            System.err.println("RECEIVE CivilianSeenMessage : id = "+civilianSeenMessage.getCivilianId()+" buriedness = "+civilianSeenMessage.getBuriedness()+" damage = "+civilianSeenMessage.getDamage()+" hp = "+civilianSeenMessage.getHealthPoint()+" posIndex = "+civilianSeenMessage.getAreaIdIndex()+"  "+civilianSeenMessage.toString());
            civilian = (Civilian) world.getEntity(new EntityID(civilianSeenMessage.getCivilianId() + maxID));
            boolean shouldUpdate = false;
            boolean isFirstTime = false;

            if (civilian == null) {
                civilian = new Civilian(new EntityID((civilianSeenMessage.getCivilianId() + maxID)));
                world.addNewCivilian(civilian);
                shouldUpdate = true;
            } else if (propertyHelper.getEntityLastUpdateTime(civilian) < time) {
                shouldUpdate = true;
            }
            if (!civilian.isBuriednessDefined()) {
                isFirstTime = true;
            }

            positionID = indexSort.getAreaID(civilianSeenMessage.getAreaIdIndex());
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
                civilian.setHP(civilianSeenMessage.getHealthPoint());
                propertyHelper.setPropertyTime(civilian.getHPProperty(), time);
                civilian.setDamage(civilianSeenMessage.getDamage());
                propertyHelper.setPropertyTime(civilian.getDamageProperty(), time);
                civilian.setBuriedness(civilianSeenMessage.getBuriedness());
                propertyHelper.setPropertyTime(civilian.getBuriednessProperty(), time);
                humanHelper.setFromSense(civilian.getID(), false);
                //Informations
//            humanHelper.setCurrentHP(civilian.getID(), message.getHP());
//            humanHelper.setCurrentDamage(civilian.getID(), message.getDamage());

                if (isFirstTime) {
                    humanHelper.setFirstHP(civilian.getID(), civilian.getHP());
                    humanHelper.setFirstDamage(civilian.getID(), civilian.getDamage());
                    humanHelper.setFirstBuriedness(civilian.getID(), civilian.getBuriedness());
                }


                humanHelper.setTimeToRefuge(civilian.getID(), civilianSeenMessage.getTimeToRefuge());
//                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + civilian.getID() + " >>> NearestRefuge Message >> " +civilianSeenMessage.getTimeToRefuge() + " " + indexSort.getAreaID(index));
//
//                    if(DEBUG_AMBULANCE_TEAM) {
//                        System.out.println(world.getTime()+" "+world.getSelf().getID()+" "+civilian.getID() +" >>> NearestRefuge Message >> "+message.getTimeToRefuge()+" "+message.getNearestRefuge());
//                    }
            }
        }
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

    private void ProcessChannelScannerMessage(Packet packet) {
        ChannelScannerMessage channelScannerMessage;
        Channel channel;
        List<Channel> ATChannels = new ArrayList<Channel>();
        List<Channel> FBChannels = new ArrayList<Channel>();
        List<Channel> PFChannels = new ArrayList<Channel>();

        for (Message message : packet) {
            channelScannerMessage = (ChannelScannerMessage) message;
            channel = channels.getChannel(channelScannerMessage.getChannelId());

            channel.setRepeatCont(Priority.Low, channelScannerMessage.getRepeatForPriorityOne());
            channel.setRepeatCont(Priority.Medium, channelScannerMessage.getRepeatForPriorityTwo());
            channel.setRepeatCont(Priority.High, channelScannerMessage.getRepeatForPriorityThree());
            channel.setRepeatCont(Priority.VeryHigh, channelScannerMessage.getRepeatForPriorityFour());

            switch (channelScannerMessage.getAgentInThisChannel()) {
                case 0:
                    ATChannels.add(channel);
                    break;
                case 1:
                    FBChannels.add(channel);
                    break;
                case 2:
                    PFChannels.add(channel);
                    break;
                case 3:
                    ATChannels.add(channel);
                    FBChannels.add(channel);
                    break;
                case 4:
                    FBChannels.add(channel);
                    PFChannels.add(channel);
                    break;
                case 5:
                    PFChannels.add(channel);
                    ATChannels.add(channel);
                    break;
                case 6:
                    ATChannels.add(channel);
                    FBChannels.add(channel);
                    PFChannels.add(channel);
                    break;
                default:
                    if (DEBUG_MESSAGING) {
                        System.err.println(platoonAgent.getDebugString() + " - Channel[ " + channel.getId() + " ] is unemployed.");
//                        Debbuge.fileAppending(platoonAgent.getDebugString() + " - Channel[ " + channel.getId() + " ] is unemployed.");
                    }
            }
        }

        int agentInThisChannel;
        int ATSize = world.getAmbulanceTeams().size();
        int PFSize = world.getPoliceForces().size();
        int FBSize = world.getFireBrigades().size();
        for (Channel ch : channels) {
            agentInThisChannel = 0;
            if (ATChannels.contains(ch)) {
                agentInThisChannel += ATSize;
            }
            if (FBChannels.contains(ch)) {
                agentInThisChannel += FBSize;
            }
            if (PFChannels.contains(ch)) {
                agentInThisChannel += PFSize;
            }
            ch.setAverageBandwidth(agentInThisChannel);
        }
        channels.setATChannels(ATChannels);
        channels.setPFChannels(PFChannels);
        channels.setFBChannels(FBChannels);

        if ((world.getSelf() instanceof MrlAmbulanceTeam) || (world.getSelf() instanceof MrlAmbulanceCentre)) {
            channels.sendSubscribeToHear(world.getSelf(), channels.getATChannels());
        } else if ((world.getSelf() instanceof MrlFireBrigade) || (world.getSelf() instanceof MrlFireStation)) {
            channels.sendSubscribeToHear(world.getSelf(), channels.getFBChannels());
        } else if ((world.getSelf() instanceof MrlPoliceForce) || (world.getSelf() instanceof MrlPoliceOffice)) {
            channels.sendSubscribeToHear(world.getSelf(), channels.getPFChannels());
        }
        messageManager.setMyOwnBW();

        if (DEBUG_MESSAGING) {
            String text;
            world.printData("");
            System.out.println(" -----------------------------------------");
            System.out.print("AmbulanceTeam channels: ");
            text = "AmbulanceTeam channels: ";
            for (Channel c : channels.getATChannels()) {
                System.out.print("  " + c.getId());
                text += "  " + c.getId();
            }
            System.out.println("");
            System.out.print("PoliceForce channels: ");
            text += "PoliceForce channels: ";
            for (Channel c : channels.getPFChannels()) {
                System.out.print("  " + c.getId());
                text += "  " + c.getId();
            }
            System.out.println("");
            System.out.print("FireBrigade channels: ");
            text += "FireBrigade channels: ";
            for (Channel c : channels.getFBChannels()) {
                System.out.print("  " + c.getId());
                text += "  " + c.getId();
            }
            System.out.println("");
            System.out.println(" -----------------------------------------");
//            Debbuge.fileAppending(text);
        }
    }

    private void processBurningBuildingMessage(Packet packet) {
        BurningBuildingMessage burningBuildingMessage;
        Building building;
        int time = packet.getHeader().getPacketCycle(world.getTime());

        for (Message message : packet) {
            burningBuildingMessage = (BurningBuildingMessage) message;
            building = (Building) world.getEntity(indexSort.getAreaID(burningBuildingMessage.getAreaIdIndex()));
            if (propertyHelper.getPropertyTime(building.getFierynessProperty()) < time) {
                if (building.isFierynessDefined() && building.getFieryness() == 8 && burningBuildingMessage.getFieriness() != 8) {
                    if (MRLConstants.LAUNCH_VIEWER) {
                        System.out.println("aaaa");
                    }
                }
                building.setFieryness(burningBuildingMessage.getFieriness());
                propertyHelper.setPropertyTime(building.getFierynessProperty(), time);
                building.setTemperature(burningBuildingMessage.getTemperature());
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
//            System.out.println("ProcessMessageHelper ["  + sender + "] Time:"+ world.getTime()
//                    + " Building ID:" + building.getID()
//                    + " Fieryness:" + building.getFieryness()
//                    + " Receiver:" + world.getSelf().getID());
        }
    }

    private void ProcessBuriedAgentMessage(Packet packet, EntityID sender) {
        BuriedAgentMessage buriedAgentMessage;
        Human human = (Human) world.getEntity(sender);
//        int time = packet.getHeader().getPacketCycle(world.getTime());
        int time = world.getTime();

        for (Message message : packet) {
            if (propertyHelper.getEntityLastUpdateTime(human) <= time) {
                buriedAgentMessage = (BuriedAgentMessage) message;
                human.setHP(buriedAgentMessage.getHealthPoint());
                propertyHelper.setPropertyTime(human.getHPProperty(), time);
                human.setDamage(buriedAgentMessage.getDamage());
                propertyHelper.setPropertyTime(human.getDamageProperty(), time);
                human.setBuriedness(buriedAgentMessage.getBuriedness());
                propertyHelper.setPropertyTime(human.getBuriednessProperty(), time);
                human.setPosition(indexSort.getAreaID(buriedAgentMessage.getAreaIdIndex()));
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
    }

    private void processAgentInfoMessage(Packet packet, EntityID sender) {
        AgentInfoMessage agentInfoMessage;
        Human human = (Human) world.getEntity(sender);
//        int time = packet.getHeader().getPacketCycle(world.getTime());
        int time = world.getTime();

        for (Message message : packet) {
            agentInfoMessage = (AgentInfoMessage) message;

            if (propertyHelper.getPropertyTime(human.getPositionProperty()) < time) {
                human.setPosition(indexSort.getAreaID(agentInfoMessage.getAreaIdIndex()));
                propertyHelper.setPropertyTime(human.getPositionProperty(), time);
                humanHelper.setAgentSate(human.getID(), agentInfoMessage.getState());
            }
        }
    }

    private void processClearedRoadMessage(Packet packet) {
        ClearedRoadMessage clearedRoadMessage;
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);

        for (Message message : packet) {
            clearedRoadMessage = (ClearedRoadMessage) message;
            Road road = (Road) world.getEntity(world.getIndexes().getAreaID(clearedRoadMessage.getAreaIdIndex()));
            road.setBlockades(new ArrayList<EntityID>());
//            MrlRoad mrlRoad = world.getMrlRoad(road.getID());
//            mrlRoad.reset();
//            roadHelper.updatePassably(mrlRoad);
//            world.printData(" ----------------------- receive clear road message:   "+road);

            roadHelper.setRoadPassable(road.getID(), true);
        }
    }

}
