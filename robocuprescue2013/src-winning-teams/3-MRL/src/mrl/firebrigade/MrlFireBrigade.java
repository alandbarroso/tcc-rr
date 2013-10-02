package mrl.firebrigade;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.clustering.ConvexObject;
import mrl.communication.Packet;
import mrl.firebrigade.simulator.WaterCoolingEstimator;
import mrl.firebrigade.sterategy.*;
import mrl.firebrigade.tools.ProcessAdvantageRatio;
import mrl.firebrigade.tools.ProcessExtinguishAreas;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.message.type.MessageTypes;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.genericsearch.*;
import mrl.viewer.layers.MrlConvexHullLayer;
import mrl.world.MrlWorld;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.properties.IntProperty;
import mrl.mosCommunication.helper.FireBrigadeMessageHelper;

import java.util.*;

/**
 * MRL fire brigade agent.
 */
public class MrlFireBrigade extends MrlPlatoonAgent<FireBrigade> {

    protected MrlFireBrigadeWorld world;
    private FireBrigadeMessageHelper fireBrigadeMessageHelper;


//    public FBCLStrategy getFBCLStrategy() {
//        return FBCLStrategy;
//    }

    //    protected MrlZone targetMrlZone;
//    private FireBrigadeDecisionManager decisionManager;
    private WaterCoolingEstimator coolingEstimator;
    private MrlFireBrigadeDirectionManager fireBrigadeDirectionManager;
    private ConvexObject convexObject;
    //    private FBCLStrategy FBCLStrategy;
    public BurningBuildingSearchManager fireSearcher;
    private BurningBuildingSearchDecisionMaker decisionMaker;
    private ProcessAdvantageRatio processAdvantageRatio;
    private ProcessExtinguishAreas processExtinguishAreas;

    private IFireBrigadeActionStrategy actionStrategy;
    private FireBrigadeActionStrategyType actionStrategyType = FireBrigadeActionStrategyType.DEFAULT;
    private FireBrigadeUtilities fireBrigadeUtilities;


    @Override
    public String toString() {
        return "MRL fire brigade ID: " + this.getID().getValue();
    }

    @Override
    protected void postConnect() {
        long startTime = System.currentTimeMillis();
        super.postConnect();
        this.world = (MrlFireBrigadeWorld) super.world;
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE);

        setFireBrigadeMessageHelper(new FireBrigadeMessageHelper(world, this, messageManager));
//        directionManager = new DirectionManager(world, maxWater);
//        this.decisionManager = new FireBrigadeDecisionManager(world, directionManager);
        this.coolingEstimator = new WaterCoolingEstimator();
        this.fireBrigadeDirectionManager = new MrlFireBrigadeDirectionManager(world);


//        FBCLStrategy = new FBCLStrategy(world, this, pathPlanner);


        //--------------SEARCH INITIATION-------------------

        decisionMaker = new BurningBuildingSearchDecisionMaker(world);
//        possibleBuildingSearchDecisionMaker = new PossibleBuildingSearchDecisionMaker(world);
        stupidSearchDecisionMaker = new StupidSearchDecisionMaker(world);
        heatTracerDecisionMaker = new HeatTracerDecisionMaker(world);
        fireSearcher = new BurningBuildingSearchManager(world, world.getPlatoonAgent(), decisionMaker, senseSearchStrategy);

//        myPower = maxPower;
//        possibleBuildingSearchManager = new PossibleBuildingSearchManager(world, this, possibleBuildingSearchDecisionMaker, senseSearchStrategy);
        stupidSearchManager = new StupidSearchManager(world, this, stupidSearchDecisionMaker, senseSearchStrategy);
        simpleSearchDecisionMaker = new SimpleSearchDecisionMaker(world);
        simpleSearchManager = new SimpleSearchManager(world, this, simpleSearchDecisionMaker, senseSearchStrategy);
//        civilianSearchBBDecisionMaker = new CivilianSearchBBDecisionMaker(world);
//        civilianSearchManager = new CivilianSearchManager(world, this, civilianSearchBBDecisionMaker, civilianSearchStrategy);
        heatTracerSearchManager = new HeatTracerSearchManager(world, this, heatTracerDecisionMaker, senseSearchStrategy);
        defaultSearchManager = new DefaultSearchManager(world, this, stupidSearchDecisionMaker, senseSearchStrategy);
        //--------------SEARCH INITIATION-------------------
        //--------------Border Initiation------------------
        world.setBorderBuildings();
        //--------------Border Initiation------------------

        //============Mostafa PreProcesses=================
        fireBrigadeUtilities = new FireBrigadeUtilities(world);
        processAdvantageRatio = new ProcessAdvantageRatio(world, config);
        processAdvantageRatio.process();
        processExtinguishAreas = new ProcessExtinguishAreas(world, config);
        processExtinguishAreas.process();

        //=================================================
        chooseActionStrategy();
        long endTime = System.currentTimeMillis();
        System.out.println("   success    ---->   total(" + (endTime - startTime) + ")");
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {

        super.think(time, changed, heard);
    }

    @Override
    public void processMessage(MessageEntity messageEntity) {
        getFireBrigadeMessageHelper().processMessage(messageEntity);
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
    }

    @Override
    public void act() throws CommandException, TimeOutException {
        chooseActionStrategy();
        actionStrategy.execute();
    }

    private void chooseActionStrategy() {
        if (world.getPlatoonAgent().isStuck()) {
            actionStrategyType = FireBrigadeActionStrategyType.STUCK_SITUATION;
        } else {
            actionStrategyType = FireBrigadeActionStrategyType.DEFAULT;
        }

        switch (actionStrategyType) {
            case LEGACY:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.LEGACY)) {
                    actionStrategy = new FBLegacyActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
                }
                break;
            case FIRE_CLUSTER_CONDITION_BASED:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.FIRE_CLUSTER_CONDITION_BASED)) {
                    actionStrategy = new FireClusterConditionBasedActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
                }
                break;
            case DEFAULT:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.DEFAULT)) {
                    actionStrategy = new DefaultFireBrigadeActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
                }
                break;
            case STUCK_SITUATION:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.STUCK_SITUATION)) {
                    actionStrategy = new StuckFireBrigadeActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
                }
                break;
        }
    }

    private boolean shouldBeRenewed(FireBrigadeActionStrategyType actionStrategyType) {
        return this.actionStrategy == null || !this.actionStrategy.getType().equals(actionStrategyType);
    }

    public ConvexObject getConvexObject() {
        return convexObject;
    }

    public MrlFireBrigadeDirectionManager getFireBrigadeDirectionManager() {
        return fireBrigadeDirectionManager;
    }

    public int getWater() {
        return me().getWater();
    }

    public IntProperty getWaterProperty() {
        return me().getWaterProperty();
    }

    public Boolean isWaterDefined() {
        return me().isWaterDefined();
    }

    public void setWater(int water) {
        me().setWater(water);
    }

    public void undefinedWater() {
        me().undefineWater();
    }

    public EntityID getPosition() {
        return me().getPosition();
    }

    public StandardEntity getPosition(MrlWorld world) {
        return me().getPosition(world);
    }

    public void setConvexObject(ConvexObject convexObject) {
        this.convexObject = convexObject;
    }


    public FireBrigadeMessageHelper getFireBrigadeMessageHelper() {
        return fireBrigadeMessageHelper;
    }

    public FireBrigadeUtilities getFireBrigadeUtilities() {
        return fireBrigadeUtilities;
    }

    public void setFireBrigadeMessageHelper(FireBrigadeMessageHelper fireBrigadeMessageHelper) {
        this.fireBrigadeMessageHelper = fireBrigadeMessageHelper;
    }
}