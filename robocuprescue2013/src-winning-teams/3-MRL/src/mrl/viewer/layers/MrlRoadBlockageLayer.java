package mrl.viewer.layers;

import javolution.util.FastSet;
import mrl.viewer.StaticViewProperties;
import mrl.world.object.MrlRoad;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.view.StandardEntityViewLayer;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 13, 2010
 * Time: 5:42:05 PM
 */
public class MrlRoadBlockageLayer extends StandardEntityViewLayer<Blockade> {
    private static final int BLOCK_SIZE = 3;
    private static final int BLOCK_STROKE_WIDTH = 2;

    private static final Color COLOUR = Color.black;

    /**
     * Construct a road blockage rendering layer.
     */
    public MrlRoadBlockageLayer() {
        super(Blockade.class);
    }

    @Override
    public String getName() {
        return "Road blockages";
    }

    @Override
    public Shape render(Blockade b, Graphics2D g, ScreenTransform t) {
        if (b.isPositionDefined()) {
            Road road = (Road) world.getEntity(b.getPosition());
            if(!MrlRoad.VIEWER_ROAD_BLOCKADES.containsKey(road)){
                MrlRoad.VIEWER_ROAD_BLOCKADES.put(road,new FastSet<Blockade>());
            }
            Set<Blockade> blockSet = new HashSet<Blockade>(MrlRoad.VIEWER_ROAD_BLOCKADES.get(road));
            blockSet.add(b);
            MrlRoad.VIEWER_ROAD_BLOCKADES.put(road, blockSet);
        }
        int[] apexes = b.getApexes();
        int count = apexes.length / 2;
        int[] xs = new int[count];
        int[] ys = new int[count];
        for (int i = 0; i < count; ++i) {
            xs[i] = t.xToScreen(apexes[i * 2]);
            ys[i] = t.yToScreen(apexes[(i * 2) + 1]);
        }
        Polygon shape = new Polygon(xs, ys, count);
        if (b == StaticViewProperties.selectedObject) {
            g.setColor(Color.MAGENTA);
        } else {
            g.setColor(COLOUR);
        }
        g.fill(shape);
        return shape;
    }
}