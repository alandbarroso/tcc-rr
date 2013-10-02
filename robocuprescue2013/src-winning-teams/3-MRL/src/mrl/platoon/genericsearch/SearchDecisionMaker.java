package mrl.platoon.genericsearch;

import javolution.util.FastSet;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.worldmodel.EntityID;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation for {@link ISearchDecisionMaker}
 *
 * @author Siavash
 * @see SearchManager
 */
public abstract class SearchDecisionMaker implements ISearchDecisionMaker {

    protected MrlWorld world;
    protected boolean searchInPartition;

    protected Set<Path> validPaths;
    protected Set<EntityID> validBuildings;
    protected Partition partition;
    protected Map<EntityID, Integer> notVisitable;
    //TODO: handle unvisited buildings check

    public SearchDecisionMaker(MrlWorld world) {
        this.world = world;
        validBuildings = new FastSet<EntityID>();
        validPaths = new FastSet<Path>();
        notVisitable = new HashMap<EntityID, Integer>();

    }

    @Override
    public void update() {
        if (searchInPartition) {
            validBuildings.clear();
            Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
            if (myPartition == null) {
                validBuildings.addAll(world.getBuildingIDs());
                validPaths.addAll(world.getPaths());
            } else {
                validBuildings.addAll(myPartition.getBuildingIDs());
                validPaths.addAll(myPartition.getPaths());

            }
        }
    }

    public void setSearchInPartition(boolean searchInPartition) {
        this.searchInPartition = searchInPartition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

}
