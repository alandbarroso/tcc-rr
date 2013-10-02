package Communication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import clustering.Cluster;
import Think.*;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class Comm {

	/*
	 * The type (id) of the message
	 */
	public static enum MESSAGE_ID {
		BUILDING_ON_FIRE_MESSAGE_ID, BLOCKED_ROAD_MESSAGE_ID, EXTINGUISHED_FIRE_MESSAGE_ID, FINISHED_CLUSTER_POLICE_MESSAGE_ID, COLLAPSED_BUILDING_MESSAGE_ID, FINISHED_CLUSTER_FIRE_MESSAGE_ID, FINISHED_CLUSTER_AMBULANCE_MESSAGE_ID, CLEARED_ROAD_MESSAGE_ID, CIVILIAN_LOCATION_BURIED, CIVILIAN_LOCATION_NOT_BURIED, AGENT_LOCATION_HEARD_CIVILIAN, BURIED_AGENT_LOCATION, WARM_BUILDING, CLEARED_BUILDING, STUCK_INSIDE_BLOCKADE, BLOCKED_ROAD_PRIORITIZED, UNBURNT_BUILDING, ROAD_OCCUPIED, CLUSTER_STATUS, ONE_CLUSTER_STATUS, OCUPPIED_HYDRANT, AVAILABLE_HYDRANT
	};

	/**
	 * The message reporting a burning building
	 * 
	 * @param building
	 *            : the building on fire being reported
	 * @param time
	 *            : the time the message is sent
	 * @param clusterIndex
	 *            : the index of the cluster that the building belongs to
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] fireMessage(Building building, int time,
			int clusterIndex, ArrayList<StandardEntity> list) {
		BuildingInformation bi = new BuildingInformation(building,
				building.getFieryness(), building.getTemperature(), time,
				clusterIndex);
		return (MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.ordinal() + ","
				+ Think.getIndexIfExists(list, building.getID()) + "," + bi
					.toString()).getBytes();
	}

	/**
	 * The message reporting an unburnt building
	 * 
	 * @param building
	 *            : the unburnt building being reported
	 * @param time
	 *            : the time the message is sent
	 * @param clusterIndex
	 *            : the index of the cluster that the building belongs to
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] unburntBuilding(Building building, int time,
			int clusterIndex, ArrayList<StandardEntity> list) {
		BuildingInformation bi = new BuildingInformation(building,
				building.getFieryness(), building.getTemperature(), time,
				clusterIndex);
		return (MESSAGE_ID.UNBURNT_BUILDING.ordinal() + ","
				+ Think.getIndexIfExists(list, building.getID()) + "," + bi
					.toString()).getBytes();
	}

	/**
	 * The message reporting a collapsed building
	 * 
	 * @param building
	 *            : the collapsed building being reported
	 * @param time
	 *            : the time the message is sent
	 * @param clusterIndex
	 *            : the index of the cluster that the building belongs to
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] CollapsedMessage(Building building, int time,
			int clusterIndex, ArrayList<StandardEntity> list) {
		BuildingInformation bi = new BuildingInformation(building,
				building.getFieryness(), building.getTemperature(), time,
				clusterIndex);
		return (MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.ordinal() + ","
				+ Think.getIndexIfExists(list, building.getID()) + "," + bi
					.toString()).getBytes();
	}

	/**
	 * The message reporting a warm building
	 * 
	 * @param building
	 *            : the warm building being reported
	 * @param time
	 *            : the time the message is sent
	 * @param clusterIndex
	 *            : the index of the cluster that the building belongs to
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] warmBuilding(Building b, int time, int clusterIndex,
			ArrayList<StandardEntity> list) {
		BuildingInformation bi = new BuildingInformation(b, b.getFieryness(),
				b.getTemperature(), time, clusterIndex);
		return (MESSAGE_ID.WARM_BUILDING.ordinal() + ","
				+ Think.getIndexIfExists(list, b.getID()) + "," + bi.toString())
				.getBytes();
	}

	/**
	 * The message reporting the location of an agent stuck inside a blockade
	 * 
	 * @param location
	 *            : the location of the stuck agent
	 * @param blockade
	 *            : the id of the blockade that the agent is stuck in
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] stuckInsideBlockade(EntityID location,
			EntityID blockade, ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.STUCK_INSIDE_BLOCKADE.ordinal() + ","
				+ Think.getIndexIfExists(list, location) + "," + blockade
					.getValue()).getBytes();
	}

	/**
	 * The message reporting a cleared building
	 * 
	 * @param buildingID
	 *            : the building that has been cleared
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] clearedBuilding(EntityID buildingID,
			ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.CLEARED_BUILDING.ordinal() + "," + Think
				.getIndexIfExists(list, buildingID)).getBytes();
	}

	/**
	 * The message reporting the buried agent
	 * 
	 * @param position
	 *            : the position of the buried agent
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] buriedAgent(EntityID position,
			ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.BURIED_AGENT_LOCATION.ordinal() + "," + Think
				.getIndexIfExists(list, position)).getBytes();
	}

	/**
	 * The message reporting a road that is blocked
	 * 
	 * @param roadID
	 *            : the id of the road that is blocked
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] blockedRoad(EntityID roadID,
			ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.BLOCKED_ROAD_MESSAGE_ID.ordinal() + "," + Think
				.getIndexIfExists(list, roadID)).getBytes();
	}

	/**
	 * The message reporting an extinguished building
	 * 
	 * @param building
	 *            : the extinguished building being reported
	 * @param time
	 *            : the time the message is sent
	 * @param clusterIndex
	 *            : the index of the cluster that the building belongs to
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] extinguishedFire(Building building, int time,
			int clusterIndex, ArrayList<StandardEntity> list) {
		BuildingInformation bi = new BuildingInformation(building,
				building.getFieryness(), building.getTemperature(), time,
				clusterIndex);
		return (MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.ordinal() + ","
				+ Think.getIndexIfExists(list, building.getID()) + "," + bi
					.toString()).getBytes();
	}

	/**
	 * The message reporting a finished police cluster
	 * 
	 * @param clusterIndex
	 *            : the index of the finished cluster being reported
	 * @return
	 */
	public static byte[] finishedClusterPolice(int clusterIndex) {
		return (MESSAGE_ID.FINISHED_CLUSTER_POLICE_MESSAGE_ID.ordinal() + "," + clusterIndex)
				.getBytes();
	}

	/**
	 * The message reporting a finished fire brigade cluster
	 * 
	 * @param clusterIndex
	 *            : the index of the finished cluster being reported
	 * @return
	 */
	public static byte[] finishedClusterFire(int clusterIndex) {
		return (MESSAGE_ID.FINISHED_CLUSTER_FIRE_MESSAGE_ID.ordinal() + "," + clusterIndex)
				.getBytes();
	}

	/**
	 * The message reporting a finished ambulance cluster
	 * 
	 * @param clusterIndex
	 *            : the index of the finished cluster being reported
	 * @return
	 */
	public static byte[] finishedClusterAmbulance(int clusterIndex) {
		return (MESSAGE_ID.FINISHED_CLUSTER_AMBULANCE_MESSAGE_ID.ordinal()
				+ "," + clusterIndex).getBytes();
	}

	/**
	 * The message reporting a cleared road
	 * 
	 * @param roadID
	 *            : the id of the road being reported
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] clearedRoad(EntityID roadID,
			ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID.ordinal() + "," + Think
				.getIndexIfExists(list, roadID)).getBytes();
	}

	/**
	 * The message reporting the location of a buried civilian
	 * 
	 * @param position
	 *            : the position of the buried civilian
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] civilianLocationBuried(EntityID position,
			ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.CIVILIAN_LOCATION_BURIED.ordinal() + "," + Think
				.getIndexIfExists(list, position)).getBytes();
	}

	/**
	 * The message reporting a road that is blocked with its priority
	 * 
	 * @param roadID
	 *            : the id of the road that is blocked
	 * @param priority
	 *            : the priority of the blocked road
	 * 
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] blockedRoadWithPriority(EntityID roadID, int priority,
			ArrayList<StandardEntity> list) {
		return (MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.ordinal() + ","
				+ Think.getIndexIfExists(list, roadID) + "," + priority)
				.getBytes();
	}

	/**
	 * The message reporting a building status(on fire, warm, extinguished,
	 * collapsed), to be used in voice communication only
	 * 
	 * @param messageOrdinal
	 *            : the ordinal referring to the message type (id)
	 * @param building
	 *            : the building being reported
	 * @param list
	 *            : the list of the entities computed during postconnect
	 * @return
	 */
	public static byte[] fireMessageVoice(int messageOrdinal,
			Building building, ArrayList<StandardEntity> list) {
		return (messageOrdinal + ","
				+ Think.getIndexIfExists(list, building.getID()) + "," + building
					.getFieryness()).getBytes();
	}

	/**
	 * Discovers the available communication channels and their properties
	 * 
	 * @param voiceChannel
	 *            : list of voice channels
	 * @param radioChannel
	 *            : list of radio channels
	 * @param config
	 */
	public static void discoverChannels(ArrayList<VoiceChannel> voiceChannel,
			ArrayList<RadioChannel> radioChannel, Config config) {
		String comm = rescuecore2.standard.kernel.comms.ChannelCommunicationModel.PREFIX;
		int channels = config.getIntValue(comm + "count");
		for (int i = 0; i < channels; i++) {
			String type = config.getValue(comm + i + ".type");
			if (type.equalsIgnoreCase("radio")) {
				int bw = config.getIntValue(comm + i + ".bandwidth");
				radioChannel.add(new RadioChannel(i, type, bw));
			} else if (type.equalsIgnoreCase("voice")) {
				int range = config.getIntValue(comm + i + ".range");
				int msgSize = config.getIntValue(comm + i + ".messages.size");
				int maxMsg = config.getIntValue(comm + i + ".messages.max");
				voiceChannel.add(new VoiceChannel(i, type, range, maxMsg,
						msgSize));
			}
		}
	}

	/**
	 * Decides the radio channels that the agent will subscribe to
	 * 
	 * @param channels
	 *            : the list of available channels
	 * @param type
	 *            : the type of the agent ('f','p' or 'a')
	 * @return
	 */
	public static int decideRadioChannel(ArrayList<RadioChannel> channels,
			char type) {
		int size = channels.size();

		// arrange according to bandwidth
		Comparator<RadioChannel> comp = new Comparator<RadioChannel>() {
			public int compare(RadioChannel o1, RadioChannel o2) {
				if (o1.bandwidth < o2.bandwidth)
					return -1;
				if (o1.bandwidth > o2.bandwidth)
					return 1;
				return 0;
			}
		};
		Collections.sort(channels, comp);

		// channel decision
		switch (size) {
		case 0:
			;
			break;
		case 1:
			return channels.get(0).id;
		case 2:
			switch (type) {
			case 'f':
				return channels.get(1).id;
			case 'p':
				return channels.get(0).id;
			case 'a':
				return channels.get(0).id;
			}
			;
			break;
		default:
			switch (type) {
			case 'f':
				return channels.get(size - 1).id;
			case 'p':
				return channels.get(size - 3).id;
			case 'a':
				return channels.get(size - 2).id;
			}
			;
			break;
		}
		return -1;
	}

	/**
	 * Adds voice messages to the agent's list of available messages to be
	 * reported during voice communication
	 * 
	 * @param message
	 *            : the voice message to be reported
	 * @param voiceMessages
	 *            : the agent's voice messages list to be reported
	 * @param comparator
	 *            : the comparator that is used in sorting the voice messages
	 *            according to their priority
	 */
	public static void reportVoice(VoiceMessage message,
			ArrayList<VoiceMessage> voiceMessages,
			Comparator<VoiceMessage> comparator) {
		Think.addIfNotExists(message, voiceMessages);
		Collections.sort(voiceMessages, comparator);
	}

	/**
	 * Concatenates messages from the list of voice messages to form one message
	 * that is equal to the allowed size in bytes to be sent.
	 * 
	 * @param voiceChannel
	 *            : the voice channel used in the scenario
	 * @param voiceMessages
	 *            : the list of voice messages that need to be sent
	 * @return
	 */
	public static byte[] reportAllVoiceMessages(VoiceChannel voiceChannel,
			ArrayList<VoiceMessage> voiceMessages) {
		int max = voiceChannel.messageSize;
		String message = "";
		int counter = 0;
		int i = 0;
		for (VoiceMessage msg : voiceMessages) {
			msg.decrementTTL();
		}
		for (VoiceMessage msg : voiceMessages) {
			if (i == 0) {
				counter += msg.getData().length
						+ ("," + msg.getTimeToLive()).length();
			} else {
				counter += msg.getData().length + 1
						+ ("," + msg.getTimeToLive()).length();
			}
			if (counter > max) {
				break;
			} else {
				String m = new String(msg.getData());
				String newMessage = m + "," + msg.getTimeToLive();
				if (i == 0) {
					message += newMessage;
				} else {
					message += "-" + newMessage;
				}
			}
			i++;
		}
		for (int j = 0; j < voiceMessages.size(); j++) {
			if (voiceMessages.get(j).getTimeToLive() <= 0) {
				voiceMessages.remove(j--);
			}
		}
		return message.getBytes();
	}
}