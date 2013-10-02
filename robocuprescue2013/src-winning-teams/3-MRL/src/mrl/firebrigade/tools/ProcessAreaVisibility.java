package mrl.firebrigade.tools;

import mrl.LaunchMRL;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.viewer.layers.MrlAreaVisibilityLayer;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/12/13
 * Time: 4:29 PM
 * Author: Mostafa Movahedi
 */

/**
 * A preprocessor to determine each area is visible from which areas and which areas are observable from specific area
 */
public class ProcessAreaVisibility {


    /**
     * @param world
     * @param config
     */
    public static void process(MrlFireBrigadeWorld world, Config config) {
        long startTime = System.currentTimeMillis();

        String visibleFromFileName = MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".vif";
        String observableAreasFileName = MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".oba";
        String lineOfSightsNameFileName = MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".los";

        if (new File(visibleFromFileName).exists() && new File(observableAreasFileName).exists() && new File(lineOfSightsNameFileName).exists()) {
            try {
                FileEntityIDMap visibleFrom = (FileEntityIDMap) Util.readObject(visibleFromFileName);
                FileEntityIDMap observableArea = (FileEntityIDMap) Util.readObject(observableAreasFileName);
                FileLineOfSight lineOfSight = (FileLineOfSight) Util.readObject(lineOfSightsNameFileName);
                for (Integer next : visibleFrom.keySet()) {
                    MrlRoad road = world.getMrlRoad(new EntityID(next));
                    if (road != null) {
                        road.setVisibleFrom(Util.IntegerListToEIDList(visibleFrom.get(next)));
                    } else {
                        MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                        building.setVisibleFrom(Util.IntegerListToEIDList(visibleFrom.get(next)));
                    }
                }
                for (Integer next : observableArea.keySet()) {
                    MrlRoad road = world.getMrlRoad(new EntityID(next));
                    if (road != null) {
                        road.setObservableAreas(Util.IntegerListToEIDList(observableArea.get(next)));
                    } else {
                        MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                        building.setObservableAreas(Util.IntegerListToEIDList(observableArea.get(next)));
                    }
                }
                for (Integer next : lineOfSight.keySet()) {
                    MrlRoad road = world.getMrlRoad(new EntityID(next));
                    if (road != null) {
                        road.setLineOfSight(lineOfSight.get(next));
                    } else {
                        MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                        building.setLineOfSight(lineOfSight.get(next));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            FileEntityIDMap visibleFrom = new FileEntityIDMap();
            FileEntityIDMap observableAreas = new FileEntityIDMap();
            FileLineOfSight lineOfSights = new FileLineOfSight();

            MrlLineOfSightPerception lineOfSightPerception = new MrlLineOfSightPerception();
            lineOfSightPerception.initialise(config, world);
            List<MrlRoad> allRoads = world.getMrlRoads();
            List<MrlBuilding> allBuildings = world.getMrlBuildings();

            for (MrlRoad road : allRoads) {
                road.setObservableAreas(lineOfSightPerception.getVisibleAreas(road.getID()));
                road.setLineOfSight(lineOfSightPerception.getRays());
                observableAreas.put(road.getID().getValue(), Util.EIDListToIntegerList(road.getObservableAreas()));
                lineOfSights.put(road.getID().getValue(), road.getLineOfSight());
            }
            for (MrlBuilding building : allBuildings) {
                building.setObservableAreas(lineOfSightPerception.getVisibleAreas(building.getID()));
                building.setLineOfSight(lineOfSightPerception.getRays());
                observableAreas.put(building.getID().getValue(), Util.EIDListToIntegerList(building.getObservableAreas()));
                lineOfSights.put(building.getID().getValue(), building.getLineOfSight());
            }

            for (MrlRoad road1 : allRoads) {
                for (MrlRoad road2 : allRoads) {
                    if (road1.equals(road2)) continue;
                    if (road2.getObservableAreas().contains(road1.getID())) {
                        road1.getVisibleFrom().add(road2.getID());

                    }
                }
                for (MrlBuilding building : allBuildings) {
                    if (building.getObservableAreas().contains(road1.getID())) {
                        road1.getVisibleFrom().add(building.getID());
                    }
                }
                visibleFrom.put(road1.getID().getValue(), Util.EIDListToIntegerList(road1.getVisibleFrom()));
            }
            for (MrlBuilding building1 : allBuildings) {
                for (MrlBuilding building2 : allBuildings) {
                    if (building1.equals(building2)) continue;
                    if (building2.getObservableAreas().contains(building1.getID())) {
                        building1.getVisibleFrom().add(building2.getID());

                    }
                }
                for (MrlRoad road : allRoads) {
                    if (road.getObservableAreas().contains(building1.getID())) {
                        building1.getVisibleFrom().add(road.getID());
                    }
                }
                visibleFrom.put(building1.getID().getValue(), Util.EIDListToIntegerList(building1.getVisibleFrom()));
            }
            try {
                if (LaunchMRL.SHOULD_PRECOMPUTE) {
                    Util.writeObject(observableAreas, observableAreasFileName);
                    Util.writeObject(visibleFrom, visibleFromFileName);
                    Util.writeObject(lineOfSights, lineOfSightsNameFileName);
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        if (MRLConstants.LAUNCH_VIEWER) {
            for (MrlRoad road : world.getMrlRoads()) {
                //fill visible areas from roads for viewer
                MrlAreaVisibilityLayer.VISIBLE_FROM_AREAS.put(road.getID(), road.getVisibleFrom());
                //fill observableAreas from roads for viewer
                MrlAreaVisibilityLayer.OBSERVABLE_AREAS.put(road.getID(), road.getObservableAreas());

                MrlAreaVisibilityLayer.LINE_OF_SIGHT_RAYS.put(road.getID(), road.getLineOfSight());
            }

            for (MrlBuilding building : world.getMrlBuildings()) {
                //fill visible areas from buildings for viewer
                MrlAreaVisibilityLayer.VISIBLE_FROM_AREAS.put(building.getID(), building.getVisibleFrom());
                //fill observable areas from buildings for viewer
                MrlAreaVisibilityLayer.OBSERVABLE_AREAS.put(building.getID(), building.getObservableAreas());

                MrlAreaVisibilityLayer.LINE_OF_SIGHT_RAYS.put(building.getID(), building.getLineOfSight());
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.print(" ProcessAreaVisibility(" + (endTime - startTime) + ")");
    }
}
