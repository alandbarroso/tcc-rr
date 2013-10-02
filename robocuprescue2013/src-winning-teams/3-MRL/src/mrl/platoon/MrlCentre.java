package mrl.platoon;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.message.MessageManager;
import mrl.communication.Packet;
import mrl.mosCommunication.message.type.MessageTypes;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.IPathPlanner;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * A sample centre agent.
 */
public abstract class MrlCentre extends StandardAgent<Building> implements MRLConstants {

    protected MrlWorld world;
    protected IPathPlanner pathPlanner;
    protected MessageManager messageManager;
    protected int thinkTime;
    protected Date thinkStartTime_;
    protected int ignoreCommandTime;

    @Override
    public String toString() {
        return "MrlCentre";
    }

    public void sendSubscribe(int... channel) {
        sendSubscribe(world.getTime(), channel);
    }

    public void sendMessage(int channel, byte[] message) {
        sendSpeak(world.getTime(), channel, message);
    }

    public int getIgnoreCommandTime() {
        return ignoreCommandTime;
    }

    protected void postConnect() {
        super.postConnect();
        System.out.print(this);
        this.ignoreCommandTime = getConfig().getIntValue(MRLConstants.IGNORE_AGENT_COMMANDS_KEY);
        this.thinkTime = config.getIntValue(THINK_TIME_KEY);
        world = new MrlWorld(this, model.getAllEntities(), config);

        world.retrieveConfigParameters(config);

        this.pathPlanner = new PathPlanner(world);
//        world.preRoutingPartitions();

        int seed = getID().getValue() % 100;
        seed *= seed;
        this.random = new Random(System.currentTimeMillis() + seed);

        this.messageManager = new MessageManager(world,config);
        System.out.println("   success");
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        long start = System.currentTimeMillis();
        thinkStartTime_ = new Date();
        world.setTime(time);


        if (time < getIgnoreCommandTime()) {
            return;
        }

//        scanChannel(heard);
        sendSubscribe(getChannelsToSubscribe());
//        messageManager.receive(time, heard);
        messageManager.receive(time, heard);

        world.merge(changed);
        world.updateEveryCycle();

        messageManager.initializeCenterMessages();
        messageManager.sendEmergencyMessages();

        try {
            act();

        } catch (CommandException e) {
            Logger.info("ACT:" + e.getMessage());
        } catch (TimeOutException e) {
            Logger.error("Time Up:", e);
        }

//        It should send messages after all works, if had time
        messageManager.sendMessages();

//        setAndSendDebugData();
        sendRest(time);
        long end = System.currentTimeMillis();
        if (end - start > thinkTime) {
            Logger.warn("Time:" + time + " cycle needed:" + (end - start) + "ms");
            System.err.println("Time:" + time + " Agent:" + this + " cycle needed:" + (end - start) + "ms");
        }
    }

    public abstract void act() throws CommandException, TimeOutException;

    public abstract void processMessage(Packet packet, EntityID sender);

    public String getDebugString() {
        return "Time:" + world.getTime() + " Me:" + me() + " ";
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        if (world != null && world.getSelfBuilding() != null) {
            if (world.getSelfBuilding() instanceof FireStation) {
                return EnumSet.of(StandardEntityURN.FIRE_STATION);
            } else if (world.getSelfBuilding() instanceof PoliceOffice) {
                return EnumSet.of(StandardEntityURN.POLICE_OFFICE);
            } else {
                return EnumSet.of(StandardEntityURN.AMBULANCE_CENTRE);
            }
        } else {

            return EnumSet.of(StandardEntityURN.FIRE_STATION,
                    StandardEntityURN.AMBULANCE_CENTRE,
                    StandardEntityURN.POLICE_OFFICE);
        }
    }

    public MrlWorld getWorld() {
        return world;
    }

    public Config getConfig() {
        return config;
    }

    private void scanChannel(Collection<Command> commands) {
//        messageManager.scanChannels(commands);
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public void createMessages() {

    }

    public IPathPlanner getPathPlanner() {
        return pathPlanner;
    }

    public Random getRandom() {
        return random;
    }
    public List<MessageTypes> getMessagesToListen() {
        List<MessageTypes> types = new ArrayList<MessageTypes>();
        Collections.addAll(types, MessageTypes.values());
        return types;
    }

    public int[] getChannelsToSubscribe() {
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

    public void processMessage(MessageEntity messageEntity) {
        //throw new NotImplementedException();
    }
}