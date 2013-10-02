package mrl.firebrigade.sterategy;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.common.clustering.FireCluster;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.HumanHelper;
import mrl.platoon.State;
import mrl.viewer.layers.MrlConvexHullLayer;
import mrl.viewer.layers.MrlForbiddenLocationsLayer;
import mrl.viewer.layers.MrlHumanLayer;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.List;

import static rescuecore2.misc.Handy.objectsToIDs;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/18/13
 * Time: 10:41 PM
 * Author: Mostafa Movahedi
 */
public class FireClusterConditionBasedActionStrategy extends FireBrigadeActionStrategy {

    private FireBrigadeUtilities fireBrigadeUtilities;
    private FireClusterConditionBasedTargetSelector fireClusterConditionBasedTargetSelector;


    public FireClusterConditionBasedActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        super(world, fireBrigadeUtilities, directionManager);
        this.fireBrigadeUtilities = fireBrigadeUtilities;
        this.fireClusterConditionBasedTargetSelector = new FireClusterConditionBasedTargetSelector(world, this, directionManager, fireBrigadeUtilities);
    }

    @Override
    public void execute() throws CommandException, TimeOutException {
        if (world.getHelper(HumanHelper.class).isBuried(selfHuman.getID())) {
            self.setAgentState(State.BURIED);
            return;
        }
        initialAct();
        selectTarget();
        extinguishTarget();
        finalizeAct();
    }

    /**
     * gets type of the action strategy
     */
    @Override
    public FireBrigadeActionStrategyType getType() {
        return FireBrigadeActionStrategyType.FIRE_CLUSTER_CONDITION_BASED;
    }

    //===============================do act=====================================


    private void selectTarget() throws TimeOutException {
        target = fireClusterConditionBasedTargetSelector.selectTarget();
        self.isThinkTimeOver("selectBuilding");
        if (MRLConstants.LAUNCH_VIEWER) {
            if (target != null) {

                MrlHumanLayer.TARGETS_TO_GO_MAP.put(world.getSelf().getID(), target.getID());
                MrlHumanLayer.FIRE_BRIGADE_TARGETS_TO_GO_MAP.put(world.getSelf().getID(), target.getID());

                MrlConvexHullLayer.MY_TARGET.remove(self.getID());
                List<Building> trgt = new ArrayList<Building>();
                trgt.add(target.getSelfBuilding());
                MrlConvexHullLayer.MY_TARGET.put(self.getID(), trgt);
            } else {
                MrlHumanLayer.TARGETS_TO_GO_MAP.put(world.getSelf().getID(), null);
                MrlHumanLayer.FIRE_BRIGADE_TARGETS_TO_GO_MAP.put(world.getSelf().getID(), null);

            }
        }
    }

    private void extinguishTarget() throws TimeOutException, CommandException {
        if (target == null) return;
//        lastTarget = target;
        int myDistanceToTarget = world.getDistance(world.getSelfHuman(), target.getSelfBuilding());
        EntityID targetId = target.getSelfBuilding().getID();
        moveToEntranceForCheck();
        // move to target if I can't see this.
        if (myDistanceToTarget > world.getMaxExtinguishDistance()) {
            StandardEntity locationToGo = chooseBestLocationToStandForExtinguishFire();
            if (locationToGo == null) {
                chooseBestLocationToStandForExtinguishFire();
            }
            if (MRLConstants.LAUNCH_VIEWER) {
                List<StandardEntity> stands = new ArrayList<StandardEntity>();
                stands.add(locationToGo);
                MrlConvexHullLayer.STAND.put(world.getSelf().getID(), stands);
            }
            if (!self.move((Area) locationToGo, world.getMaxExtinguishDistance(), false)) {
                //age jayee ke mikhast bere rahi behesh nabood miad in khate code
                Collection<StandardEntity> inRange = world.getObjectsInRange(locationToGo, world.getMaxExtinguishDistance() / 3);
                int counter = 0;
                for (StandardEntity entity : inRange) {
                    if (entity instanceof Area && world.getDistance(target.getSelfBuilding(), entity) < world.getMaxExtinguishDistance()) {
                        if (MRLConstants.LAUNCH_VIEWER) {
                            List<StandardEntity> stands = new ArrayList<StandardEntity>();
                            stands.add(entity);
                            MrlConvexHullLayer.STAND.put(world.getSelf().getID(), stands);
                        }
                        counter++;
                        self.move((Area) entity, world.getMaxExtinguishDistance(), false);
                        if (counter > 3) {
                            break;
                        }
                    }
                }

            }
        }
        if (myDistanceToTarget <= world.getMaxExtinguishDistance()) {
            //go to another location if possibles
            List<EntityID> forbiddenLocations = getForbiddenLocations();
            if (!forbiddenLocations.isEmpty() && forbiddenLocations.contains(self.getPosition())) {
                StandardEntity newLocation = chooseBestLocationToStandForExtinguishFire();
                if (newLocation != null) {
                    self.move((Area) newLocation, 0, false);
                }
            }
            if (((FireBrigade) world.getSelfHuman()).getWater() != 0) {
                int waterPower = self.getFireBrigadeUtilities().calculateWaterPower(world, target);

                self.getFireBrigadeMessageHelper().sendWaterMessage(targetId, waterPower);
                target.increaseWaterQuantity(waterPower);

                self.sendExtinguishAct(world.getTime(), targetId, waterPower);
            }

        }
        self.isThinkTimeOver("extinguishTarget");
    }


    //=========================internal functions================================
    //extinguish target
    private List<EntityID> getForbiddenLocations() {
        List<EntityID> forbiddenLocations = new ArrayList<EntityID>();
        //get out of cluster
        List<FireCluster> clusters = world.getFireClusterManager().getClusters();
        for (FireCluster next : clusters) {
            if (next.getEntities().contains(world.getEntity(target.getID()))) {
                targetCluster = next;
                break;
            }
        }
        if (targetCluster != null) {
            forbiddenLocations.addAll(objectsToIDs(targetCluster.getAllEntities()));
        }
        forbiddenLocations.addAll(world.getBurningBuildings());
        //if i am nth smallest FB, i should move over there
        //to force FBs to create a ring around fire
        int n = 3;
        if (world.isMapMedium()) n = 10;
        if (world.isMapHuge()) n = 15;
        for (FireBrigade next : world.getFireBrigadeList()) {
            if (selfHuman.getID().getValue() < next.getID().getValue() && world.getDistance(selfHuman.getID(), next.getID()) < world.getViewDistance() && --n <= 0) {
                MrlRoad roadOfNearFB = world.getMrlRoad(next.getPosition());
                MrlBuilding buildingOfNearFB = world.getMrlBuilding(next.getPosition());
                if (roadOfNearFB != null) {
                    forbiddenLocations.addAll(roadOfNearFB.getObservableAreas());
                }
                if (buildingOfNearFB != null) {
                    forbiddenLocations.addAll(buildingOfNearFB.getObservableAreas());
                }
            }
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlForbiddenLocationsLayer.FORBIDDEN_LOCATIONS.put(self.getID(), forbiddenLocations);
        }
        return forbiddenLocations;
    }

    private StandardEntity chooseBestLocationToStandForExtinguishFire() {
        double minDistance = Integer.MAX_VALUE;
        List<EntityID> forbiddenLocationIDs = getForbiddenLocations();
        List<StandardEntity> possibleAreas = new ArrayList<StandardEntity>();
        StandardEntity targetToExtinguish = null;
        double dis;
        List<EntityID> extinguishableFromAreas = target.getExtinguishableFromAreas();
        List<StandardEntity> bigBorder = world.getAreasIntersectWithShape(targetCluster.getBigBorderPolygon());
        for (EntityID next : extinguishableFromAreas) {
            StandardEntity entity = world.getEntity(next);
            if (bigBorder.contains(entity)) {
                possibleAreas.add(entity);
            }
        }

        if (MRLConstants.LAUNCH_VIEWER) {
            Set<StandardEntity> inRangeSet = new HashSet<StandardEntity>(world.getEntities(extinguishableFromAreas));
            MrlConvexHullLayer.BEST_PLACE_TO_STAND.put(world.getSelf().getID(), inRangeSet);
        }
        List<StandardEntity> forbiddenLocations = world.getEntities(forbiddenLocationIDs);
        possibleAreas.removeAll(forbiddenLocations);
        if (possibleAreas.isEmpty()) {
            Collection<Area> possibleLocations = new ArrayList<Area>();
            for (StandardEntity next : world.getAreas()) {
                possibleLocations.add((Area) next);
            }
            possibleLocations.removeAll(forbiddenLocations);
            return Util.nearestAreaTo(possibleLocations, self.getPosition(world).getLocation(world));
        }


        //fist search for a road to stand there
        for (StandardEntity entity : possibleAreas) {
            if (entity instanceof Road) {
                dis = Util.distance(selfHuman.getX(), selfHuman.getY(), ((Road) entity).getX(), ((Road) entity).getY());
                if (dis < minDistance) {
                    minDistance = dis;
                    targetToExtinguish = entity;
                }
            }
        }
        //if there is no road to stand, search for a no fiery building to go
        if (targetToExtinguish == null) {
            for (StandardEntity entity : possibleAreas) {
                if (entity instanceof Building) {
                    Building building = (Building) entity;
                    dis = Util.distance(selfHuman.getX(), selfHuman.getY(), building.getX(), building.getY());
                    if (dis < minDistance && (!building.isFierynessDefined() || (building.isFierynessDefined() && (building.getFieryness() >= 4 || building.getFieryness() <= 1)))) {
                        minDistance = dis;
                        targetToExtinguish = entity;
                    }
                }
            }
        }
        return targetToExtinguish;
    }
}
