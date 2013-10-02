package mrl.helper.info;

import javolution.util.FastList;
import javolution.util.FastSet;
import mrl.common.Util;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 2:04:46 PM
 */
public class CivilianInfo {

    protected MrlWorld world;
    private final Set<EntityID> possibleBuildings = new FastSet<EntityID>();
    private List<Pair> heardPositions = new ArrayList<Pair>();
    private boolean isFound = false;
    private List<EntityID> visited = new FastList<EntityID>();
    int voiceRange;

    public CivilianInfo(MrlWorld world) {
        this.world = world;
        visited.addAll(world.getVisitedBuildings());
        voiceRange = world.getVoiceRange();
    }

    public void updatePossibleBuilding() {
        if (isFound) {
            return;
        }

        possibleBuildings.removeAll(visited);

        ArrayList<EntityID> possibleList = new ArrayList<EntityID>();
        for (Pair pair : heardPositions) {
            if (possibleBuildings.isEmpty()) {
                possibleBuildings.addAll(getGuessedBuildings(pair));
            } else {
                ArrayList<EntityID> toRemove = new ArrayList<EntityID>();
                possibleList.addAll(getGuessedBuildings(pair));
                for (EntityID building : possibleBuildings) {
                    if (!possibleList.contains(building) && visited.contains(building)) {
                        toRemove.add(building);
                    }
                }
                possibleBuildings.removeAll(toRemove);
            }
        }
        heardPositions.clear();
    }

    private ArrayList<EntityID> getGuessedBuildings(Pair pair) {
        ArrayList<EntityID> builds = new ArrayList<EntityID>();
        for (EntityID entityID : world.getUnvisitedBuildings()) {
            Building building = (Building) world.getEntity(entityID);
            if (Util.distance(building.getX(), building.getY(), (Integer) pair.first(), (Integer) pair.second()) <= voiceRange) {
                builds.add(entityID);
            }
        }
        return builds;
    }

    public List<Pair> getHeardPositions() {
        return heardPositions;
    }

    public Set<EntityID> getPossibleBuildings() {
        return possibleBuildings;
    }


    public void clearPossibleBuildings() {
        isFound = true;
        possibleBuildings.clear();
    }
}
