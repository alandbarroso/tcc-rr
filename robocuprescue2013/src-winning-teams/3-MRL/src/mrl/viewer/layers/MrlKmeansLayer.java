package mrl.viewer.layers;

import javolution.util.FastMap;
import mrl.viewer.StaticViewProperties;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/12/12
 * Time: 10:03 AM
 */
public class MrlKmeansLayer extends StandardViewLayer {
    private static final java.util.List<Color> colors = new ArrayList<Color>();
    private static final Stroke STROKE = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, List<Point>> CENTER_POINTS = Collections.synchronizedMap(new FastMap<EntityID, List<Point>>());
    public static Map<EntityID, java.util.List<java.util.List<Point>>> COMMON_POINTS = Collections.synchronizedMap(new FastMap<EntityID, java.util.List<java.util.List<Point>>>());


    public MrlKmeansLayer() {
    }

    @Override
    public String getName() {
        return "K-means";
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {

        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        if (StaticViewProperties.selectedObject != null) {
            List<Point> centers = null;
            java.util.List<java.util.List<Point>> points = null;

            try {
                centers = Collections.synchronizedList(this.CENTER_POINTS.get(StaticViewProperties.selectedObject.getID()));
                points = Collections.synchronizedList(this.COMMON_POINTS.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }


            if(centers==null || points==null){
                return list;
            }

            g.setStroke(STROKE);

            Random generator = new Random();
            for (java.util.List<Point> ps : points) {
                int red = generator.nextInt(255);
                int green = generator.nextInt(255);
                int blue = generator.nextInt(255);
                Color color = new Color(red, green, blue);
                g.setColor(color);
                for (Point point : ps) {
                    int x = (transform.xToScreen(point.getX()));
                    int y = (transform.yToScreen(point.getY()));
                    g.fillOval(x, y, 5, 5);
                }
            }
            paintCenters(g, transform, centers);
        }
        return list;
    }

    public void paintCenters(Graphics2D g, ScreenTransform transform, List<Point> centers) {
//        System.out.println("number of center points in viewer:" + KMeansCenters.size());
        g.setStroke(STROKE);
        g.setColor(Color.GREEN);
        ScreenTransform t = transform;
        for (Point point : centers) {
            int x = t.xToScreen(point.getX());
            int y = t.yToScreen(point.getY());
            g.fillOval(x, y, 15, 15);
        }

    }

}