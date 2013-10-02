package mrl.police.strategies;

import mrl.common.MRLConstants;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.PoliceConditionChecker;
import mrl.mosCommunication.helper.PoliceMessageHelper;
import mrl.police.moa.PoliceForceUtilities;
import mrl.police.moa.Target;
import mrl.task.MoveStyle;
import mrl.task.Task;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Pooya
 * Date: 3/1/12
 * Time: 7:45 PM
 */
public class PolicePrioritizationTaskSelector extends PoliceTaskSelection {


    public PolicePrioritizationTaskSelector(MrlWorld world, MrlPlatoonAgent self, Partition partition, PoliceForceUtilities utilities, PoliceMessageHelper messageHelper, PoliceConditionChecker conditionChecker, ITargetManager targetManager) {
        super(world, self, partition, utilities, messageHelper, conditionChecker, targetManager);
    }

    @Override
    public Task act() {

        List<Target> targetList = new ArrayList<Target>(targetManager.getTargets(partition).values());


        //TODO: Make VALUE for targets based on different parameters and sort targets based on them
        Task prevTask = myTask;
        if (prevTask == null || conditionChecker.isTaskDone(prevTask)) {
            prevTask = null;
        }

        if (!targetList.isEmpty()) {
            Collections.sort(targetList, utilities.TARGET_IMPORTANCE_COMPARATOR);
            myTask = null;
            Target mostImportantTarget = null;
            int i = 0;
            while (!targetList.isEmpty() && i < targetList.size()) {
                if (!targetList.isEmpty()) {
                    mostImportantTarget = targetList.get(i);
                }

                //TODO: change isThereMoreImportantTask with isThereMoreValuable task
                if (myTask == null || isThereMoreImportantTask(myTask.getTarget(), mostImportantTarget)) {
                    if (mostImportantTarget != null) {
                        mostImportantTarget.setRoadsToMove(utilities.getRoadsToMove(mostImportantTarget));
                        myTask = new Task(mostImportantTarget, utilities.getActionStyle(mostImportantTarget), MoveStyle.FASTEST_PATH);

                        if (conditionChecker.isTaskDone(myTask)) {
                            doneTaskOperations(targetList);
                            continue;// don't count i
                        } else {
                            break;//found new task, so break te loop immediately
                        }
                    }
                } else if (conditionChecker.isTaskDone(myTask)/*utilities.isDoneTask(myTask)*/) {

                    doneTaskOperations(targetList);

                }
                i++;

            }

        } else {
            myTask = null;
        }

        if (myTask != null && prevTask != null && !prevTask.equals(myTask)) {
            //this task is not same with previous task.
            if (self != null) {
                self.getPathPlanner().planMove((Area) world.getSelfPosition(), world.getEntity(prevTask.getTarget().getPositionID(), Area.class), MRLConstants.IN_TARGET, true);
                int prevPathCost = self.getPathPlanner().getPathCost();
                self.getPathPlanner().planMove((Area) world.getSelfPosition(), world.getEntity(myTask.getTarget().getPositionID(), Area.class), MRLConstants.IN_TARGET, true);
                int pathCost = self.getPathPlanner().getPathCost();
                if (pathCost < prevPathCost) {
                    //this mean new task is closer to agent! so we choose it as new task.
                }else {
                    //this mean new task is farther than previous one. so to prevent trapping in loop we don't choose it as new task..
                    myTask = prevTask;
                }
            }
        }


        return myTask;


    }


    private void doneTaskOperations(List<Target> targetList) {
//        if (myTask.getTarget().getId().getValue() == 36517) {
//            System.out.println("KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK");
//            conditionChecker.isTaskDone(myTask);
//        }

        targetManager.getDoneTargets().add(world.getEntity(myTask.getTarget().getId()));
        targetList.remove(myTask.getTarget());
//        System.out.println("time:" + world.getTime() + " me:" + world.getSelf().getID() + " Done Task:" + myTask.getTarget().getId());

        myTask = null;
    }


    private boolean isThereMoreImportantTask(Target myTarget, Target mostImportantTarget) {

        return mostImportantTarget != null && myTarget.getImportance() < mostImportantTarget.getImportance();

    }


}
