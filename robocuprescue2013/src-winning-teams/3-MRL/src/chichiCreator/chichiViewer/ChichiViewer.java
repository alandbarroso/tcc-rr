package chichiCreator.chichiViewer;

import chichiCreator.chichiViewer.chichiLayers.ChichiBuildingLayer;
import chichiCreator.chichiViewer.chichiLayers.ChichiRoadLayer;
import chichiCreator.chichiViewer.chichiObjects.ChichiHighway;
import chichiCreator.chichiViewer.chichiObjects.ChichiZoneEntity;
import javolution.util.FastMap;
import mrl.common.MRLConstants;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.*;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ChichiViewer extends StandardViewer {
    public ChichiViewer() {
        initMapNames();
    }

    private Map<Long, String> mapFiles = new FastMap<Long, String>();
    private Long uniqueMapNumber;
    private ChichiAnimatedWorldModelViewer viewer;
    private JPanel panel = new JPanel(new BorderLayout());
    JPopupMenu popup = new JPopupMenu("popupMenu");
    StandardEntity selectedObject;
    public static boolean highwayFlag = false;
    public static boolean zoneFlag = false;
    public static boolean zoneNeighbourFlag = false;
    public static ChichiZoneEntity mainZone;
    public static ChichiZoneEntity selectedZone;
    JTextField objectTextField = new JTextField();
    JTextField xTextField = new JTextField();
    JTextField yTextField = new JTextField();
    JTextField groupIdTextField = new JTextField();
    JTextField zoneNeighbourTextField = new JTextField();

    public void initMapNames() {
        mapFiles.put(2141239244L, "Kobe");
        mapFiles.put(17687985466L, "Berlin1");
        mapFiles.put(17687924365L, "Berlin");
        mapFiles.put(14542274827L, "Paris");
        mapFiles.put(14542369921L, "Paris");
        mapFiles.put(14542187322L, "Paris");
        mapFiles.put(14542322143L, "Paris");
        mapFiles.put(34912632L, "Test");
        mapFiles.put(4440193226L, "VC");
        mapFiles.put(4440283520L, "VC");
        mapFiles.put(4440193226L, "VC");
        mapFiles.put(4440103773L, "VC");
    }

    public void createUniqueMapNumber() {
        long sum = 0;
        for (Object building : model.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.POLICE_OFFICE, StandardEntityURN.FIRE_STATION, StandardEntityURN.REFUGE)) {
            Building b = (Building) building;
            int[] ap = b.getApexList();
            for (int anAp : ap) {
                if (Long.MAX_VALUE - sum <= anAp) {
                    sum = 0;
                }
                sum += anAp;
            }
        }
        uniqueMapNumber = sum;
    }

    public String getMapName() {
        System.out.println("uniqueMapNumber: " + uniqueMapNumber);
        return mapFiles.get(uniqueMapNumber);
    }

    public void selectObject(int id) {

        selectedObject = model.getEntity(new EntityID(id));

        objectTextField.setText(selectedObject.toString());
        xTextField.setText(String.valueOf(selectedObject.getLocation(model).first()));
        yTextField.setText(String.valueOf(selectedObject.getLocation(model).second()));

        if (highwayFlag) {

        } else if (zoneFlag || zoneNeighbourFlag) {
            if (selectedObject instanceof Building) {
            }
            String idText = "";
            String neighbourIdText = "";
            if (selectedObject instanceof Building) {
                idText = String.valueOf("unZoned");
                Building b = (Building) selectedObject;
                for (ChichiZoneEntity zone : ChichiBuildingLayer.zones) {
                    if (zone.contains(b)) {
                        selectedZone = zone;
                        idText = String.valueOf(zone.getId());
                        for (Integer neighbour : zone.getNeighbors()) {
                            neighbourIdText += (String.valueOf(neighbour) + ", ");
                        }
                        break;
                    }
                }
            }

            groupIdTextField.setText(idText);
            zoneNeighbourTextField.setText(neighbourIdText);
        }

        ChichiStaticViewProperties.selectedObject = selectedObject;

        viewer.repaint();

    }

    @Override
    protected void postConnect() {
        super.postConnect();
        JFrame frame = new JFrame("ChiChi Editor " + getViewerID() + " (" + model.getAllEntities().size() + " entities)");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        viewer = new ChichiAnimatedWorldModelViewer();
        viewer.initialise(config);
        viewer.view(model);
        viewer.setPreferredSize(new Dimension(600, 500));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        topPanel.setPreferredSize(new Dimension(50, 100));

        objectTextField.setPreferredSize(new Dimension(50, 25));
        xTextField.setPreferredSize(new Dimension(50, 25));
        yTextField.setPreferredSize(new Dimension(50, 25));
        groupIdTextField.setPreferredSize(new Dimension(50, 25));
        zoneNeighbourTextField.setPreferredSize(new Dimension(50, 25));

        Box topBox = Box.createVerticalBox();

        Box idBox = Box.createHorizontalBox();

        idBox.add(new JLabel("    object:        "));
        idBox.add(objectTextField);
        topBox.add(Box.createRigidArea(new Dimension(10, 12)));
        topBox.add(idBox);
        topBox.add(Box.createRigidArea(new Dimension(10, 12)));
        Box xBox = Box.createHorizontalBox();
        xBox.add(new JLabel("         X:            "));
        xBox.add(xTextField);
        topBox.add(xBox);
        topBox.add(Box.createRigidArea(new Dimension(10, 12)));
        Box yBox = Box.createHorizontalBox();
        yBox.add(new JLabel("         Y:            "));
        yBox.add(yTextField);
        topBox.add(yBox);
        topBox.add(Box.createRigidArea(new Dimension(10, 12)));
        Box zoneIdBox = Box.createHorizontalBox();
        zoneIdBox.add(new JLabel("      group:      "));
        zoneIdBox.add(groupIdTextField);
        topBox.add(zoneIdBox);
        topBox.add(Box.createRigidArea(new Dimension(10, 12)));
        Box neighbourIdBox = Box.createHorizontalBox();
        neighbourIdBox.add(new JLabel(" neighbours: "));
        neighbourIdBox.add(zoneNeighbourTextField);
        topBox.add(neighbourIdBox);
        topBox.add(Box.createRigidArea(new Dimension(10, 12)));
        topPanel.add(topBox);

        JSplitPane bottomPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        bottomPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        bottomPanel.setPreferredSize(new Dimension(100, 200));
        bottomPanel.setDividerSize(1);

        JRadioButton none = new JRadioButton("none", true);
        JRadioButton highway = new JRadioButton("ChichiHighway", false);
        JRadioButton zone = new JRadioButton("MrlZone", false);
        JRadioButton zoneNeighbour = new JRadioButton("MrlZone Neighbours", false);
        ButtonGroup zoneOrHighway = new ButtonGroup();

        JButton saveButton = new JButton("Save");
        JButton clearButton = new JButton("Clear");
        JButton clearAllButton = new JButton("Clear All");
        JButton editButton = new JButton("Edit");
        JButton addNeibButton = new JButton("Add neighbour");
        JButton refColorButton = new JButton("Refresh Color");
        JButton loadButton = new JButton("Load File");
        JButton exportButton = new JButton("Export File");
        JButton deleteFileButton = new JButton("Delete File");

        zoneOrHighway.add(none);
        zoneOrHighway.add(highway);
        zoneOrHighway.add(zone);
        zoneOrHighway.add(zoneNeighbour);

        JPanel radioPanel = new JPanel();
        radioPanel.setPreferredSize(new Dimension(100, 10));

        radioPanel.setLayout(new GridLayout(4, 1));
        radioPanel.add(none);
        radioPanel.add(highway);
        radioPanel.add(zone);
        radioPanel.add(zoneNeighbour);
        radioPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "select once"));

        JPanel buttonPanel = new JPanel();
        radioPanel.setPreferredSize(new Dimension(100, 100));
        buttonPanel.setLayout(new GridLayout(9, 1));

        buttonPanel.add(saveButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(clearAllButton);
        buttonPanel.add(editButton);
        buttonPanel.add(addNeibButton);
        buttonPanel.add(refColorButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(deleteFileButton);

        bottomPanel.setTopComponent(radioPanel);
        bottomPanel.setBottomComponent(buttonPanel);

        JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pane.setDividerSize(10);
        pane.setDividerLocation(100);
        pane.setTopComponent(topPanel);
        pane.setBottomComponent(bottomPanel);

        this.panel.add(pane);
        this.panel.setPreferredSize(new Dimension(500, 500));

        JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(viewer);
        splitPane.setRightComponent(this.panel);
        splitPane.setDividerSize(5);

        frame.add(splitPane, BorderLayout.CENTER);

        frame.pack();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        splitPane.setDividerLocation(0.99);
        frame.setVisible(true);


        objectTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JTextField field = (JTextField) actionEvent.getSource();
                try {
                    selectObject(Integer.parseInt(field.getText()));
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        });

        xTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JTextField field = (JTextField) actionEvent.getSource();
                try {
                    if (yTextField.getText() != null) {
                        for (StandardEntity entity : model.getAllEntities()) {
                            if (entity instanceof Area) {
                                Area area = (Area) entity;
                                if (Integer.toString(area.getLocation(model).first()).equals(field.getText())
                                        && Integer.toString(area.getLocation(model).second()).equals(yTextField.getText())) {
                                    selectObject(entity.getID().getValue());
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        });

        yTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JTextField field = (JTextField) actionEvent.getSource();
                try {
                    if (xTextField.getText() != null) {
                        for (StandardEntity entity : model.getAllEntities()) {
                            if (entity instanceof Area) {
                                Area area = (Area) entity;
                                if (Integer.toString(area.getLocation(model).first()).equals(xTextField.getText())
                                        && Integer.toString(area.getLocation(model).second()).equals(field.getText())) {
                                    selectObject(entity.getID().getValue());
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        });


        groupIdTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JTextField field = (JTextField) actionEvent.getSource();
                try {
                    if (highwayFlag) {

                    } else if (zoneFlag || zoneNeighbourFlag) {
                        for (ChichiZoneEntity zoneEntity : ChichiBuildingLayer.zones) {
                            if (zoneEntity.getId() == Integer.parseInt(field.getText())) {
                                selectObject(zoneEntity.get(0).getID().getValue());
                            }
                        }
                    }
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        });

//        zoneNeighbourTextField.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                JTextField field = (JTextField) actionEvent.getSource();
//                try {
//                    selectObject(Integer.parseInt(field.getText()));
//                    if (highwayFlag) {
//
//                    } else if (zoneFlag) {
//                        ChichiBuildingLayer.zoneBuildings.clear();
//                    } else if (zoneNeighbourFlag) {
//                        ChichiBuildingLayer.zoneNeighbour.clear();
//                        mainZone = null;
//                    }
//                } catch (Exception ignore) {
//                }
//            }
//        });


        none.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                highwayFlag = false;
                zoneFlag = false;
                zoneNeighbourFlag = false;

                viewer.repaint();
            }
        });

        highway.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                highwayFlag = true;
                zoneFlag = false;
                zoneNeighbourFlag = false;

                viewer.repaint();
            }
        });

        zone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                highwayFlag = false;
                zoneFlag = true;
                zoneNeighbourFlag = false;

                viewer.repaint();
            }
        });

        zoneNeighbour.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                highwayFlag = false;
                zoneFlag = false;
                zoneNeighbourFlag = true;

                viewer.repaint();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (highwayFlag) {

                    } else if (zoneFlag) {
                        if (ChichiBuildingLayer.zoneBuildings.isEmpty()) {
                            return;
                        }
                        ChichiZoneEntity zone = new ChichiZoneEntity(ChichiBuildingLayer.getANewId());
                        for (Building building : ChichiBuildingLayer.zoneBuildings) {
                            zone.add(building);
                        }
                        ChichiBuildingLayer.zoneBuildings.clear();
                        ChichiBuildingLayer.zones.add(zone);

                        ChichiStaticViewProperties.selectedObject = null;

                    } else if (zoneNeighbourFlag) {
                        if (ChichiBuildingLayer.zoneNeighbour.isEmpty()) {
                            return;
                        }
                        ChichiZoneEntity zone = mainZone;
                        for (ChichiZoneEntity zoneEntity : ChichiBuildingLayer.zoneNeighbour) {
                            zone.addNeighbor(zoneEntity.getId());
                            zoneEntity.addNeighbor(zone.getId());
                        }
                        ChichiBuildingLayer.zoneNeighbour.clear();
                        mainZone = null;

                        ChichiStaticViewProperties.selectedObject = null;
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                viewer.repaint();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (highwayFlag) {

                } else if (zoneFlag) {
                    ChichiBuildingLayer.zoneBuildings.clear();
                } else if (zoneNeighbourFlag) {
                    ChichiBuildingLayer.zoneNeighbour.clear();
                    mainZone = null;
                }
                ChichiStaticViewProperties.selectedObject = null;
                viewer.repaint();
            }
        });

        clearAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (highwayFlag) {

                } else if (zoneFlag || zoneNeighbourFlag) {
                    ChichiBuildingLayer.zones.clear();
                    ChichiBuildingLayer.zoneBuildings.clear();
                    ChichiBuildingLayer.addedBuildings.clear();
                    ChichiBuildingLayer.zoneNeighbour.clear();
                    ChichiBuildingLayer.zoneIds.clear();
                    mainZone = null;
                    ChichiStaticViewProperties.selectedObject = null;
                }
                viewer.repaint();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (highwayFlag) {

                } else if (zoneFlag) {
                    if (selectedZone == null) {
                        return;
                    }
                    ChichiBuildingLayer.zoneBuildings.clear();
                    ChichiBuildingLayer.zoneBuildings.addAll(selectedZone);
                    for (ChichiZoneEntity zoneEntity : ChichiBuildingLayer.zones) {
                        zoneEntity.getNeighbors().remove((Integer) selectedZone.getId());
                    }
                    ChichiBuildingLayer.zoneIds.remove((Integer) selectedZone.getId());
                    ChichiBuildingLayer.zones.remove(selectedZone);
                    selectedZone = null;
                } else if (zoneNeighbourFlag) {
                    if (mainZone == null) {
                        mainZone = selectedZone;
                    }
                    if (selectedZone == null) {
                        return;
                    }
                    ChichiBuildingLayer.zoneNeighbour.clear();
                    for (Integer neighbour : selectedZone.getNeighbors()) {
                        ChichiBuildingLayer.zoneNeighbour.add(ChichiBuildingLayer.zones.getZone(neighbour));
                    }
                    for (ChichiZoneEntity zoneEntity : ChichiBuildingLayer.zones) {
                        zoneEntity.getNeighbors().remove((Integer) selectedZone.getId());
                    }
                }
                viewer.repaint();
            }
        });

        addNeibButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (highwayFlag) {
                } else if (zoneFlag) {
                } else if (zoneNeighbourFlag) {
                    if (mainZone == null) {
                        mainZone = selectedZone;
                    }
                }
                viewer.repaint();
            }
        });

        refColorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (highwayFlag) {
                    for (ChichiHighway highway : ChichiRoadLayer.highways) {
                        highway.refreshColor();
                    }
                } else if (zoneFlag || zoneNeighbourFlag) {
                    for (ChichiZoneEntity zone : ChichiBuildingLayer.zones) {
                        zone.refreshColor();
                    }
                }
                viewer.repaint();
            }
        });

        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    if (highwayFlag) {
                        BufferedReader bf = new BufferedReader(new FileReader(MRLConstants.PRECOMPUTE_DIRECTORY + getMapName() + ".highway"));
                        String str = bf.readLine();
                        int id;
                        ChichiHighway highway = null;
                        while (str != null) {
                            if (!str.isEmpty()) {
                                if (str.startsWith("id: ")) {
                                    id = Integer.parseInt(str.replaceFirst("id: ", ""));
                                    if (!ChichiRoadLayer.highwayIds.contains(id)) {
                                        ChichiRoadLayer.highwayIds.add(id);
                                        highway = new ChichiHighway(id);
                                    }
                                } else if (highway != null) {

                                    String[] values = str.split(",");
                                    int x = Integer.parseInt(values[0]);
                                    int y = Integer.parseInt(values[1]);
                                    Road road = null;
                                    try {
                                        Object obj = getObjectInPoint(new Point(x, y));
                                        if (obj instanceof Road) {
                                            road = (Road) obj;
                                        }
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }
                                    if (road != null) {
                                        ChichiRoadLayer.addedRoads.add(road);
                                        highway.add(road);
                                    }
                                }
                            } else if (highway != null) {
                                ChichiRoadLayer.highways.add(highway);
                                highway = null;
                            }
                            str = bf.readLine();
                        }
                        if (highway != null) {
                            ChichiRoadLayer.highways.add(highway);
                        }
                    } else if (zoneFlag || zoneNeighbourFlag) {
                        BufferedReader bf = new BufferedReader(new FileReader("poly/" + getMapName() + ".zone"));
                        String str = bf.readLine();
                        int id;
                        ChichiZoneEntity zone = null;
                        while (str != null) {
                            if (!str.isEmpty()) {
                                if (str.startsWith("id: ")) {
                                    id = Integer.parseInt(str.replaceFirst("id: ", ""));
                                    if (!ChichiBuildingLayer.zoneIds.contains(id)) {
                                        ChichiBuildingLayer.zoneIds.add(id);
                                        zone = new ChichiZoneEntity(id);
                                    }
                                } else if (str.startsWith("neighbours: ") && zone != null) {
                                    String s = str.replaceFirst("neighbours: ", "");
                                    String[] neighbourIds = s.split(", ");
                                    for (String neighbour : neighbourIds) {
                                        if (!neighbour.equals("")) {
                                            int neibId = Integer.parseInt(neighbour);
                                            zone.addNeighbor(neibId);
//                                            for (ChichiZoneEntity zoneEntity : ChichiBuildingLayer.zones) {
//                                                if (neibId == zoneEntity.getId()) {
//                                                    zone.addNeighbor(zoneEntity);
//                                                    break;
//                                                }
//                                            }
                                        }
                                    }
                                } else if (zone != null) {

                                    String[] values = str.split(",");
                                    int x = Integer.parseInt(values[0]);
                                    int y = Integer.parseInt(values[1]);
                                    Building building = null;
                                    try {
                                        Object obj = getObjectInPoint(new Point(x, y));
                                        if (obj instanceof Building) {
                                            building = (Building) obj;
                                        }
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }
                                    if (building != null) {
                                        ChichiBuildingLayer.addedBuildings.add(building);
                                        zone.add(building);
                                    }
                                }
                            } else if (zone != null) {
                                ChichiBuildingLayer.zones.add(zone);
                                zone = null;
                            }
                            str = bf.readLine();
                        }
                        if (zone != null) {
                            ChichiBuildingLayer.zones.add(zone);
                        }
                    }

                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
                viewer.repaint();
            }
        });

        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    if (highwayFlag) {

                    } else if (zoneFlag || zoneNeighbourFlag) {
                        if (ChichiBuildingLayer.zones.isEmpty()) {
                            return;
                        }
//                        File file = new File("data/" + getMapName() + ".zone");
//                        if (file.exists()) {
//                            file.delete();
//                        }
//                        FileWriter fr = new FileWriter(file, true);
//                        for (ChichiZoneEntity zone : ChichiBuildingLayer.zones) {

//                            fr.write("\r\n");
//                            fr.write("id: " + (zone.getId()) + "\r\n");
//                            fr.write("neighbour: ");
//                            for (Integer neighbour : zone.getNeighbors()) {
//                                fr.write(neighbour + ", ");
//                            }
//                            fr.write("\r\n");
//                            for (Building building : zone) {
//                                int x = building.getX();
//                                int y = building.getY();
//                                fr.write(x + "," + y + "\r\n");
//                            }
//                        }
//                        fr.flush();
//                        fr.close();
                        ChichiStaticViewProperties.selectedObject = null;
                    }
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
                viewer.repaint();
            }
        });

        deleteFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (highwayFlag) {
                    File file = new File(getMapName() + ".highway");
                    file.delete();
                } else if (zoneFlag || zoneNeighbourFlag) {
                    File file = new File(getMapName() + ".zone");
                    file.delete();
                }
                viewer.repaint();
            }
        });

        viewer.addViewListener(new ViewListener() {

            @Override
            public void objectsClicked(ViewComponent view, List<RenderedObject> objects) {
                ChichiBuildingLayer.thisCycleEditedZoneIds.clear();
                ChichiStaticViewProperties.selectedObject = null;
                selectedObject = null;
                if (objects.isEmpty()) {
                    viewer.repaint();
                }
                if (objects.size() == 1) {
                    StandardEntity entity = (StandardEntity) objects.get(0).getObject();
                    selectObject(entity.getID().getValue());
                } else {
                    popup.removeAll();
                    for (RenderedObject next : objects) {
                        StandardEntity entity = (StandardEntity) next.getObject();
                        JMenuItem jmi = new JMenuItem(entity.getID().getValue() + "-" + entity);
                        jmi.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {


                                JMenuItem source = (JMenuItem) actionEvent.getSource();
                                StringTokenizer st = new StringTokenizer(source.getText(), "-");
                                String menuID = st.nextToken();
                                int id = Integer.parseInt(menuID);
                                selectObject(id);
                            }
                        });
                        popup.add(jmi);
                    }
                    double x = MouseInfo.getPointerInfo().getLocation().getX();
                    double y = MouseInfo.getPointerInfo().getLocation().getY();

                    popup.show(viewer, (int) Math.round(x), (int) Math.round(y));
                }
            }

            @Override
            public void objectsRollover(ViewComponent viewComponent, List<RenderedObject> renderedObjects) {

            }

        });
    }

    @Override
    public String toString() {
        return "MRLs ChiChi Creator";
    }

    public Object getObjectInPoint(Point point) {
        for (StandardEntity entity : model.getAllEntities()) {
            if ((entity instanceof Road) || (entity instanceof Building)) {
                if (((Area) entity).getShape().contains(point)) {
                    return entity;
                }
            }
        }
        throw new RuntimeException("Object in This Point Not Exist: " + point);
    }


}
