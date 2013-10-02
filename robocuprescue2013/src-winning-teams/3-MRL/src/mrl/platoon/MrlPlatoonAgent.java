package mrl.platoon;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.ambulance.MrlAmbulanceTeamWorld;
//import mrl.common.BenchMark.CivilianLogManager;
//import mrl.common.BenchMark.TotalCivilianLogManager;
import mrl.common.*;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.HumanHelper;
import mrl.mosCommunication.MessageTester;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.message.MessageManager;
import mrl.mosCommunication.message.type.MessageTypes;
import mrl.partition.Partition;
import mrl.platoon.genericsearch.*;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceForceWorld;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.AverageTools;
import mrl.world.routing.pathPlanner.IPathPlanner;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.messages.control.KASense;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.util.*;

/**
 * User: mrl
 * Date: Apr 28, 2010
 * Time: 10:56:00 PM
 */
public abstract class MrlPlatoonAgent<T extends StandardEntity> extends StandardAgent<T> implements MRLConstants {
    // for viewer
//    public static Map<EntityID, Integer> VALUE_FOR_DISPLAY_IN_VIEWER = new FastMap<EntityID, Integer>();
    public static Map<EntityID, MrlPlatoonAgent> PLATOON_AGENTS_FOR_VIEWER = new HashMap<EntityID, MrlPlatoonAgent>();
    public static EntityID CHECK_ID = new EntityID(0);
    public static EntityID CHECK_ID2 = new EntityID(0);
    //-------------------------------------------
    protected MrlWorld world;
    protected IPathPlanner pathPlanner;
    protected MessageManager messageManager;
    //    protected ISearchHelper searchHelper;
    //Sajjad
    public double refillRate = 0;

    private String lastCommand;
    protected State agentState = State.SEARCHING;
    private List<EntityID> visitedBuildings;
//    private List<EntityID> visitedBuildingsHelp;

    protected boolean isHardWalking = false;
    //    protected boolean isStuck = false;
    protected boolean isWorking = false;
    private boolean movingRendezvous = false;
    private boolean onRendezvous = false;

    public DefaultSearchManager defaultSearchManager;
    public StupidSearchManager stupidSearchManager;
    public SimpleSearchManager simpleSearchManager;
    public SimpleSearchDecisionMaker simpleSearchDecisionMaker;
    public CheckBlockadesManager checkBlockadesManager;
    //    public ManualSearchManager manualSearchManager;
    private List<StandardEntity> roads;
    public HeatTracerSearchManager heatTracerSearchManager;
    public CivilianSearchManager civilianSearchManager;
    public CivilianSearchBBDecisionMaker civilianSearchBBDecisionMaker;
    public SenseSearchStrategy senseSearchStrategy;
    public CivilianSearchStrategy civilianSearchStrategy;
    public StupidSearchDecisionMaker stupidSearchDecisionMaker;
    public HeatTracerDecisionMaker heatTracerDecisionMaker;
    public CheckBlockadesDecisionMaker checkBlockadesDecisionMaker;
    public ManualSearchDecisionMaker manualSearchDecisionMaker;
    public AverageTools averageTools;
//    public CivilianLogManager civilianLogManager;
//    public TotalCivilianLogManager totalCivilianLogManager;

    private Set<StandardEntity> unReachablePositions = new FastSet<StandardEntity>();
    private Map<EntityID, Integer> unReachablePositionTime = new FastMap<EntityID, Integer>();

    public StuckChecker stuckChecker;
    private int tryCount = 5;
    private Blockade coveringBlockade;
    private StuckState stuckState;
    private int idleTimes = 0;// times in which the agent dose nothing

    public void sendSubscribe(int... channel) {
        sendSubscribe(world.getTime(), channel);
    }

    public void sendMessage(int channel, byte[] message) {
        sendSpeak(world.getTime(), channel, message);
    }

    public void sendMoveAct(int time, List<EntityID> path) throws CommandException {
        super.sendMove(time, path);
        throw new CommandException("Move");
    }

    public void sendMoveAct(int time, List<EntityID> path, int destinationX, int destinationY) throws CommandException {
        super.sendMove(time, path, destinationX, destinationY);
        throw new CommandException("Move To Point");
    }

    public void sendRandomWalkAct(int time, List<EntityID> path) throws CommandException {
        super.sendMove(time, path);
        throw new CommandException("Random Walk");
    }

    public void sendRestAct(int time) throws CommandException {
        super.sendRest(time);
        throw new CommandException("Rest");
    }

    protected void sendRescueAct(int time, EntityID target) throws CommandException {
        super.sendRescue(time, target);
        throw new CommandException("Rescue");
    }

    protected void sendLoadAct(int time, EntityID target) throws CommandException {
        super.sendLoad(time, target);
        throw new CommandException("Load");
    }

    protected void sendUnloadAct(int time) throws CommandException {
        super.sendUnload(time);
        throw new CommandException("Unload");
    }

    public void sendClearAct(int time, EntityID target) throws CommandException {
        super.sendClear(time, target);
        throw new CommandException("Clear");
    }

    public void sendClearAct(int time, int destinationX, int destinationY) throws CommandException {
        super.sendClear(time, destinationX, destinationY);
        throw new CommandException("Clear");
    }

    public void sendExtinguishAct(int time, EntityID target, int water) throws CommandException {
        super.sendExtinguish(time, target, water);
        throw new CommandException("Extinguish");
    }

    protected void updateSelfPosition(ChangeSet changeSet) {
        for (EntityID entity : changeSet.getChangedEntities()) {
            if (getID().equals(entity)) {
                Human human = (Human) world.getEntity(entity);
                for (Property p : changeSet.getChangedProperties(entity)) {
                    human.getProperty(p.getURN()).takeValue(p);
                }
                return;
            }
        }
    }

    public void restAtRefuge() throws CommandException {
        setAgentState(State.RESTING);
        if (world.getSelfHuman().getBuriedness() > 0) {
            throw new CommandException("I'm Buried");
        }
        if (world.getSelfPosition() instanceof Refuge) {
            sendRestAct(world.getTime());
        } else if (!world.getRefuges().isEmpty()) {
            if (world.getSelfHuman() instanceof PoliceForce) {
//                ((MrlPoliceForce) world.getSelf()).clearHere(PoliceActionStyle.CLEAR_NORMAL);
                ((MrlPoliceForce) world.getSelf()).getClearHelper().clearWay(pathPlanner.getNextPlan(), null);
            }
            moveToRefuge();
        }
    }

    public boolean move(Area target, int maxDistance, boolean force) throws CommandException {
        pathPlanner.move(target, maxDistance, force);
        return false;
    }

    protected boolean move(Collection<? extends Area> targets, int maxDistance, boolean force) throws CommandException {

//        pathPlanner.move(targets, maxDistance, force);
        throw new UnsupportedOperationException();
    }

    protected void moveToRefuge() throws CommandException {
        pathPlanner.moveToRefuge();
    }


    public void moveToHydrant() throws CommandException {
        pathPlanner.moveToHydrant();
    }

    public void moveToPoint(EntityID area, int destX, int destY) throws CommandException {
        pathPlanner.moveToPoint(area, destX, destY);
    }

    public Config getConfig() {
        return config;
    }

    public IPathPlanner getPathPlanner() {
        return pathPlanner;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public Partition getMyPartition() {
        return world.getPartitions().getMyPartition();
    }

    public List<EntityID> getVisitedBuildings() {
        return visitedBuildings;
    }

    public Collection<StandardEntity> getObjectsInRange(StandardEntity entity, int range) {
        //TAVAJOH: baraye gereftane blockade ha hargez az in estefade nashe.
        return model.getObjectsInRange(entity, range);
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public State getAgentState() {
        return agentState;
    }

    public void setAgentState(State state) {
        world.getAgentStateMap().put(world.getTime(), state);
        world.getHelper(HumanHelper.class).setAgentSate(me().getID(), state);
        agentState = state;
    }

    public String getDebugString() {
        return "Time:" + world.getTime() + " Me:" + me() + " ";
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.POLICE_FORCE);
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        System.out.print(this);

        Logger.debug("Communication model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(MRLConstants.SPEAK_COMMUNICATION_MODEL_KEY) ? "Using speak model" : "Using say model");

        if (this instanceof MrlPoliceForce) {
            world = new MrlPoliceForceWorld(this, model.getAllEntities(), config);
        } else if (this instanceof MrlFireBrigade) {
            world = new MrlFireBrigadeWorld(this, model.getAllEntities(), config);
        } else if (this instanceof MrlAmbulanceTeam) {
            world = new MrlAmbulanceTeamWorld(this, model.getAllEntities(), config);
        } else {
            world = new MrlWorld(this, model.getAllEntities(), config);
        }

        if (MRLConstants.LAUNCH_VIEWER) {
            PLATOON_AGENTS_FOR_VIEWER.put(this.getID(), this);
        }

        updateAgentStates(0);

//        world.setKernelTimeSteps(getConfig().getIntValue(KERNEL_TIMESTEPS));
        model = world;
        this.pathPlanner = new PathPlanner(world);

        if (HIGHWAY_STRATEGY) {
            pathPlanner.getGraph().setHighWayStrategy();
        }
//        world.getPathLength();


//        world.partitionMakingOperations(world.getSelfHuman());


        world.preRoutingPartitions();

        this.messageManager = new MessageManager(world,config);
        //messageManager.setMyOwnBW();
//        dataToFile = new LearnerIO("data/" + me().getID() + "data.txt", true);
//        dataToFile2 = new LearnerIO("data/civilianFound.txt", true);

        this.random = new Random(System.currentTimeMillis());
        visitedBuildings = new ArrayList<EntityID>();
//        visitedBuildingsHelp = new ArrayList<EntityID>();
        stuckChecker = new StuckChecker(world, 80, 6);

        senseSearchStrategy = new SenseSearchStrategy(this, world);
        civilianSearchStrategy = new CivilianSearchStrategy(this, world);
        checkBlockadesDecisionMaker = new CheckBlockadesDecisionMaker(world);
        checkBlockadesManager = new CheckBlockadesManager(world, this, checkBlockadesDecisionMaker, senseSearchStrategy);
        roads = new ArrayList<StandardEntity>(world.getRoads());
        averageTools = new AverageTools(world);
        if (MYSQL_LOG) {
            initiateMySQLLog();
        }
        if (MYSQL_DEBUG_MESSAGING) {
            List<FireBrigade> fireBrigades = world.getEntitiesOfType(FireBrigade.class, StandardEntityURN.FIRE_BRIGADE);
            if (!fireBrigades.isEmpty() && fireBrigades.get(0).getID().equals(getID())) {
                MessageTester.main(null);
            }
        }
    }

    private void initiateMySQLLog() {
//        civilianLogManager = new CivilianLogManager(world);
//        totalCivilianLogManager = new TotalCivilianLogManager(world);
    }

    @Override
    protected void processSense(KASense sense) {
        long start = System.currentTimeMillis();
        Collection<Command> heard = sense.getHearing();
        try {
            think(sense.getTime(), sense.getChangeSet(), heard);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long end = System.currentTimeMillis();
        if (end - start > world.getThinkTime()) {
//            Logger.warn("Time:" + sense.getTime() + " cycle needed:" + (end - start) + "ms");
            System.err.println("Time:" + sense.getTime() + " Agent:" + this + " cycle needed:" + (end - start) + "ms");
        }
    }

    protected List<MessageTypes> getMessagesToListen() {
        List<MessageTypes> types = new ArrayList<MessageTypes>();
        Collections.addAll(types, MessageTypes.values());
        return types;
    }

    protected int[] getChannelsToSubscribe() {
        List<Integer> channelList;
        int[] channels;
        int max;

        max = config.getIntValue(MAX_PLATOON_CHANNELS_KEY);
        channelList = new ArrayList<Integer>();
        for (MessageTypes type : getMessagesToListen()) {
            if (channelList.size() >= max) {
                break;
            }
            int channel;
            channel = messageManager.getChannel(type);
            if (!channelList.contains(channel)) {
                channelList.add(channel);
            }
        }
        channels = new int[channelList.size()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = channelList.get(i);
        }
        return channels;
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        world.setThinkStartTime_(System.currentTimeMillis());
        isWorking = false;

        world.setTime(time);

        if (getID().equals(CHECK_ID)) {
            System.out.print(""); //todo
        }

        updateAgentStates(time);
        updateSelfPosition(changed);


//        scanChannel(heard);
        sendSubscribe(getChannelsToSubscribe());
//        messageManager.processHearing(heard);
        messageManager.receive(time, heard);
        world.merge(changed);
//        markVisitedBuildings();

        TimestampThreadLogger threadLogger = TimestampThreadLogger.getCurrentThreadLogger();
        world.updateEveryCycle();
        pathPlanner.update();

        world.updatePreviousChangeSet();

        if (time < world.getIgnoreCommandTime()) {
            return;
        }
        world.getPlatoonAgent().stuckChecker.amITotallyStuck();
//        updateUnreachablePositions();
        messageManager.initializePlatoonMessages();
        messageManager.sendEmergencyMessages();
        if (shouldRestAtRefuge()) {
            try {
                restAtRefuge();
            } catch (CommandException e) {
                lastCommand = e.getMessage();
                Logger.info(getDebugString() + " - ACT: " + e.getMessage());
                setAgentState(State.RESTING);

                // age ye agent damage dasht va nemitoonest kari bokone hade aghal message bede.
//                messageManager.sendMessages(onRendezvous);
                messageManager.sendMessages();
//                messageManager.sendSayMessages();
                return;
            }
        }
//        civilianLogManager.execute();
        try {
            act();
//            randomWalk();
//            setAgentState(State.Inactive);
//            world.getHelper(HumanHelper.class).setAmIStuck(true);
        } catch (CommandException e) {

            lastCommand = e.getMessage();
            Logger.info(getDebugString() + " - ACT: " + e.getMessage());
            isWorking = true;
        } catch (TimeOutException e) {
            System.err.println(getDebugString() + "TimeOutException" + e.getMessage());
            isWorking = false;
            setAgentState(State.THINKING);
        } catch (Exception e) {
            e.printStackTrace();
            world.printData("I was crashed in this cycle, so going to walk random...");
            randomWalk();
            isWorking = false;
            setAgentState(State.CRASH);
        }

        if (world.getSelfHuman().getBuriedness() > 0) {
            isWorking = false;
            setAgentState(State.BURIED);
        }

        if (MYSQL_LOG) {
//            totalCivilianLogManager.execute();
        }
//        messageManager.sendMessages(onRendezvous);
        messageManager.sendMessages();
//        messageManager.sendSayMessages();
//        world.getThisCycleVisitedBuildings().clear();
    }

    private void updateAgentStates(int time) {

        Human human;
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        if (time == 0) {
            for (StandardEntity entity : world.getPlatoonAgents()) {
                human = (Human) entity;
                // if it is in building at the beginning of simulation, so it might be buried

                world.getAgentPositionMap().put(entity.getID(), human.getPosition());
                world.getAgentFirstPositionMap().put(entity.getID(), human.getPosition());

                if (human.getPosition(world) instanceof Building) {
                    world.getBuriedAgents().add(entity.getID());

//                    humanHelper.setLockedByBlockade(entity.getID(), true);

//                    System.out.println("time:"+world.getTime() + " me:"+world.getSelf().getID()+" think this is Buried:"+entity.getID());

                } else if (!(entity instanceof PoliceForce)) {// if position is instance of Road , so it might be blocked
                    humanHelper.setLockedByBlockade(entity.getID(), true);
//                    System.out.println("time:"+world.getTime() + " me:"+world.getSelf().getID()+" think this is BLOCKED:"+entity.getID());
                }

            }
        } else {
            for (StandardEntity entity : world.getPlatoonAgents()) {
                human = (Human) entity;
                if (humanHelper.isBlocked(entity.getID())) {
                    // if it is in building at the beginning of simulation, so it might be buried
                    if (!stuckChecker.isBlocked(entity)) {

                        world.getBuriedAgents().remove(entity.getID());
                        humanHelper.setLockedByBlockade(entity.getID(), false);
                        world.getAgentPositionMap().put(entity.getID(), human.getPosition());
//                        System.out.println("time:"+world.getTime() + " me:"+world.getSelf().getID()+" BAD Thinking, it was free:"+entity.getID());
                    }
                }
            }
        }


    }


    private void updateUnreachablePositions() {

        PathPlanner pp = (PathPlanner) pathPlanner;
        EntityID positionID = null;
        if (pp.escapeHere.getTryCount() > 2) {

            positionID = pp.getPreviousTarget();
            if (positionID != null) {
                int postponeTime = random.nextInt(6) + 15;

                unReachablePositionTime.put(positionID, postponeTime);
                if (!unReachablePositions.contains(world.getEntity(positionID))) {
                    unReachablePositions.add(world.getEntity(positionID));
                }
                if (MRLConstants.LAUNCH_VIEWER) {
//                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>>inPlatoon PostPoned >>>> " + positionID + " " + postponeTime);
                }
            }
        }

        ArrayList<StandardEntity> toRemove = new ArrayList<StandardEntity>();
        int postponeTime = 0;
        for (StandardEntity standardEntity : unReachablePositions) {

            postponeTime = unReachablePositionTime.get(standardEntity.getID());
            postponeTime--;
            if (postponeTime <= 0) {
                toRemove.add(standardEntity);
                unReachablePositionTime.remove(standardEntity.getID());
                if (MRLConstants.LAUNCH_VIEWER) {
//                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>>inPlatoon Removed From PostPoned >>>> " + standardEntity.getID() + " " + postponeTime);
                }
            } else {
                unReachablePositionTime.put(standardEntity.getID(), postponeTime);
                if (MRLConstants.LAUNCH_VIEWER) {
//                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>>inPlatoon PostPoned >>>> " + standardEntity.getID() + " " + postponeTime);
                }
            }

        }
        unReachablePositions.removeAll(toRemove);


    }

    public abstract void act() throws CommandException, TimeOutException;

    public abstract void processMessage(MessageEntity messageEntity);

    public boolean isThinkTimeOver(String s) throws TimeOutException {
        if (!MRLConstants.LAUNCH_VIEWER && (System.currentTimeMillis() - world.getThinkStartTime_()) > world.getThinkTimeThreshold()) {
            throw new TimeOutException("  Timeout(" + world.getThinkTimeThreshold() + ") on: " + s);
        }
        return false;
    }

    private boolean shouldRestAtRefuge() {
        Human self = world.getSelfHuman();
        return (self.getBuriedness() > 0) || (self.getBuriedness() == 0
                && ((self.getDamage() > 4 && self.getHP() < 7000)
                || self.getDamage() > 9
                || (self.getDamage() > 0 && self.getHP() < 5000)));
    }

    private void scanChannel(Collection<Command> commands) {
        if (world.getTime() < world.getIgnoreCommandTime()) {
            return;
        }
        //messageManager.scanChannels(commands);
    }


    public boolean isMovingRendezvous() {
        return movingRendezvous;
    }

    public void setMovingRendezvous(boolean movingRendezvous) {
        this.movingRendezvous = movingRendezvous;
    }

    public void setIAmOnRendezvousPlace(boolean onRendezvous) {
        this.onRendezvous = onRendezvous;
    }

    public Random getRandom() {
        return random;
    }

    public boolean isStuck() {
        return stuckChecker.isStuck();
    }

    public StuckState getStuckState() {
        return stuckChecker.getStuckState();
    }

    public AverageTools getAverageTools() {
        return averageTools;
    }

    public void setHardWalking(boolean hardWalking) {
        isHardWalking = hardWalking;
    }

    public boolean isHardWalking() {
        return isHardWalking;
    }

    StandardEntity loopTarget;

    public void loopBetweenTwoArea(StandardEntity area1, StandardEntity area2) throws CommandException {
        if (world.getSelfPosition().getID().equals(area2.getID()) || loopTarget == null) {
            loopTarget = area1;
        } else if (world.getSelfPosition().getID().equals(area1.getID())) {
            loopTarget = area2;
        }
        move((Area) loopTarget, 0, true);
    }

    Pair<Integer, Integer> loopTargetPoint;

    public void loopBetweenTwoPoint(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) throws CommandException {
        if (world.getSelfLocation().equals(p2) || loopTargetPoint == null) {
            loopTargetPoint = p1;
        } else if (world.getSelfLocation().equals(p1)) {
            loopTargetPoint = p2;
        }
        moveToPoint(world.getSelfPosition().getID(), loopTargetPoint.first(), loopTargetPoint.second());
    }

    public Set<StandardEntity> getUnReachablePositions() {
        return unReachablePositions;
    }

    public boolean randomWalk() {

        int randomRoadIndex = random.nextInt(roads.size() - 1);
        List<EntityID> plan = pathPlanner.planMove((Area) world.getSelfPosition(), (Area) roads.get(randomRoadIndex), IN_TARGET, true);
        sendMove(world.getTime(), plan);
        return false;
    }
}