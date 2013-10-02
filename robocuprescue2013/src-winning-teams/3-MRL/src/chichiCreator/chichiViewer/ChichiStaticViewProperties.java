package chichiCreator.chichiViewer;

import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;

/**
 * User: roohi
 * Date: May 7, 2010
 * Time: 9:47:19 AM
 */
public class ChichiStaticViewProperties {
    public static StandardEntity selectedObject;
    public static ArrayList<ChichiStandardEntityToPaint> objectToPaint = new ArrayList<ChichiStandardEntityToPaint>();

    public static ChichiStandardEntityToPaint getPaintObject(StandardEntity entity) {
        for (ChichiStandardEntityToPaint e : objectToPaint) {
            if (e.getEntity() == entity) {
                return e;
            }
        }
        return null;
    }
}
