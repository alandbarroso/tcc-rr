package mrl.viewer.layers;

import mrl.world.routing.path.Path;
import mrl.world.routing.path.Paths;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.view.RenderedObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * User: mrl
 */
public class MrlPathLayer extends MrlAreaLayer<Road> {
    private static final Color ROAD_EDGE_COLOUR = Color.GRAY.darker();
    private static final Color ROAD_SHAPE_COLOUR = new Color(185, 185, 185);

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    public static Paths PATHLIST = null;
    protected Random random;


    /**
     * Construct a road rendering layer.
     */
    public MrlPathLayer() {
        super(Road.class);
    }

    @Override
    public String getName() {
        return "Paths";
    }


    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        int[] xp;
        int[] yp;

        int count;
        int i;
        Polygon shape;
        Road b;
        Color color = Color.red;

        int coutx = 0;
        int couty = 0;
        int j = 0;

        for (Path path : PATHLIST) {
            g.setColor(color);
            j=0;
            coutx = 0;
            couty = 0;
            for (Road road : path) {
                b = (Road) world.getEntity(road.getID());
                count = b.getEdges().size();
                xp = new int[count];
                yp = new int[count];

                i = 0;

                for (Edge e : b.getEdges()) {
                    xp[i] = t.xToScreen(e.getStartX());
                    yp[i] = t.yToScreen(e.getStartY());
                    ++i;
                }

                coutx += road.getX();
                couty += road.getY();
                j++;
                shape = new Polygon(xp, yp, count);

//                drawPathNeighbour(road, shape, g);
                g.fill(shape);
            }
            g.setColor(Color.WHITE);
//            g.drawString(String.valueOf(path.getId()), t.xToScreen(coutx / j), t.yToScreen(couty / j));
            g.drawString(String.valueOf(path.getId()), t.xToScreen(path.getMiddleRoad().getX()), t.yToScreen(path.getMiddleRoad().getY()));
            random = new Random(path.getId().getValue() * 1000 * path.getId().getValue() * 1000 * System.currentTimeMillis());
            color = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
        }

        return list;
    }


    @Override
    public void initialise(Config config) {

    }
}
