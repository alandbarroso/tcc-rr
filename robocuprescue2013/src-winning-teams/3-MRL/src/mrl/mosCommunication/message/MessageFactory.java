package mrl.mosCommunication.message;

import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.comparator.ConstantComparators;
import mrl.helper.HumanHelper;
import mrl.mosCommunication.entities.*;
import mrl.mosCommunication.message.type.*;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.IndexSort;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 6:17 PM
 *
 * @Author: Mostafa Movahedi
 */
public class MessageFactory {
    public static final int maxDamageToReport = 256;
    private MessageManager messageManager;
    private AmbulanceUtilities ambulanceUtilities;

    public MessageFactory(MessageManager messageManager, MrlWorld world) {
        this.messageManager = messageManager;
        this.ambulanceUtilities = new AmbulanceUtilities(world);
    }

    public void createPlatoonMessages(MrlWorld world, MrlPlatoonAgent platoonAgent) {
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        // agent info
        messageManager.addMessage(new AgentInfoMessage(new AgentInfo(world.getSelfPosition() instanceof Road ? PositionTypes.Road : PositionTypes.Building, world.getSelfPosition().getID(), platoonAgent.getAgentState(), world.getTime())));
        // buried & CLBuried Agent
        int damage;
        Human human = world.getSelfHuman();
        if (human.getBuriedness() > 0) {
            damage = human.getDamage();
            if (damage < maxDamageToReport) {
                if (damage == 0) {
                    damage = 1;
                }

                if (ambulanceUtilities.isAlivable(human)) {
                    messageManager.addMessage(new BuriedAgentMessage(new BuriedAgent(human.getPosition(), human.getHP(), human.getBuriedness(), damage, world.getTime())));
                } else {
                    messageManager.addMessage(new BuriedAgentMessage(new BuriedAgent(human.getPosition(), 0, human.getBuriedness(), damage, world.getTime())));
                }
            }
        }
        world.getThisCycleVisitedBuildings().clear();

        // heard civilian
        Pair<Integer, Integer> location = world.getSelfLocation();
        for (EntityID id : world.getHeardCivilians()) {
            messageManager.addMessage(new HeardCivilianMessage(new HeardCivilian(id, location.first(), location.second(), world.getTime())));
        }
        world.getHeardCivilians().clear();

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
                        messageManager.addMessage(new ExtinguishedBuildingMessage(new ExtinguishedBuilding(id, building.getFieryness(),world.getTime())));
                    } else if (building.getFieryness() > 0 && building.getFieryness() != 8) {
                        messageManager.addMessage(new BurningBuildingMessage(new BurningBuilding(id, building.getFieryness(), building.getTemperature(), world.getTime())));
                    }
                }
            }
            // civilian
            else if (iCanSendMessage && (entity instanceof Civilian)) {
                Civilian civilian = (Civilian) entity;
                if (civilian.getDamage() < maxDamageToReport) {
                    if (!(world.getEntity(civilian.getPosition()) instanceof Refuge) && (world.getEntity(civilian.getPosition()) instanceof Building)) {
                        messageManager.addMessage(new CivilianSeenMessage(new CivilianSeen(id, civilian.getBuriedness(), civilian.getDamage(), civilian.getHP(), civilian.getPosition(), humanHelper.getTimeToRefuge(id), world.getTime())));
                    }
                }
            }
        }
    }

    public void createCenterMessages(MrlWorld world) {

        IndexSort indexSort = world.getIndexes();
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        for (EntityID id : world.getChanges()) {
            StandardEntity entity = world.getEntity(id);
            // burning building
            if (entity instanceof Building) {
                Building building = (Building) entity;
                if (building.isFierynessDefined()) {
                    if (building.getFieryness() > 3 && building.getFieryness() < 8) {
                        messageManager.addMessage(new ExtinguishedBuildingMessage(new ExtinguishedBuilding(id, building.getFieryness(), world.getTime())));
                    } else if (building.getFieryness() > 0 && building.getFieryness() != 8) {
                        messageManager.addMessage(new BurningBuildingMessage(new BurningBuilding(id, building.getFieryness(), building.getTemperature(), world.getTime())));
                    }
                }
            }
            // civilian
            else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if (civilian.getDamage() < maxDamageToReport) {
                    if (!(world.getEntity(civilian.getPosition()) instanceof Refuge)
                            && (world.getEntity(civilian.getPosition()) instanceof Building)
                            && civilian.getDamage() != 0) {
                        messageManager.addMessage(new CivilianSeenMessage(new CivilianSeen(id, civilian.getBuriedness(), civilian.getDamage(), civilian.getHP(), civilian.getPosition(), humanHelper.getTimeToRefuge(id), world.getTime())));
                    }
                }
            }
        }
    }
}
