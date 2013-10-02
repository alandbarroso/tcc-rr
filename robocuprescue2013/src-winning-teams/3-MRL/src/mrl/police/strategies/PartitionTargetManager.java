package mrl.police.strategies;

import javolution.util.FastMap;
import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.partitioning.Partition;
import mrl.police.moa.Target;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/16/12
 *         Time: 1:21 PM
 */
public class PartitionTargetManager extends DefaultTargetManager {

    public PartitionTargetManager(MrlWorld world) {
        super(world);

    }

}
