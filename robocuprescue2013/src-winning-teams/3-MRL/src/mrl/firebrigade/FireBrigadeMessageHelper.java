package mrl.firebrigade;

import javolution.util.FastMap;
import mrl.communication.*;
import mrl.communication.messages.Header;
import mrl.communication.messages.fireBrigadeMessages.FBGoToClusterMessage;
import mrl.communication.messages.fireBrigadeMessages.FireBrigadePriorityMessage;
import mrl.communication.messages.fireBrigadeMessages.WaterMessage;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 29, 2010
 * Time: 12:50:11 PM
 */
public class FireBrigadeMessageHelper extends PlatoonMessageHelper {
    MrlFireBrigadeWorld world;
    Map<EntityID, EntityID> letsGo;

    public FireBrigadeMessageHelper(MrlFireBrigadeWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
        letsGo = new FastMap<EntityID, EntityID>();
    }

    public void processMessage(Packet packet, EntityID sender) {
        Header header;
        MessageType messageType;

        header = packet.getHeader();
        messageType = header.getPacketType();

        if (messageType.equals(MessageType.Water)) {
            try {
                processWaterMessage(packet);
            } catch (Exception ex) {
//                System.out.println("unknown exception");
            }
        } else if (messageType.equals(MessageType.FireBrigadePriority)) {
            try {
                processCivCountMessage(packet, sender, world);
            } catch (Exception ex) {
//                System.out.println("unknown exception");
            }

        } else if (messageType.equals(MessageType.GoToCluster)) {
            try {
                processGoToClusterMessage(packet, sender);
            } catch (Exception ex) {
//                System.out.println("unknown exception");
            }

        }
    }

    public void sendWaterMessage(EntityID id, int water) {
        WaterMessage message = new WaterMessage(world.getIndexes().getAreaIndex(id), water);
        messageManager.addMessageToQueue(message, MessageType.Water);
    }

    public void sendPriorityMessage(Integer id, int i, int c) {
        FireBrigadePriorityMessage message = new FireBrigadePriorityMessage(id, i, c);
        messageManager.addMessageToQueue(message, message.getType());
    }

    public void sendGoToClusterMessage(EntityID id) {
        if (id != null) {
            FBGoToClusterMessage message = new FBGoToClusterMessage(world.getIndexes().getAreaIndex(id));
            messageManager.addMessageToQueue(message, MessageType.GoToCluster);
        } else {
            EntityID refugeId = new EntityID(0);
            for (StandardEntity rg : world.getRefuges()) {
                refugeId = rg.getID();
                break;
            }
            FBGoToClusterMessage message = new FBGoToClusterMessage(world.getIndexes().getAreaIndex(refugeId));
            messageManager.addMessageToQueue(message, MessageType.GoToCluster);
        }
    }

    public void processCivCountMessage(Packet packet, EntityID id, MrlWorld world) {
        if (packet.getHeader().getPacketType().equals(MessageType.FireBrigadePriority)) {
            for (Message message : packet) {
                FireBrigadePriorityMessage priorityMessage = (FireBrigadePriorityMessage) message;
                MrlZone zoneEntity = world.getZones().getZone(priorityMessage.getZoneId());

                if (world.getEntity(new EntityID(priorityMessage.getCivilianID())) instanceof Civilian) {
                    Civilian civilian = (Civilian) world.getEntity(new EntityID(priorityMessage.getCivilianID()));
                    if (world.getZones().setCivilianSet(civilian)) {
                        world.getZones().setCivZone(zoneEntity, priorityMessage.getCivilianCount());
                    }
                }
            }
        }
    }

    private void processWaterMessage(Packet packet) {
        WaterMessage waterMessage;

        for (Message message : packet) {
            waterMessage = (WaterMessage) message;
            world.getMrlBuilding(world.getIndexes().getAreaID(waterMessage.getAreaIdIndex())).increaseWaterQuantity(waterMessage.getWaterValue());

        }
    }

    private void processGoToClusterMessage(Packet packet, EntityID sender) {
        FBGoToClusterMessage clusterMessage;
        letsGo.clear();
        for (Message message : packet) {
            clusterMessage = (FBGoToClusterMessage) message;
            EntityID id = world.getIndexes().getAreaID(clusterMessage.getAreaIdIndex());
            letsGo.put(sender, id);
        }
        world.addGotoMap(letsGo);
    }

}
