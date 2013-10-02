package mrl.police.strategies;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.police.ClearHelper;
import mrl.police.PoliceConditionChecker;
import mrl.mosCommunication.helper.PoliceMessageHelper;
import mrl.police.moa.Auction;
import mrl.police.moa.IAuction;
import mrl.police.moa.PoliceForceUtilities;
import mrl.world.MrlWorld;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 11:27 PM
 */
public class AuctionBasedActionStrategy extends DefaultActionStrategy {

    private IAuction auction;
    private boolean inPartition = true;
    private ITargetManager targetManager;

    public AuctionBasedActionStrategy(MrlWorld world, ClearHelper clearHelper, PoliceMessageHelper policeMessageHelper, PoliceForceUtilities utilities, PoliceConditionChecker conditionChecker) {
        super(world, clearHelper, policeMessageHelper, utilities, conditionChecker);

        auction = new Auction(world, me, policeMessageHelper,utilities);
        auction.setTargetBidsMap(world.getTargetBidsMap());
        auction.setAvailableAgents(world.getPoliceForceList().size());
        targetManager=new PartitionTargetManager(world);
    }

    @Override
    public void execute() throws CommandException, TimeOutException {
        throw new NotImplementedException();
    }


    private void act_Auctioning() {
        if (myTask != null && conditionChecker.isTaskDone(myTask)) {
            auction.doneTask();
            System.err.println(world.getTime() + " " + selfHuman.getID() + " I have done my assigned task");
            myTask = null;
            targetRoad = null;
        }
        auction.startAuction(world.getTime(), targetManager.getTargets(world.getPartitionManager().findHumanPartition(selfHuman)));
        auction.taskAllocation(world.getTime());

        myTask = auction.getTask(selfHuman.getID());
    }

}
