package mrl.viewer.layers;

import javolution.util.FastMap;
import mrl.partitioning.Partition;
import mrl.viewer.MrlViewer;
import mrl.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * @author Vahid Hooshangi
 */
public class MrlClusterLayer extends MrlAreaLayer<Building> {
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke STROKE_All_SEGMENTS = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke STROKE_MY_SEGMENT = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT);
    private boolean visible;
    protected Random random;

    public static Map<EntityID, List<Partition>> PARTITIONS_MAP = Collections.synchronizedMap(new FastMap<EntityID, List<Partition>>());
    public static Map<EntityID, List<Partition>> POLICE_TARGET_PARTITIONS_MAP = Collections.synchronizedMap(new FastMap<EntityID, List<Partition>>());
    public static Map<EntityID, Partition> HUMAN_PARTITION_MAP = Collections.synchronizedMap(new FastMap<EntityID, Partition>());

    public static List<Point2D> POINTS = new ArrayList<Point2D>();

    private boolean policeTargetsInfo;
    private RenderPoliceTargetsAction policeTargetsAction;


    private boolean scaleInfo;
    private RenderScaleInfoAction scaleInfoAction;

    private boolean allSegmentsInfo;
    private RenderAllSegmentsAction allSegmentsAction;

    private boolean mySegmentInfo;
    private RenderMySegmentAction mySegmentAction;


    /**
     * Construct an area view layer.
     */
    public MrlClusterLayer() {
        super(Building.class);
    }

    @Override
    public void initialise(Config config) {
        policeTargetsInfo=true;
        policeTargetsAction=new RenderPoliceTargetsAction();

        scaleInfo = false;
        scaleInfoAction = new RenderScaleInfoAction();
        allSegmentsInfo = false;
        allSegmentsAction = new RenderAllSegmentsAction();
        mySegmentInfo = true;
        mySegmentAction = new RenderMySegmentAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();

        result.add(new JMenuItem(policeTargetsAction));
        result.add(new JMenuItem(scaleInfoAction));
        result.add(new JMenuItem(allSegmentsAction));
        result.add(new JMenuItem(mySegmentAction));

        return result;
    }


    @Override
    protected void paintShape(Building area, Polygon p, Graphics2D g) {


    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {

        List<Partition> partitions = null;
//        List<Partition> tempPartitions = null;

        Partition myPartition = null;

        try {
            partitions = Collections.synchronizedList(PARTITIONS_MAP.get(StaticViewProperties.selectedObject.getID()));
//            policeTargetsPartitions  = Collections.synchronizedList(POLICE_TARGET_PARTITIONS_MAP.get(StaticViewProperties.selectedObject.getID()));
            
//            tempPartitions = Collections.synchronizedList(TEMP_PARTITIONS_MAP.get(StaticViewProperties.selectedObject.getID()));
            myPartition = HUMAN_PARTITION_MAP.get(StaticViewProperties.selectedObject.getID());
        } catch (NullPointerException ignored) {

        }

        if (POINTS != null) {
            g.setColor(Color.red);
            g.setStroke(STROKE_All_SEGMENTS);
            for (Point2D p : POINTS) {
                g.drawRoundRect(t.xToScreen(p.getX()), t.yToScreen(p.getY()), 10, 10, 10, 10);
            }
        }

        if (allSegmentsInfo) {
            List<Partition> partitionList = new ArrayList<Partition>();
            for (List<Partition> pList : PARTITIONS_MAP.values()) {
                for (Partition partition : pList) {
                    if (!partitionList.contains(partition)) {
                        partitionList.add(partition);
                    }
                }

            }
            renderAllSegments(partitionList, g, t);
        }
        if (mySegmentInfo) {

            renderMySegment(myPartition, g, t);
        }


//        if(policeTargetsInfo){
//            List<Partition> policeTargetPartitionList = new ArrayList<Partition>();
//            for (List<Partition> pList : POLICE_TARGET_PARTITIONS_MAP.values()) {
//                for (Partition partition : pList) {
//                    if (!policeTargetPartitionList.contains(partition)) {
//                        policeTargetPartitionList.add(partition);
//                    }
//                }
//
//            }
//            renderAllSegments(policeTargetPartitionList,g,t);
//        }

        g.setStroke(STROKE);
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        Color COLOUR;
        if (partitions == null) {
            return list;
        }

        for (Partition partition : partitions) {
            int[] x = new int[partition.getPolygon().npoints];
            int[] y = new int[partition.getPolygon().npoints];
            for (int i = 0; i < partition.getPolygon().npoints; i++) {
                x[i] = t.xToScreen(partition.getPolygon().xpoints[i]);
                y[i] = t.yToScreen(partition.getPolygon().ypoints[i]);
            }

            Polygon shape = new Polygon(x, y, partition.getPolygon().npoints);
            random = new Random(7 * partition.getId().getValue() * MrlViewer.randomValue);
            COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
            g.setColor(COLOUR);


            g.drawString(String.valueOf(partition.getId()), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY());
            g.draw(shape);
//
        }
//
//        if (tempPartitions == null) {
//            return list;
//        }
//
//        for (Partition partition : tempPartitions) {
//            int[] x = new int[partition.getPolygon().npoints];
//            int[] y = new int[partition.getPolygon().npoints];
//            for (int i = 0; i < partition.getPolygon().npoints; i++) {
//                x[i] = t.xToScreen(partition.getPolygon().xpoints[i]);
//                y[i] = t.yToScreen(partition.getPolygon().ypoints[i]);
//            }
//
//            Polygon shape = new Polygon(x, y, partition.getPolygon().npoints);
//            random = new Random(7 * partition.getId().getValue() * MrlViewer.randomValue);
//            COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
//            g.setColor(COLOUR);
//
//
//            g.drawString(String.valueOf(partition.getId()), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY());
//            g.draw(shape);
//
//        }
//

        if (scaleInfo) {
            renderScaling(partitions, g, t);
        }

//        Color color = Color.red;
//        int[] xp;
//        int[] yp;
//        int count;
//        int i;
//        Polygon shape;
//        Building b;
//        for (Partition pa : partitions) {
//            g.setColor(color);
//            for (MrlBuilding building : pa.getBuildings()) {
//                b = (Building) world.getEntity(building.getID());
//                count = b.getEdges().size();
//                xp = new int[count];
//                yp = new int[count];
//
//                i = 0;
//
//                for (Edge e : b.getEdges()) {
//                    xp[i] = t.xToScreen(e.getStartX());
//                    yp[i] = t.yToScreen(e.getStartY());
//                    ++i;
//                }
//                shape = new Polygon(xp, yp, count);
//                g.fill(shape);
//            }
//            random = new Random(pa.getId().getValue() * 1000 * pa.getId().getValue() * 1000 * System.currentTimeMillis());
//            color = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
//        }
//
        return list;
    }


    private void renderScaling(List<Partition> partition, Graphics2D g, ScreenTransform t) {
//        Color COLOUR;
//        int sumX;
//        int sumY;
//        for (Partition pa : partitions) {
//            random = new Random();
//            COLOUR = Color.CYAN;
//            g.setColor(COLOUR);
//            sumX = 0;
//            sumY = 0;
//            int[] x = new int[pa.scalePolygon(pa.getPolygon()).npoints];
//            int[] y = new int[pa.scalePolygon(pa.getPolygon()).npoints];
//            for (int i = 0; i < pa.scalePolygon(pa.getPolygon()).npoints; i++) {
//                x[i] = t.xToScreen((int) pa.scalePolygon(pa.getPolygon()).xpoints[i]);
//                y[i] = t.yToScreen((int) pa.scalePolygon(pa.getPolygon()).ypoints[i]);
//
//                sumX += x[i];
//                sumY += y[i];
//            }
//
//            Polygon shape = new Polygon(x, y, pa.scalePolygon(pa.getPolygon()).npoints);
//
//            g.draw(shape);
//
//            g.setColor(Color.red);
//
//            g.drawRoundRect(sumX/pa.getPolygon().npoints, sumY/pa.getPolygon().npoints, 5, 5, 10, 10);
//        }


    }

    private void renderAllSegments(List<Partition> partitions, Graphics2D g, ScreenTransform t) {


        for (Partition partition : partitions) {
            int[] x = new int[partition.getPolygon().npoints];
            int[] y = new int[partition.getPolygon().npoints];
            for (int i = 0; i < partition.getPolygon().npoints; i++) {
                x[i] = t.xToScreen(partition.getPolygon().xpoints[i]);
                y[i] = t.yToScreen(partition.getPolygon().ypoints[i]);
            }

            Polygon shape = new Polygon(x, y, partition.getPolygon().npoints);
            g.setColor(Color.red);
            g.setStroke(STROKE_All_SEGMENTS);
            g.draw(shape);


        }

    }

    private void renderMySegment(Partition partition, Graphics2D g, ScreenTransform t) {

        if (partition == null) {
            return;
        }

        int[] x = new int[partition.getPolygon().npoints];
        int[] y = new int[partition.getPolygon().npoints];
        for (int i = 0; i < partition.getPolygon().npoints; i++) {
            x[i] = t.xToScreen(partition.getPolygon().xpoints[i]);
            y[i] = t.yToScreen(partition.getPolygon().ypoints[i]);
        }
        Polygon shape = new Polygon(x, y, partition.getPolygon().npoints);
        g.setColor(Color.GREEN);
        g.setStroke(STROKE_MY_SEGMENT);
        g.draw(shape);


    }

    private final class RenderPoliceTargetsAction extends AbstractAction {
        public RenderPoliceTargetsAction() {
            super("Police Targets");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setScaleRenderInfo(!policeTargetsInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(scaleInfo));
            putValue(Action.SMALL_ICON, scaleInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setPoliceTargetInfo(boolean render) {
        policeTargetsInfo = render;
        policeTargetsAction.update();
    }


    private final class RenderScaleInfoAction extends AbstractAction {
        public RenderScaleInfoAction() {
            super("scale info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setScaleRenderInfo(!scaleInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(scaleInfo));
            putValue(Action.SMALL_ICON, scaleInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setScaleRenderInfo(boolean render) {
        scaleInfo = render;
        scaleInfoAction.update();
    }


    private final class RenderAllSegmentsAction extends AbstractAction {
        public RenderAllSegmentsAction() {
            super("All Segments");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setAllSegmentsInfo(!allSegmentsInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(allSegmentsInfo));
            putValue(Action.SMALL_ICON, allSegmentsInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setAllSegmentsInfo(boolean render) {
        allSegmentsInfo = render;
        allSegmentsAction.update();
    }

    private final class RenderMySegmentAction extends AbstractAction {
        public RenderMySegmentAction() {
            super("My Segment");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setMySegmentInfo(!mySegmentInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(mySegmentInfo));
            putValue(Action.SMALL_ICON, mySegmentInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setMySegmentInfo(boolean render) {
        mySegmentInfo = render;
        mySegmentAction.update();
    }


    @Override
    public void setVisible(boolean b) {
        visible = b;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String getName() {
        return "Clusters";
    }
}
