package mrl.communication.channels;

import mrl.communication.property.Receivers;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.config.Config;
import rescuecore2.standard.components.StandardAgent;

import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 6, 2011
 * Time: 11:31:13 AM
 */
public class Channels extends ArrayList<Channel> {

    private static final Log logger = LogFactory.getLog(Channels.class);

    public static final String MAX_PLATOON_CHANNEL_KEY = "max.platoon";
    public static final String MAX_CENTRE_CHANNEL_KEY = "max.centre";
    public static final String COMMUNICATIONS_CHANNELS_KEY = "comms.channels.";
    public static final String RANGE_KEY = ".range";
    public static final String MESSAGES_MAX_KEY = ".messages.max";
    public static final String MESSAGES_SIZE_KEY = ".messages.size";
    public static final String COUNT_KEY = "count";
    public static final String CHANNEL_BANDWIDTH = ".bandwidth";
    public static final String TYPE_KEY = ".type";
    public static final String RADIO_CHANNEL_KEY = "radio";
//    public static final String NOISE_KEY = ".noise";
//    public static final String USE_KEY = ".use";
//    public static final String PROBABILITY_KEY = ".p";
//    public static final String INPUT_KEY = ".input";
//    public static final String OUTPUT_KEY = ".output";
//    public static final String DROPOUT_KEY = ".dropout";
//    public static final String FAILURE_KEY = ".failure";

    public static int VOICE_CHANNEL = 0;
    private int voiceChannelBandwidth;
    private int voiceChannelRange;
    private int voiceChannelMessagesMax;
    private int platoonMaxChannel;
    private int centreMaxChannel;
    private boolean scanFinished;
    private boolean scanBreak;
//      private List<Pair<Receivers, List<Channel>>> agentChannels;
    private Map<Receivers, List<Channel>> agentChannels;

    public Channels(final MrlWorld world) {

        createChannels(world.getConfig(), world);
        agentChannels = new HashMap<Receivers, List<Channel>>(){
            @Override
            public List<Channel> put(Receivers key, List<Channel> value) {
                logger.fatal(world.getSelf().getID() + " Adding Pair : (" + key + ", " + value + ")");
                return super.put(key, value);    //To change body of overridden methods use File | Settings | File Templates.
            }
        };
        scanFinished = false;
    }

    private void createChannels(Config config, MrlWorld world) {
        int channelNumber;
        channelNumber = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + COUNT_KEY);
        platoonMaxChannel = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + MAX_PLATOON_CHANNEL_KEY);
        centreMaxChannel = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + MAX_CENTRE_CHANNEL_KEY);

        for (int ch = 0; ch < channelNumber; ch++) {
            String type = config.getValue(COMMUNICATIONS_CHANNELS_KEY + ch + TYPE_KEY);
            int bandWidth;

            if (type.equalsIgnoreCase(RADIO_CHANNEL_KEY)) {
                bandWidth = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + ch + CHANNEL_BANDWIDTH);
                world.setCommunicationLess(false);
            } else {
                bandWidth = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + ch + MESSAGES_SIZE_KEY);
                voiceChannelBandwidth = bandWidth;
                VOICE_CHANNEL = ch;
            }

            Channel channel = new Channel(ch, bandWidth, type);
            this.add(channel);
        }

        Collections.sort(this);

        voiceChannelRange = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + VOICE_CHANNEL + RANGE_KEY);
        voiceChannelMessagesMax = config.getIntValue(COMMUNICATIONS_CHANNELS_KEY + VOICE_CHANNEL + MESSAGES_MAX_KEY);

    }

    public void subscribe(MrlWorld world) {
        new Subscriber(world, this);
        if (world.getPlatoonAgent() != null) {
//            world.getPlatoonAgent().getMessageManager().setMyOwnBW();
        } else {
//            world.getCenterAgent().getMessageManager().setMyOwnBW();
        }
    }

    public void setScanFinished() {
        this.scanFinished = true;
    }

    public void setScanBreak() {
        this.scanBreak = true;
    }

    public void setATChannels(List<Channel> channels) {
//        agentChannels.add(new Pair<Receivers, List<Channel>>(Receivers.AmbulanceTeam, channels));
        agentChannels.put(Receivers.AmbulanceTeam, channels);
    }

    public void setFBChannels(List<Channel> channels) {
//        agentChannels.add(new Pair<Receivers, List<Channel>>(Receivers.FireBrigade, channels));
        agentChannels.put(Receivers.FireBrigade, channels);
    }

    public void setPFChannels(List<Channel> channels) {
//        agentChannels.add(new Pair<Receivers, List<Channel>>(Receivers.PoliceForce, channels));
        agentChannels.put(Receivers.PoliceForce, channels);
    }

    public List<Channel> getATChannels() {
//        List<Channel> channels=new ArrayList<Channel>();
//        for (Pair<Receivers, List<Channel>> pair:agentChannels){
//            if(pair.first().equals(Receivers.AmbulanceTeam)){
//                 channels.addAll(pair.second());
//            }
//        }

        return agentChannels.get(Receivers.AmbulanceTeam);
//        return channels;
    }

    public List<Channel> getFBChannels() {
//        List<Channel> channels=new ArrayList<Channel>();
//        for (Pair<Receivers, List<Channel>> pair:agentChannels){
//            if(pair.first().equals(Receivers.FireBrigade)){
//                channels.addAll(pair.second());
//            }
//        }

        return agentChannels.get(Receivers.FireBrigade);
//        return channels;
    }

    public List<Channel> getPFChannels() {
//        List<Channel> channels=new ArrayList<Channel>();
//        for (Pair<Receivers, List<Channel>> pair:agentChannels){
//            if(pair.first().equals(Receivers.PoliceForce)){
//                channels.addAll(pair.second());
//            }
//        }

        return agentChannels.get(Receivers.PoliceForce);
//        return channels;
    }

    public int getVoiceChannelBandwidth() {
        return voiceChannelBandwidth;
    }

    public int getVoiceChannelRange() {
        return voiceChannelRange;
    }

    public int getVoiceChannelMessagesMax() {
        return voiceChannelMessagesMax;
    }

    public int getPlatoonMaxChannel() {
        return platoonMaxChannel;
    }

    public int getCentreMaxChannel() {
        return centreMaxChannel;
    }

    public boolean isScanFinished() {
        return scanFinished;
    }

    public boolean isScanBreak() {
        return scanBreak;
    }

    public Channel getChannel(int id) {
        for (Channel ch : this) {
            if (ch.getId() == id) {
                return ch;
            }
        }
        return null;
    }

    public void sendSubscribeToHear(StandardAgent agent, List<Channel> channels) {
        int[] channelIds = new int[channels.size()];
        int i = 0;
        for (Channel channel : channels) {
            channelIds[i++] = channel.getId();
        }

        if (agent instanceof MrlPlatoonAgent) {
            ((MrlPlatoonAgent) agent).sendSubscribe(channelIds);
        } else {
            ((MrlCentre) agent).sendSubscribe(channelIds);
        }
    }

    public void printChannelsInfo() {
        System.out.println("----------------------------- CHANNELS INFO -----------------------------");
        for (Channel channel : this) {
            System.out.println(channel.toString());
        }
        System.out.println("-------------------------------------------------------------------------");
    }

    public int radioChannelsSize() {
        int count = 0;
        for (Channel channel : this) {
            if (channel.getType().equalsIgnoreCase("radio")) {
                count++;
            }
        }
        return count;
    }
}
