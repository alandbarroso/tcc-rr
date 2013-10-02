package mrl.police.strategies;

import mrl.helper.RoadHelper;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.PoliceConditionChecker;
import mrl.mosCommunication.helper.PoliceMessageHelper;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.Task;
import mrl.world.MrlWorld;

/**
 * User: Pooya
 * Date: 3/1/12
 * Time: 8:10 PM
 */
public abstract class PoliceTaskSelection {

    protected MrlWorld world;
    protected MrlPlatoonAgent self;
    protected PoliceForceUtilities utilities;
    protected PoliceConditionChecker conditionChecker;
    protected PoliceMessageHelper messageHelper;
    RoadHelper roadHelper;
    protected ITargetManager targetManager;

    protected Task myTask;
    protected Partition partition;
    public PoliceTaskSelection(MrlWorld world, MrlPlatoonAgent self,  Partition partition,    PoliceForceUtilities utilities, PoliceMessageHelper messageHelper, PoliceConditionChecker conditionChecker, ITargetManager targetManager) {
        this.partition = partition;
        this.messageHelper = messageHelper;
        this.world = world;
        this.utilities = utilities;
        this.conditionChecker = conditionChecker;
        this.self = self;
        this.roadHelper = world.getHelper(RoadHelper.class);
        this.targetManager=targetManager;
    }

    public abstract Task act() ;

    public void setPartition(Partition partition){
        this.partition=partition;
    }

    public void setTargetManager(ITargetManager targetManager) {
        this.targetManager = targetManager;
    }
}
