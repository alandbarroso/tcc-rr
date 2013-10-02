package mrl.police;

import javolution.util.FastMap;
import mrl.communication.*;
import mrl.communication.messages.ClearedRoadMessage;
import mrl.communication.messages.Header;
import mrl.communication.messages.TargetToGoMessage;
import mrl.communication.messages.policeMessages.PoliceBidMessage;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.moa.Bid;
import mrl.police.moa.Target;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Mostafa Shabani.
 * Edited by Pooya Deldar Gohardani
 * Date: Dec 29, 2010
 * Time: 12:53:41 PM
 */
public class PoliceMessageHelper extends PlatoonMessageHelper {
    MrlPoliceForceWorld world;

    public PoliceMessageHelper(MrlPoliceForceWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
    }

    public void processMessage(Packet packet, EntityID sender) {

        Header header;
        MessageType messageType;

        header = packet.getHeader();
        messageType = header.getPacketType();

//        if (messageType.equals(MessageType.ClearedRoad)) {
//            processClearedRoadMessage(packet);
//        } else if (messageType.equals(MessageType.Block)) {
//            processBlockMessage(packet);
//        } else if (messageType.equals(MessageType.TargetToGo)) {
//            processTargetToGoMessage(packet, sender);
        /*} else */if (messageType.equals(MessageType.PoliceBid)) {
            PoliceBidMessage policeBidMessage;
            for (Message message : packet) {
                policeBidMessage = (PoliceBidMessage) message;
                try {
                    processBidMessage(policeBidMessage, sender);
                }catch (IndexOutOfBoundsException ex){
                    world.printData("Exception : Bid Message........");
                }
            }
        }

    }

    private void processTargetToGoMessage(Packet packet, EntityID sender) {
        TargetToGoMessage targetToGoMessage;

        for (Message message : packet) {
            targetToGoMessage = (TargetToGoMessage) message;
            world.getTargetToGoHelper().addTargetToGo(world.getIndexes().getAreaID(targetToGoMessage.getAreaIdIndex()), sender);
        }
    }

    private void processClearedRoadMessage(Packet packet) {
        ClearedRoadMessage clearedRoadMessage;
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);

        for (Message message : packet) {
            clearedRoadMessage = (ClearedRoadMessage) message;
            Road road = (Road) world.getEntity(world.getIndexes().getAreaID(clearedRoadMessage.getAreaIdIndex()));
            road.setBlockades(new ArrayList<EntityID>());
//            world.printData(" ----------------------- receive clear road message:   "+road);

            roadHelper.setRoadPassable(road.getID(), true);
        }
    }

//    private void processBlockMessage(Packet packet) {
//        BlockedRoadMessage blockMessage;
//        int time = packet.getHeader().getPacketCycle();
//        PropertyHelper propertyHelper = world.getHelper(PropertyHelper.class);
//        RoadHelper roadHelper = world.getHelper(RoadHelper.class);
//
//        for (Message message : packet) {
//            blockMessage = (BlockedRoadMessage) message;
//            Area area = (Area) world.getEntity(world.getIndexes().getAreaID(blockMessage.getAreaIdIndex()));
//            if (propertyHelper.getPropertyTime(area.getBlockadesProperty()) < time) {
//                roadHelper.setRoadPassable(area.getID(), false);
//                propertyHelper.setPropertyTime(area.getBlockadesProperty(), time);
//            }
//        }
//    }


    public void sendBidMessage(List<Bid> bids) {
        PoliceBidMessage policeBidMessage;
        for (Bid bid : bids) {
            policeBidMessage = new PoliceBidMessage(world.getIndexes().getAreaIndex(bid.getTarget().getId()), bid.getValue(), bid.getTarget().getImportance());
            System.out.println("Sending>>> " + world.getTime() + " " + world.getSelf().getID() + " Bid:" + bid.getTarget().getId() + " value:" + bid.getValue());
            messageManager.addMessageToQueue(policeBidMessage, policeBidMessage.getType());
        }
    }


    private void processBidMessage(PoliceBidMessage message, EntityID sender) {

        Map<EntityID, Bid> bids;
        Target target = new Target(world.getIndexes().getAreaID(message.getPathIdIndex()), message.getImportance());
        bids = world.getTargetBidsMap().get(target.getId());
        if (bids == null) {
            bids = new FastMap<EntityID, Bid>();
        }
        // each sender should have just one bid per each target in a cycle
        bids.put(sender, new Bid(sender, target, message.getValue()));
        System.err.println("<<<< Received " + world.getTime() + " " + world.getSelf().getID() + " Sender:" + sender + " Bid:" + target.getId() + " value:" + message.getValue());
        world.getTargetBidsMap().put(target.getId(), bids);
    }
}
