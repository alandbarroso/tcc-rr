package mrl.police;

import javolution.util.FastSet;
import mrl.helper.RoadHelper;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import rescuecore2.config.Config;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * created by Mandana.
 * Date: May 5, 2010
 * Time: 11:28:02 PM
 */
public class MrlPoliceForceWorld extends MrlWorld {

    RoadHelper roadHelper;
    //    Map<Integer, ArrayList<PoliceForceBid>> bidsMap = new FastMap<Integer, ArrayList<PoliceForceBid>>();
//    List<PoliceForceBid> badePaths = new ArrayList<PoliceForceBid>();// the paths that I sent as bid
    Set<Path> badePaths = new FastSet<Path>();
    TargetToGoHelper targetToGoHelper;
    PoliceDecision decision;

    public MrlPoliceForceWorld(StandardAgent standardAgent, Collection<? extends Entity> entities, Config config) {
        super(standardAgent, entities,config);
        roadHelper = this.getHelper(RoadHelper.class);
        targetToGoHelper = new TargetToGoHelper(this);
        helpers.add(targetToGoHelper);
        decision = new PoliceDecision(this);
    }
//
//    public List<Path> getPathsOfThisArea(Area area) {
//        int loop = 0;
//        List<Path> paths = new ArrayList<Path>();
//        List<Area> neighbours = new ArrayList<Area>();
//        List<Area> added = new ArrayList<Area>();
//        Road road = null;
//        Area tempArea;
//        neighbours.add(area);
//        added.add(area);
//        Area neighbour;
//        EntityID pathId;
//        while ((road == null || !neighbours.isEmpty()) && loop < 20) {
//            loop++;
//            tempArea = neighbours.get(0);
//            neighbours.remove(0);
//
//            for (EntityID entityID : tempArea.getNeighbours()) {
//                neighbour = (Area) getEntity(entityID);
//                if (getEntity(entityID) instanceof Road) {
//                    road = (Road) getEntity(entityID);
//                    pathId = roadHelper.getPathId(road.getID());
//                    if (pathId != null) {
//                        Path path = getPath(pathId);
//                        if (!paths.contains(path)) {
//                            paths.add(path);
//                        }
//                    }
//                } else {
//                    if (!added.contains(neighbour)) {
//                        neighbours.add(neighbour);
//                        added.add(neighbour);
//                    }
//                }
//            }
//        }
//
//        return paths;
//    }

    public List<EntityID> getPathsIDsOfThisArea(Area area) {
        List<EntityID> pathsId = new ArrayList<EntityID>();
        List<Path> paths = getPathsOfThisArea(area);

        for (Path path : paths) {
            pathsId.add(path.getId());
        }
        return pathsId;
    }

//    public Path getPath(Road road) {
//        for (Path path : getPaths()) {
//            if (path.contains(road))
//                return path;
//        }
//        return null;
//    }

    public Path getPath(int pathId) {
        for (Path path : getPaths()) {
            if (path.getId().equals(new EntityID(pathId)))
                return path;
        }
        return null;
    }

    public Path getPath(EntityID entityID) {
        if (this.getEntity(entityID) instanceof Road) {
            // Road road = (Road)this.getEntity(entityID);
            for (Path path : getPaths()) {
                if (path.containsId(entityID)) {
                    return path;
                }
            }
        }
        return null;
    }

//    private void fillViewer() {
//        p.createPaths();
//        ArrayList<D_Road> roads = new ArrayList<D_Road>();
//        for (Path p : this.paths) {
//            EntityID id = p.getId();
//            roads.add(new D_Road(id.getValue()));
//        }
//        this.getDebugDataObject().paths().add(new D_Path(roads));
//    }


    @Override
    public void updateEveryCycle() {
        super.updateEveryCycle();
        targetToGoHelper.update();
    }


//    public List<Road> getConnectedRoadsToThisBuilding(Building building) {
//        Road road = null;
//        Set<Road> connectedRoads = new HashSet<Road>();
//        List<Road> connectedRoadsL = new ArrayList<Road>();
//        BuildingHelper buildingHelper = getHelper(BuildingHelper.class);
//
//        for (Entrance entrance : getMrlBuilding(building.getID()).getEntrances()) {
//            if (entrance.getNeighbour() instanceof Road)
//                road = (Road) entrance.getNeighbour();
//            connectedRoads.add(road);
//        }
//        connectedRoadsL.addAll(connectedRoads);
//        return connectedRoadsL;
//    }

//    public Map<Integer, ArrayList<PoliceForceBid>> getBidsMap() {
//        return bidsMap;
//    }

//    public List<PoliceForceBid> getBadePaths() {
//        return badePaths;
//    }

    public Set<Path> getBadePaths() {
        return badePaths;
    }

    public RoadHelper getRoadHelper() {
        return roadHelper;
    }

    public TargetToGoHelper getTargetToGoHelper() {
        return targetToGoHelper;
    }

    public PoliceDecision getDecision() {
        return decision;
    }


}
