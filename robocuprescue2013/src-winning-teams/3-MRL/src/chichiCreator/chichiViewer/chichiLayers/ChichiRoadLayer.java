package chichiCreator.chichiViewer.chichiLayers;

import chichiCreator.chichiViewer.ChichiStandardEntityToPaint;
import chichiCreator.chichiViewer.ChichiStaticViewProperties;
import chichiCreator.chichiViewer.ChichiViewer;
import chichiCreator.chichiViewer.chichiObjects.ChichiHighway;
import chichiCreator.chichiViewer.chichiObjects.ChichiHighways;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.view.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 12, 2010
 * Time: 4:42:03 PM
 */
public class ChichiRoadLayer extends ChichiAreaLayer<Road> {
    protected Random random;

    public static ChichiHighways highways;
    public static ArrayList<Road> addedRoads = new ArrayList<Road>();
    public static ArrayList<Road> highwayRoads = new ArrayList<Road>();
    public static ArrayList<Integer> highwayIds = new ArrayList<Integer>();
    public static ArrayList<Integer> thisCycleEditedHighWayIds = new ArrayList<Integer>();

    private static final Color ROAD_EDGE_COLOUR = Color.GRAY.darker();
    private static final Color ROAD_SHAPE_COLOUR = new Color(185, 185, 185);

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    private Action showIdsAction;

    /**
     * Construct a road rendering layer.
     */
    public ChichiRoadLayer() {
        super(Road.class);
        highways = new ChichiHighways();
    }

    public static int getANewId() {
        int id = 0;
        while (highwayIds.contains(id)) {
            id++;
        }
        highwayIds.add(id);
        return id;
    }

    @Override
    public String getName() {
        return "Roads";
    }

    @Override
    protected void paintShape(Road r, Polygon shape, Graphics2D g) {
        if (ChichiViewer.highwayFlag) {
            ChichiHighway thisRoadHighway = null;
            ChichiHighway selectedRoadHighway = null;

            if (r == ChichiStaticViewProperties.selectedObject) {
                if (highwayRoads.contains(r)) {
                    highwayRoads.remove(r);
                    addedRoads.remove(r);
                } else if (!addedRoads.contains(r)) {
                    highwayRoads.add(r);
                    addedRoads.add(r);
                }
            }
            if (highwayRoads.contains(r)) {
                g.setColor(Color.BLACK);
                g.fill(shape);
                return;
            }

            if (r == ChichiStaticViewProperties.selectedObject) {
                g.setColor(Color.MAGENTA);
                g.fill(shape);
                return;
            }
            Road selected = null;
            if (ChichiStaticViewProperties.selectedObject instanceof Road) {
                selected = (Road) ChichiStaticViewProperties.selectedObject;
            }
            if (selected != null) {

                for (ChichiHighway highway : highways) {
                    if (highway.contains(selected)) {
                        selectedRoadHighway = highway;
                    }
                    if (highway.contains(r)) {
                        thisRoadHighway = highway;
                    }
                }
//                if (thisRoadHighway == selectedRoadHighway) {
//                    if (ChichiViewer.mainZone == null) {
//                        ChichiViewer.selectedZone = selectedRoadHighway;
//                    } else if (selectedRoadHighway != null && !thisCycleEditedZoneIds.contains(selectedRoadHighway.getId())) {
//                        thisCycleEditedZoneIds.add(selectedRoadHighway.getId());
//                        if (zoneNeighbour.contains(selectedRoadHighway)) {
//                            zoneNeighbour.remove(selectedRoadHighway);
//                        } else if (selectedRoadHighway != ChichiViewer.mainZone) {
//                            zoneNeighbour.add(selectedRoadHighway);
//                        }
//                    }
//                }
//
//                if (thisRoadHighway != null) {
//                    if (ChichiViewer.mainZone == thisRoadHighway) {
//                        g.setColor(Color.WHITE);
//                        g.fill(shape);
//                        return;
//                    }
//                    if ((!zoneNeighbour.isEmpty() && zoneNeighbour.contains(thisRoadHighway))
//                            || (selectedRoadHighway != null && selectedRoadHighway.getNeighbors().contains(thisRoadHighway.getId()) && zoneNeighbour.isEmpty())) {
//                        g.setColor(Color.BLACK);
//                        g.fill(shape);
//                        return;
//                    }
//
//                    if (thisRoadHighway == selectedRoadHighway) {
//                        g.setColor(Color.MAGENTA);
//                        g.fill(shape);
//                        return;
//                    }
//                }
            }
            for (ChichiHighway highway : highways) {
                if (highway.contains(r)) {
                    if (selectedRoadHighway == null || !selectedRoadHighway.contains(r)) {
                        Color color = highway.color;
                        g.setColor(color);
                        g.fill(shape);
                        return;
                    }
                }
            }
        }

        ChichiStandardEntityToPaint entityToPaint = ChichiStaticViewProperties.getPaintObject(r);
        if (entityToPaint != null) {
            g.setColor(entityToPaint.getColor());
            g.fill(shape);
            return;

        }
        if (r == ChichiStaticViewProperties.selectedObject) {
            g.setColor(Color.MAGENTA);
            g.fill(shape);
            return;
        }
        g.setColor(ROAD_SHAPE_COLOUR);
        g.fill(shape);
    }

    @Override
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(ROAD_EDGE_COLOUR);
        g.setStroke(e.isPassable() ? ENTRANCE_STROKE : WALL_STROKE);
        g.drawLine(t.xToScreen(e.getStartX()),
                t.yToScreen(e.getStartY()),
                t.xToScreen(e.getEndX()),
                t.yToScreen(e.getEndY()));
    }

    @Override
    public void initialise(Config config) {
        showIdsAction = new ShowIdsAction();
    }

    private final class ShowIdsAction extends AbstractAction {
        public ShowIdsAction() {
            super("show ids");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRIds));
            putValue(Action.SMALL_ICON, showRIds ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showRIds = !showRIds;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRIds));
            putValue(Action.SMALL_ICON, showRIds ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(showIdsAction));

        return result;
    }
}
