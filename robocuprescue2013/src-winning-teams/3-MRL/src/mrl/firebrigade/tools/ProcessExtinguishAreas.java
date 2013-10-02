package mrl.firebrigade.tools;

import javolution.util.FastMap;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.viewer.layers.MrlExtinguishableFromLayer;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/4/13
 * Time: 5:20 PM
 * Author: Mostafa Movahedi
 */

/**
 *
 */
public class ProcessExtinguishAreas {
    private MrlFireBrigadeWorld world;

    public ProcessExtinguishAreas(MrlFireBrigadeWorld world, Config config) {
        this.world = world;
    }

    public void process() {
        long startTime = System.currentTimeMillis();
        /*String extinguishableFromAreasFileName = "data/extinguishableFromAreas";
        String buildingsInExtinguishRangeFileName = "data/buildingsInExtinguishRange";
        if (new File(extinguishableFromAreasFileName).exists() && new File(buildingsInExtinguishRangeFileName).exists()) {
            try {
                FileEntityIDMap extinguishableFromAreasMap = (FileEntityIDMap) Util.readObject(extinguishableFromAreasFileName);
                FileEntityIDMap buildingsInExtinguishRangeMap = (FileEntityIDMap) Util.readObject(buildingsInExtinguishRangeFileName);
                for (Integer next : extinguishableFromAreasMap.keySet()) {
                    MrlBuilding mrlBuilding = world.getMrlBuilding(new EntityID(next));
                    mrlBuilding.setExtinguishableFromAreas(Util.IntegerListToEIDList(extinguishableFromAreasMap.get(next)));
                }
                for (Integer next : buildingsInExtinguishRangeMap.keySet()) {
                    MrlRoad road = world.getMrlRoad(new EntityID(next));
                    if (road != null) {
                        road.setBuildingsInExtinguishRange(Util.IntegerListToMrlBuildingList(world, buildingsInExtinguishRangeMap.get(next)));
                    } else {
                        MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                        building.setBuildingsInExtinguishRange(Util.IntegerListToMrlBuildingList(world, buildingsInExtinguishRangeMap.get(next)));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {*/
//        FileEntityIDMap extinguishableFromAreasMap = new FileEntityIDMap();
//        FileEntityIDMap buildingsInExtinguishRangeMap = new FileEntityIDMap();

        Map<EntityID,List<EntityID>> extinguishableFromMap = new FastMap<EntityID, List<EntityID>>();
        Map<EntityID,List<MrlBuilding>> buildingsInExtinguishRangeMap = new FastMap<EntityID, List<MrlBuilding>>();

        for (MrlBuilding mrlBuilding : world.getMrlBuildings()) {
            List<EntityID> extinguishableFromAreas = FireBrigadeUtilities.findAreaIDsInExtinguishRange(world, mrlBuilding.getID());
            List<MrlBuilding> buildingsInExtinguishRange = new ArrayList<MrlBuilding>();
            for (EntityID next : extinguishableFromAreas) {
                if (world.getEntity(next) instanceof Building) {
                    buildingsInExtinguishRange.add(world.getMrlBuilding(next));
                }
            }
            mrlBuilding.setExtinguishableFromAreas(extinguishableFromAreas);
            extinguishableFromMap.put(mrlBuilding.getID(), extinguishableFromAreas);
//            extinguishableFromAreasMap.put(mrlBuilding.getID().getValue(), Util.EIDListToIntegerList(extinguishableFromAreas));
            mrlBuilding.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
            buildingsInExtinguishRangeMap.put(mrlBuilding.getID(), buildingsInExtinguishRange);
//            buildingsInExtinguishRangeMap.put(mrlBuilding.getID().getValue(), Util.MrlBuildingListToIntegerList(buildingsInExtinguishRange));
        }
        for (MrlRoad mrlRoad : world.getMrlRoads()) {
            List<MrlBuilding> buildingsInExtinguishRange = FireBrigadeUtilities.findBuildingsInExtinguishRangeOf(world, mrlRoad.getID());
            mrlRoad.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
            buildingsInExtinguishRangeMap.put(mrlRoad.getID(), buildingsInExtinguishRange);
//            buildingsInExtinguishRangeMap.put(mrlRoad.getID().getValue(), Util.MrlBuildingListToIntegerList(buildingsInExtinguishRange));
        }
//            try {
//                Util.writeObject(buildingsInExtinguishRangeMap, buildingsInExtinguishRangeFileName);
//                Util.writeObject(extinguishableFromAreasMap, extinguishableFromAreasFileName);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        // }

        if (MRLConstants.LAUNCH_VIEWER) {
            MrlExtinguishableFromLayer.EXTINGUISHABLE_FROM.putAll(extinguishableFromMap);
            MrlExtinguishableFromLayer.BUILDINGS_IN_EXTINGUISH_RANGE.putAll(buildingsInExtinguishRangeMap);
        }

        long endTime = System.currentTimeMillis();
        System.out.print(" ProcessExtinguishAreas(" + (endTime - startTime) + ")");
    }

}
