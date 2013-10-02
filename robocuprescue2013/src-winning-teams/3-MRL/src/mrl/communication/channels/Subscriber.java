package mrl.communication.channels;

import mrl.ambulance.MrlAmbulanceCentre;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.common.Debbuge;
import mrl.common.MRLConstants;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireStation;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceOffice;
import mrl.world.MrlWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 31, 2010
 * Time: 5:05:41 PM
 */
public class Subscriber {

    public Subscriber(MrlWorld world, Channels channels) {
//        subscribeIO2011(world, channels);
//        subscribe(world, channels);
        simpleSubscription(world,channels);
    }

//    private List<EntityID> getHealthyCentres(MrlWorld world) {
//        List<EntityID> healthy = new ArrayList<EntityID>();
//
//        for (StandardEntity entity : world.getCentres()) {
//            Building centre = (Building) entity;
//
//            if (centre.isFierynessDefined() && centre.getFieryness() > 0) {
//            } else {
//                healthy.add(entity.getID());
//            }
//        }
//        return healthy;
//    }
//

    private void simpleSubscription(MrlWorld world, Channels channels) {

        if (channels == null || channels.isEmpty()) {
            return;
        } else {

            int platoonMaxChannel;
            List<Channel> voiceChannels = new ArrayList<Channel>();
            int channelsSize;
            int agentTypeNumber = 0;
            int ATSize;
            int PFSize;
            int FBSize;
            List<Channel> ATChannels;
            List<Channel> PFChannels;
            List<Channel> FBChannels;

            platoonMaxChannel = channels.getPlatoonMaxChannel();
            channelsSize = channels.size();
            ATSize = world.getAmbulanceTeams().size();
            PFSize = world.getPoliceForces().size();
            FBSize = world.getFireBrigades().size();
            ATChannels = new ArrayList<Channel>();
            PFChannels = new ArrayList<Channel>();
            FBChannels = new ArrayList<Channel>();

            if (ATSize > 0) {
                agentTypeNumber++;
            }
            if (PFSize > 0) {
                agentTypeNumber++;
            }
            if (FBSize > 0) {
                agentTypeNumber++;
            }

            // remove voice channel from channels.
            List<Channel> toRemove = new ArrayList<Channel>();
            for (Channel channel : channels) {
                if (channel.getType().equalsIgnoreCase("voice")) {
                    voiceChannels.add(channel);
                    toRemove.add(channel);
                }
            }
            channels.removeAll(toRemove);
            channelsSize--;

            Collections.sort(channels);
            int temp = platoonMaxChannel;
            for (Channel channel : channels) {
                if(temp > 0){
                    temp--;
                } else {
                    break;
                }

                if (ATSize > 0) {
                    ATChannels.add(channel);
                }
                if (FBSize > 0) {
                    FBChannels.add(channel);
                }
                if (PFSize > 0) {
                    PFChannels.add(channel);
                }
            }

            int agentInThisChannel;
            for (Channel channel : channels) {
                agentInThisChannel = 0;
                if (ATChannels.contains(channel)) {
                    agentInThisChannel += ATSize;
                }
                if (FBChannels.contains(channel)) {
                    agentInThisChannel += FBSize;
                }
                if (PFChannels.contains(channel)) {
                    agentInThisChannel += PFSize;
                }
                if (!channels.isScanBreak()) {
                    channel.setAverageBandwidth(agentInThisChannel);
                } else {
                    channel.setBreakConditionAverageChannel(agentInThisChannel);
                }
            }

            channels.setATChannels(ATChannels);
            channels.setPFChannels(PFChannels);
            channels.setFBChannels(FBChannels);

            if ((world.getSelf() instanceof MrlAmbulanceTeam) || (world.getSelf() instanceof MrlAmbulanceCentre)) {
                channels.sendSubscribeToHear(world.getSelf(), channels.getATChannels());
            } else if ((world.getSelf() instanceof MrlFireBrigade) || (world.getSelf() instanceof MrlFireStation)) {
                channels.sendSubscribeToHear(world.getSelf(), channels.getFBChannels());
            } else if ((world.getSelf() instanceof MrlPoliceForce) || (world.getSelf() instanceof MrlPoliceOffice)) {
                channels.sendSubscribeToHear(world.getSelf(), channels.getPFChannels());
            }

            if (MRLConstants.DEBUG_MESSAGING) {
                String text;
                System.out.println("SELF:" + world.getSelf());
                text = "SELF:" + world.getSelf();
                channels.printChannelsInfo();
                System.out.println("");
                System.out.println(" -----------------------------------------");
                System.out.print("AmbulanceTeam channels: ");
                text += "AmbulanceTeam channels: ";
                for (Channel c : channels.getATChannels()) {
                    System.out.print("  " + c.getId());
                    text += "  " + c.getId();
                }
                System.out.println("");
                System.out.print("PoliceForce channels: ");
                text += "PoliceForce channels: ";
                for (Channel c : channels.getPFChannels()) {
                    System.out.print("  " + c.getId());
                    text += "  " + c.getId();
                }
                System.out.println("");
                System.out.print("FireBrigade channels: ");
                text += "FireBrigade channels: ";
                for (Channel c : channels.getFBChannels()) {
                    System.out.print("  " + c.getId());
                    text += "  " + c.getId();
                }
                System.out.println("");
                System.out.println(" -----------------------------------------");
                Debbuge.fileAppending(text);
            }

            // add voice channel again in to channels.
            channels.addAll(voiceChannels);
        }

    }

    private void subscribeIO2011(MrlWorld world, Channels channels) {

        int platoonMaxChannel;
        Channel voiceChannel = null;
        int channelsSize;
        int agentTypeNumber = 0;
        int ATSize;
        int PFSize;
        int FBSize;
        List<Channel> ATChannels;
        List<Channel> PFChannels;
        List<Channel> FBChannels;

        platoonMaxChannel = channels.getPlatoonMaxChannel();
        channelsSize = channels.size();
        ATSize = world.getAmbulanceTeams().size();
        PFSize = world.getPoliceForces().size();
        FBSize = world.getFireBrigades().size();
        ATChannels = new ArrayList<Channel>();
        PFChannels = new ArrayList<Channel>();
        FBChannels = new ArrayList<Channel>();

        if (ATSize > 0) {
            agentTypeNumber++;
        }
        if (PFSize > 0) {
            agentTypeNumber++;
        }
        if (FBSize > 0) {
            agentTypeNumber++;
        }

        // remove voice channel from channels.
        for (Channel channel : channels) {
            if (channel.getType().equalsIgnoreCase("voice")) {
                voiceChannel = channel;
                channels.remove(channel);
                break;
            }
        }
        channelsSize--;

        Collections.sort(channels);

        switch (channelsSize) {

            case 0: {
                // Communication Less
                return;
            }
            case 1: {
                if (ATSize > 0) {
                    ATChannels.add(channels.get(0));
                }
                if (FBSize > 0) {
                    FBChannels.add(channels.get(0));
                }
                if (PFSize > 0) {
                    PFChannels.add(channels.get(0));
                }
                break;
            }
            case 2: {
                if (platoonMaxChannel > 1) {
                    List<Channel> ch = new ArrayList<Channel>();
                    ch.add(channels.get(0));
                    ch.add(channels.get(1));
                    if (ATSize > 0) {
                        ATChannels = ch;
                    }
                    if (FBSize > 0) {
                        FBChannels = ch;
                    }
                    if (PFSize > 0) {
                        PFChannels = ch;
                    }
                } else {
                    if (agentTypeNumber < 3) {
                        if (ATSize == 0) {
                            if (FBSize >= PFSize) {
                                FBChannels.add(channels.get(0));
                                PFChannels.add(channels.get(1));
                            } else {
                                PFChannels.add(channels.get(0));
                                FBChannels.add(channels.get(1));
                            }
                        } else if (FBSize == 0) {
                            if (ATSize >= PFSize) {
                                ATChannels.add(channels.get(0));
                                PFChannels.add(channels.get(1));
                            } else {
                                PFChannels.add(channels.get(0));
                                ATChannels.add(channels.get(1));
                            }
                        } else if (PFSize == 0) {
                            if (FBSize > ATSize) {
                                FBChannels.add(channels.get(0));
                                ATChannels.add(channels.get(1));
                            } else {
                                ATChannels.add(channels.get(0));
                                FBChannels.add(channels.get(1));
                            }
                        }

                    } else {
                        if (ATSize > FBSize + PFSize) {
                            ATChannels.add(channels.get(0));
                            FBChannels.add(channels.get(1));
                            PFChannels.add(channels.get(1));
                        } else if (PFSize > FBSize + ATSize) {
                            PFChannels.add(channels.get(0));
                            FBChannels.add(channels.get(1));
                            ATChannels.add(channels.get(1));
                        } else if (FBSize > ATSize + PFSize) {
                            FBChannels.add(channels.get(0));
                            ATChannels.add(channels.get(1));
                            PFChannels.add(channels.get(1));
                        } else if ((ATSize >= (PFSize * 0.9f)) && (ATSize >= (FBSize * 0.9f))) {
                            ATChannels.add(channels.get(1));
                            FBChannels.add(channels.get(0));
                            PFChannels.add(channels.get(0));

                        } else if ((FBSize >= ATSize) && (FBSize >= PFSize)) {
                            FBChannels.add(channels.get(1));
                            ATChannels.add(channels.get(0));
                            PFChannels.add(channels.get(0));

                        } else {
                            PFChannels.add(channels.get(1));
                            ATChannels.add(channels.get(0));
                            FBChannels.add(channels.get(0));
                        }
                    }
                }
                break;
            }
            default: {
                if ((ATSize >= (PFSize * 0.9f)) && (ATSize >= (FBSize * 0.9f))) {
                    if (PFSize >= FBSize) {
                        ATChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, ATSize, 0, channels);
                        PFChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, PFSize, 1, channels);
                        FBChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, FBSize, 2, channels);
                    } else {
                        ATChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, ATSize, 0, channels);
                        FBChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, FBSize, 1, channels);
                        PFChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, PFSize, 2, channels);
                    }
                } else {
                    if ((FBSize >= ATSize) && (FBSize >= PFSize)) {
                        if (ATSize >= PFSize) {
                            FBChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, FBSize, 0, channels);
                            ATChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, ATSize, 1, channels);
                            PFChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, PFSize, 2, channels);
                        } else {
                            FBChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, FBSize, 0, channels);
                            PFChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, PFSize, 1, channels);
                            ATChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, ATSize, 2, channels);
                        }
                    } else {
                        if (ATSize >= FBSize) {
                            PFChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, PFSize, 0, channels);
                            ATChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, ATSize, 1, channels);
                            FBChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, FBSize, 2, channels);
                        } else {
                            PFChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, PFSize, 0, channels);
                            FBChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, FBSize, 1, channels);
                            ATChannels = getThisAgentChannels(agentTypeNumber, platoonMaxChannel, ATSize, 2, channels);
                        }
                    }
                }
            }
        }

        int agentInThisChannel;
        for (Channel channel : channels) {
            agentInThisChannel = 0;
            if (ATChannels.contains(channel)) {
                agentInThisChannel += ATSize;
            }
            if (FBChannels.contains(channel)) {
                agentInThisChannel += FBSize;
            }
            if (PFChannels.contains(channel)) {
                agentInThisChannel += PFSize;
            }
            if (!channels.isScanBreak()) {
                channel.setAverageBandwidth(agentInThisChannel);
            } else {
                channel.setBreakConditionAverageChannel(agentInThisChannel);
            }
        }

        channels.setATChannels(ATChannels);
        channels.setPFChannels(PFChannels);
        channels.setFBChannels(FBChannels);

        if ((world.getSelf() instanceof MrlAmbulanceTeam) || (world.getSelf() instanceof MrlAmbulanceCentre)) {
            channels.sendSubscribeToHear(world.getSelf(), channels.getATChannels());
        } else if ((world.getSelf() instanceof MrlFireBrigade) || (world.getSelf() instanceof MrlFireStation)) {
            channels.sendSubscribeToHear(world.getSelf(), channels.getFBChannels());
        } else if ((world.getSelf() instanceof MrlPoliceForce) || (world.getSelf() instanceof MrlPoliceOffice)) {
            channels.sendSubscribeToHear(world.getSelf(), channels.getPFChannels());
        }

        if (MRLConstants.DEBUG_MESSAGING) {
            String text;
            System.out.println("SELF:" + world.getSelf());
            text = "SELF:" + world.getSelf();
            channels.printChannelsInfo();
            System.out.println("");
            System.out.println(" -----------------------------------------");
            System.out.print("AmbulanceTeam channels: ");
            text += "AmbulanceTeam channels: ";
            for (Channel c : channels.getATChannels()) {
                System.out.print("  " + c.getId());
                text += "  " + c.getId();
            }
            System.out.println("");
            System.out.print("PoliceForce channels: ");
            text += "PoliceForce channels: ";
            for (Channel c : channels.getPFChannels()) {
                System.out.print("  " + c.getId());
                text += "  " + c.getId();
            }
            System.out.println("");
            System.out.print("FireBrigade channels: ");
            text += "FireBrigade channels: ";
            for (Channel c : channels.getFBChannels()) {
                System.out.print("  " + c.getId());
                text += "  " + c.getId();
            }
            System.out.println("");
            System.out.println(" -----------------------------------------");
//            Debbuge.fileAppending(text);
        }

        // add voice channel again in to channels.
        channels.add(voiceChannel);
    }

    private List<Channel> getThisAgentChannels(int numberOfAgentType, int platoonMaxChannel, int agentSize, int priority, List<Channel> channels) {
        if (agentSize < 1) {
            return new ArrayList<Channel>();
        }
        List<Channel> agentChannels = new ArrayList<Channel>();
        int channelsNumber = channels.size();
        int index = priority;

        while (platoonMaxChannel != 0) {

            if (index < channelsNumber) {
                agentChannels.add(channels.get(index));
                index += numberOfAgentType;
                platoonMaxChannel--;
            } else {
                break;
            }
        }

        return agentChannels;
    }

/*
    private void subscribe(MrlWorld world, Channels channels) {
        StandardAgent self;
        int platoonMaxChannel;
        int centreMaxChannel;
        int centreNumber;
        int channelNumber;
        Channel voiceChannel = null;
        List<EntityID> healthyCentres = getHealthyCentres(world);

        broadcastChannels = new ArrayList<Channel>();
        privateMessagesChannels = new ArrayList<Channel>();
        centreChannels = new ArrayList<Channel>();
        self = world.getSelf();
        platoonMaxChannel = channels.getPlatoonMaxChannel();
        centreMaxChannel = channels.getCentreMaxChannel();
        centreNumber = healthyCentres.size();
        channelNumber = channels.size();

        if (centreNumber == 0) {
            centreMaxChannel = 0;
        }

        // remove voice channel from channels.
        for (Channel channel : channels) {
            if (channel.getType().equalsIgnoreCase("voice")) {
                voiceChannel = channel;
                channels.remove(channel);
                break;
            }
        }

        Collections.sort(channels);

        if (self instanceof MrlAmbulanceTeam) {
            System.out.println("");
            System.out.println("----------------------------------------------------------------------------------");
            System.out.println("platoonMaxChannel: " + platoonMaxChannel);
            System.out.println("centreMaxChannel: " + centreMaxChannel);
            System.out.println("centreNumber: " + centreNumber);
            System.out.println("channelNumber: " + channelNumber);
            System.out.println("channels:");
            for (Channel channel : channels) {
                System.out.println(channel.toString());
            }
        }

        if (channelNumber > 2) {
            if (channels.get(0).getBandwidth() >= (channels.get(1).getBandwidth() * 3)) {
                if (self instanceof MrlCentre) {
                    centreChannels.add(channels.get(0));
                } else {
                    broadcastChannels.add(channels.get(0));
                    privateMessagesChannels.add(channels.get(0));
                }
                return;
            } else if (channelNumber > 3 && channels.get(1).getBandwidth() >= (channels.get(2).getBandwidth() * 2)) {
                if (self instanceof MrlCentre) {
                    centreChannels.add(channels.get(0));
                    centreChannels.add(channels.get(1));
                } else {
                    broadcastChannels.add(channels.get(0));
                    privateMessagesChannels.add(channels.get(1));
                }
                return;
            }
        }

        channelNumber--; // remove channel 0 from list.
        switch (channelNumber) {

            case 0: {
                // Communication Less
                return;
            }
            case 1: {
                if (self instanceof MrlCentre) {
                    centreChannels.add(channels.get(0));
                } else {
                    broadcastChannels.add(channels.get(0));
                    privateMessagesChannels.add(channels.get(0));
                }
                break;
            }
            case 2: {
                subscribeForTwoChannel(self, platoonMaxChannel, centreMaxChannel, channels);
                break;
            }
            case 3: {
                subscribeForThreeChannel(self, platoonMaxChannel, centreMaxChannel, centreNumber, channels, healthyCentres);
                break;
            }
            default: {
                subscribeForAboveFourChannel(self, platoonMaxChannel, centreMaxChannel, centreNumber, channels, healthyCentres);
            }
        }

        // add voice channel again in to channels.
        channels.add(voiceChannel);

        System.out.println("");
        System.out.println(" -----------------------------------------");
        System.out.print("broadcast channels: ");
        for (Channel c : getBroadcastChannels()) {
            System.out.print("  " + c.getId());
        }
        System.out.println("");
        System.out.print("private channels: ");
        for (Channel c : getPrivateMessagesChannels()) {
            System.out.print("  " + c.getId());
        }
        System.out.println("");
        System.out.print("centre channels: ");
        for (Channel c : getCentreChannels()) {
            System.out.print("  " + c.getId());
        }
        System.out.println("");
    }

    private void subscribeForTwoChannel(StandardAgent self, int platoonMaxChannel, int centreMaxChannel, Channels channels) {

        if (platoonMaxChannel >= 2 && !(self instanceof MrlCentre)) {
            broadcastChannels.add(channels.get(0));
            privateMessagesChannels.add(channels.get(1));

        } else if (centreMaxChannel < 2) {
            if (self instanceof MrlCentre) {
                centreChannels.add(channels.get(0));
            } else {
                broadcastChannels.add(channels.get(0));
                privateMessagesChannels.add(channels.get(0));
            }

        } else { // platoonMaxChannel == 1 /centreMaxChannel >= 2

            if (self instanceof MrlCentre) {
                centreChannels.add(channels.get(0));
                centreChannels.add(channels.get(1));

            } else if (self instanceof MrlAmbulanceTeam) {
                broadcastChannels.add(channels.get(1));
                privateMessagesChannels.add(channels.get(1));

            } else {
                broadcastChannels.add(channels.get(0));
                privateMessagesChannels.add(channels.get(0));
            }
        }
    }

    private void subscribeForThreeChannel(StandardAgent self, int platoonMaxChannel, int centreMaxChannel, int centreNumber, Channels channels, List<EntityID> healthyCentres) {

        if (platoonMaxChannel >= 2 && !(self instanceof MrlCentre)) {

            broadcastChannels.add(channels.get(0));
            if (self instanceof MrlAmbulanceTeam) {
                privateMessagesChannels.add(channels.get(2));
            } else {
                privateMessagesChannels.add(channels.get(1));
            }

        } else if (centreMaxChannel < 3) {

            if (centreMaxChannel == 2) {

                if (centreNumber >= 2) {

                    if (self instanceof MrlCentre) {
                        // centreNumber >= 2 && centreMaxChannel == 2 && platoonMaxChannel < 2

                        Collections.sort(healthyCentres, ConstantComparators.EntityID_COMPARATOR);

                        if (healthyCentres.indexOf(self.getID()) % 2 == 0) {

                            centreChannels.add(channels.get(0));
                            centreChannels.add(channels.get(1));
                        } else {

                            centreChannels.add(channels.get(0));
                            centreChannels.add(channels.get(2));
                        }
                    } else if (self instanceof MrlAmbulanceTeam) {
                        broadcastChannels.add(channels.get(1));
                        privateMessagesChannels.add(channels.get(1));

                    } else if (self instanceof MrlFireBrigade) {
                        broadcastChannels.add(channels.get(2));
                        privateMessagesChannels.add(channels.get(2));
                    } else {
                        broadcastChannels.add(channels.get(0));
                        privateMessagesChannels.add(channels.get(0));
                    }
                } else {
                    // centreNumber < 2 && centreMaxChannel == 2 && platoonMaxChannel < 2
                    if (self instanceof MrlCentre) {
                        centreChannels.add(channels.get(0));
                        centreChannels.add(channels.get(1));

                    } else if (self instanceof MrlAmbulanceTeam) {
                        broadcastChannels.add(channels.get(1));
                        privateMessagesChannels.add(channels.get(1));
                    } else {
                        broadcastChannels.add(channels.get(0));
                        privateMessagesChannels.add(channels.get(0));
                    }
                }
            } else {
                // centreMaxChannel < 2 && platoonMaxChannel < 2
                if (self instanceof MrlCentre) {
                    centreChannels.add(channels.get(0));
                } else {
                    broadcastChannels.add(channels.get(0));
                    privateMessagesChannels.add(channels.get(0));
                }
            }

        } else {
            // platoonMaxChannel == 1 /centreMaxChannel >= 3
            if (self instanceof MrlCentre) {
                centreChannels.add(channels.get(0));
                centreChannels.add(channels.get(1));
                centreChannels.add(channels.get(2));

            } else if (self instanceof MrlAmbulanceTeam) {
                broadcastChannels.add(channels.get(0));
                privateMessagesChannels.add(channels.get(0));

            } else if (self instanceof MrlFireBrigade) {
                broadcastChannels.add(channels.get(1));
                privateMessagesChannels.add(channels.get(1));
            } else {
                broadcastChannels.add(channels.get(2));
                privateMessagesChannels.add(channels.get(2));
            }
        }
    }

    private void subscribeForAboveFourChannel(StandardAgent self, int platoonMaxChannel, int centreMaxChannel, int centreNumber, Channels channels, List<EntityID> healthyCentres) {

        if (platoonMaxChannel >= 2 && !(self instanceof MrlCentre)) {

            for (Channel channel : channels) {
                // full communication
                if (channels.indexOf(channel) % 4 == 0) {
                    broadcastChannels.add(channel);

                } else if ((channels.indexOf(channel) % 4 == 1) && (self instanceof MrlAmbulanceTeam)) {
                    privateMessagesChannels.add(channel);

                } else if ((channels.indexOf(channel) % 4 == 2) && (self instanceof MrlFireBrigade)) {
                    privateMessagesChannels.add(channel);

                } else if (self instanceof MrlPoliceForce) {
                    privateMessagesChannels.add(channel);
                    if (platoonMaxChannel == privateMessagesChannels.size()) {
                        break;
                    }
                }
            }

        } else if (centreMaxChannel < 3) {

            if (centreMaxChannel == 2) {

                if (centreNumber >= 2) {

                    if (self instanceof MrlCentre) {
                        // centreNumber >= 2 && centreMaxChannel == 2 && platoonMaxChannel < 2

                        Collections.sort(healthyCentres, ConstantComparators.EntityID_COMPARATOR);

                        if (healthyCentres.indexOf(self.getID()) % 2 == 0) {

                            centreChannels.add(channels.get(0));
                            centreChannels.add(channels.get(1));
                        } else {

                            centreChannels.add(channels.get(0));
                            centreChannels.add(channels.get(2));
                        }
                    } else if (self instanceof MrlAmbulanceTeam) {
                        broadcastChannels.add(channels.get(1));
                        privateMessagesChannels.add(channels.get(1));

                    } else if (self instanceof MrlFireBrigade) {
                        broadcastChannels.add(channels.get(2));
                        privateMessagesChannels.add(channels.get(2));
                    } else {
                        broadcastChannels.add(channels.get(0));
                        privateMessagesChannels.add(channels.get(0));
                    }
                } else {
                    // centreNumber < 2 && centreMaxChannel == 2 && platoonMaxChannel < 2
                    if (self instanceof MrlCentre) {
                        centreChannels.add(channels.get(0));
                        centreChannels.add(channels.get(1));

                    } else if (self instanceof MrlAmbulanceTeam) {
                        broadcastChannels.add(channels.get(1));
                        privateMessagesChannels.add(channels.get(1));
                    } else {
                        broadcastChannels.add(channels.get(0));
                        privateMessagesChannels.add(channels.get(0));
                    }
                }
            } else {
                // centreMaxChannel < 2 && platoonMaxChannel < 2
                if (self instanceof MrlCentre) {
                    centreChannels.add(channels.get(0));
                } else {
                    broadcastChannels.add(channels.get(0));
                    privateMessagesChannels.add(channels.get(0));
                }
            }

        } else {
            // platoonMaxChannel == 1 /centreMaxChannel >= 3
            if (self instanceof MrlCentre) {
                for (int counter = 0; counter < centreMaxChannel; counter++) {
                    centreChannels.add(channels.get(counter));
                }

            } else if (self instanceof MrlAmbulanceTeam) {
                broadcastChannels.add(channels.get(0));
                privateMessagesChannels.add(channels.get(0));

            } else if (self instanceof MrlFireBrigade) {
                broadcastChannels.add(channels.get(1));
                privateMessagesChannels.add(channels.get(1));
            } else {
                broadcastChannels.add(channels.get(2));
                privateMessagesChannels.add(channels.get(2));
            }
        }
    }
*/
}