package chichiCreator.chichiViewer.chichiObjects;


import java.util.ArrayList;

/**
 * Created by Mostafa
 * Date: Mar 29, 2011
 * Time: 9:43:18 PM
 */
public class ChichiHighways extends ArrayList<ChichiHighway> {

    public ChichiHighway getHighway(int id) {
        for (ChichiHighway highway : this) {
            if (highway.id == id)
                return highway;
        }
        return null;
    }
}
