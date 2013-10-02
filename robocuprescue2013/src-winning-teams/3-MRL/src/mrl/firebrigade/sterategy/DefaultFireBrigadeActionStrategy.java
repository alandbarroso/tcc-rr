package mrl.firebrigade.sterategy;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.extinguishBehaviour.DirectionBasedExtinguishBehaviour;
import mrl.firebrigade.extinguishBehaviour.ExtinguishBehaviourType;
import mrl.firebrigade.extinguishBehaviour.IExtinguishBehaviour;
import mrl.firebrigade.extinguishBehaviour.MutualLocationExtinguishBehaviour;
import mrl.firebrigade.targetSelection.*;
import mrl.helper.HumanHelper;
import mrl.partitioning.FireBrigadePartitionManager;
import mrl.partitioning.IPartitionManager;
import mrl.partitioning.Partition;
import mrl.platoon.State;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 5:39 PM
 * Author: Mostafa Movahedi
 */
public class DefaultFireBrigadeActionStrategy extends FireBrigadeActionStrategy {
    public DefaultFireBrigadeActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        super(world, fireBrigadeUtilities, directionManager);
        setTargetSelectorApproach();
        setExtinguishBehaviourApproach();

    }


    private IFireBrigadeTargetSelector targetSelector;
    private TargetSelectorType targetSelectorType = TargetSelectorType.DIRECTION_BASED;
    private IExtinguishBehaviour extinguishBehaviour;
    private ExtinguishBehaviourType extinguishBehaviourType = ExtinguishBehaviourType.CLUSTER_BASED;
    private FireBrigadeTarget lastFireBrigadeTarget;


    @Override
    public void execute() throws CommandException, TimeOutException {
        if (fireBrigadePartitionManager != null) {
            fireBrigadePartitionManager.update();
            myPartition = fireBrigadePartitionManager.findHumanPartition(selfHuman);
        }

        if (world.getHelper(HumanHelper.class).isBuried(selfHuman.getID())) {
            self.setAgentState(State.BURIED);
            return;
        }


        if (isTimeToSwitchTargetSelectorApproach()) {
            FireBrigadeUtilities.refreshFireEstimator(world);
            //targetSelectorType = TargetSelectorType.DIRECTION_BASED;
            //setTargetSelectorApproach();
        }
        initialAct();

        FireBrigadeTarget fireBrigadeTarget = targetSelector.selectTarget();
        if (fireBrigadeTarget != null) {
            updateViewer(fireBrigadeTarget.getMrlBuilding());
        } else {
            updateViewer(null);
        }
        extinguishBehaviour.extinguish(world, fireBrigadeTarget);

        finalizeAct();

    }

    /**
     * gets type of the action strategy
     */
    @Override
    public FireBrigadeActionStrategyType getType() {
        return FireBrigadeActionStrategyType.DEFAULT;
    }

    private boolean isTimeToSwitchTargetSelectorApproach() {
        //todo: check blockade situation
        if (world.getTime() >= 120 && world.getTime() % 30 == 0) {
            return true;
        }
        return false;
    }


    private void setTargetSelectorApproach() {

        switch (targetSelectorType) {
            case DIRECTION_BASED:
                targetSelector = new DirectionBasedTargetSelector(world);
                break;
            case GREEDY_DIRECTION_BASED:
                targetSelector = new GreedyDirectionBasedTargetSelector(world);
                break;
            case BLOCKADE_BASED:
                targetSelector = new BlockadeBasedTargetSelector(world);
                break;
            case ZJU_BASED:
                break;
            case TIME_BASED:
                targetSelector = new Direction_Time_BasedTargetSelector(world);
                break;
        }
    }

    private void setExtinguishBehaviourApproach() {
        switch (extinguishBehaviourType) {
            case CLUSTER_BASED:
                extinguishBehaviour = new DirectionBasedExtinguishBehaviour();
                break;
            case MUTUAL_LOCATION:
                extinguishBehaviour = new MutualLocationExtinguishBehaviour();
                break;
        }
    }
}
