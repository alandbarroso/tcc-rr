package mrl.communication.channels;

import mrl.common.MRLConstants;
import mrl.communication.MessageTranslator;
import mrl.communication.messages.ChannelScannerMessage;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 5, 2011
 * Time: 11:04:21 AM
 */
public class ChannelScanner implements MRLConstants {

    public ChannelScanner() {
    }

    /**
     * dar in method har agent be channeli ke amade scan shodane packet haye yek byte-i ersal mikone.
     *
     * @param channels:          list hame-ye channels.
     * @param scannerMaxChannel: max te'dade chanel-haei ke scanner mitavanad subscribe konad.
     * @param agent:             khode agent-e
     * @param agentsNumber:      te'dade hame-ye agent ha.
     * @param amIScanner:        aya man scanner hastam?
     */
    public void sendMessageForScanChannel(Channels channels, int scannerMaxChannel, StandardAgent agent, int agentsNumber, boolean amIScanner) {
        List<Channel> channelIds = new ArrayList<Channel>();

        for (Channel channel : channels) {
            if (!channel.isSendMessage() && channel.getType().equalsIgnoreCase(Channels.RADIO_CHANNEL_KEY)) {
                sendMessage(channel, agent, agentsNumber, amIScanner);
                channel.setSendMessage();
                scannerMaxChannel--;
                channelIds.add(channel);
            }

            if (scannerMaxChannel == 0) {
                break;
            }
        }
        if (amIScanner && !channelIds.isEmpty()) {
            channels.sendSubscribeToHear(agent, channelIds);
        }
    }

    /**
     * dar in method har agent rooye ye chanel-e khas ye seri message
     *
     * @param channel:      chanel-i ke roosh message miferestan vase scan
     * @param agent:        ferestandeye message.
     * @param agentsNumber: te'dade kolle agent ha
     * @param amIScanner:   moshakhas mikone ke man scanner
     */
    private void sendMessage(Channel channel, StandardAgent agent, int agentsNumber, boolean amIScanner) {
        int numberOfMessageToSend;
        numberOfMessageToSend = (channel.getBandwidth() / agentsNumber);

        if (amIScanner) {
            int extra = channel.getBandwidth() % agentsNumber;
            if (extra != 0) {
                numberOfMessageToSend += extra;
            }
        }

        ChannelScannerMessage message = new ChannelScannerMessage();
        byte[] packetByteArray = MessageTranslator.getSimpleByteArray(message.getType());
//        System.out.println(agent + " send scan message.  " + channel.getId());
        for (int counter = 0; counter < numberOfMessageToSend; counter++) {

            if (agent instanceof MrlPlatoonAgent) {
                ((MrlPlatoonAgent) agent).sendMessage(channel.getId(), packetByteArray);
            } else {
                ((MrlCentre) agent).sendMessage(channel.getId(), packetByteArray);
            }
        }
    }

    /**
     * dar in method scanner be channel ha goosh karde va te'dade message haye daryafti ra barresi va mizane noise ra bedast miavarad.
     *
     * @param channels:          list-e hameye channel ha.
     * @param commands:          tamame chiz haei ke shenide.
     * @param scannerMaxChannel: max te'dade channel haei ke scanner mitavanad subscribe konad.
     * @param agentSize:         te'dade kolle agent-ha.
     */
    public void scanChannel(Channels channels, Collection<Command> commands, int scannerMaxChannel, int agentSize) {

        int scannedChannelNumber = 0;
        List<EntityID> receivedAgents = new ArrayList<EntityID>();

        for (Channel channel : channels) {

            if (channel.getType().equalsIgnoreCase(Channels.RADIO_CHANNEL_KEY)) {
                scannedChannelNumber++;

                if (!channel.isScanned()) {

                    scannerMaxChannel--;

                    if (commands.size() == 0) {
                        if (MRLConstants.DEBUG_MESSAGING) {
                            System.err.println(" ERROR: commands for scan in empty");
//                            Debbuge.fileAppending(" ERROR: commands for scan in empty");
                        }
                        break;
                    }

                    double receivedMessage = 0;
                    double dropoutMessage = 0;
                    double numberOfMessageToReceive;

                    numberOfMessageToReceive = channel.getBandwidth();

                    for (Command command : commands) {
                        AKSpeak speak = (AKSpeak) command;
                        if (!receivedAgents.contains(speak.getAgentID())) {
                            receivedAgents.add(speak.getAgentID());
                        }
                        byte[] message = speak.getContent();

                        if (speak.getChannel() == Channels.VOICE_CHANNEL) {
                            continue;
                        }
                        if (channel.getId() == speak.getChannel()) {
                            receivedMessage++;
                            if (message == null || message.length == 0) {
                                dropoutMessage++;
                            }
                        }
                    }

                    if (receivedAgents.size() == agentSize) {
                        channel.setDropout(dropoutMessage / receivedMessage);
                        channel.setFailure((numberOfMessageToReceive - receivedMessage) / numberOfMessageToReceive);
                    } else {
                        if (DEBUG_MESSAGING) {
                            System.err.println(" Cancel scan      received : " + receivedAgents.size() + " agent size : " + agentSize + " for : " + channel.toString());
//                            Debbuge.fileAppending(" Cancel scan      received : " + receivedAgents.size() + " agent size : " + agentSize + " for : " + channel.toString());
                        }
                        channels.setScanBreak();
                    }

                    channel.calculateRealBandwidthAndRepeatCont();
                    channel.setScanned();
                    if (DEBUG_MESSAGING) {
                        System.out.println("       SCAN:  " + channel.toString());
//                        Debbuge.fileAppending("       SCAN:  " + channel.toString());
                    }
                }
                if (scannerMaxChannel == 0) {
                    break;
                }
            }
        }

        if (scannedChannelNumber == channels.radioChannelsSize()) {
            channels.setScanFinished();
        }
    }

}
