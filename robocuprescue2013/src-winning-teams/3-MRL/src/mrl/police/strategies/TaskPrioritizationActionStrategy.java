package mrl.police.strategies;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.partitioning.IPartitionManager;
import mrl.partitioning.Partition;
import mrl.partitioning.PolicePartitionManager;
import mrl.platoon.State;
import mrl.police.ClearHelper;
import mrl.police.PoliceConditionChecker;
import mrl.mosCommunication.helper.PoliceMessageHelper;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.Task;
import mrl.viewer.layers.MrlClusterLayer;
import mrl.viewer.layers.MrlPartitionsLayer;
import mrl.viewer.layers.MrlRendezvousLayer;
import mrl.viewer.layers.MrlSubClusterLayer;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 9:00 PM
 */
public class TaskPrioritizationActionStrategy extends DefaultActionStrategy {
    private static final Log logger = LogFactory.getLog(TaskPrioritizationActionStrategy.class);

    private IPartitionManager partitionManager;
    private boolean inPartition = true;


    private PoliceTaskSelection taskStrategy;
    private ITargetManager targetManager;


    public TaskPrioritizationActionStrategy(MrlWorld world, ClearHelper clearHelper, PoliceMessageHelper policeMessageHelper, PoliceForceUtilities utilities, PoliceConditionChecker conditionChecker) {
        super(world, clearHelper, policeMessageHelper, utilities, conditionChecker);


        targetManager=new PartitionTargetManager(world);

        // In both SINGULAR_PARTITIONING and DUAL_PARTITIONING, we need to do the partitioning at Time 0 (right here) based
        // on Healthy Police Forces, namely those police forces which are already on the road.
        initializePartitionManager();

        if (MRLConstants.LAUNCH_VIEWER) {
            for (Partition partition : partitionManager.getPartitions()) {
                partition.computeConvexHull();
            }

            MrlClusterLayer.PARTITIONS_MAP.put(selfHuman.getID(), partitionManager.getPartitions());
            MrlClusterLayer.HUMAN_PARTITION_MAP.put(selfHuman.getID(), partitionManager.findHumanPartition(selfHuman));
            if (partitionManager.findHumanPartition(selfHuman) == null ) {
                System.out.println(selfHuman.getID() + " I have no partition in time :" + 0);
//        }
            } else {

                System.out.println(selfHuman.getID() + " my Partition is:" + partitionManager.findHumanPartition(selfHuman).getId() + " NumberOfNeeded:" + partitionManager.findHumanPartition(selfHuman).getNumberOfNeededPFs());
            }
            MrlSubClusterLayer.Partitions = partitionManager.getPartitions();
            MrlPartitionsLayer.PARTITIONS = partitionManager.getPartitions();
            MrlRendezvousLayer.partitions = partitionManager.getPartitions();
        }

    }


    @Override
    public void execute() throws CommandException {

        // I'm buried and can't do anything
        if (selfHuman.getBuriedness() > 0) {
            //todo send buriedAgentMessage
            return;
        }

        utilities.updateHealthyPoliceForceList();

        partitionManager.update();

        scapeOut();

        myTask = getNextTask();

        List<EntityID> pathToGo;

        if (myTask == null) {
            clearHelper.clearWay(null, null);
            logger.debug("getNextTask() returned null.");
        } else {
            me.setAgentState(State.WORKING);
            targetRoad = utilities.getNearestRoad(myTask.getTarget().getRoadsToMove());
            pathToGo = me.getPathPlanner().planMove((Area) world.getSelfPosition(), targetRoad, 0, true);

            if (!pathToGo.isEmpty()) {
                clearHelper.clearWay(pathToGo, myTask.getTarget().getId());
                me.getPathPlanner().moveOnPlan(pathToGo);
//                sendMoveAct(world.getTime(), pathToGo);
                clearHelper.clearWay(pathToGo, myTask.getTarget().getId());
            } else if (targetRoad != null) {
                clearHelper.clearWay(pathToGo, myTask.getTarget().getId());
            }

            targetRoad = null;
        }

        search();
    }

    /**
     * Creates {@link #partitionManager} and initializes it, Then sets {@link #world} partitionManager reference to the newly
     * created and initialized partition manager.
     */
    private void initializePartitionManager() {
//        if (world.getPoliceForceList().size() == 1) {
//            partitionManager = new NullPartitionManager(world);
//        } else {
        if (partitionManager == null) {
            partitionManager = new PolicePartitionManager(world, utilities);
        }
        partitionManager.initialise();
//        }
        world.setPartitionManager(partitionManager);
    }


    private Task getNextTask() {
        if (taskStrategy == null) {
            taskStrategy = new PolicePrioritizationTaskSelector(world, me, partitionManager.findHumanPartition(selfHuman), utilities, policeMessageHelper, conditionChecker,targetManager);
        }
        return taskStrategy.act();
    }

}
