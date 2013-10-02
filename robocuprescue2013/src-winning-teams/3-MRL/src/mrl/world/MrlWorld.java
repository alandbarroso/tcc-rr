package mrl.world;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.MRLConstants;
import mrl.common.TimestampThreadLogger;
import mrl.common.Util;
import mrl.common.clustering.ClusterManager;
import mrl.helper.*;
import mrl.mosCommunication.entities.PositionTypes;
import mrl.partition.*;
import mrl.partitioning.IPartitionManager;
import mrl.partitioning.PolicePartitionManager;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.police.moa.Bid;
import mrl.viewer.StaticViewProperties;
import mrl.viewer.layers.*;
import mrl.world.object.*;
import mrl.world.object.mrlZoneEntity.MrlZoneFactory;
import mrl.world.object.mrlZoneEntity.MrlZones;
import mrl.world.routing.grid.AreaGrids;
import mrl.world.routing.path.Path;
import mrl.world.routing.path.Paths;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: mrl
 * Date: Apr 28, 2010
 * Time: 10:11:22 PM
 */
public class MrlWorld extends StandardWorldModel {
    public static Map<EntityID, Set<EntityID>> UNDEFINED_POSITION_CIVILIANS = new HashMap<EntityID, Set<EntityID>>();  //map(agentID => positionIDs)
    public static Map<EntityID, Set<EntityID>> DEFINED_POSITION_CIVILIANS = new HashMap<EntityID, Set<EntityID>>();    //map(agentID => positionIDs)
    protected StandardAgent self;
    protected MrlPlatoonAgent platoonAgent;
    protected MrlCentre centre;
    protected Human selfHuman;
    protected Building selfBuilding;

    protected int time;
    private Set<EntityID> changes = new FastSet<EntityID>();
    private Set<EntityID> previousChanges = new FastSet<EntityID>();

    protected boolean CommunicationLess = true;
    //    protected boolean CommunicationLimited = false;
    protected boolean isCommunicationLow = false;
    protected boolean isCommunicationMedium = false;
    protected boolean isCommunicationHigh = false;
    protected boolean isMapHuge = false;
    protected boolean isMapMedium = false;
    protected boolean isMapSmall = false;
    protected Area centerOfMap;
    protected int worldTotalArea;
    private int longestDistanceOfTheMap = 0; // when there is no blockade in the map

    protected IndexSort indexSort;
    protected List<IHelper> helpers = new ArrayList<IHelper>();
    protected List<PoliceForce> policeForceList = new ArrayList<PoliceForce>();
    protected List<FireBrigade> fireBrigadeList = new ArrayList<FireBrigade>();
    protected List<AmbulanceTeam> ambulanceTeamList = new ArrayList<AmbulanceTeam>();
    protected List<EntityID> unvisitedBuildings = new ArrayList<EntityID>();
    public List<EntityID> viewerEmptyBuildings = new ArrayList<EntityID>();
    public List<EntityID> viewerPartitionVisitedBuildings = new ArrayList<EntityID>();
    public List<EntityID> viewerPartitionVictimBuildings = new ArrayList<EntityID>();
    public Boolean viewerIsMergedPartitionVisitedBuildings = false;
    protected Set<EntityID> visitedBuildings = new FastSet<EntityID>();
    private Set<EntityID> thisCycleVisitedBuildings = new FastSet<EntityID>();
    protected Set<EntityID> sensedBuildings = new FastSet<EntityID>();
    protected List<MrlBuilding> shouldCheckInsideBuildings = new ArrayList<MrlBuilding>();
    protected MrlZones zones;
    protected List<MrlBuilding> mrlBuildings;
    protected Map<EntityID, MrlBuilding> tempBuildingsMap;
    protected List<MrlRoad> mrlRoads;
    protected Map<EntityID, MrlRoad> mrlRoadsMap;
    protected Set<EntityID> burningBuildings = new FastSet<EntityID>();
    protected Partitions partitions;
    protected Map<Human, Partition> humanPartitionMap;
    protected PreRoutingPartitions preRoutingPartitions;
    protected Paths paths;
    protected Map<EntityID, EntityID> entranceRoads = new FastMap<EntityID, EntityID>();

    protected int totalAreaOfAllBuildings = 0;

    protected Set<EntityID> shouldCheckBuildings = new FastSet<EntityID>();
    protected Set<EntityID> fullBuildings;
    protected Set<EntityID> emptyBuildings;

    /*---------------Sajjad-------------*/
    protected Set<EntityID> borderBuildings;
    public BorderEntities borderFinder;
    private ClusterManager fireClusterManager;

    /*---------------------------------------*/
    /*---------------Mostafa-------------*/
    private ClusterManager civilianClusterManager;
    protected Set<MrlBuilding> estimatedBurningBuildings = new FastSet<MrlBuilding>();
    /*-----------------------------------*/

    //    protected Highways highways;
    protected double pole = 0;
    private Map<Long, String> mapFiles = new FastMap<Long, String>();
    protected Long uniqueMapNumber;
    protected int minX, minY, maxX, maxY;
    protected double mapDiameter;

    public float rayRate = 0.0025f;
    private int kernel_TimeSteps = 1000;
    public int maxID = 0;

    Map<String, Building> buildingXYMap = new FastMap<String, Building>();
    Map<String, Road> roadXYMap = new FastMap<String, Road>();

    protected HashSet<Road> roadsSeen = new HashSet<Road>();
    protected Set<MrlBlockade> mrlBlockadesSeen = new HashSet<MrlBlockade>();
    protected Set<MrlRoad> mrlRoadsSeen = new HashSet<MrlRoad>();
    protected HashSet<Blockade> blockadeSeen = new HashSet<Blockade>();
    protected List<Building> buildingSeen = new ArrayList<Building>();

    //DELDAR..........
    protected AmbulanceUtilities ambulanceUtilities;
    private EntityID ambulanceLeaderID = null; // the leader who shoud process bids and allocate ambulances to victims and send their tasks
    private List<StandardEntity> firstTimeSeenVictims = new ArrayList<StandardEntity>();

    private Map<Integer, State> agentStateMap;

    private IPartitionManager partitionManager;
    private IPartitionManager pManager;

    private Map<EntityID, Map<EntityID, Bid>> targetBidsMap;
    private Map<EntityID, EntityID> civilianPositionMap;
    private Map<EntityID, EntityID> agentPositionMap;
    private Map<EntityID, EntityID> agentFirstPositionMap;
    private List<EntityID> heardCivilians;

    protected ClusterManager policeTargetClusterManager;

    public static Map<EntityID, Set<EntityID>> BLOCKED_BUILDINGS = new FastMap<EntityID, Set<EntityID>>();
    private long thinkStartTime_;
    private long thinkTime;
    private long thinkTimeThreshold;
    private boolean useSpeak;
    private int viewDistance;
    private int clearDistance;
    private int clearRadius;
    private int ignoreCommandTime;
    private int maxExtinguishDistance;
    private int voiceRange;
    private Set<StandardEntity> availableHydrants = new HashSet<StandardEntity>();


    public MrlWorld(StandardAgent self, Collection<? extends Entity> entities, Config config) {
        super();

        addEntities(entities);
        for (StandardEntity standardEntity : getEntitiesOfType(StandardEntityURN.POLICE_FORCE, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.AMBULANCE_TEAM)) {
            if (standardEntity instanceof FireBrigade) {
                fireBrigadeList.add((FireBrigade) standardEntity);
            } else if (standardEntity instanceof PoliceForce) {
                policeForceList.add((PoliceForce) standardEntity);
            } else if (standardEntity instanceof AmbulanceTeam) {
                ambulanceTeamList.add((AmbulanceTeam) standardEntity);
            }
            if (maxID < standardEntity.getID().getValue()) {
                maxID = standardEntity.getID().getValue();
            }
        }
        this.self = self;
        if (self instanceof MrlCentre) {
            selfBuilding = (Building) getEntity(self.getID());
            this.centre = (MrlCentre) self;
        } else {
            this.platoonAgent = (MrlPlatoonAgent) self;
            selfHuman = (Human) getEntity(self.getID());
        }

        retrieveConfigParameters(config);

        this.indexSort = new IndexSort();
        this.humanPartitionMap = new FastMap<Human, Partition>();
        this.civilianPositionMap = new FastMap<EntityID, EntityID>();
        this.agentPositionMap = new FastMap<EntityID, EntityID>();
        this.agentFirstPositionMap = new FastMap<EntityID, EntityID>();
        heardCivilians = new ArrayList<EntityID>();
        createUniqueMapNumber();
        initMapNames();
//        System.out.println("map name: " + getMapName());
//        System.out.println("unique Number: " + getUniqueMapNumber());

        initHelpers();
        for (StandardEntity s : getBuildings()) {
            Building b = (Building) s;
            String xy = b.getX() + "," + b.getY();
            buildingXYMap.put(xy, b);
        }

        createMrlBuildings();

        if (totalAreaOfAllBuildings == 0) {
            computeBuildingsTotalArea();
        }

//        createAreaGrids();

        for (StandardEntity s : getRoads()) {
            Road b = (Road) s;
            String xy = b.getX() + "," + b.getY();
            roadXYMap.put(xy, b);
        }
        calculateMapDimensions();
        this.paths = new Paths(this);
        createMrlRoads();
        getHelper(VisibilityHelper.class).setBuildingsVisitablePart();
//        HighwayFactory highwayFactory = new HighwayFactory(this);
//        this.highways = highwayFactory.createHighways("data/" + getMapName() + ".highway");
//        highways.initRoadHighwayMap();
//        if (MRLConstants.LAUNCH_VIEWER) {
//            MrlHighwaysLayer.ALL_HIGHWAYS = highways; // only for viewer
//        }
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlPathLayer.PATHLIST = getPaths();
        }


        Rendezvous r;
        MrlZoneFactory newMrlZoneFactory = new MrlZoneFactory(this);
// TODO @Pooya: Review it
        zones = newMrlZoneFactory.createZones(MRLConstants.PRECOMPUTE_DIRECTORY + getMapName() + ".zone");

//        for (mrl.partitioning.Partition partition : segmentationHelper.getPartitions()) {
//            for (Rendezvous rendezvous : partition.getRendezvous()) {
//                System.out.println(rendezvous.getPartitions() + " : " + rendezvous.getRoadList());
//            }
//        }

        if (MRLConstants.LAUNCH_VIEWER) {
            if (MrlZonePolygonLayer.ZONES == null) {
                MrlZonePolygonLayer.ZONES = zones;
            }
        }
        indexSort.fillLists(this);
        ambulanceUtilities = new AmbulanceUtilities(this);
        targetBidsMap = new FastMap<EntityID, Map<EntityID, Bid>>();
        agentStateMap = new FastMap<Integer, State>();
        fullBuildings = new FastSet<EntityID>();
        emptyBuildings = new FastSet<EntityID>();

        verifyMap();

        borderBuildings = new FastSet<EntityID>();
        borderFinder = new BorderEntities(this);


        createClusterManager();
        availableHydrants.addAll(getHydrants());
    }

    private void verifyMap() {

//        if (getEntity(getSelf().getID()) instanceof Human) {
//            if (getMapHeight() > (getPlatoonAgent().viewDistance * 16)
//                    || getMapWidth() > (getPlatoonAgent().viewDistance * 16)) {
//                isMapHuge = true;
//            }
//        }

        double mapDimension = Math.hypot(getMapWidth(), getMapHeight());

        double rate = mapDimension / MRLConstants.MEAN_VELOCITY_OF_MOVING;

        if (rate > 60) {
            isMapHuge = true;
        } else if (rate > 30) {
            isMapMedium = true;
        } else {
            isMapSmall = true;
        }


    }

    private void createClusterManager() {
        fireClusterManager = new mrl.common.clustering.FireClusterManager(this);
//        setCivilianClusterManager(new CivilianClusterManager(this));
//        policeTargetClusterManager=new PoliceTargetClusterManager(this);

    }

    private void computeBuildingsTotalArea() {
        Building building;
        for (StandardEntity buildingEntity : getBuildings()) {
            building = (Building) buildingEntity;
            totalAreaOfAllBuildings += building.getTotalArea();
        }
    }

    private void createAreaGrids() {
        Road road;
        int agentSize = 500;
        AreaGrids areaGrids;
        for (StandardEntity entity : getRoads()) {
            road = (Road) entity;
            areaGrids = new AreaGrids(road, agentSize);

        }
    }

    @Override
    public void merge(ChangeSet changeSet) {
        TimestampThreadLogger threadLogger = TimestampThreadLogger.getCurrentThreadLogger();
        threadLogger.log("merge(ChangeSet changeSet) started.");
        if (pManager == null) {
            pManager = new PolicePartitionManager(this, null);
        }


        threadLogger.log("merge(ChangeSet changeSet) 1");
//        if (!isCommunicationLess() && !(selfHuman instanceof FireBrigade)) {
//            try {
//                changeSet = pManager.preprocessChanges(changeSet);
//            } catch (Exception ex) {
//                ex.printStackTrace();
////                System.out.println("unKnown exception");
//            }
//        }

        if (changeSet != null) {
            changes = changeSet.getChangedEntities();
        } else {
            System.out.println(" NULL changeSet  " + getTime() + " " + self);
            return;
        }
        PropertyHelper propertyHelper = getHelper(PropertyHelper.class);
        HumanHelper humanHelper = getHelper(HumanHelper.class);
        roadsSeen.clear();
        mrlBlockadesSeen.clear();
        mrlRoadsSeen.clear();
        blockadeSeen.clear();
        buildingSeen.clear();

        for (EntityID entityID : changeSet.getChangedEntities()) {

            try {
                //<<<<<<<<<<<<<<<<<<<<<<<< CIVILIAN >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.CIVILIAN.toString())) {
                    Civilian civilian = (Civilian) getEntity(entityID);
                    if (civilian == null) {
                        civilian = new Civilian(entityID);
                        addNewCivilian(civilian);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        civilian.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(civilian.getProperty(p.getURN()), time);
                    }
                    humanHelper.setFromSense(civilian.getID(), true);

                    //updating Civilian position map
                    EntityID prevPosition = civilianPositionMap.get(civilian.getID());
                    EntityID currentPosition = civilian.getPosition();
                    if (prevPosition != null && getEntity(prevPosition) instanceof Building) {

                        if (!prevPosition.equals(currentPosition)) {
                            getMrlBuilding(prevPosition).getHumans().remove(civilian);
                            if (getEntity(currentPosition) instanceof Building) {
                                getMrlBuilding(currentPosition).getHumans().add(civilian);
                            }
                        }
                    } else if (getEntity(currentPosition) instanceof Building) {
                        getMrlBuilding(currentPosition).getHumans().add(civilian);
                    }
                    civilianPositionMap.put(civilian.getID(), civilian.getPosition());


                    setDamage(civilian);


//                humanHelper.setPreviousDamage(civilian.getID(),civilian.getDamage());
//                humanHelper.setCurrentDamage(civilian.getID(),civilian.getDamage());
//                humanHelper.setLastTimeDamageChanged(civilian.getID(),getTime());
//
//                humanHelper.setPreviousHP(civilian.getID(),civilian.getHP());
//                humanHelper.setCurrentHP(civilian.getID(),civilian.getHP());
//                humanHelper.setLastTimeHPChanged(civilian.getID(),getTime());

                    if (humanHelper.getNearestRefugeID(civilian.getID()) == null) {

                        firstTimeSeenVictims.add(civilian); //todo add it for other agents

                        humanHelper.setFirstHP(civilian.getID(), civilian.getHP());
                        humanHelper.setFirstDamage(civilian.getID(), civilian.getDamage());
                        humanHelper.setFirstBuriedness(civilian.getID(), civilian.getBuriedness());
                        if ((civilian.getDamage() != 0 || civilian.getBuriedness() != 0) && civilian.getHP() > 0 && civilian.getPosition(this) instanceof Area
                                && !(civilian.getPosition(this) instanceof Refuge)) {  //todo, should We let all agents do it or just ATs should do it?
                            Pair<Integer, EntityID> p = ambulanceUtilities.approximatingTTR(civilian);
                            humanHelper.setNearestRefuge(civilian.getID(), p.second());
                            humanHelper.setTimeToRefuge(civilian.getID(), p.first());
//                        System.out.println(getTime() + " " + self.getID() + " " + civilian.getID() + " >>> NearestRefuge SEEEEEEEEN >> " + p);
                        }
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< BLOCKADE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BLOCKADE.toString())) {
                    Blockade blockade = (Blockade) getEntity(entityID);
                    if (blockade == null) {
                        blockade = new Blockade(entityID);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {

                        blockade.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(blockade.getProperty(p.getURN()), time);
                    }
                    if (getEntity(blockade.getPosition()) != null) {
                        Area area = (Area) getEntity(blockade.getPosition());
                        if (area.getBlockades() == null) {
                            area.setBlockades(new ArrayList<EntityID>());
                        }
                        if (!area.getBlockades().contains(blockade.getID())) {
                            ArrayList<EntityID> blockades = new ArrayList<EntityID>(area.getBlockades());
                            blockades.add(blockade.getID());
                            area.setBlockades(blockades);
                        }
                    }

                    if (getEntity(blockade.getID()) == null) {
                        addEntityImpl(blockade);
                        propertyHelper.addEntityProperty(blockade, time);
                    }


                    blockadeSeen.add(blockade);
                    //<<<<<<<<<<<<<<<<<<<<<<<< BUILDING >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BUILDING.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.REFUGE.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.AMBULANCE_CENTRE.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.FIRE_STATION.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.POLICE_OFFICE.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.GAS_STATION.toString())) {
                    Building building = (Building) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        building.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(building.getProperty(p.getURN()), time);
                    }
                    if (/*(selfHuman instanceof FireBrigade) &&*/ building.isFierynessDefined() && building.isTemperatureDefined()) {
//                    MrlFireBrigadeWorld w = (MrlFireBrigadeWorld) this;
                        MrlBuilding mrlBuilding = getMrlBuilding(entityID);
                        mrlBuilding.setEnergy(building.getTemperature() * mrlBuilding.getCapacity());
                        switch (building.getFieryness()) {
                            case 0:
                                mrlBuilding.setFuel(mrlBuilding.getInitialFuel());
                                if (mrlBuilding.getEstimatedTemperature() >= mrlBuilding.getIgnitionPoint()) {
                                    mrlBuilding.setEnergy(mrlBuilding.getIgnitionPoint() / 2);
                                }
                                break;
                            case 1:
                                if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.66) {
                                    mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.75));
                                } else if (mrlBuilding.getFuel() == mrlBuilding.getInitialFuel()) {
                                    mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.90));
                                }
                                break;

                            case 2:
                                if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.33
                                        || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.66) {
                                    mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.50));
                                }
                                break;

                            case 3:
                                if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.01
                                        || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.33) {
                                    mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.15));
                                }
                                break;

                            case 8:
                                mrlBuilding.setFuel(0);
                                break;
                        }
                    }
                    if (getEntity(building.getID()) == null) {
                        addEntityImpl(building);
                        propertyHelper.addEntityProperty(building, time);
                    }


                    //updating burning buildings set
                    if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                        burningBuildings.add(building.getID());
                    } else {
                        burningBuildings.remove(building.getID());
                    }


                    buildingSeen.add(building);
                    sensedBuildings.add(building.getID());
                    MrlBuilding mrlBuilding = getMrlBuilding(building.getID());
                    mrlBuilding.setSensed(getTime());
                    if (building.isOnFire()) {
                        mrlBuilding.setIgnitionTime(getTime());
                    }


                    //<<<<<<<<<<<<<<<<<<<<<<<< ROAD >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.ROAD.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.HYDRANT.toString())) {
                    Road road = (Road) getEntity(entityID);
                    if (road == null) {
                        road = new Road(entityID);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        road.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(road.getProperty(p.getURN()), time);
                    }
                    if (getEntity(road.getID()) == null) {
                        addEntityImpl(road);
                        propertyHelper.addEntityProperty(road, time);
                    }
                    roadsSeen.add(road);
                    MrlRoad mrlRoad = this.getMrlRoad(entityID);
                    mrlRoadsSeen.add(mrlRoad);
                    mrlBlockadesSeen.addAll(mrlRoad.getMrlBlockades());
                    //<<<<<<<<<<<<<<<<<<<<<<<< FIRE_BRIGADE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.FIRE_BRIGADE.toString())) {
                    FireBrigade fireBrigade = (FireBrigade) getEntity(entityID);

//                    System.out.println(getTime()+" "+getSelf().getID() +" agent: "+ fireBrigade.getID());

//                    if (!(selfHuman instanceof AmbulanceTeam) || !fireBrigade.isBuriednessDefined() || fireBrigade.getHP() == 0) {
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        fireBrigade.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(fireBrigade.getProperty(p.getURN()), time);
                    }
//                    } else {
//
//                        Property p = changeSet.getChangedProperty(entityID, fireBrigade.getPositionProperty().getURN());
//                        fireBrigade.setPosition((EntityID) p.getValue());
//                        propertyHelper.setPropertyTime(fireBrigade.getPositionProperty(), time);
//
//                        p = changeSet.getChangedProperty(entityID, fireBrigade.getBuriednessProperty().getURN());
//                        fireBrigade.setBuriedness((Integer) p.getValue());
//                        propertyHelper.setPropertyTime(fireBrigade.getBuriednessProperty(), time);
//
//                    }

//                    if (getPlatoonAgent() != null) {
//                        getPlatoonAgent().markVisitedBuildings((Area) getEntity(fireBrigade.getPosition()));
//                    }

                    if (Util.isOnBlockade(this, fireBrigade)) {
                        getHelper(HumanHelper.class).setLockedByBlockade(fireBrigade.getID(), true);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< POLICE_FORCE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.POLICE_FORCE.toString())) {
                    PoliceForce policeForce = (PoliceForce) getEntity(entityID);
//                    if (!(selfHuman instanceof AmbulanceTeam) || !policeForce.isBuriednessDefined() || policeForce.getHP() == 0) {
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        policeForce.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(policeForce.getProperty(p.getURN()), time);
                    }
//                    } else {
//
//                        if (policeForce.isBuriednessDefined() && policeForce.getBuriedness() > 0) {
//                            if (policeForce.isDamageDefined() && policeForce.getDamage() == 0) {
//                                System.out.print("");
//                                if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
//                                    System.out.println(getTime() + " " + getSelf().getID() + " >>>>>>>> SENSE ID:" + policeForce.getID() + " DMG:" + policeForce.getDamage() + " HP:" + policeForce.getHP() + " BRD:" + policeForce.getBuriedness());
//                                }
//                            }
//                        }
//
//                        Property p = changeSet.getChangedProperty(entityID, policeForce.getPositionProperty().getURN());
//                        policeForce.setPosition((EntityID) p.getValue());
//                        propertyHelper.setPropertyTime(policeForce.getPositionProperty(), time);
//
//                        p = changeSet.getChangedProperty(entityID, policeForce.getBuriednessProperty().getURN());
//                        policeForce.setBuriedness((Integer) p.getValue());
//                        propertyHelper.setPropertyTime(policeForce.getBuriednessProperty(), time);
//                    }

//                    if (getPlatoonAgent() != null) {
//                        getPlatoonAgent().markVisitedBuildings((Area) getEntity(policeForce.getPosition()));
//                    }

                    if (Util.isOnBlockade(this, policeForce)) {
                        getHelper(HumanHelper.class).setLockedByBlockade(policeForce.getID(), true);
                    }

                    //<<<<<<<<<<<<<<<<<<<<<<<< AMBULANCE_TEAM >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.AMBULANCE_TEAM.toString())) {
                    AmbulanceTeam ambulanceTeam = (AmbulanceTeam) getEntity(entityID);

//                    System.out.println(getTime()+" "+getSelf().getID() +" agent: "+ ambulanceTeam.getID());

//                    if (!(selfHuman instanceof AmbulanceTeam) || ambulanceTeam.getID().equals(self.getID()) || !ambulanceTeam.isBuriednessDefined() || ambulanceTeam.getHP() == 0) {
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        ambulanceTeam.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(ambulanceTeam.getProperty(p.getURN()), time);
                    }
//                    } else {
//
//                        Property p = changeSet.getChangedProperty(entityID, ambulanceTeam.getPositionProperty().getURN());
//                        ambulanceTeam.setPosition((EntityID) p.getValue());
//                        propertyHelper.setPropertyTime(ambulanceTeam.getPositionProperty(), time);
//
//                        p = changeSet.getChangedProperty(entityID, ambulanceTeam.getBuriednessProperty().getURN());
//                        ambulanceTeam.setBuriedness((Integer) p.getValue());
//                        propertyHelper.setPropertyTime(ambulanceTeam.getBuriednessProperty(), time);
//
//                    }

//                    if (getPlatoonAgent() != null) {
//                        getPlatoonAgent().markVisitedBuildings((Area) getEntity(ambulanceTeam.getPosition()));
//                    }

                    if (Util.isOnBlockade(this, ambulanceTeam)) {
                        getHelper(HumanHelper.class).setLockedByBlockade(ambulanceTeam.getID(), true);
                    }

                    //<<<<<<<<<<<<<<<<<<<<<<<< FIRE_STATION >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.FIRE_STATION.toString())) {
                    FireStation fireStation = (FireStation) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        fireStation.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(fireStation.getProperty(p.getURN()), time);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< POLICE_OFFICE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.POLICE_OFFICE.toString())) {
                    PoliceOffice policeOffice = (PoliceOffice) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        policeOffice.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(policeOffice.getProperty(p.getURN()), time);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< AMBULANCE_CENTRE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.AMBULANCE_CENTRE.toString())) {
                    AmbulanceCentre ambulanceCentre = (AmbulanceCentre) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        ambulanceCentre.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(ambulanceCentre.getProperty(p.getURN()), time);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< StandardEntity >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else {
                    StandardEntity standardEntity = getEntity(entityID);
                    if (!(standardEntity instanceof Refuge)) {
                        System.out.println("unknown standardEntity :" + standardEntity);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        standardEntity.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(standardEntity.getProperty(p.getURN()), time);
                    }
                }
            } catch (NullPointerException e) {
                if (MRLConstants.LAUNCH_VIEWER) {
                    System.out.println("null in Merge Operation");
                }
                e.printStackTrace();
            } catch (ClassCastException ex) {
                ex.printStackTrace();
//                printData(ex.getMessage());
            }
        }
        threadLogger.log("merge(ChangeSet changeSet) 2");
    }

    private ChangeSet processPreviousChanges(ChangeSet changeSet) {
        if (!isCommunicationLess()) {
            try {
                changeSet = pManager.preprocessChanges(changeSet);
            } catch (Exception ex) {
//                System.out.println("unKnown exception");
            }
        }
        return changeSet;
    }

    public void updateEveryCycle() {


        if (time > 1) {
            for (StandardEntity road : getRoadsSeen()) {
//                try {
                MrlRoad mrlRoad = getMrlRoad(road.getID());
                if (mrlRoad.isNeedUpdate()) {
                    mrlRoad.update();
                }
                getHelper(RoadHelper.class).updatePassably(getMrlRoad(road.getID()));
                mrlRoad.setLastSeenTime(getTime());
                mrlRoad.setSeen(true);
//                } catch (Exception ex) {
//                    System.out.println(" update exception");
//                }
            }
            if (selfHuman != null && /*getTime() % MRLConstants.AVAILABLE_HYDRANTS_UPDATE_TIME == 0 &&*/ selfHuman instanceof FireBrigade && !getHydrants().isEmpty()) {
                availableHydrants.clear();
                availableHydrants.addAll(getHydrants());
                StandardEntity position;
                MrlRoad hydrantMrlRoad;
                PropertyHelper propertyHelper = getHelper(PropertyHelper.class);
                for (FireBrigade fireBrigade : getFireBrigadeList()) {
                    if (fireBrigade.getID().equals(selfHuman.getID())) {
                        continue;
                    }
                    if (fireBrigade.isPositionDefined()) {
                        position = fireBrigade.getPosition(this);
                        if (position instanceof Hydrant) {
                            hydrantMrlRoad = getMrlRoad(position.getID());
                            int agentDataTime = propertyHelper.getEntityLastUpdateTime(fireBrigade);
                            int hydrantSeenTime = hydrantMrlRoad.getLastSeenTime();
                            if (getTime() - agentDataTime > 10 && getTime() - hydrantSeenTime > 10) {
                                printData("my data from " + fireBrigade + " is out of date... my data time is : " + agentDataTime + " and hydrant seen time is: " + hydrantSeenTime);
                                continue;
                            }
                            availableHydrants.remove(position);
                        }
                    }
                }
            }

            for (StandardEntity road : getRoads()) {
                MrlRoad mrlRoad = getMrlRoad(road.getID());
                mrlRoad.resetOldPassably();
            }
            for (StandardEntity buildingEntity : getBuildings()) {
                if (MRLConstants.LAUNCH_VIEWER) {
                    StandardEntity selected = StaticViewProperties.selectedObject;
                }
                MrlBuilding mrlBuilding;
                if (buildingEntity instanceof Refuge) {
                    mrlBuilding = getMrlBuilding(buildingEntity.getID());
                    mrlBuilding.resetOldPassability(MRLConstants.REFUGE_PASSABLY_RESET_TIME);
                } else {
                    mrlBuilding = getMrlBuilding(buildingEntity.getID());
                    mrlBuilding.resetOldPassability(MRLConstants.BUILDING_PASSABLY_RESET_TIME);
                }
                boolean reachable = false;
                if (mrlBuilding.isOneEntranceOpen(this)) {
                    reachable = true;
                }
                MrlRoad mrlRoad;
                if (reachable) {
                    boolean tempReachable = false;
                    for (Road road : BuildingHelper.getEntranceRoads(this, mrlBuilding.getSelfBuilding())) {
                        mrlRoad = getMrlRoad(road.getID());
                        if (mrlRoad.isReachable()) {
                            tempReachable = true;
                            break;
                        }
                    }
                    if (!tempReachable) {
                        reachable = false;
                    }
                }
                mrlBuilding.setReachable(reachable);
                mrlBuilding.getCivilianPossibly().clear();
            }

            if (MRLConstants.LAUNCH_VIEWER && platoonAgent != null) {
                EntityID agentID = platoonAgent.getID();
                DEFINED_POSITION_CIVILIANS.put(agentID, new HashSet<EntityID>());
                UNDEFINED_POSITION_CIVILIANS.put(agentID, new HashSet<EntityID>());
                Civilian civilian;
                for (StandardEntity civ : getCivilians()) {
                    civilian = (Civilian) civ;
                    if (civilian.isPositionDefined()) {
                        DEFINED_POSITION_CIVILIANS.get(agentID).add(civilian.getID());
                    } else {
                        UNDEFINED_POSITION_CIVILIANS.get(agentID).add(civilian.getID());
                    }
                }
            }
        }
        for (IHelper helper : helpers) {
            helper.update();
        }
        if (getSelfHuman() instanceof FireBrigade) {
            zones.update();
        }
        try {
            fireClusterManager.updateClusters();
//           getCivilianClusterManager().updateClusters();
//            policeTargetClusterManager.updateClusters();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            try {
                MrlUnvisitedBuildingLayer.UNVISITED_BUILDINGS_MAP.put(self.getID(), getUnvisitedBuildings());
                MrlAmbulanceImprtantBuildingsLayer.PARTITION_VISITED_BUILDINGS_MAP.put(self.getID(), getViewerPartitionVisitedBuildings());
                MrlAmbulanceImprtantBuildingsLayer.IS_MERGED_VISITED_BUILDINGS_MAP.put(self.getID(), getViewerIsMergedPartitionVisitedBuildings());
                MrlAmbulanceImprtantBuildingsLayer.VICTIM_BUILDINGS_MAP.put(self.getID(), getViewerPartitionVictimBuildings());
                MrlUnvisitedFireBasedBuildingLayer.UNVISITED_FIRE_BASED_BUILDINGS_MAP.put(self.getID(), getBuildingSeen());
                MrlBurningBuildingLayer.BURNING_BUILDINGS_MAP.put(self.getID(), getMrlBuildings());
                MrlEstimatedLayer.BURNING_BUILDINGS_MAP.put(self.getID(), getMrlBuildings());
                MrlObjectsValueLayer.ZONE_VALUE_MAP.put(self.getID(), zones);
                MrlObjectsValueLayer.VISITED_CIVILIAN_MAP.put(self.getID(), getCivilians());
                fireClusterManager.updateConvexHullsForViewer();
//                getCivilianClusterManager().updateConvexHullsForViewer();
//                policeTargetClusterManager.updateConvexHullsForViewer();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }


    private void fillPropertiesFromViewer() {
        Building building;
        Building selfBuilding;
        System.err.println("Remember to remove this function");
        for (StandardEntity entity : MrlConvexHullLayer.BUILDINGS.values()) {
            building = (Building) entity;
            selfBuilding = this.getMrlBuilding(building.getID()).getSelfBuilding();
            try {
                if (building.isFierynessDefined()) {
                    selfBuilding.setFieryness(building.getFieryness());
                    selfBuilding.setTemperature(building.getTemperature());
                } else {
                }
            } catch (Exception ex) {
            }
        }
    }


//    private void updateBurningBuilding() {
//        burningBuildings.clear();
//        for (StandardEntity standardEntity : getBuildings()) {
//            Building building = (Building) standardEntity;
//            if (building.isFierynessDefined()) {
//                if (building.getFieryness() > 0 && building.getFieryness() < 4) {
//                    burningBuildings.add(building.getID());
//                }
//            }
//        }
//    }

    public void initMapNames() {
//        mapFiles.put(2141239244L, "Kobe");
//        mapFiles.put(17687985466L, "Berlin");
//        mapFiles.put(17687924365L, "Berlin");
//        mapFiles.put(14542274827L, "Paris");
//        mapFiles.put(14542369921L, "Paris");
//        mapFiles.put(14542187322L, "Paris");
//        mapFiles.put(14542322143L, "Paris");
//        mapFiles.put(34912632L, "Test");
//        mapFiles.put(4440193226L, "VC");
//        mapFiles.put(4440283520L, "VC");
//        mapFiles.put(4440193226L, "VC");
//        mapFiles.put(4440103773L, "VC");

    }

    protected void initHelpers() {
        //-- add helpers
        helpers.add(new PropertyHelper(this));
        helpers.add(new AreaHelper(this));
        helpers.add(new RoadHelper(this));
        helpers.add(new EdgeHelper());
        helpers.add(new HumanHelper(this));
        helpers.add(new CivilianHelper(this));
        helpers.add(new VisibilityHelper(this));

        for (IHelper helper : helpers) {
            helper.init();
        }
    }

    private void createUniqueMapNumber() {
        long sum = 0;
        for (StandardEntity building : getBuildings()) {
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

//        System.out.println("Unique Map Number=" + uniqueMapNumber);
    }

    private void createMrlRoads() {
        mrlRoads = new ArrayList<MrlRoad>();
        mrlRoadsMap = new FastMap<EntityID, MrlRoad>();
        for (StandardEntity rEntity : getRoads()) {
            Road road = (Road) rEntity;
            MrlRoad mrlRoad = new MrlRoad(road, this);
            mrlRoads.add(mrlRoad);
            mrlRoadsMap.put(road.getID(), mrlRoad);
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            Map<EntityID, MrlRoad> roadMap = new HashMap<EntityID, MrlRoad>();
            for (MrlRoad road : mrlRoads) {
                roadMap.put(road.getID(), road);
            }
            MrlRoad.VIEWER_ROADS_MAP.put(self.getID(), roadMap);
        }
    }

    private void createMrlBuildings() {

        tempBuildingsMap = new FastMap<EntityID, MrlBuilding>();
        mrlBuildings = new ArrayList<MrlBuilding>();
        MrlBuilding mrlBuilding;
        Building building;

        for (StandardEntity standardEntity : getBuildings()) {
            building = (Building) standardEntity;
            String xy = building.getX() + "," + building.getY();
            buildingXYMap.put(xy, building);

            mrlBuilding = new MrlBuilding(standardEntity, this);

            if ((standardEntity instanceof Refuge)
                    || (standardEntity instanceof FireStation)
                    || (standardEntity instanceof PoliceOffice)
                    || (standardEntity instanceof AmbulanceCentre)) {  //todo all of these buildings may be flammable..............
                mrlBuilding.setFlammable(false);
            }
            mrlBuildings.add(mrlBuilding);
            tempBuildingsMap.put(standardEntity.getID(), mrlBuilding);

            // ina bejaye building helper umade.
            unvisitedBuildings.add(standardEntity.getID());
            viewerEmptyBuildings.add(standardEntity.getID());
            worldTotalArea += mrlBuilding.getSelfBuilding().getTotalArea();

        }
        shouldCheckInsideBuildings.clear();

        for (MrlBuilding b : mrlBuildings) {
            Collection<StandardEntity> neighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_SAMPLE_DISTANCE);
//            Collection<StandardEntity> fireNeighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
            List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
//            List<EntityID> fireNeighbours = new ArrayList<EntityID>();
            for (StandardEntity entity : neighbour) {
                if (entity instanceof Building) {
                    neighbourBuildings.add(entity.getID());
                    b.addMrlBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
                }
            }
//            for (StandardEntity entity : fireNeighbour) {
//                if (entity instanceof Building) {
//                    fireNeighbours.add(entity.getID());
//                    //b.addMrlBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
//                }
//            }
            b.setNeighbourIdBuildings(neighbourBuildings);
            //MTN
            if (b.getEntrances() != null) {
                building = b.getSelfBuilding();
                List<Road> rEntrances = BuildingHelper.getEntranceRoads(this, building);
                for (Road road : rEntrances) {
                    entranceRoads.put(road.getID(), b.getID());
                }


                boolean shouldCheck = true;
//                if (rEntrances != null) {
//                    if (rEntrances.size() == 0)
//                        shouldCheck = false;
                VisibilityHelper visibilityHelper = getHelper(VisibilityHelper.class);
                for (Road road : rEntrances) {
                    boolean shouldCheckTemp = !visibilityHelper.isInsideVisible(new Point(road.getX(), road.getY()), new Point(building.getX(), building.getY()), building.getEdgeTo(road.getID()), viewDistance);
                    if (!shouldCheckTemp) {
                        shouldCheck = false;
                        break;
//                    }
                    }
                }
                b.setShouldCheckInside(shouldCheck);
                if (shouldCheck) {
                    shouldCheckInsideBuildings.add(b);
                }


            }
//            b.setNeighbourFireBuildings(fireNeighbours);
            if (MRLConstants.LAUNCH_VIEWER) {
                MrlBuildingLayer.MRL_BUILDINGS_MAP.put(b.getID(), b);
            }
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            Map<EntityID, MrlBuilding> buildingMap = new HashMap<EntityID, MrlBuilding>();
            for (MrlBuilding building1 : mrlBuildings) {
                buildingMap.put(building1.getID(), building1);
            }
            MrlBuilding.VIEWER_BUILDINGS_MAP.put(self.getID(), buildingMap);
        }
    }

    private void calculateMapDimensions() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        Pair<Integer, Integer> pos;
        for (StandardEntity standardEntity : this.getAllEntities()) {
            pos = standardEntity.getLocation(this);
            if (pos.first() < this.minX)
                this.minX = pos.first();
            if (pos.second() < this.minY)
                this.minY = pos.second();
            if (pos.first() > this.maxX)
                this.maxX = pos.first();
            if (pos.second() > this.maxY)
                this.maxY = pos.second();
        }
    }

    private void calculateMapDiameter() {
        mapDiameter = Math.sqrt(Math.pow(getBounds().getHeight(), 2) + Math.pow(getBounds().getWidth(), 2)) / 2;
    }

    public void partitionMakingOperations(Human self) {
        if (!(self instanceof PoliceForce)) {
            return;
        }

        this.partitions = new Partitions(this, self);

    }

    public void preRoutingPartitions() {
        this.preRoutingPartitions = new PreRoutingPartitions(this);
        this.preRoutingPartitions.setColumnNums(2);
        this.preRoutingPartitions.setRowNums(2);
    }

    private List<EntityID> getNeighbours(StandardEntity building) {
        List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
        Collection<StandardEntity> entityCollection = getObjectsInRange(building, Wall.MAX_SAMPLE_DISTANCE);
        for (StandardEntity entity : entityCollection) {
            if (entity instanceof Building) {
                neighbourBuildings.add(entity.getID());
            }
        }
        return neighbourBuildings;
    }

    public List<MrlBuilding> getMrlBuildings() {
        return mrlBuildings;
    }

    public List<MrlRoad> getMrlRoads() {
        return mrlRoads;
    }

    public MrlRoad getMrlRoad(EntityID roadID) {
        return mrlRoadsMap.get(roadID);
    }

    public MrlBuilding getMrlBuilding(EntityID id) {
        return tempBuildingsMap.get(id);
    }

    private void setDamage(Human human) {
        if (human.getBuriedness() > 0 && human.getDamage() == 0) {
            human.setDamage(6);
        }
    }

    public MrlPlatoonAgent getPlatoonAgent() {
        return platoonAgent;
    }

    public MrlCentre getMrlCentre() {
        return centre;
    }

    public MrlCentre getCenterAgent() {
        return this.centre;
    }

    public boolean isCommunicationLess() {
        return CommunicationLess;
    }

    public void setCommunicationLess(boolean CL) {
        this.CommunicationLess = CL;
    }

//    public boolean isCommunicationLimited() {
//        return CommunicationLimited;
//    }
//
//    public void setCommunicationLimited(boolean cl) {
//        CommunicationLimited = cl;
//    }

    public MrlZones getZones() {
        return zones;
    }

    public List<EntityID> getBuriedAgents() {
        return getHelper(HumanHelper.class).getBuriedAgents();
    }

    public List<MrlBuilding> getShouldCheckInsideBuildings() {
        return shouldCheckInsideBuildings;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void addNewCivilian(Civilian civilian) {
        this.addEntityImpl(civilian);
        getHelper(PropertyHelper.class).addEntityProperty(civilian, getTime());
        getHelper(HumanHelper.class).setInfoMap(civilian.getID());
        getHelper(CivilianHelper.class).setInfoMap(civilian.getID());
    }

    public <T extends IHelper> T getHelper(Class<T> c) {
        for (IHelper helper : helpers) {
            if (c.isInstance(helper)) {
                return c.cast(helper);
            }
        }
        throw new RuntimeException("Helper not available for:" + c);
    }

    public Area getCenterOfMap() {
        if (centerOfMap != null) {
            return centerOfMap;
        }

        double ret;
        int min_x = Integer.MAX_VALUE;
        int max_x = Integer.MIN_VALUE;
        int min_y = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;

        Collection<StandardEntity> areas = getAreas();

        long x = 0, y = 0;
        Area result;

        for (StandardEntity entity : areas) {
            Area area1 = (Area) entity;
            x += area1.getX();
            y += area1.getY();
        }

        x /= areas.size();
        y /= areas.size();
        result = (Area) areas.iterator().next();
        for (StandardEntity entity : areas) {
            Area temp = (Area) entity;
            if (Util.distance((int) x, (int) y, result.getX(), result.getY()) > Util.distance((int) x, (int) y, temp.getX(), temp.getY())) {
                result = temp;
            }

            if (temp.getX() < min_x) {
                min_x = temp.getX();
            } else if (temp.getX() > max_x)
                max_x = temp.getX();

            if (temp.getY() < min_y) {
                min_y = temp.getY();
            } else if (temp.getY() > max_y)
                max_y = temp.getY();
        }
        ret = (Math.pow((min_x - max_x), 2) +
                Math.pow((min_y - max_y), 2));
        ret = Math.sqrt(ret);
        pole = ret;
        centerOfMap = result;

        return result;
    }

    public PartitionsI getPartitions() {
        //    if(isCommunicationLess())
        return partitions;
        //  else
        //      return searchingPartitions;
    }

    public PreRoutingPartitions getPreRoutingPartitions() {
        return preRoutingPartitions;
    }

    public StandardAgent getSelf() {
        return self;
    }

    public Human getSelfHuman() {
        return selfHuman;
    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }

    public StandardEntity getSelfPosition() {
        if (self instanceof MrlCentre) {
            return selfBuilding;
        } else {
            return selfHuman.getPosition(this);
        }
    }

    public PositionTypes getSelfPositionType() {
        if (getSelfPosition() instanceof Road) {
            return PositionTypes.Road;
        } else {
            return PositionTypes.Building;
        }
    }

    public Pair<Integer, Integer> getSelfLocation() {
        if (self instanceof MrlPlatoonAgent) {
            return selfHuman.getLocation(this);
        } else {
            return selfBuilding.getLocation(this);
        }
    }

    public Paths getPaths() {
        return paths;
    }

    public Path getPath(EntityID id) {

        for (Path path : paths) {
            if (path.getId().equals(id)) {
                return path;
            }
        }
        return null;
    }

//    public Highways getHighways() {
//        return highways;
//    }

    public Config getConfig() {
        if (self instanceof MrlPlatoonAgent) {
            return ((MrlPlatoonAgent) self).getConfig();
        } else if (self instanceof MrlCentre) {
            return ((MrlCentre) self).getConfig();
        }
        return null;
    }

    public Set<EntityID> getVisitedBuildings() {
        return visitedBuildings;
    }

    public List<EntityID> getUnvisitedBuildings() {
        return unvisitedBuildings;
    }

    public List<EntityID> getViewerEmptyBuildings() {
        return viewerEmptyBuildings;
    }

    public List<EntityID> getViewerPartitionVisitedBuildings() {
        return viewerPartitionVisitedBuildings;
    }

    public List<EntityID> getViewerPartitionVictimBuildings() {
        return viewerPartitionVictimBuildings;
    }

    public Boolean getViewerIsMergedPartitionVisitedBuildings() {
        return viewerIsMergedPartitionVisitedBuildings;
    }

    public Set<EntityID> getBurningBuildings() {
        return burningBuildings;
    }

    public List<EntityID> getUnvisitedBuildings(boolean openEntrance) {
        if (openEntrance) {
            List<EntityID> openEntranceBuildings = new ArrayList<EntityID>();
            for (EntityID buildingId : getUnvisitedBuildings()) {
                if (getMrlBuilding(buildingId).isOneEntranceOpen(this)) {
                    openEntranceBuildings.add(buildingId);
                }
            }
            return openEntranceBuildings;
        } else {
            List<EntityID> blockedEntranceBuildings = new ArrayList<EntityID>();
            for (EntityID buildingId : getUnvisitedBuildings()) {
                if (getMrlBuilding(buildingId).isAllEntrancesBlocked(this)) {
                    blockedEntranceBuildings.add(buildingId);
                }
            }
            if (blockedEntranceBuildings.isEmpty()) {
                return getUnvisitedBuildings();
            }
            return blockedEntranceBuildings;
        }
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getMapWidth() {
        return maxX - minX;
    }

    public int getMapHeight() {
        return maxY - minY;
    }

    public Long getUniqueMapNumber() {
        return uniqueMapNumber;
    }

    public HashSet<Road> getRoadsSeen() {
        return roadsSeen;
    }

    public Set<MrlRoad> getMrlRoadsSeen() {
        return mrlRoadsSeen;
    }

    public Set<MrlBlockade> getMrlBlockadesSeen() {
        return mrlBlockadesSeen;
    }

    public Set<Blockade> getBlockadeSeen() {
        return blockadeSeen;
    }

    public List<Building> getBuildingSeen() {
        return buildingSeen;
    }

    public IndexSort getIndexes() {
        return indexSort;
    }

    public List<EntityID> getInMyPartition(Collection<EntityID> buildings) {
        List<EntityID> inPartition = new ArrayList<EntityID>();
        for (EntityID entityID : buildings) {
            Building building = (Building) getEntity(entityID);
            if (getPartitions().getMyPartition().getBuildings().contains(getMrlBuilding(building.getID()))) {
                inPartition.add(entityID);
            }
        }
        return inPartition;
    }

    public <T extends StandardEntity> T getEntity(EntityID id, Class<T> c) {
        StandardEntity entity;

        entity = getEntity(id);
        if (c.isInstance(entity)) {
            T castedEntity;

            castedEntity = c.cast(entity);
            return castedEntity;
        } else {
            return null;
        }
    }

    public List<EntityID> getEntityIdsOfType(StandardEntityURN urn) {
        Collection<StandardEntity> entities = getEntitiesOfType(urn);
        List<EntityID> list = new ArrayList<EntityID>();
        for (StandardEntity entity : entities) {
            list.add(entity.getID());
        }
        return list;
    }

    public List<EntityID> getBuildingsIds() {
        return getEntityIdsOfType(StandardEntityURN.BUILDING);//todo should check deeply
    }

    public Collection<StandardEntity> getBuildings() {
        return getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getHydrants() {
        return getEntitiesOfType(StandardEntityURN.HYDRANT);
    }

    public Set<StandardEntity> getAvailableHydrants() {
        return availableHydrants;
    }

    public Collection<StandardEntity> getGasStations() {
        return getEntitiesOfType(StandardEntityURN.GAS_STATION);
    }


    public Set<EntityID> getBuildingIDs() {
        Set<EntityID> buildingIDs = new FastSet<EntityID>();
        Collection<StandardEntity> buildings = getBuildings();
        for (StandardEntity entity : buildings) {
            buildingIDs.add(entity.getID());
        }

        return buildingIDs;
    }

    public Collection<StandardEntity> getRefuges() {
        return getEntitiesOfType(StandardEntityURN.REFUGE);
    }

    public Collection<StandardEntity> getCentres() {
        return getEntitiesOfType(
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION);
    }

    public Collection<StandardEntity> getRoads() {
        return getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
    }

    public Collection<StandardEntity> getAreas() {
        return getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.ROAD,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getHumans() {
        return getEntitiesOfType(
                StandardEntityURN.CIVILIAN,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getAgents() {
        return getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    public Collection<StandardEntity> getPlatoonAgents() {
        return getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getPoliceForces() {
        return getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
    }

    public Collection<StandardEntity> getPoliceOffices() {
        return getEntitiesOfType(StandardEntityURN.POLICE_OFFICE);
    }

    public Collection<StandardEntity> getAmbulanceTeams() {
        return getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getAmbulanceCentres() {
        return getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE);
    }

    public Collection<StandardEntity> getFireBrigades() {
        return getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE);
    }

    public Collection<StandardEntity> getFireStations() {
        return getEntitiesOfType(StandardEntityURN.FIRE_STATION);
    }

    public Collection<StandardEntity> getCivilians() {
        return getEntitiesOfType(StandardEntityURN.CIVILIAN);
    }

    public List<PoliceForce> getPoliceForceList() {
        return policeForceList;
    }

    public List<FireBrigade> getFireBrigadeList() {
        return fireBrigadeList;
    }

    public List<AmbulanceTeam> getAmbulanceTeamList() {
        return ambulanceTeamList;
    }

    public Set<EntityID> getChanges() {
        return changes;
    }

    public boolean isVisible(StandardEntity entity) {
        return changes.contains(entity.getID()) && Util.distance(entity.getLocation(this), getSelfLocation()) <= viewDistance;
//        if(entity instanceof Human )
//        {
//        return ((Human) entity).getPosition().equals(getSelfPosition().getID());
//        }else {
//           entity.getID().equals(getSelfPosition().getID());
//        }
    }

    public boolean isVisible(EntityID entityID) {
        return changes.contains(entityID);
    }

    public int getWorldTotalArea() {
        return worldTotalArea;
    }

    public String getMapName() {
        String s = mapFiles.get(getUniqueMapNumber());
        if (s == null) {
            return getUniqueMapNumber().toString();
        }
        return s;
    }

    public int getLongestDistanceOfTheMap() {
        return longestDistanceOfTheMap;
    }

    public void setLongestDistanceOfTheMap(int predictedDistance) {
        this.longestDistanceOfTheMap = predictedDistance;
    }

    public Building getBuildingInPoint(int x, int y) {
        String xy = x + "," + y;
        return buildingXYMap.get(xy);
//        if (building == null) {
//            for (StandardEntity entity : getBuildings()) {
//                if (((Area) entity).getShape().contains(point)) {
//                    return entity;
//                }
//            }
//        }
//        return building;
    }

    public Road getRoadInPoint(Point point) {
        String xy = point.getX() + "," + point.getY();
        Road road = roadXYMap.get(xy);
        if (road == null) {
            for (StandardEntity entity : getRoads()) {
                Road r = (Road) entity;
                if (r.getShape().contains(point)) {
                    return r;
                }
            }
        }
        return road;
    }

    public void printData(String data) {
        System.out.println("Time:" + time + " " + self + " - " + data);
    }

    public List<Path> getPathsOfThisArea(Area area) {
        int loop = 0;
        List<Path> paths = new ArrayList<Path>();
        List<Area> neighbours = new ArrayList<Area>();
        Road road = null;
        Area tempArea;
        neighbours.add(area);
        Area neighbour;
        EntityID pathId;
        RoadHelper roadHelper = getHelper(RoadHelper.class);
        while (road == null && !neighbours.isEmpty() && loop < 20) {
            loop++;
            tempArea = neighbours.get(0);
            neighbours.remove(0);

            for (EntityID entityID : tempArea.getNeighbours()) {
                neighbour = (Area) getEntity(entityID);
                if (getEntity(entityID) instanceof Road) {
                    road = (Road) getEntity(entityID);
                    pathId = roadHelper.getPathId(road.getID());
                    if (pathId != null) {
                        Path path = getPath(pathId);
                        if (!paths.contains(path)) {
                            paths.add(path);
                        }
                    }
                } else {
                    if (!neighbours.contains(neighbour)) {
                        neighbours.add(neighbour);
                    }
                }
            }
        }

        return paths;
    }

    public EntityID getAmbulanceLeaderID() {
        return ambulanceLeaderID;
    }

    public void setAmbulanceLeaderID(EntityID entityID) {
        this.ambulanceLeaderID = entityID;
    }

    public boolean amIAmbulanceLeader() {
        if (this.ambulanceLeaderID != null && this.ambulanceLeaderID.equals(self.getID())) {
            return true;
        } else {
            return false;
        }

    }

    public List<StandardEntity> getFirstTimeSeenVictims() {
        return firstTimeSeenVictims;
    }

//    public List<StandardEntity> getFreeAgents() {
//        HumanHelper humanHelper = getHelper(HumanHelper.class);
//        List<StandardEntity> freeAgents = new ArrayList<StandardEntity>();
//        freeAgents.addAll(getAgents());
//        freeAgents.removeAll(humanHelper.getBlockedAgents());
//        freeAgents.removeAll(humanHelper.getBuriedAgents());
//        return freeAgents;
//    }

    public int getKernel_TimeSteps() {
        return kernel_TimeSteps;
    }

    public void setKernelTimeSteps(int timeSteps) {
        this.kernel_TimeSteps = timeSteps;
    }

    public double getMapDiameter() {
        if (mapDiameter == 0) {
            calculateMapDiameter();
        }
        return mapDiameter;
    }


    /**
     * It is a data structure for keeping a Map of bidder and their bids for each specified target
     *
     * @return bids for each target
     */
    public Map<EntityID, Map<EntityID, Bid>> getTargetBidsMap() {
        return targetBidsMap;
    }


    /**
     * It keeps states of the agent in each time cycle
     *
     * @return a amp of agent states Cycle-State
     */
    public Map<Integer, State> getAgentStateMap() {
        return agentStateMap;
    }

    public IPartitionManager getPartitionManager() {
        return partitionManager;
    }

    public void setPartitionManager(IPartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public void getPathLength() {
        for (Path p : getPaths()) {
            getPlatoonAgent().getPathPlanner().planMove(p.getHeadOfPath(), p.getEndOfPath(), 0, false);
            p.setLenght(getPlatoonAgent().getPathPlanner().getPathCost());
        }
    }

    public Map<EntityID, EntityID> getCivilianPositionMap() {
        return civilianPositionMap;
    }

    public Map<EntityID, EntityID> getAgentPositionMap() {
        return agentPositionMap;
    }

    public Map<EntityID, EntityID> getAgentFirstPositionMap() {
        return agentFirstPositionMap;
    }

    public int getMyDistanceTo(StandardEntity entity) {
        return getDistance(getSelfPosition(), entity);
    }

    public int getMyDistanceTo(EntityID entityID) {
        return getDistance(getSelfPosition(), getEntity(entityID));
    }

    /**
     * gets buildings which contain one or more civilians
     *
     * @return set of full building ids
     */
    public Set<EntityID> getFullBuildings() {
        return fullBuildings;
    }

    /**
     * gets buildings which is empty  means contains no civilian
     *
     * @return set of empty buildings
     */
    public Set<EntityID> getEmptyBuildings() {
        return emptyBuildings;
    }

    public int getTotalAreaOfAllBuildings() {
        return totalAreaOfAllBuildings;
    }

    public Polygon getWorldPolygon() {
        Polygon worldPolygon;

        double[] point = new double[4];
        int xs[] = new int[4];
        int ys[] = new int[4];

        point[0] = this.getMinX() - 1;
        point[1] = this.getMinY() - 1;
        point[2] = this.getMaxX() + 1;
        point[3] = this.getMaxY() + 1;

        xs[0] = (int) point[0];
        ys[0] = (int) point[1];

        xs[1] = (int) point[2];
        ys[1] = (int) point[1];

        xs[2] = (int) point[2];
        ys[2] = (int) point[3];

        xs[3] = (int) point[0];
        ys[3] = (int) point[3];

        worldPolygon = new Polygon(xs, ys, 4);

        return worldPolygon;
    }

    public boolean isMapHuge() {
        return isMapHuge;
    }

    public boolean isMapMedium() {
        return isMapMedium;
    }

    public boolean isMapSmall() {
        return isMapSmall;
    }

    public boolean isEntrance(Road road) {
        return entranceRoads.containsKey(road.getID());

    }

    /**
     * this method remove input building from {@code visitedBuildings}, add it in the {@code unvisitedBuilding} and prepare
     * message that should be send.<br/><br/>
     * <font color="red"><b>Note: </b></font> this method is calling automatically in  agent {@code act} in {@link MrlPlatoonAgent}
     *
     * @param buildingID {@code EntityID} of building that visited!
     */
    public void setBuildingVisited(EntityID buildingID) {
        MrlBuilding mrlBuilding = getMrlBuilding(buildingID);
        if (!mrlBuilding.isVisited() && platoonAgent != null) {
            thisCycleVisitedBuildings.add(buildingID);
            mrlBuilding.setVisited();
            visitedBuildings.add(buildingID);
            unvisitedBuildings.remove(buildingID);
        }
    }

    /**
     * add civilian who speak of it was heard in current cycle!
     *
     * @param civID EntityID of civilian
     */
    public void addHeardCivilian(EntityID civID) {
        if (MRLConstants.LAUNCH_VIEWER) {
            if (!MrlHumanLayer.HEARD_POSITIONS.containsKey(civID)) {
                MrlHumanLayer.HEARD_POSITIONS.put(civID, new HashSet<Pair<Integer, Integer>>());
            }
            MrlHumanLayer.HEARD_POSITIONS.get(civID).add(getSelfLocation());
        }
        if (!heardCivilians.contains(civID)) {
            heardCivilians.add(civID);
        }
    }

    /**
     * Gets heard civilians at current cycle;<br/>
     * <br/>
     * <b>Note: </b> At each cycle the list will be cleared
     *
     * @return EntityIDs of heard civilians
     */
    public List<EntityID> getHeardCivilians() {
        return heardCivilians;
    }

    /**
     * Map of entrance RoadID to BuildingID
     *
     * @return
     */
    public Map<EntityID, EntityID> getEntranceRoads() {
        return entranceRoads;
    }

    public Set<EntityID> getShouldCheckBuildings() {
        return shouldCheckBuildings;
    }

    public void setShouldCheckBuildings(Set<EntityID> shouldCheckBuildings) {
        this.shouldCheckBuildings = shouldCheckBuildings;
    }

    public Set<EntityID> getBorderBuildings() {
        return borderBuildings;
    }


    /**
     * @return
     */
    public Set<EntityID> getThisCycleVisitedBuildings() {
        return thisCycleVisitedBuildings;
    }

    /**
     * this method keeps last changeSet entries in {@code 'previousChanges'} object
     */
    public void updatePreviousChangeSet() {

        previousChanges.clear();
        previousChanges.addAll(changes);
    }

    public Set<EntityID> getPreviousChanges() {
        return previousChanges;
    }


    @Override
    public Collection<StandardEntity> getObjectsInRange(EntityID entity, int range) {
        return super.getObjectsInRange(getEntity(entity), range);
    }

    @Override
    public Collection<StandardEntity> getObjectsInRange(StandardEntity entity, int range) {
        return super.getObjectsInRange(entity, range);
    }

    @Override
    public Collection<StandardEntity> getObjectsInRange(int x, int y, int range) {
        int newRange = (int) (0.64 * range);
        return super.getObjectsInRange(x, y, newRange);
    }

    public ClusterManager getFireClusterManager() {
        return fireClusterManager;
    }

    public ClusterManager getPoliceTargetClusterManager() {
        return policeTargetClusterManager;
    }


    public boolean isBuried(Human human) {
        return human.isBuriednessDefined() && human.getBuriedness() > 0;
    }

    public void retrieveConfigParameters(Config config) {
        thinkTime = config.getIntValue(MRLConstants.THINK_TIME_KEY);
        thinkTimeThreshold = (long) (thinkTime * 0.9);
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(MRLConstants.SPEAK_COMMUNICATION_MODEL_KEY);
//        this.viewDistance = config.getIntValue(MAX_VIEW_DISTANCE_KEY);
        viewDistance = config.getIntValue(MRLConstants.MAX_VIEW_DISTANCE_KEY);
        ignoreCommandTime = getConfig().getIntValue(MRLConstants.IGNORE_AGENT_COMMANDS_KEY);
        clearDistance = config.getIntValue(MRLConstants.MAX_CLEAR_DISTANCE_KEY);
        clearRadius = config.getIntValue(MRLConstants.CLEAR_RADIUS_KEY, 2000);//todo <====================== clear radius key is not visible ... Kernel Bug
        maxExtinguishDistance = config.getIntValue(MRLConstants.MAX_EXTINGUISH_DISTANCE_KEY);
        voiceRange = config.getIntValue(MRLConstants.VOICE_RANGE_KEY);

    }

    public int getIgnoreCommandTime() {
        return ignoreCommandTime;
    }

    public int getVoiceRange() {
        return voiceRange;
    }

    public int getClearDistance() {
        return clearDistance;
    }

    public int getClearRadius() {
        return clearRadius;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public int getMaxExtinguishDistance() {
        return maxExtinguishDistance;
    }

    public boolean isUseSpeak() {
        return useSpeak;
    }

    public long getThinkStartTime_() {
        return thinkStartTime_;
    }

    public void setThinkStartTime_(long thinkStartTime_) {
        this.thinkStartTime_ = thinkStartTime_;
    }

    public long getThinkTime() {
        return thinkTime;
    }

    public long getThinkTimeThreshold() {
        return thinkTimeThreshold;
    }

    /**
     * This method finds nearest refuge based on euclidean distance
     *
     * @param positionId entityID of the position to find nearest refuge based on it
     * @return EntityID of the nearest refuge; if there is no refuges, returns null
     */
    public EntityID findNearestRefuge(EntityID positionId) {

        Collection<StandardEntity> refuges = getRefuges();
        EntityID nearestID = null;
        int nearestDistance = Integer.MAX_VALUE;
        int tempDistance;
        if (positionId != null && refuges != null && !refuges.isEmpty()) {

            for (StandardEntity refugeEntity : refuges) {
                tempDistance = getDistance(refugeEntity.getID(), positionId);
                if (tempDistance < nearestDistance) {
                    nearestDistance = tempDistance;
                    nearestID = refugeEntity.getID();
                }
            }

        }

        return nearestID;
    }

    public List<StandardEntity> getEntities(List<EntityID> entityIDs) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (EntityID next : entityIDs) {
            result.add(getEntity(next));
        }
        return result;
    }

    public List<StandardEntity> getAreasInShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getAreas()) {
            Area area = (Area) next;
            if (shape.contains(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    public List<StandardEntity> getAreasIntersectWithShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getAreas()) {
            Area area = (Area) next;
            if (shape.intersects(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    public ClusterManager getCivilianClusterManager() {
        return civilianClusterManager;
    }

    public void setCivilianClusterManager(ClusterManager civilianClusterManager) {
        this.civilianClusterManager = civilianClusterManager;
    }

    public Set<MrlBuilding> getEstimatedBurningBuildings() {
        return estimatedBurningBuildings;
    }

    public void setEstimatedBurningBuildings(Set<MrlBuilding> estimatedBurningBuildings) {
        this.estimatedBurningBuildings = estimatedBurningBuildings;
    }

    public <T extends StandardEntity> List<T> getEntitiesOfType(Class<T> c, StandardEntityURN urn) {
        Collection<StandardEntity> entities = getEntitiesOfType(urn);
        List<T> list = new ArrayList<T>();
        for (StandardEntity entity : entities) {
            if (c.isInstance(entity)) {
                list.add(c.cast(entity));
            }
        }
        return list;
    }

    public <T extends Area> List<T> getEntitiesOfType(Class<T> c, Collection<StandardEntity> entities) {
        List<T> list = new ArrayList<T>();
        for (StandardEntity entity : entities) {
            if (c.isInstance(entity)) {
                list.add(c.cast(entity));
            }
        }
        return list;
    }

    public boolean isCommunicationLow() {
        return isCommunicationLow;
    }

    public void setCommunicationLow(boolean communicationLow) {
        isCommunicationLow = communicationLow;
    }

    public boolean isCommunicationMedium() {
        return isCommunicationMedium;
    }

    public void setCommunicationMedium(boolean communicationMedium) {
        isCommunicationMedium = communicationMedium;
    }

    public boolean isCommunicationHigh() {
        return isCommunicationHigh;
    }

    public void setCommunicationHigh(boolean communicationHigh) {
        isCommunicationHigh = communicationHigh;
    }
}
