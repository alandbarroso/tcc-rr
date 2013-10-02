package mrl.ambulance;

import javolution.util.FastMap;
import mrl.ambulance.marketLearnerStrategy.AmbulanceTeamBid;
import mrl.ambulance.marketLearnerStrategy.Task;
import mrl.ambulance.marketLearnerStrategy.VictimImportance;
import mrl.ambulance.structures.RescuedCivilian;
import mrl.common.MRLConstants;
import mrl.communication.*;
import mrl.communication.messages.EmptyBuildingMessage;
import mrl.communication.messages.ambulanceMessages.*;
import mrl.helper.PropertyHelper;
import mrl.platoon.MrlPlatoonAgent;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by P.D.G.
 * User: Pooyad
 * Date: Nov 2, 2010
 * Time: 7:02:07 PM
 */
public class AmbulanceMessageHelper extends PlatoonMessageHelper {
    MrlAmbulanceTeamWorld world;
    private int lastClearTime = -1;

    public AmbulanceMessageHelper(MrlAmbulanceTeamWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
    }

    public boolean processMessage(Packet packet, EntityID sender) {
        int time = packet.getHeader().getPacketCycle(world.getTime());

        if ((world.getTime() - time) <= 2 && packet.getHeader().getPacketType().equals(MessageType.AmbulanceCivilianBid)) {
            AmbulanceCivilianBidMessage agentIsBuriedMessage;
            for (Message message : packet) {
                agentIsBuriedMessage = (AmbulanceCivilianBidMessage) message;
                processCivilianBidMessage(agentIsBuriedMessage, time, sender);
            }
            return false;
        }

        if ((world.getTime() - time) <= 2 && packet.getHeader().getPacketType().equals(MessageType.AmbulanceAgentBid)) {
            AmbulanceAgentBidMessage agentIsBuriedMessage;
            for (Message message : packet) {
                agentIsBuriedMessage = (AmbulanceAgentBidMessage) message;
                processAgentBidMessage(agentIsBuriedMessage, time, sender);
            }
            return false;
        }

        if (packet.getHeader().getPacketType().equals(MessageType.AmbulanceLeaderBid)) {
            AmbulanceLeaderBidMessage ambulanceLeaderBidMessage;
            for (Message message : packet) {
                ambulanceLeaderBidMessage = (AmbulanceLeaderBidMessage) message;
                processLeaderBidMessage(ambulanceLeaderBidMessage);
            }

            return false;
        }

        if (packet.getHeader().getPacketType().equals(MessageType.AmbulanceCivilianTask)) {
            AmbulanceCivilianTaskMessage ambulanceTaskMessage;
            for (Message message : packet) {
                ambulanceTaskMessage = (AmbulanceCivilianTaskMessage) message;
                processCivilianTaskMessage(ambulanceTaskMessage, sender);
            }
            return false;

        }

        if (packet.getHeader().getPacketType().equals(MessageType.AmbulanceAgentTask)) {
            AmbulanceAgentTaskMessage ambulanceTaskMessage;
            for (Message message : packet) {
                ambulanceTaskMessage = (AmbulanceAgentTaskMessage) message;
                processAgentTaskMessage(ambulanceTaskMessage, sender);
            }
            return false;

        }

        if (packet.getHeader().getPacketType().equals(MessageType.ValueFunctionMessage)) {
            ValueFunctionMessage valueFunctionMessage;
            for (Message message : packet) {
                valueFunctionMessage = (ValueFunctionMessage) message;
                processValueFunctionMessage(valueFunctionMessage);
            }
            return false;
        }
        if (packet.getHeader().getPacketType().equals(MessageType.RescuedCivilian)) {
            RescuedCivilianMessage rescuedCivilianMessage;
            for (Message message : packet) {
                rescuedCivilianMessage = (RescuedCivilianMessage) message;
                processRescuedCivilianMessage(rescuedCivilianMessage, sender);
            }
            return false;
        }
        if (packet.getHeader().getPacketType().equals(MessageType.CurrentRescuingCivilianMessage)) {
            CurrentRescuingCivilianMessage currentRescuingCivilianMessage;
            for (Message message : packet) {
                currentRescuingCivilianMessage = (CurrentRescuingCivilianMessage) message;
                processCurrentRescuingCivilianMessage(currentRescuingCivilianMessage);
            }
            return false;
        }
        if (packet.getHeader().getPacketType().equals(MessageType.StartRescuingCivilianMessage)) {
            StartRescuingCivilianMessage startRescuingCivilianMessage;
            for (Message message : packet) {
                startRescuingCivilianMessage = (StartRescuingCivilianMessage) message;
                processStartRescuingCivilianMessage(startRescuingCivilianMessage, time, sender);
            }
            return false;
        }
        if (packet.getHeader().getPacketType().equals(MessageType.TransportingCivilianMessage)) {
            TransportingCivilianMessage transportingCivilianMessage;
            for (Message message : packet) {
                transportingCivilianMessage = (TransportingCivilianMessage) message;
                processTransportingCivilianMessage(transportingCivilianMessage, time, sender);
            }
            return false;
        }
        if (packet.getHeader().getPacketType().equals(MessageType.LoaderMessage)) {
            LoaderMessage loaderMessage;
            for (Message message : packet) {
                loaderMessage = (LoaderMessage) message;
                processLoaderMessage(loaderMessage, time, sender);
            }
            return false;
        }

        return false;
    }

    // Leader Bid Message
    public void sendVictimsToSell(List<VictimImportance> victimsToSell) {

        if (victimsToSell == null) {
            return;
        }
        for (VictimImportance victimImportance : victimsToSell) {
            AmbulanceLeaderBidMessage leaderBidMessage = new AmbulanceLeaderBidMessage(
                    victimImportance.getVictim().getID().getValue());
            messageManager.addMessageToQueue(leaderBidMessage, leaderBidMessage.getType());

            // because the sender wouldn't recive its message, it should keep them for itself
            world.getLeaderBids().add(victimImportance.getVictim().getID());

        }

//        System.out.println(world.getTime() + " VICTIMS SENT TO SELL " + world.getSelf().getID());

    }

    public void processLeaderBidMessage(AmbulanceLeaderBidMessage message) {
        world.getLeaderBids().add(new EntityID(message.getVictimId()));
    }

    // Bid Message
    public void sendBidMessage(List<AmbulanceTeamBid> bids) {
        AmbulanceCivilianBidMessage civilianBidMessage;
        AmbulanceAgentBidMessage agentBidMessage;
        for (AmbulanceTeamBid bid : bids) {

            if (bid.isCivilian()) {
                civilianBidMessage = new AmbulanceCivilianBidMessage(bid.getHumanID().getValue() - messageManager.getMaxID(), bid.getBidValue()); //todo: MESS-> joda beshe
                messageManager.addMessageToQueue(civilianBidMessage, civilianBidMessage.getType());

            } else {
                agentBidMessage = new AmbulanceAgentBidMessage(world.getIndexes().getAgentIndex(bid.getHumanID()), bid.getBidValue()); //todo: MESS-> joda beshe
                messageManager.addMessageToQueue(agentBidMessage, agentBidMessage.getType());
            }

            setVictimBidMap_For_Leader_Itself(bid);
        }
        // todo ==> Send others the civilian with higher CAOP in its Civilian List  and  request Their Max State Value And TTF
        // todo ==> in a AmbulanceCivilianBidMessage Structure send my data
        //
    }

    /**
     * because the leader won't get its message, we set it here
     *
     * @param bid the bade victim
     */
    private void setVictimBidMap_For_Leader_Itself(AmbulanceTeamBid bid) {
        ArrayList<Pair<EntityID, Integer>> bidPairs;
        bidPairs = world.getVictimBidsMap().get(bid.getHumanID());
        if (bidPairs == null) {
            bidPairs = new ArrayList<Pair<EntityID, Integer>>();
        }
        if (!bidPairs.contains(new Pair<EntityID, Integer>(bid.getBidderID(), bid.getBidValue())))
            bidPairs.add(new Pair<EntityID, Integer>(bid.getBidderID(), bid.getBidValue()));

        if (bid.getHumanID() == null) {
            System.out.print("");
        }
        world.getVictimBidsMap().put(bid.getHumanID(), bidPairs);
    }

    private void processCivilianBidMessage(AmbulanceCivilianBidMessage message, int time, EntityID sender) {
//        LOG.info(me() + " received civilianinformationmessage " + message);

        if (time < world.getTime() - 1)
            return;

        AmbulanceTeamBid bid = new AmbulanceTeamBid();
        ArrayList<AmbulanceTeamBid> bids;
        ArrayList<Pair<EntityID, Integer>> bidPairs;
        bid.setBidderID(new EntityID(sender.getValue()));
//        if (world.getEntity(new EntityID(message.getHumanId())) == null)
//            System.out.println("ohohohohohohoh");
        bid.setHumanID(new EntityID(message.getVictimCivilianId() + messageManager.getMaxID()));
        bid.setBidValue(message.getCAOP());

        bids = world.getBids().get(bid.getBidderID());
        if (bids == null) {
            bids = new ArrayList<AmbulanceTeamBid>();
        }
        bids.add(bid);

        if (!world.getBadeHumans().contains(bid.getHumanID())) {
            world.getBadeHumans().add(bid.getHumanID());
        }
        world.getBids().put(bid.getBidderID(), bids);

        bidPairs = world.getVictimBidsMap().get(bid.getHumanID());
        if (bidPairs == null) {
            bidPairs = new ArrayList<Pair<EntityID, Integer>>();
        }
        if (!bidPairs.contains(new Pair<EntityID, Integer>(bid.getBidderID(), bid.getBidValue())))
            bidPairs.add(new Pair<EntityID, Integer>(bid.getBidderID(), bid.getBidValue()));
        if (bid.getHumanID() == null) {
            System.out.print("");
        }

        world.getVictimBidsMap().put(bid.getHumanID(), bidPairs);

    }

    private void processAgentBidMessage(AmbulanceAgentBidMessage message, int time, EntityID sender) {
//        LOG.info(me() + " received civilianinformationmessage " + message);

        if (time < world.getTime() - 1)
            return;

        AmbulanceTeamBid bid = new AmbulanceTeamBid();
        ArrayList<AmbulanceTeamBid> bids;
        ArrayList<Pair<EntityID, Integer>> bidPairs;
        bid.setBidderID(new EntityID(sender.getValue()));
//        if (world.getEntity(new EntityID(message.getHumanId())) == null)
//            System.out.println("ohohohohohohoh");
        bid.setHumanID(world.getIndexes().getAgentID(message.getVictimAgentId()));
        bid.setBidValue(message.getCAOP());

        bids = world.getBids().get(bid.getBidderID());
        if (bids == null) {
            bids = new ArrayList<AmbulanceTeamBid>();
        }
        bids.add(bid);

        if (!world.getBadeHumans().contains(bid.getHumanID())) {
            world.getBadeHumans().add(bid.getHumanID());
        }
        world.getBids().put(bid.getBidderID(), bids);

        bidPairs = world.getVictimBidsMap().get(bid.getHumanID());
        if (bidPairs == null) {
            bidPairs = new ArrayList<Pair<EntityID, Integer>>();
        }
        if (!bidPairs.contains(new Pair<EntityID, Integer>(bid.getBidderID(), bid.getBidValue())))
            bidPairs.add(new Pair<EntityID, Integer>(bid.getBidderID(), bid.getBidValue()));
        if (bid.getHumanID() == null) {
            System.out.print("");
        }

        world.getVictimBidsMap().put(bid.getHumanID(), bidPairs);

    }


    // task message
    public void sendTasks() {
        EntityID agentID;
        EntityID victimID;
        AmbulanceCivilianTaskMessage civilianTaskMessage;
        AmbulanceAgentTaskMessage agentTaskMessage;
        for (Iterator it = world.getTaskAssignment().keySet().iterator(); it.hasNext(); ) {
            agentID = (EntityID) it.next();
            victimID = world.getTaskAssignment().get(agentID);
            //world.getEntity(victimID)==null means I don't have this civilian in my list so assign it to its bidder
            if (world.getEntity(victimID)== null || world.getEntity(victimID) instanceof Civilian) {
                civilianTaskMessage = new AmbulanceCivilianTaskMessage(world.getIndexes().getAgentIndex(agentID), victimID.getValue() - messageManager.getMaxID());//todo: MESS-> joda beshe
//            if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
//            System.out.println(" >>>>>>>>> " + world.getTime() + " " + world.getSelf().getID() + " SENT A TASK: " + agentID + " --> " + world.getTaskAssignment().get(agentID).getValue());
                messageManager.addMessageToQueue(civilianTaskMessage, MessageType.AmbulanceCivilianTask);
            } else {

                agentTaskMessage = new AmbulanceAgentTaskMessage(world.getIndexes().getAgentIndex(agentID), world.getIndexes().getAgentIndex(victimID));//todo: MESS-> joda beshe
                messageManager.addMessageToQueue(agentTaskMessage, MessageType.AmbulanceAgentTask);

            }
//            }
        }
    }

    public void processCivilianTaskMessage(AmbulanceCivilianTaskMessage message, EntityID sender) {

//        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
//            System.out.println(" <<<<<<<<< " + world.getTime() + " " + world.getSelf().getID() + " GOT A TASK: " + world.getIndexes().getAgentID(message.getAgentID()) + " --> " + message.getVictimID());
//        }

        if (!world.amIAmbulanceLeader() && world.getTime() != lastClearTime) {
            world.getTaskAssignment().clear();
            lastClearTime = world.getTime();
        }

        EntityID agentID = world.getIndexes().getAgentID(message.getAgentID());
        EntityID victimID = new EntityID(message.getVictimID() + messageManager.getMaxID());
        world.getTaskAssignment().put(agentID, victimID);

        if (world.getAmbulanceLeaderID() == null) {
            world.setAmbulanceLeaderID(sender);
        }
        keepAssignedAgents(agentID, victimID);
        keepTasks(agentID, victimID);

    }

    public void processAgentTaskMessage(AmbulanceAgentTaskMessage message, EntityID sender) {

//        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
//            System.out.println(" <<<<<<<<< " + world.getTime() + " " + world.getSelf().getID() + " GOT A TASK: " + world.getIndexes().getAgentID(message.getAgentID()) + " --> " + message.getVictimID());
//        }

        if (!world.amIAmbulanceLeader() && world.getTime() != lastClearTime) {
            world.getTaskAssignment().clear();
            lastClearTime = world.getTime();
        }

        EntityID agentID = world.getIndexes().getAgentID(message.getAgentID());
        EntityID victimID = world.getIndexes().getAgentID(message.getVictimID());
        world.getTaskAssignment().put(agentID, victimID);

        if (world.getAmbulanceLeaderID() == null) {
            world.setAmbulanceLeaderID(sender);
        }
        keepAssignedAgents(agentID, victimID);
        keepTasks(agentID, victimID);

    }

    private void keepAssignedAgents(EntityID agentID, EntityID victimID) {
        List<EntityID> assignedAgents = world.getAgentAssignment().get(victimID);
        if (assignedAgents == null) {
            assignedAgents = new ArrayList<EntityID>();
        }
        assignedAgents.add(agentID);
        world.getAgentAssignment().put(victimID, assignedAgents);
    }

    private void keepTasks(EntityID agentID, EntityID victimID) {
        Map<EntityID, Task> taskList = world.getTaskLists().get(agentID);
        if (taskList == null) {
            taskList = new FastMap<EntityID, Task>();
        }
        taskList.put(victimID, new Task(victimID));
        world.getTaskLists().put(agentID, taskList);
    }

    // Value Functions
    public void sendValueFunction(int time, int bestStateValue) {

        ValueFunctionMessage valueFunctionMessage
                = new ValueFunctionMessage(time, bestStateValue);
        messageManager.addMessageToQueue(valueFunctionMessage, valueFunctionMessage.getType());

        //todo seeeeeeeeeeeeeeeend

    }

    private void processValueFunctionMessage(ValueFunctionMessage message) {

        //todo check it wheter it is null or empty
        if (world.getValueFunctions().isEmpty() || world.getValueFunctions().get(message.getTime()) == null) {
            ArrayList<Integer> valueFunctions = new ArrayList<Integer>();
            valueFunctions.add(message.getValueFunction());
            world.getValueFunctions().put(message.getTime(), valueFunctions);
        } else {
            world.getValueFunctions().get(message.getTime()).add(message.getValueFunction());
        }
    }

    // Rescued Civilian
    public void sendRescuedCivilianMessage(EntityID shouldRescueCivilianId) {

        //todo       send it too all ATs
//        Human shouldRescueCivilian = (Human) world.getEntity(shouldRescueCivilianId);
        RescuedCivilianMessage rescuedCivilianMessage = new RescuedCivilianMessage(shouldRescueCivilianId.getValue() - messageManager.getMaxID());
        messageManager.addMessageToQueue(rescuedCivilianMessage, rescuedCivilianMessage.getType());

    }

    private void processRescuedCivilianMessage(RescuedCivilianMessage message, EntityID sender) {

        RescuedCivilian rescuedCivilian = new RescuedCivilian();
        rescuedCivilian.setAmbulanceID(sender);
        rescuedCivilian.setCivilianId(new EntityID(message.getCivilianId() + messageManager.getMaxID()));
//        rescuedCivilian.setHP(message.getHP());
//        rescuedCivilian.setTotalATsInThisRescue(message.getTotalATsInThisRescue());
//        rescuedCivilian.setTotalRescueTime(message.getTotalRescueTime());

        world.getCurrentlyRescuedCivilians().add(rescuedCivilian);
        world.getRescuedCivilians().add(rescuedCivilian.getCivilianId());
        world.getTransportingCivilians().remove((Civilian) world.getEntity(rescuedCivilian.getCivilianId()));


    }

    // Current Rescuing Civilian
    public void sendOthersMyCurrentRescuingCivilian(Civilian currentRescueCivilian, int currentTime) {
        CurrentRescuingCivilianMessage currentRescuingCivilianMessage
                = new CurrentRescuingCivilianMessage(currentRescueCivilian.getID().getValue(), currentTime);
        messageManager.addMessageToQueue(currentRescuingCivilianMessage, currentRescuingCivilianMessage.getType());

        //todo  send Others My CurrentRescuingCivilian
    }

    private void processCurrentRescuingCivilianMessage(CurrentRescuingCivilianMessage message) {

//        Pair<EntityID, EntityID> AmbulanceCivilianPair =
//                new Pair<EntityID, EntityID>(message.getSender(), new EntityID(message.getHumanId()));
//        Pair<Integer, Integer> startCurrentTimePair =
//                new Pair<Integer, Integer>(message., message.getCurrentTime());
//        world.getAmbulanceCivilianMap().put
//                (AmbulanceCivilianPair, startCurrentTimePair);

    }

    // Start Rescuing Civilian
    public void sendOthersMyStartRescuingCivilian(Civilian currentRescueCivilian, int startRescueTime) {
        StartRescuingCivilianMessage startRescuingCivilianMessage
                = new StartRescuingCivilianMessage(currentRescueCivilian.getID().getValue(), startRescueTime);

        messageManager.addMessageToQueue(startRescuingCivilianMessage, startRescuingCivilianMessage.getType());
        //todo  send Others My StartRescuingCivilian

    }

    public void sayOthersMyStartRescuingCivilian(Civilian currentRescueCivilian, int startRescueTime) {
        StartRescuingCivilianMessage startRescuingCivilianMessage
                = new StartRescuingCivilianMessage(currentRescueCivilian.getID().getValue(), startRescueTime);

        messageManager.addMessageToQueue(startRescuingCivilianMessage, startRescuingCivilianMessage.getType());
        //todo  send Others My StartRescuingCivilian
    }

    private void processStartRescuingCivilianMessage(StartRescuingCivilianMessage message, int time, EntityID sender) {
        Pair<EntityID, EntityID> AmbulanceCivilianPair =
                new Pair<EntityID, EntityID>(sender, new EntityID(message.getCivilianId()));
        Pair<Integer, Integer> startCurrentTimePair =
                new Pair<Integer, Integer>(message.getStartTime(), time);

        if (world.getAmbulanceCivilianMap() == null || world.getEntity(new EntityID(message.getCivilianId())) == null) {
//            System.out.println("nullllllllllll");
            return;
        }

//        System.out.println(world.getTime() + "---StartRescuing-------- me:" + world.getSelf().getID() + " sender:" + message.getSender());
        world.getAmbulanceCivilianMap().put
                (AmbulanceCivilianPair, startCurrentTimePair);
    }

    // transportingCivilian
    public void sendTransportingCivilian(EntityID currentRescueCivilianId) {
        if (currentRescueCivilianId == null) {
            return;
        }
        TransportingCivilianMessage transportingCivilianMessage = new TransportingCivilianMessage(currentRescueCivilianId.getValue() - messageManager.getMaxID());

        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println(" Transporting Civilian Sent------------- " + currentRescueCivilianId + " time:" + world.getTime());
        }
        messageManager.addMessageToQueue(transportingCivilianMessage, transportingCivilianMessage.getType());
    }

    private void processTransportingCivilianMessage(TransportingCivilianMessage message, int time, EntityID sender) {
        int id = message.getCivilianId() + messageManager.getMaxID();

        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println("meID:" + platoonAgent.getID() + " got Transproted civilian: " + id);
        }
        Civilian civ = (Civilian) world.getEntity(new EntityID(id));

        if (civ == null) {
            civ = new Civilian(new EntityID(id));
        }

        civ.setPosition(sender);

        world.getCivilians().add(civ);

        world.getHelper(PropertyHelper.class).setPropertyTime(civ.getPositionProperty(), time);
        if (!(world.getTransportingCivilians().contains(civ)))
            world.getTransportingCivilians().add(civ);
    }

    // Loader Message
    public void sendLoaderMessage(EntityID civilianID) {

        LoaderMessage loaderMessage = new LoaderMessage(civilianID.getValue() - messageManager.getMaxID());
        world.getLoaders().add(new Pair<EntityID, EntityID>(world.getSelf().getID(), civilianID));
//        loaderMessage.setId(civilianID.getValue());
        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println(" Loader Message Sent------------- " + world.getSelf().getID() + " time:" + world.getTime());
        }
        messageManager.addMessageToQueue(loaderMessage, loaderMessage.getType());
    }

    private void processLoaderMessage(LoaderMessage message, int time, EntityID sender) {

//        System.out.println("meID:" + platoonAgent.getID() + " got LoaderMessage from: " + sender.getValue() + " inCycle: " + time);
        world.getLoaders().add(new Pair<EntityID, EntityID>(sender, new EntityID(message.getCivilianId() + messageManager.getMaxID())));
    }


    /**
     * it gets entrance id of a full building and sends it in communication less situations
     *
     * @param id id of entrance
     */
//    public void sendCLFullBuildingMessage(EntityID id) {
//
//        CLFullBuildingMessage clFullBuildingMessage = new CLFullBuildingMessage(world.getIndexes().getAreaIndex(id));
//
//        world.getFullBuildings().add(id);
//
//        messageManager.addMessageToQueue(clFullBuildingMessage, clFullBuildingMessage.getType());
//    }


    /**
     * it gets entrance id of an empty building and sends it in communication less situations
     *
     * @param id id of entrance
     */
    public void sendCLEmptyBuildingMessage(EntityID id) {

        EmptyBuildingMessage emptyBuildingMessage = new EmptyBuildingMessage(world.getIndexes().getAreaIndex(id));

        world.getEmptyBuildings().add(id);

        messageManager.addMessageToQueue(emptyBuildingMessage, emptyBuildingMessage.getType());
    }


}
