package mrl.helper;

import javolution.util.FastMap;
import mrl.helper.info.CivilianInfo;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 2:02:07 PM
 */
public class CivilianHelper implements IHelper {

    protected MrlWorld world;

    protected Map<EntityID, CivilianInfo> civilianInfoMap = new FastMap<EntityID, CivilianInfo>();

    public CivilianHelper(MrlWorld world) {
        this.world = world;
    }

    @Override
    public void init() {
//        myCurrentPosition = world.getSelfLocation();
//        myPreviousPosition = world.getSelfLocation();
    }

    public void setInfoMap(EntityID id) {
        civilianInfoMap.put(id, new CivilianInfo(world));
    }

    @Override
    public void update() {
        CivilianInfo civilianInfo;
        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civ = (Civilian) standardEntity;

            civilianInfo = civilianInfoMap.get(standardEntity.getID());
            if (civilianInfo != null) {
//                civilianInfo.updatePossibleBuilding();
                if (civ.isPositionDefined()) {
                    removePossibleBuildings(civ.getID());
                }
            }
        }
        List<EntityID> heardCivilians = world.getHeardCivilians();
        Pair<Integer, Integer> location = world.getSelfLocation();
        for (EntityID civID : heardCivilians) {
            addHeardCivilianPosition(civID, location.first(), location.second());
        }
//        myPreviousPosition = world.getSelfLocation();
    }

    public void addHeardCivilianPosition(EntityID civilianId, int x, int y) {

        CivilianInfo civilianInfo = civilianInfoMap.get(civilianId);
        Pair pair = new Pair<Integer, Integer>(x, y);

        if (!civilianInfo.getHeardPositions().contains(pair)) {
            civilianInfo.getHeardPositions().add(pair);
            civilianInfo.updatePossibleBuilding();
//            List<Integer> list = new ArrayList<Integer>();
//            for (EntityID id : civilianInfo.getPossibleBuildings()) {
//                list.add(id.getValue());
//            }
//            if (MRLConstants.LAUNCH_VIEWER) {
//                MrlViewer.civilianPassableBuildingMap.put(civilianId, list);
//            }
            civilianInfoMap.put(civilianId, civilianInfo);
        }
    }

    //    };
//        }
//            return -1;
//
//                return 0;
//            if (civ1.benefit == civ2.benefit)
//                return 1;
//            if (civ1.benefit > civ2.benefit) //increase
//
//            CivilianInfo civ2 = (CivilianInfo) o2;
//            CivilianInfo civ1 = (CivilianInfo) o1;
//        public int compare(Object o1, Object o2) {
//    public Pair<Integer, Integer> myCurrentPosition;
//    public Pair<Integer, Integer> myPreviousPosition;

    public Set<EntityID> getPossibleBuildings(EntityID id) {
        return civilianInfoMap.get(id).getPossibleBuildings();
    }

    public void removePossibleBuildings(EntityID id) {
        civilianInfoMap.get(id).clearPossibleBuildings();
    }


//    public static Comparator Civilian_BENEFITComparator = new Comparator() {

    public Map<EntityID, CivilianInfo> getCivilianInfoMap() {
        return civilianInfoMap;
    }

}
