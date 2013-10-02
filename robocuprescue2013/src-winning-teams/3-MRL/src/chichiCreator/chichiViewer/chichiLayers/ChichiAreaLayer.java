package chichiCreator.chichiViewer.chichiLayers;

import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.view.StandardEntityViewLayer;

import java.awt.*;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 13, 2010
 * Time: 3:37:10 PM
 */
public abstract class ChichiAreaLayer<E extends Area> extends StandardEntityViewLayer<E> {

    public boolean showBIds;
    //    private Action showBIdsAction;
    public boolean showRIds;
//    private Action showRIdsAction;

    /**
     * Construct an area view layer.
     *
     * @param clazz The subclass of Area this can render.
     */
    protected ChichiAreaLayer(Class<E> clazz) {
        super(clazz);
    }

    @Override
    public Shape render(E area, Graphics2D g, ScreenTransform t) {
        java.util.List<Edge> edges = area.getEdges();
        if (edges.isEmpty()) {
            return null;
        }
        int count = edges.size();
        int[] xs = new int[count];
        int[] ys = new int[count];
        int i = 0;
        for (Edge e : edges) {
            xs[i] = t.xToScreen(e.getStartX());
            ys[i] = t.yToScreen(e.getStartY());
            ++i;
        }
        Polygon shape = new Polygon(xs, ys, count);
        paintShape(area, shape, g);
        for (Edge edge : edges) {
            paintEdge(edge, g, t);
        }
        if (((area instanceof Building) && showBIds) || ((area instanceof Road) && showRIds)) {
            drawInfo(g, t, String.valueOf(area.getID().getValue()), getLocation(area), area.getClass());
        }

        return shape;
    }

    /**
     * Paint an individual edge.
     *
     * @param e The edge to paint.
     * @param g The graphics to paint on.
     * @param t The screen transform.
     */
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
    }

    /**
     * Paint the overall shape.
     *
     * @param area The area.
     * @param p    The overall polygon.
     * @param g    The graphics to paint on.
     */
    protected void paintShape(E area, Polygon p, Graphics2D g) {
    }

    private void drawInfo(Graphics2D g, ScreenTransform t, String strInfo, Pair<Integer, Integer> location, Class clazz) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.first());
            y = t.yToScreen(location.second());
            if (clazz.equals(Road.class)) {
                g.setColor(Color.CYAN);
            } else {
                g.setColor(Color.MAGENTA.brighter().brighter());
            }
            g.drawString(strInfo, x - 15, y + 4);
        }
    }

    protected Pair<Integer, Integer> getLocation(Area area) {
        return area.getLocation(world);
    }
}