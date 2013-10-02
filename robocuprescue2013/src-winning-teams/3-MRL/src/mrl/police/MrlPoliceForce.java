package mrl.police;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.common.comparator.ConstantComparators;
import mrl.communication.Packet;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.message.type.MessageTypes;
import mrl.partitioning.IPartitionManager;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.search.SearchHelper;
import mrl.police.moa.PoliceForceUtilities;
import mrl.police.strategies.*;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import mrl.mosCommunication.helper.PoliceMessageHelper;

import java.util.*;


/**
 * MRL police force agent.
 * <p/>
 * Since 1.1: Added multiple Operation Mode Support
 *
 * @author Legacy
 * @author BrainX
 * @version 1.1
 */
public class MrlPoliceForce extends MrlPlatoonAgent<PoliceForce> {
    protected MrlPoliceForceWorld world;
    protected ClearHereHelper clearHereHelper;
    protected PoliceMessageHelper policeMessageHelper;
    private Collection<EntityID> unexploredBuildings;


    private PoliceForceUtilities utilities;
    private PoliceConditionChecker pcc;
    //    List<IBehavior> myBehaviors;
//    private AbstractBehaviorHelper policeBehaviorsHelper;
    private ActionStrategyType actionStrategyType;
    private IPoliceActionStrategy actionStrategy;

    private ClearHelper clearHelper;
    private IPartitionManager targetsPartitionManager;
    private SearchHelper searchHelper;


    @Override
    public String toString() {
        return "MRL police force ID: " + this.getID().getValue();
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        this.world = (MrlPoliceForceWorld) super.world;
        model.indexClass(StandardEntityURN.ROAD,StandardEntityURN.HYDRANT);
        clearHelper = new ClearHelper(world);
        policeMessageHelper = new PoliceMessageHelper(world, this, messageManager);
        Collections.sort(world.getPoliceForceList(), ConstantComparators.ID_COMPARATOR);
        actionStrategyType = ActionStrategyType.TARGET_CLUSTERING;

//        unexploredBuildings = new HashSet<EntityID>(getMyPartition().getBuildingsID());

        utilities = new PoliceForceUtilities(world, config);

        pcc = new PoliceConditionChecker(world, utilities, policeMessageHelper);
//        behaviorHelper = new PoliceBehaviorsHelper(world);
//        myBehaviors = behaviorHelper.getBehaviors();
//        policeBehaviorsHelper = new PoliceBehaviorsHelper(world, this, config, policeMessageHelper);



        utilities.updateHealthyPoliceForceList();


        chooseActionStrategy();


        //TODO: This method was for making clusters for important targets of policeForces in firs Cycles
//        initializeTargetsPartitionManager();

//        searchHelper = new SearchHelper(world, world.getPlatoonAgent());

        System.out.println("   success");

    }

    private void chooseActionStrategy() {
        switch (actionStrategyType) {
            case PRIORITIZATION:
                actionStrategy = new TaskPrioritizationActionStrategy(world,clearHelper,policeMessageHelper,utilities,pcc);
                break;
            case TARGET_CLUSTERING:
                actionStrategy=new PoliceTargetClusteringActionStrategy(world,clearHelper,policeMessageHelper,utilities,pcc);
                break;
            case AUCTIONING:
                actionStrategy= new AuctionBasedActionStrategy(world,clearHelper,policeMessageHelper,utilities,pcc);
                break;
            case LEGACY:
                actionStrategy=new LegacyActionStrategy(world,clearHelper,policeMessageHelper,utilities,pcc);
                break;
        }

    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        super.think(time, changed, heard);
    }

    @Override
    public void act() throws CommandException, TimeOutException {
        actionStrategy.execute();
    }


    private void updateUnexploredBuildings(ChangeSet changed) {
        if (changed == null) {
            return;
        }
        for (EntityID next : changed.getChangedEntities()) {
            unexploredBuildings.remove(next);
        }
    }


    @Override
    public void processMessage(MessageEntity messageEntity) {
        try {
            policeMessageHelper.processMessage(messageEntity);
        } catch (Exception ex) {
            //todo: Handle it
            ex.printStackTrace();
        }

    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
    }

    public ClearHelper getClearHelper() {
        return clearHelper;
    }
}