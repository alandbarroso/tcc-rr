package mrl.platoon.search.la;

import mrl.la.Action;
import mrl.viewer.layers.MrlZIOLayer;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.worldmodel.EntityID;

/**
 * User: roohola
 * Date: 5/10/11
 * Time: 2:51 PM
 */
public class ZoneAction implements Action {
    private MrlZone zone;

    public ZoneAction(MrlZone zone) {
        this.zone = zone;
    }

    public MrlZone getZone() {
        return zone;
    }

    public int getIndex() {
        return zone.getId();
    }


    public String getActionName() {
        return "ZoneAction[" + getIndex() + "]";
    }

    public void addToViewer(EntityID id) {
        try {
            MrlZIOLayer.SELECTED_ZONE_MAP.put(id, zone);
        } catch (Exception ignore) {
        }
    }

    @Override
    public int compareTo(Action o) {
        if (o.getIndex() < getIndex())
            return 1;
        if (o.getIndex() == getIndex())
            return 0;
        return -1;
    }

    @Override
    public String toString() {
        return getActionName();
    }
}
