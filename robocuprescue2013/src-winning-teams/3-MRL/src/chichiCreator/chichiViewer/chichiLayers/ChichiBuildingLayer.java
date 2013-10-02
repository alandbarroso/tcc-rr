package chichiCreator.chichiViewer.chichiLayers;

import chichiCreator.chichiViewer.ChichiStandardEntityToPaint;
import chichiCreator.chichiViewer.ChichiStaticViewProperties;
import chichiCreator.chichiViewer.ChichiViewer;
import chichiCreator.chichiViewer.chichiObjects.ChichiZoneEntity;
import chichiCreator.chichiViewer.chichiObjects.ChichiZones;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.view.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 12, 2010
 * Time: 4:47:21 PM
 */
public class ChichiBuildingLayer extends ChichiAreaLayer<Building> {

    public static ChichiZones zones;
    public static ArrayList<Building> addedBuildings = new ArrayList<Building>();
    public static ArrayList<Building> zoneBuildings = new ArrayList<Building>();
    public static ArrayList<ChichiZoneEntity> zoneNeighbour = new ArrayList<ChichiZoneEntity>();
    //    public static ArrayList<Integer> zoneNeighbour = new ArrayList<Integer>();
    public static ArrayList<Integer> zoneIds = new ArrayList<Integer>();
    public static ArrayList<Integer> thisCycleEditedZoneIds = new ArrayList<Integer>();

    private static final Color OUTLINE_COLOUR = Color.GRAY.darker().darker();

    private static final Color REFUGE_BUILDING_COLOR = Color.GREEN.darker();
    private static final Color CENTER_BUILDING_COLOR = Color.WHITE.brighter().brighter();

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    private Action showIdsAction;

    /**
     * Construct a building view layer.
     */
    public ChichiBuildingLayer() {
        super(Building.class);
        zones = new ChichiZones();
    }

    public static int getANewId() {
        int id = 0;
        while (zoneIds.contains(id)) {
            id++;
        }
        zoneIds.add(id);
        return id;
    }

    @Override
    public String getName() {
        return "Building shapes";
    }

    @Override
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(OUTLINE_COLOUR);
        g.setStroke(e.isPassable() ? ENTRANCE_STROKE : WALL_STROKE);
        g.drawLine(t.xToScreen(e.getStartX()),
                t.yToScreen(e.getStartY()),
                t.xToScreen(e.getEndX()),
                t.yToScreen(e.getEndY()));

    }

    @Override
    protected void paintShape(Building b, Polygon shape, Graphics2D g) {
        drawShapes(b, shape, g);
    }

    private void drawShapes(Building b, Polygon shape, Graphics2D g) {

        if (ChichiViewer.zoneFlag || ChichiViewer.zoneNeighbourFlag) {
            ChichiZoneEntity thisBuildingZone = null;
            ChichiZoneEntity selectedBuildingZone = null;

            if (ChichiViewer.zoneFlag) {
                if (b == ChichiStaticViewProperties.selectedObject) {
                    if (zoneBuildings.contains(b)) {
                        zoneBuildings.remove(b);
                        addedBuildings.remove(b);
                    } else if (!addedBuildings.contains(b)) {
                        zoneBuildings.add(b);
                        addedBuildings.add(b);
                    }
                }
                if (zoneBuildings.contains(b)) {
                    g.setColor(Color.BLACK);
                    g.fill(shape);
                    return;
                }

                if (b == ChichiStaticViewProperties.selectedObject) {
                    g.setColor(Color.MAGENTA);
                    g.fill(shape);
                    return;
                }
            } else {
                Building selected = null;
                if (ChichiStaticViewProperties.selectedObject instanceof Building) {
                    selected = (Building) ChichiStaticViewProperties.selectedObject;
                }
                if (selected != null) {

                    for (ChichiZoneEntity zone : zones) {
                        if (zone.contains(selected)) {
                            selectedBuildingZone = zone;
                        }
                        if (zone.contains(b)) {
                            thisBuildingZone = zone;
                        }
                    }
                    if (thisBuildingZone == selectedBuildingZone) {
                        if (ChichiViewer.mainZone == null) {
                            ChichiViewer.selectedZone = selectedBuildingZone;
                        } else if (selectedBuildingZone != null && !thisCycleEditedZoneIds.contains(selectedBuildingZone.getId())) {
                            thisCycleEditedZoneIds.add(selectedBuildingZone.getId());
                            if (zoneNeighbour.contains(selectedBuildingZone)) {
                                zoneNeighbour.remove(selectedBuildingZone);
                            } else if (selectedBuildingZone != ChichiViewer.mainZone) {
                                zoneNeighbour.add(selectedBuildingZone);
                            }
                        }
                    }

                    if (thisBuildingZone != null) {
                        if (ChichiViewer.mainZone == thisBuildingZone) {
                            g.setColor(Color.WHITE);
                            g.fill(shape);
                            return;
                        }
                        if ((!zoneNeighbour.isEmpty() && zoneNeighbour.contains(thisBuildingZone))
                                || (selectedBuildingZone != null && selectedBuildingZone.getNeighbors().contains(thisBuildingZone.getId()) && zoneNeighbour.isEmpty())) {
                            g.setColor(Color.BLACK);
                            g.fill(shape);
                            return;
                        }

                        if (thisBuildingZone == selectedBuildingZone) {
                            g.setColor(Color.MAGENTA);
                            g.fill(shape);
                            return;
                        }
                    }
                }
            }
            for (ChichiZoneEntity zone : zones) {
                if (zone.contains(b)) {
                    if (selectedBuildingZone == null || !selectedBuildingZone.contains(b)) {
                        Color color = zone.color;
                        g.setColor(color);
                        g.fill(shape);
                        return;
                    }
                }
            }
        }

        ChichiStandardEntityToPaint entityToPaint = ChichiStaticViewProperties.getPaintObject(b);
        if (entityToPaint != null) {
            g.setColor(entityToPaint.getColor());
            g.fill(shape);
            return;

        }
        if (b == ChichiStaticViewProperties.selectedObject) {
            g.setColor(Color.MAGENTA);
            g.fill(shape);
            return;
        }
        if (b instanceof Refuge) {
            g.setColor(REFUGE_BUILDING_COLOR);
            g.fill(shape);
        } else if ((b instanceof AmbulanceCentre) || (b instanceof FireStation) || (b instanceof PoliceOffice)) {
            g.setColor(CENTER_BUILDING_COLOR);
            g.fill(shape);
        } else {
            g.setColor(Color.gray);
            g.fill(shape);
        }
    }

    @Override
    public void initialise(Config config) {
        showBIds = false;
        showIdsAction = new ShowIdsAction();
    }

    private final class ShowIdsAction extends AbstractAction {
        public ShowIdsAction() {
            super("show ids");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showBIds));
            putValue(Action.SMALL_ICON, showBIds ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showBIds = !showBIds;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showBIds));
            putValue(Action.SMALL_ICON, showBIds ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems
            () {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(showIdsAction));

        return result;
    }
}
