package chichiCreator.chichiViewer.chichiObjects;

import java.util.ArrayList;

/**
 * Created by Mostafa Shabani.
 * Date: Feb 21, 2011
 * Time: 11:27:14 AM
 */
public class ChichiZones extends ArrayList<ChichiZoneEntity> {

    public ChichiZoneEntity getZone(int id) {
        for (ChichiZoneEntity zone : this) {
            if (zone.id == id)
                return zone;
        }
        return null;
    }
}
