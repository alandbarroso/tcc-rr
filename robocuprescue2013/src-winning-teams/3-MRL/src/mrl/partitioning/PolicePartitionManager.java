package mrl.partitioning;

import javolution.util.FastMap;
import mrl.assignment.HungarianAlgorithmWrapper;
import mrl.assignment.IAssignment;
import mrl.common.MRLConstants;
import mrl.common.TimestampThreadLogger;
import mrl.partitioning.costMatrixMaker.CostMatrixMaker;
import mrl.partitioning.segmentation.SegmentType;
import mrl.police.moa.PoliceForceUtilities;
import mrl.viewer.layers.MrlClusterLayer;
import mrl.viewer.layers.MrlPartitionsLayer;
import mrl.viewer.layers.MrlRendezvousLayer;
import mrl.viewer.layers.MrlSubClusterLayer;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/12/12
 * Time: 5:27 PM
 */
public class PolicePartitionManager extends DefaultPartitionManager {
    private static final Log logger = LogFactory.getLog(PolicePartitionManager.class);

    private PoliceForceUtilities utilities;

    private List<Partition> previousPartitions;
    private IPartitionValueDetermination valueDetermination;

    private IPartitionNeededAgentsComputation neededAgentsComputation;
    //Map of PartitionID to the list of nearest refuge path, which contains in that partition
    private Map<EntityID, Set<EntityID>> partitionsRefugePathMap;


    //TODO @BrainX Make this singleton, with Spring maybe?

    /**
     * Creates an instance of PolicePartitionManager
     *
     * @param world     The world this instance is supposed to perform partitioning operations.
     * @param utilities reference to a {@link PoliceForceUtilities}
     */
    public PolicePartitionManager(MrlWorld world, PoliceForceUtilities utilities) {
        super(world);
        this.utilities = utilities;
        numberOfAgents = world.getPoliceForceList().size();


    }

    @Override
    protected void initialVariables(MrlWorld world) {
        super.initialise();

        segmentType = SegmentType.ENTITY_CLUSTER;
        this.partitionHelper = new PartitionHelper(world);

        valueDetermination = new LinearValueDetermination(world);

        // TODO @Pooya Whenever we have a Merge/Repartitioning solution, we can use NeededAgentsComputation_ValueBased.
        neededAgentsComputation = new NeededAgentsComputation_OneToOne();
//      neededAgentsComputation = new NeededAgentsComputation_ValueBased();

        partitionUtilities = new PartitionUtilities(world);
        costMatrixMaker = new CostMatrixMaker(world);

        if (humanPartitionMap == null) {
            previousHumanPartitionMap = new FastMap<Human, Partition>();
            previousPartitions = new ArrayList<Partition>();
        } else {
            previousHumanPartitionMap.putAll(humanPartitionMap);
            previousPartitions.addAll(partitions);
        }

        partitions = new ArrayList<Partition>();
        humanPartitionMap = new FastMap<Human, Partition>();
        partitionsRefugePathMap = new FastMap<EntityID, Set<EntityID>>();

        // TODO @BrainX Hungarian implementation assigns a single agent to multiple partitions
//        assignmentMethod = new GreedyAssignment();
        assignmentMethod = new HungarianAlgorithmWrapper();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() {

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering initialize...");

        initialVariables(world);


        Partition worldPartition = partitionHelper.makeWorldPartition();

        // Moved to MrlPoliceForce
//        utilities.updateHealthyPoliceForceList();

        List<StandardEntity> healthyPoliceForces = new ArrayList<StandardEntity>(utilities.getHealthyPoliceForces());
        int numberOfPolices = healthyPoliceForces.size();

        if(healthyPoliceForces.isEmpty()){
            healthyPoliceForces.add(world.getSelfHuman());
        }
        partitions = split(worldPartition, numberOfPolices);

        updatePartitions(healthyPoliceForces.size());

        //TODO @Pooya In case we need neighbours, this is where they can be found
//        partitionHelper.setNeighbours(partitions);

        // Investigate performance of ArrayList for "partitions" (Suggested Implementation: LinkedList)
//        partitions = partitionHelper.mergeTooSmallPartitions(partitions);

//        partitionHelper.createRendezvous(partitions, world);

        //TODO @Pooya Performance Issue: We need to update only the merged partitions.
//        updatePartitions(healthyPoliceForces.size());

        updateAssignment(healthyPoliceForces);

        updateViewer();


        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from initialize.");
        TimestampThreadLogger.getCurrentThreadLogger().flush();
    }

    @Override
    public void update() {
        try {
            super.update();
            if (world.getTime() < SECOND_PARTITIONING_TIME) {
                // After updating Healthy Police force information, it's time to decide which OperationMode to choose:
                partitionOperationMode = partitionDecideOperationMode(utilities.getHealthyPoliceForces().size(), world.getPoliceForces().size());
            }

            this.updatePartitions(utilities.getHealthyPoliceForces().size());

            if (partitionOperationMode == OperationMode.DUAL_PARTITIONING
                    && world.getTime() == SECOND_PARTITIONING_TIME) {
                // It's about time each police force should re-calculate partitioning to include newly added healthy police
                // forces. (Whether recently buried, or healthy but inside a building)

                this.initialise();
//                System.out.println(me().getID() + " my Partition is:" + partitionManager.findHumanPartitions(selfHuman).getId() + " NumberOfNeeded:" + partitionManager.findHumanPartitions(selfHuman).getNumberOfNeededPFs());

            }

            if (world.getTime() == 3) {

                List<StandardEntity> unKnownAgents = new ArrayList<StandardEntity>(world.getPoliceForces());
                for (Map.Entry<Human, Partition> humanPartitionEntry : this.getHumanPartitionEntrySet()) {
                    unKnownAgents.remove(humanPartitionEntry.getKey());
                }

                Human unknownHuman;
                for (StandardEntity agent : unKnownAgents) {
                    // this agent is not assigned to any partition, I will assign it to the partition it is located at.
                    unknownHuman = (Human) agent;
                    this.forceAssignAgent(unknownHuman,
                            this.findPartitionAtArea(unknownHuman.getPosition()));

                }

            } else if (this.findHumanPartition(world.getSelfHuman()) == null) {
                this.forceAssignAgent(world.getSelfHuman(),
                        this.findPartitionAtArea(world.getSelfPosition().getID()));
            }


            if (this.findHumanPartition(world.getSelfHuman()).isDead()) {
                Partition currentPartition = this.findHumanPartition(world.getSelfHuman());
                Partition partitionToGo = findNearestNeighbourPartition(currentPartition, this.getPartitions());
                if (partitionToGo == null) {
                    partitionToGo = currentPartition;
                }
                this.forceAssignAgent(world.getSelfHuman(), partitionToGo);
            }

            if (MRLConstants.LAUNCH_VIEWER) {
                for (Partition partition : this.getPartitions()) {
                    if (partition.getPolygon() == null) {
                        partition.computeConvexHull();
                    }
                }

                MrlClusterLayer.PARTITIONS_MAP.put(world.getSelfHuman().getID(), this.getPartitions());
                MrlClusterLayer.HUMAN_PARTITION_MAP.put(world.getSelfHuman().getID(), this.findHumanPartition(world.getSelfHuman()));
                if (this.findHumanPartition(world.getSelfHuman()) == null) {
                    if (this.findHumanPartition(world.getSelfHuman()) == null) {
                        System.out.println(world.getSelfHuman().getID() + " I have no partition in time :" + 0);
//        }
                    } else {

                        System.out.println(world.getSelfHuman().getID() + " my Partition is:"
                                + this.findHumanPartition(world.getSelfHuman()).getId()
                                + " NumberOfNeeded:" + this.findHumanPartition(world.getSelfHuman()).getNumberOfNeededPFs());
                    }
                }
                MrlSubClusterLayer.Partitions = this.getPartitions();
                MrlPartitionsLayer.PARTITIONS = this.getPartitions();
                MrlRendezvousLayer.partitions = this.getPartitions();
            }
//            }


        } catch (Exception e) {
            logger.error("Failed to perform Partitioning related operations.");
            logger.debug("Stack Trace:", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePartitions(int agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartitions...");

        //update independent features
        for (Partition partition : partitions) {
            updatePartition(partition);
        }

        if (agents > 0) {
            //update dependent features
            Map<EntityID, Integer> neededAgentsMap = neededAgentsComputation.computeNeededAgents(partitions, agents);
            for (Partition partition : partitions) {
                partition.setNumberOfNeededPFs(neededAgentsMap.get(partition.getId()));
//                logger.debug(world.getTime() + " " + world.getSelf().getID() + " partition:" + partition.getId() + " value:" + partition.getValue() + " needs:" + partition.getNumberOfNeededPFs());
            }
        }

        if (partitionsRefugePathMap == null || partitionsRefugePathMap.isEmpty()) {
            partitionsRefugePathMap = new FastMap<EntityID, Set<EntityID>>();
            partitionsRefugePathMap.putAll(partitionUtilities.findPartitionsRefugePaths(partitions));
            for (Partition partition : partitions) {
                partition.setRefugePathsToClearInPartition(partitionUtilities.findRefugePathsToClearInPartition(partition, partitionsRefugePathMap));
            }
        }


        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartitions.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAssignment(List<StandardEntity> agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updateAssignment...");

        Collections.sort(partitions, Partition.PARTITION_NEEDED_PF_COMPARATOR);

        List<Partition> deflatedPartitions = arrangePartitions(partitions, agents);

        double[][] costMatrix;
        if (previousHumanPartitionMap.isEmpty()) {
            //do nothing
        } else {
            this.humanPartitionMap.clear();
            this.humanPartitionMap.putAll(previousHumanPartitionMap);

        }

        costMatrix = costMatrixMaker.makingCostMatrix(deflatedPartitions, agents);

        int[] assignment = assignmentMethod.computeVectorAssignments(costMatrix);

        for (int i = 0; i < assignment.length; i++) {
            this.humanPartitionMap.put((Human) agents.get(assignment[i]), deflatedPartitions.get(i));
//            logger.debug(world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i][0]).getID() + " partition: " + deflatedPartitions.get(i).getId());
//            System.out.println((world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i]).getID() + " partition: " + deflatedPartitions.get(i).getId()));
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updateAssignment.");
    }


    /**
     * Updates partition properties
     *
     * @param partition partition to update
     */
    @Override
    protected void updatePartition(Partition partition) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");
        if (!partition.isDead()) {
            partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
            partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
            partition.setBlockedAgents(partitionUtilities.getBlockedAgents(partition, false));
            partition.setBuriedAgents(partitionUtilities.getBuriedAgents(partition, false));
//            partition.setValue(valueDetermination.computeValue(partition));
            partition.setDead(partitionUtilities.isPartitionDead(partition));
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
    }

    @Override
    protected int getNeededAgentsInCommunicationLessSituation() {
        return CL_PF_IN_PARTITION;
    }

    @Override
    protected int getNeededAgentsInNormalSituation() {
        return PF_IN_PARTITION;
    }


}
