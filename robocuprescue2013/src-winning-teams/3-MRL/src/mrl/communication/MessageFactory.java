package mrl.communication;

import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.comparator.ConstantComparators;
import mrl.communication.messages.*;
import mrl.helper.HumanHelper;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForce;
import mrl.world.IndexSort;
import mrl.world.MrlWorld;
import mrl.world.routing.graph.Node;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Mostafa Shabani.
 * Date: Feb 7, 2011
 * Time: 12:25:39 PM
 */
public class MessageFactory {
    private MessageManager messageManager;
    private AmbulanceUtilities ambulanceUtilities;
    private int countDown = 5;

    public MessageFactory(MessageManager messageManager, MrlWorld world) {
        this.messageManager = messageManager;
        this.ambulanceUtilities = new AmbulanceUtilities(world);

    }

    public void createMessages(MrlWorld world, MrlPlatoonAgent platoonAgent) {
        IndexSort indexSort = world.getIndexes();
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);

        // agent info
        AgentInfoMessage agentInfoMessage = new AgentInfoMessage(indexSort.getAreaIndex(world.getSelfPosition().getID()), platoonAgent.getAgentState());
//        System.out.println(world.getSelf().getID() + " send AgentInfo");
        messageManager.addMessageToQueue(agentInfoMessage, MessageType.AgentInfo);
        // buried & CLBuried Agent
        int damage;
        Human human = world.getSelfHuman();
        if (human.getBuriedness() > 0) {
            damage = human.getDamage();
            if (damage == 0) {
                damage = 1;
            }
//            if (human.getHP() / damage > human.getBuriedness()) {


            if (ambulanceUtilities.isAlivable(human)) {
                BuriedAgentMessage message = new BuriedAgentMessage(human.getHP(), human.getBuriedness(), damage, indexSort.getAreaIndex(human.getPosition()));
                messageManager.addMessageToQueue(message, MessageType.BuriedAgent);
                CLBuriedAgentMessage clMessage = new CLBuriedAgentMessage(human.getHP(), human.getBuriedness(), damage, indexSort.getAreaIndex(human.getPosition()), indexSort.getAgentIndex(human.getID()));
                messageManager.addMessageToQueue(clMessage, MessageType.CLBuriedAgent);
            } else {
                BuriedAgentMessage message = new BuriedAgentMessage(0, human.getBuriedness(), damage, indexSort.getAreaIndex(human.getPosition()));
                messageManager.addMessageToQueue(message, MessageType.BuriedAgent);
                CLBuriedAgentMessage clMessage = new CLBuriedAgentMessage(0, human.getBuriedness(), damage, indexSort.getAreaIndex(human.getPosition()), indexSort.getAgentIndex(human.getID()));
                messageManager.addMessageToQueue(clMessage, MessageType.CLBuriedAgent);
            }
        } else if (humanHelper.isSelfHealthyRepeat()) {
            BuriedAgentMessage message = new BuriedAgentMessage(human.getHP(), human.getBuriedness(), human.getDamage(), indexSort.getAreaIndex(human.getPosition()));
            messageManager.addMessageToQueue(message, MessageType.BuriedAgent);
            CLBuriedAgentMessage clMessage = new CLBuriedAgentMessage(human.getHP(), human.getBuriedness(), human.getDamage(), indexSort.getAreaIndex(human.getPosition()), indexSort.getAgentIndex(human.getID()));
            messageManager.addMessageToQueue(clMessage, MessageType.CLBuriedAgent);
        }
        // stuck and CLStuck agent and free and CLFree agent
        if (world.getPlatoonAgent().isStuck()) {
            StuckAgentMessage message = new StuckAgentMessage(indexSort.getAreaIndex(world.getSelfPosition().getID()));
            messageManager.addMessageToQueue(message, MessageType.StuckAgent);
            CLStuckAgentMessage clMessage = new CLStuckAgentMessage(indexSort.getAreaIndex(world.getSelfPosition().getID()), indexSort.getAgentIndex(platoonAgent.getID()));
            messageManager.addMessageToQueue(clMessage, MessageType.CLStuckAgent);
        } else if (countDown > 0) {
            countDown--;
            StuckAgentMessage message = new StuckAgentMessage(65535);
            messageManager.addMessageToQueue(message, MessageType.StuckAgent);
            CLStuckAgentMessage clMessage = new CLStuckAgentMessage(65535, indexSort.getAgentIndex(platoonAgent.getID()));
            messageManager.addMessageToQueue(clMessage, MessageType.CLStuckAgent);
        }
        // visited building
        for (EntityID id : world.getThisCycleVisitedBuildings()) {
            VisitedBuildingMessage message = new VisitedBuildingMessage(indexSort.getAreaIndex(id));
            messageManager.addMessageToQueue(message, MessageType.VisitedBuilding);
            //System.out.println(">>>>>>"+world.getTime()+" "+world.getSelf().getID()+" Saw:"+(EntityID) id);
        }
        world.getThisCycleVisitedBuildings().clear();

        // heard civilian
        Pair<Integer, Integer> location = world.getSelfLocation();
        for (EntityID id : world.getHeardCivilians()) {
            HeardCivilianMessage message = new HeardCivilianMessage(id.getValue() - world.maxID, location.first(), location.second());
            messageManager.addMessageToQueue(message, MessageType.HeardCivilian);
        }
        world.getHeardCivilians().clear();

//        System.out.println("SIZZZZZEEEE : " + platoonAgent.getThisCycleVisitedBuildings().size());
        //road
        if (!(platoonAgent instanceof MrlPoliceForce)) {

            List<Node> sendNodes = new ArrayList<Node>();
            int time = world.getTime();
            for (Road road : world.getRoadsSeen()) {
                if (roadHelper.canSendMessage(road.getID(), time)) {
                    if (!roadHelper.isPassable(road.getID())) {
//                        BlockedRoadMessage message = new BlockedRoadMessage(indexSort.getAreaIndex(road.getID()));
//                        messageManager.addMessageToQueue(message, MessageType.Block);
                        List<Node> nodes = world.getPlatoonAgent().getPathPlanner().getGraph().getAreaNodes(road.getID());
                        for (Node node : nodes) {
                            if (!node.isPassable() && !sendNodes.contains(node)) {
                                sendNodes.add(node);
                                ImpassableNodeMessage impassableNodeMessage = new ImpassableNodeMessage(node.getId().getValue());
                                messageManager.addMessageToQueue(impassableNodeMessage, MessageType.ImpassableNode);
                            }
                        }
                    }
                }
            }
        }

//        List<EntityID> agentsInSamePosition = new ArrayList<EntityID>();
        SortedSet<EntityID> agentsInSamePosition = new TreeSet<EntityID>(ConstantComparators.EntityID_COMPARATOR);
        StandardEntity position = world.getSelfPosition();
        StandardEntity agent;
        boolean iCanSendMessage = true;

        for (EntityID id : world.getChanges()) {
            agent = world.getEntity(id);
            if ((agent instanceof Human) && !(agent instanceof Civilian) && ((Human) agent).isPositionDefined()) {
                if (position.getID().equals(((Human) agent).getPosition())) {
                    agentsInSamePosition.add(id);
                }
            }
        }
        //do not send this info, because it is an agent and if it was buried, it could itself send buriedMessage
        if (agentsInSamePosition.size() > 1) {
//            Collections.sort(agentsInSamePosition, ConstantComparators.EntityID_COMPARATOR);
//            if (!agentsInSamePosition.get(0).equals(world.getSelf().getID())) {
//                iCanSendMessage = false;
//            }
            if (!agentsInSamePosition.first().equals(world.getSelf().getID())) {
                iCanSendMessage = false;
            }

        }

        for (EntityID id : world.getChanges()) {
            StandardEntity entity = world.getEntity(id);
            // burning building
            if (entity instanceof Building) {
                Building building = (Building) entity;
                if (building.isFierynessDefined()) {
                    if (building.getFieryness() > 3 && building.getFieryness() < 8) {
                        ExtinguishedBuildingMessage message = new ExtinguishedBuildingMessage(indexSort.getAreaIndex(id), building.getFieryness());
                        messageManager.addMessageToQueue(message, MessageType.ExtinguishedBuilding);
//                        world.printData(" send::::::: extinguishBuilding:" + building+" f:"+message.getFieriness());

                    } else if (building.getFieryness() > 0) {
                        BurningBuildingMessage message = new BurningBuildingMessage(indexSort.getAreaIndex(id), building.getFieryness(), building.getTemperature());
                        messageManager.addMessageToQueue(message, MessageType.BurningBuilding);
//                        world.printData(" send:::::::::: burningBuilding:" + building+" f:"+message.getFieriness()+" temp:"+message.getTemperature());

                    }
                }
            }
            // civilian
            else if (iCanSendMessage && (entity instanceof Civilian)) {
                Civilian civilian = (Civilian) entity;
                Building building;
                if (!(world.getEntity(civilian.getPosition()) instanceof Refuge)
                        && (world.getEntity(civilian.getPosition()) instanceof Building)) {
                    building = (Building) world.getEntity(civilian.getPosition());
                    CivilianSeenMessage message = new CivilianSeenMessage((id.getValue() - messageManager.getMaxID()), civilian.getBuriedness(), civilian.getDamage(),
                            civilian.getHP(), indexSort.getAreaIndex(civilian.getPosition()), humanHelper.getTimeToRefuge(id));
                    messageManager.addMessageToQueue(message, MessageType.CivilianSeen);

                    CLFullBuildingMessage message1 = new CLFullBuildingMessage((id.getValue() - messageManager.getMaxID()), civilian.getBuriedness(), civilian.getDamage(),
                            civilian.getHP(), indexSort.getAreaIndex(civilian.getPosition()));
                    messageManager.addMessageToQueue(message1, MessageType.FullBuilding);

//                    if (world.isCommunicationLess() || world.isCommunicationLimited()) {
//                        MrlBuilding mrlBuilding;
//                        Road road;
//                        mrlBuilding = world.getMrlBuilding(building.getID());
//                        road = new Road(mrlBuilding.getEntrances().get(0).getNeighbour());
//                        CLFullBuildingMessage clFullBuildingMessage = new CLFullBuildingMessage(indexSort.getAreaIndex(civilian.getPosition()));
//                        world.getFullBuildings().add(road.getID());
//                        messageManager.addMessageToQueue(clFullBuildingMessage, clFullBuildingMessage.getType());
//
//                    }
                }
            }
//            // block
//            else if (entity instanceof Blockade) {
//                Blockade blockade = (Blockade) world.getEntity(id);
//                BlockedRoadMessage message = new BlockedRoadMessage(indexSort.getAreaIndex(blockade.getPosition()));
//                messageManager.addMessageToQueue(message, MessageType.Block);
//            }
        }
    }

    public void createMessages(MrlWorld world) {

        IndexSort indexSort = world.getIndexes();
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

//        // CLBuried Agent
//        for (StandardEntity entity : world.getPlatoonAgents()) {
//            Human human = (Human) entity;
//            if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
//                CLBuriedAgentMessage message = new CLBuriedAgentMessage(human.getHP(), human.getBuriedness(), human.getDamage(), indexSort.getAreaIndex(human.getPosition()), indexSort.getAgentIndex(human.getID()));
//                messageManager.addMessageToQueue(message, MessageType.CLBuriedAgent);
//            }
//        }

        for (EntityID id : world.getChanges()) {
            StandardEntity entity = world.getEntity(id);
            // burning building
            if (entity instanceof Building) {
                Building building = (Building) entity;
                if (building.isFierynessDefined()) {
                    if (building.getFieryness() > 3 && building.getFieryness() < 8) {
                        ExtinguishedBuildingMessage message = new ExtinguishedBuildingMessage(indexSort.getAreaIndex(id), building.getFieryness());
                        messageManager.addMessageToQueue(message, MessageType.ExtinguishedBuilding);
                    } else if (building.getFieryness() > 0 && building.getFieryness() != 8) {
                        BurningBuildingMessage message = new BurningBuildingMessage(indexSort.getAreaIndex(id), building.getFieryness(), building.getTemperature());
                        messageManager.addMessageToQueue(message, MessageType.BurningBuilding);
                    }
                }
            }
            // civilian
            else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if (!(world.getEntity(civilian.getPosition()) instanceof Refuge)
                        && (world.getEntity(civilian.getPosition()) instanceof Building)
                        && civilian.getDamage() != 0) {
                    CivilianSeenMessage message = new CivilianSeenMessage((id.getValue() - messageManager.getMaxID()), civilian.getBuriedness(), civilian.getDamage(),
                            civilian.getHP(), indexSort.getAreaIndex(civilian.getPosition()), humanHelper.getTimeToRefuge(id));
                    messageManager.addMessageToQueue(message, MessageType.CivilianSeen);
                }
            }
        }
    }
}
