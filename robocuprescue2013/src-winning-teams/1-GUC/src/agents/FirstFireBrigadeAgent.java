package agents;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;
import Communication.Comm;
import Communication.Comm.MESSAGE_ID;
import Communication.RadioChannel;
import Communication.VoiceChannel;
import Communication.VoiceMessage;
import PostConnect.PostConnect;
import Think.BuildingInformation.BUILDING_LIST;
import Think.GraphHelper;
import Think.Predicate;
import Think.Think;
import clustering.Cluster;
import clustering.Clustering;
import clustering.Density;
import clustering.FireCluster;

public class FirstFireBrigadeAgent extends StandardAgent<FireBrigade> {
	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	public static final String PLAN_DIRECTORY = "precompute/";
	public static final String LOG_DIRECTORY = "log/";

	public static final String PLAN_FORMAT = ".GUC_ARTSAPIENCE";
	public static final String PLAN_PREFIX = "FireBrigadePlan";

	public static enum STUCK_PRIORITY {
		NORMAL, GOING_TO_REFUGE, NOTHING, GOING_TO_FIRE,
	};

	private int maxWater, maxDistance, maxPower;
	protected ArrayList<EntityID> refuges;
	protected ArrayList<Cluster> clusters = null;
	protected Cluster currentCluster, CurrentClusterCopy;
	protected boolean agentIsStuck = false;
	protected EntityID lastPosition = null;

	protected int lastPositionX, lastPositionY;
	ArrayList<VoiceChannel> voiceChannels = new ArrayList<VoiceChannel>();
	ArrayList<RadioChannel> radioChannels = new ArrayList<RadioChannel>();
	int canSubscribeChannels;

	ArrayList<EntityID> buriedCiviliansReported = new ArrayList<EntityID>();

	int ignoreAgentCommand;

	private int policeChannel, fireChannel, ambulanceChannel;
	List<EntityID> path = null, previousPath = null;

	private int initialClusterIndex;

	int numberOfAgents;

	private static final int TIME_TO_LIVE_VOICE = 10;

	STUCK_PRIORITY priorityStuck = STUCK_PRIORITY.NORMAL;
	private GraphHelper graphHelper;
	ArrayList<EntityID> reportedHelpRadio = new ArrayList<EntityID>();
	ArrayList<EntityID> reportedOuchRadio = new ArrayList<EntityID>();

	// Buildings in Changeset, Updated every timestep
	ArrayList<Building> newBuildingUnburnt = new ArrayList<Building>();
	ArrayList<Building> newBuildingWarm = new ArrayList<Building>();
	ArrayList<Building> newBuildingsOnFire = new ArrayList<Building>();
	ArrayList<Building> newCollapsedBuildings = new ArrayList<Building>();
	ArrayList<Building> newExtinguishedFire = new ArrayList<Building>();

	// Buildings Reported are considered in the same timestep as part of the
	// changeset
	ArrayList<Building> newVoiceBuildingWarm = new ArrayList<Building>();
	ArrayList<Building> newVoiceBuildingsOnFire = new ArrayList<Building>();

	// List of Fires in the map
	ArrayList<FireCluster> fireGroups = new ArrayList<FireCluster>();
	// current Fire the agent is working on
	FireCluster currentFireGroup;

	private Logger agentLogger;

	private ArrayList<AKSpeak> dropped = new ArrayList<AKSpeak>();

	ArrayList<StandardEntity> allEntities = new ArrayList<StandardEntity>();
	Comparator<VoiceMessage> comparator = new Comparator<VoiceMessage>() {
		@Override
		public int compare(VoiceMessage o1, VoiceMessage o2) {
			if (o1.getPriority() < o2.getPriority()) {
				return -1;
			} else if (o1.getPriority() > o2.getPriority()) {
				return 1;
			}
			return 0;
		}
	};
	ArrayList<VoiceMessage> voiceMessages = new ArrayList<VoiceMessage>();

	boolean precompute = false;

	public FirstFireBrigadeAgent(int preCompute) {
		super();
		this.precompute = (preCompute == 1);
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	@Override
	protected void postConnect() {

		agentLogger = Logger.getLogger("FireBrigade Agent "
				+ this.getID().toString() + " logger");
		agentLogger.setLevel(Level.DEBUG);
		FileAppender fileAppender;
		try {
			fileAppender = new FileAppender(new PatternLayout(), "log/"
					+ getCurrentTimeStamp() + "Firebrigade-"
					+ this.getID().toString() + ".log");
			agentLogger.removeAllAppenders();
			agentLogger.addAppender(fileAppender);
			agentLogger.debug("======================================");
			agentLogger.debug("FireBrigade Agent " + this.getID().toString()
					+ " connected");
			agentLogger.debug("======================================");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Clustering c = new Clustering();

		graphHelper = new GraphHelper(model);

		// Get all fire brigades
		ArrayList<StandardEntity> nAgents = new ArrayList<StandardEntity>(
				model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));

		Comm.discoverChannels(voiceChannels, radioChannels, config);

		String comm = rescuecore2.standard.kernel.comms.ChannelCommunicationModel.PREFIX;
		canSubscribeChannels = config.getIntValue(comm + "max.platoon");

		ignoreAgentCommand = config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);

		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);

		refuges = PostConnect.getIDs(new ArrayList<StandardEntity>(model
				.getEntitiesOfType(StandardEntityURN.REFUGE)));

		numberOfAgents = nAgents.size();

		File file = new File(PLAN_DIRECTORY);
		file.mkdirs();
		file = new File(PLAN_DIRECTORY + PLAN_PREFIX + PLAN_FORMAT);

		if (file.exists() && !precompute) {
			System.out.println("File found");
			clusters = PostConnect.readAllTasks(model, PLAN_DIRECTORY
					+ PLAN_PREFIX + PLAN_FORMAT);
		} else {
			System.out.println("File not found");

			// All the buildings in the map
			ArrayList<StandardEntity> task = new ArrayList<StandardEntity>(
					model.getEntitiesOfType(StandardEntityURN.BUILDING));

			clusters = c.KMeansPlusPlus(nAgents.size(), model, 50, task);

			ArrayList<EntityID> agentsIDs = PostConnect.getIDs(nAgents);

			// Assign Agents to each cluster and write the plan to the file
			Density.assignAgentsToClusters2(model, clusters, agentsIDs,
					PLAN_DIRECTORY + PLAN_PREFIX + PLAN_FORMAT, false, true);
		}
		// get the cluster index if found
		initialClusterIndex = 0;
		for (Cluster cl : clusters) {
			boolean found = false;
			for (EntityID ag : cl.agents) {

				if (ag.getValue() == this.getID().getValue()) {
					found = true;
					break;

				}
			}
			if (found)
				break;
			initialClusterIndex++;
		}

		// incase the agent is not assigned to a cluster
		if (initialClusterIndex == clusters.size()) {
			System.out.println("not found");
			double shortestDistance = Double.POSITIVE_INFINITY;
			initialClusterIndex = 0;
			int counter = 0;
			for (Cluster cl : clusters) {
				if (model.getDistance(getID(), cl.center.getID()) < shortestDistance) {
					shortestDistance = model.getDistance(getID(),
							cl.center.getID());
					initialClusterIndex = counter;
				}
				counter++;
			}
		}
		// get the current cluster from all clusters
		currentCluster = clusters.get(initialClusterIndex);

		// copy the cluster
		CurrentClusterCopy = Think.copyCluster(currentCluster);

		// keep track of the last position of the agent
		// should be updated every step
		lastPositionX = me().getX();
		lastPositionY = me().getY();

		Collection<StandardEntity> tempAll = model.getEntitiesOfType(
				StandardEntityURN.BUILDING, StandardEntityURN.ROAD,
				StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.POLICE_OFFICE,
				StandardEntityURN.FIRE_STATION, StandardEntityURN.GAS_STATION);
		for (StandardEntity s : tempAll) {
			allEntities.add(s);
		}
		Comparator<StandardEntity> comp = new Comparator<StandardEntity>() {
			@Override
			public int compare(StandardEntity o1, StandardEntity o2) {
				if (o1.getID().getValue() < o2.getID().getValue()) {
					return -1;
				} else if (o1.getID().getValue() > o2.getID().getValue()) {
					return 1;
				}
				return 0;
			}
		};
		Collections.sort(allEntities, comp);

	}

	@Override
	protected void think(int time, ChangeSet arg1, Collection<Command> heard) {
		if (radioChannels.size() > 0)
			thinkWithCommunication(time, arg1, heard);
		else
			thinkWithVoice(time, arg1, heard);

	}

	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	protected void thinkWithCommunication(int time, ChangeSet arg1,
			Collection<Command> heard) {
		agentLogger.debug("-----------time: " + time + "----------------");
		if (time < ignoreAgentCommand) {
			agentLogger.debug("ignore agent command");
			return;
		}

		if (time == ignoreAgentCommand) {
			if (canSubscribeChannels > 0) {
				fireChannel = Comm.decideRadioChannel(radioChannels, 'f');
				ambulanceChannel = Comm.decideRadioChannel(radioChannels, 'a');
				policeChannel = Comm.decideRadioChannel(radioChannels, 'p');
				sendSubscribe(time, fireChannel);
				agentLogger.debug("subscribe to " + fireChannel);
			}
			if (Think
					.stuckInBlockade(model, arg1, lastPositionX, lastPositionY)) {
				agentLogger.debug("stuck in blockede ");

				sendSpeak(time, policeChannel, Comm.stuckInsideBlockade(
						location().getID(), getclosestBlockade(arg1).getID(),
						allEntities));
				agentLogger.debug("report Stuck in blockade ");

			}
			if (me().getBuriedness() > 0) {
				agentLogger.debug("buried");
				sendSpeak(time, ambulanceChannel,
						Comm.buriedAgent(location().getID(), allEntities));
				agentLogger.debug("report buried ");

				if (ambulanceChannel != policeChannel) {
					sendSpeak(time, policeChannel,
							Comm.buriedAgent(location().getID(), allEntities));

				}

			}
		} else {
			handleMessagesRadio2(time, heard);
			resendMessages(time, heard);
		}

		ArrayList<Building> changedBuildings = Think.getChangedBuildings(model,
				arg1);
		handleFires(arg1, time, changedBuildings);

		reportLocationOfCivilians(time, arg1, model, ambulanceChannel);

		removeTotallyBlockedRoads(time, arg1);
		addTotallyClearedRoads(time, arg1);

		if (me().getBuriedness() > 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.buriedAgent(location().getID(),
							allEntities), 1, 1,
							MESSAGE_ID.BURIED_AGENT_LOCATION.name()),
					voiceMessages, comparator);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));

			sendRest(time);
			return;
		}
		if (Think.stuckInBlockade(model, arg1, lastPositionX, lastPositionY)) {

			agentLogger.debug("time: " + time + " ,sendRest");
			Comm.reportVoice(
					new VoiceMessage(Comm.stuckInsideBlockade(location()
							.getID(), getclosestBlockade(arg1).getID(),
							allEntities), 1, 1,
							MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
					voiceMessages, comparator);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));

			sendRest(time);
			return;
		}
		// check if agent is stuck
		StuckDetectionPart();

		handleEmptyClusterCopy(arg1, time);

		if (agentIsStuck) {
			agentLogger.debug("agent is stuck");
			if (previousPath != null && previousPath.size() != 0) {
				if (graphHelper.removeNode(previousPath.get(0))) {

					reportBlockade(time);
					agentLogger
							.debug("report blockade: " + previousPath.get(0));
				}
			}

		}
		waterTankPart(time, arg1);
		if (isOutOfWater())
			return;
		path = null;
		if (path == null)
			if (extinguishFire2(arg1, time))
				return;

		path = graphHelper.getSearch().breadthFirstSearch(me().getPosition(),
				CurrentClusterCopy.cluster);

		if (path == null || path.size() == 0) {
			agentLogger.debug("normal task failed");

			agentLogger.debug("restoreGraph");

			surroundedByBlockades(time, arg1);
			restoreGraph();
			removeBuildingsFromClusters(changedBuildings,
					newCollapsedBuildings, CurrentClusterCopy, clusters);
			path = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), CurrentClusterCopy.cluster);
			EntityID firstNode = path.get(0);
			path = new ArrayList<EntityID>();
			path.add(firstNode);

		} else if (path != null) {

			if (path.size() > 1)
				path.remove(path.size() - 1);

			previousPath = path;
			priorityStuck = STUCK_PRIORITY.NORMAL;
		} else {
			agentLogger.debug("nothing");

		}
		agentLogger.debug("normal task: " + path.get(path.size() - 1));
		agentLogger.debug("CURRENT CLUSTER");
		agentLogger.debug(Think
				.ArrayListEntityIDtoString(currentCluster.cluster));
		sendMove(time, path);
	}

	protected void thinkWithVoice(int time, ChangeSet arg1,
			Collection<Command> heard) {
		agentLogger.debug("-----------time: " + time + "----------------");
		if (time < ignoreAgentCommand) {
			agentLogger.debug("ignore agent command");
			return;
		}

		if (time == ignoreAgentCommand) {
			agentLogger.debug("Voice Only");

			fireChannel = ambulanceChannel = policeChannel = 0;

			if (me().getBuriedness() > 0) {
				agentLogger.debug("buried ");
			}
			if (Think
					.stuckInBlockade(model, arg1, lastPositionX, lastPositionY)) {
				agentLogger.debug("I am stuck inside Blockade");
			}
		} else {
			handleMessagesRadio2(time, heard);
		}

		ArrayList<Building> changedBuildings = Think.getChangedBuildings(model,
				arg1);

		handleFires(arg1, time, changedBuildings);

		agentLogger.debug("number of fires" + fireGroups.size());

		if (me().getBuriedness() > 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.buriedAgent(location().getID(),
							allEntities), 1, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.BURIED_AGENT_LOCATION.name()),
					voiceMessages, comparator);

			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			sendRest(time);

			return;
		} else if (Think.stuckInBlockade(model, arg1, lastPositionX,
				lastPositionY)) {
			Comm.reportVoice(
					new VoiceMessage(Comm.stuckInsideBlockade(location()
							.getID(), getclosestBlockade(arg1).getID(),
							allEntities), 1, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
					voiceMessages, comparator);

			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			agentLogger.debug("resting");
			sendRest(time);
			return;
		}
		// check if agent is stuck
		StuckDetectionPart();

		handleEmptyClusterCopy(arg1, time);

		if (agentIsStuck) {

			if (previousPath != null && previousPath.size() != 0) {
				if (graphHelper.removeNode(previousPath.get(0))) {

					agentLogger.debug("remove Node: " + previousPath.get(0));
					reportBlockadeVoice(time);
				}
			}

		}

		removeTotallyBlockedRoads(time, arg1);

		addTotallyClearedRoads(time, arg1);

		waterTankPart(time, arg1);
		if (isOutOfWater())
			return;
		path = null;

		if (path == null)
			if (extinguishFire2(arg1, time)) {
				sendSpeak(time, 0, Comm.reportAllVoiceMessages(
						voiceChannels.get(0), voiceMessages));
				return;
			}
		path = graphHelper.getSearch().breadthFirstSearch(me().getPosition(),
				CurrentClusterCopy.cluster);

		if (path == null || path.size() == 0) {
			agentLogger.debug("normal task failed");

			surroundedByBlockades(time, arg1);
			restoreGraph();
			removeBuildingsFromClusters(changedBuildings,
					newCollapsedBuildings, CurrentClusterCopy, clusters);
			path = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), CurrentClusterCopy.cluster);
			EntityID firstNode = path.get(0);
			path = new ArrayList<EntityID>();
			path.add(firstNode);

		} else if (path != null) {

			if (path.size() > 1)
				path.remove(path.size() - 1);

			previousPath = path;
			priorityStuck = STUCK_PRIORITY.NORMAL;
		} else {
			agentLogger.debug("nothing");
		}
		agentLogger.debug("normal task: " + path.get(path.size() - 1));
		agentLogger.debug("CURRENT CLUSTER");
		agentLogger.debug(Think
				.ArrayListEntityIDtoString(currentCluster.cluster));
		sendMove(time, path);
		sendSpeak(time, 0, Comm.reportAllVoiceMessages(voiceChannels.get(0),
				voiceMessages));
	}

	protected void StuckDetectionPart() {
		if (Think.isBlocked(lastPosition, me(), lastPositionX, lastPositionY)) {
			agentIsStuck = true;
			agentLogger.debug("stuck is true");

		} else {
			agentIsStuck = false;
			lastPosition = location().getID();
		}
		lastPositionX = me().getX();
		lastPositionY = me().getY();
	}

	private void resendMessages(int time, Collection<Command> heard) {
		try {
			for (int i = 0; i < dropped.size(); i++) {
				AKSpeak messageD = dropped.get(i);
				String content = new String(messageD.getContent());
				boolean droppedMsg = true;

				for (Command next : heard) {
					AKSpeak message = (AKSpeak) next;
					String content2 = new String(message.getContent());
					if (messageD.getChannel() == message.getChannel()
							&& content.equals(content2)) {
						dropped.remove(i--);
						droppedMsg = false;
						break;
					}
				}
				// }
				if (droppedMsg) {
					sendSpeak(time, dropped.get(i).getChannel(), dropped.get(i)
							.getContent());
					agentLogger.debug("resending dropped msg: "
							+ new String(dropped.get(i).getContent()) + " "
							+ dropped.get(i).getChannel());
				}
			}
			// dropped.clear();
		} catch (Exception e) {
			e.printStackTrace();
			agentLogger.debug(e.getMessage());
		}
	}

	private void handleMessagesRadio2(int time, Collection<Command> heard) {
		for (Command next : heard) {
			AKSpeak message = (AKSpeak) next;
			String m = new String(message.getContent());
			System.out.println("Message: " + m + " , " + message.getTime());
			try {
				if (message.getChannel() == 0) {
					String[] splitted = m.split("-");
					handleVoiceMessages(splitted, message.getTime());
					continue;
				}

				String[] msg = m.split(",");

				if (message.getAgentID().getValue() == getID().getValue()) {
					agentLogger.debug("time: " + message.getTime() + ", sent, "
							+ m);
					Think.removeIfExists(message, dropped);
					continue;
				}

				int msgId = (int) (Integer.parseInt(msg[0]));
				MESSAGE_ID ms = MESSAGE_ID.values()[msgId];
				/*
				 * agentLogger.debug("time: " + time + ", agent " +
				 * me().getID().getValue() + " received " + ms.name() + " " +
				 * new EntityID(Integer.parseInt(msg[1])).getValue());
				 */
				switch (ms) {
				case ONE_CLUSTER_STATUS: {
					int clusterIndex = Integer.parseInt(msg[1]);
					int size = clusters.get(clusterIndex).cluster.size();
					int hexValue = Integer.parseInt(msg[2], 16);
					String binary = Integer.toBinaryString(hexValue);
					int diff = size - binary.length();
					for (int i = 0; i < diff; i++) {
						binary = "0".concat(binary);
					}
				}
					break;
				case CLUSTER_STATUS: {
					int size = clusters.size();
					int hexValue = Integer.parseInt(msg[1], 16);
					String binary = Integer.toBinaryString(hexValue);
					int diff = size - binary.length();
					for (int i = 0; i < diff; i++) {
						binary = "0".concat(binary);
					}
					for (int j = 0; j < binary.length(); j++) {
						if (binary.charAt(j) == '1') {
							clusters.get(j).cluster.clear();
						}
					}
				}
					break;
				case BUILDING_ON_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					/*
					 * int cId = Integer.parseInt(msg[1]); EntityID id = new
					 * EntityID(cId); id = model.getEntity(id).getID();
					 */

					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);

					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					String[] bInfo = msg[2].split(" ");
					Think.addIfNotExists(b, newVoiceBuildingsOnFire);
					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, Integer.parseInt(bInfo[0]), 0,
								message.getTime(),
								BUILDING_LIST.BUILDINGS_ON_FIRE);
					} else {
						double[] centroid = { b.getX(), b.getY() };
						c = new FireCluster(new ArrayList<EntityID>(),
								centroid, b, 0, agentLogger);

						c.agentLogger = agentLogger;
						c.addNewTarget(b, Integer.parseInt(bInfo[0]), 0,
								message.getTime(),
								BUILDING_LIST.BUILDINGS_ON_FIRE);

						c.radius = 0;
						fireGroups.add(c);

					}
				}
					;
					break;
				case EXTINGUISHED_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					// int cId = Integer.parseInt(msg[1]);
					// EntityID id = new EntityID(cId);
					// id = model.getEntity(id).getID();

					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);
					String[] bInfo = msg[2].split(" ");

					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, Integer.parseInt(bInfo[0]), 0,
								message.getTime(),
								BUILDING_LIST.BUILDINGS_EXTINGUISHED);
					}
				}
					;
					break;

				case BLOCKED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();

					if (graphHelper.removeNode(id)) {

						agentLogger
								.debug("new totally blocked reported: " + id);
					}
				}
					;
					break;
				case CLEARED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();

					if (graphHelper.restoreNode(id)) {
						agentLogger
								.debug("new totally cleared reported: " + id);

					}
				}
					;
					break;
				case WARM_BUILDING: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();

					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);
					String[] bInfo = msg[2].split(" ");

					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					Think.addIfNotExists(b, newVoiceBuildingWarm);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, Integer.parseInt(bInfo[0]), 0,
								message.getTime(), BUILDING_LIST.BUILDINGS_WARM);
					} else {
						double[] centroid = { b.getX(), b.getY() };
						c = new FireCluster(new ArrayList<EntityID>(),
								centroid, b, 0, agentLogger);
						c.agentLogger = agentLogger;
						c.addNewTarget(b, Integer.parseInt(bInfo[0]), 0,
								message.getTime(), BUILDING_LIST.BUILDINGS_WARM);

						c.radius = 0;
						fireGroups.add(c);

					}
				}
					;
					break;
				case COLLAPSED_BUILDING_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();

					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);
					String[] bInfo = msg[2].split(" ");

					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, Integer.parseInt(bInfo[0]), 0,
								message.getTime(),
								BUILDING_LIST.BUILDINGS_COLLAPSED);
					}
				}
					;
					break;
				}

			} catch (NumberFormatException e) {

			} catch (Exception e) {
				e.printStackTrace();
				agentLogger.debug(e.getMessage());
			}

		}
	}

	private void handleVoiceMessages(String[] splitted, int time) {

		for (String message : splitted) {
			String[] msg = message.split(",");

			try {

				int msgId = (int) (Integer.parseInt(msg[0]));
				MESSAGE_ID ms = MESSAGE_ID.values()[msgId];

				switch (ms) {
				case ONE_CLUSTER_STATUS: {
					int clusterIndex = Integer.parseInt(msg[1]);
					int size = clusters.get(clusterIndex).cluster.size();
					int hexValue = Integer.parseInt(msg[2], 16);
					String binary = Integer.toBinaryString(hexValue);
					int diff = size - binary.length();
					for (int i = 0; i < diff; i++) {
						binary = "0".concat(binary);
					}
				}
					break;
				case CLUSTER_STATUS: {
					int size = clusters.size();
					int hexValue = Integer.parseInt(msg[1], 16);
					String binary = Integer.toBinaryString(hexValue);
					int diff = size - binary.length();
					for (int i = 0; i < diff; i++) {
						binary = "0".concat(binary);
					}
					for (int j = 0; j < binary.length(); j++) {
						if (binary.charAt(j) == '1') {
							clusters.get(j).cluster.clear();
						}
					}
				}
					break;
				case BUILDING_ON_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);

					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					int fieryness = Integer.parseInt(msg[2]);
					int ttl = Integer.parseInt(msg[3]);
					if (radioChannels.size() > 0 || ttl == 9)
						Think.addIfNotExists(b, newVoiceBuildingsOnFire);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, fieryness, 0, time,
								BUILDING_LIST.BUILDINGS_ON_FIRE);
					} else {
						double[] centroid = { b.getX(), b.getY() };
						c = new FireCluster(new ArrayList<EntityID>(),
								centroid, b, 0, agentLogger);

						c.agentLogger = agentLogger;
						c.addNewTarget(b, fieryness, 0, time,
								BUILDING_LIST.BUILDINGS_ON_FIRE);

						c.radius = 0;
						fireGroups.add(c);

					}

					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1,
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case EXTINGUISHED_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);
					int fieryness = Integer.parseInt(msg[2]);
					int ttl = Integer.parseInt(msg[3]);
					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, fieryness, 0, time,
								BUILDING_LIST.BUILDINGS_EXTINGUISHED);
					}

					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1,
									MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;

				case BLOCKED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[2]);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 3,
									ttl - 1, MESSAGE_ID.BLOCKED_ROAD_MESSAGE_ID
											.name()), voiceMessages, comparator);
					if (graphHelper.removeNode(id)) {
						agentLogger
								.debug("new totally blocked reported: " + id);
					}
				}
					;
					break;
				case CLEARED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[2]);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 3,
									ttl - 1, MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID
											.name()), voiceMessages, comparator);

					if (graphHelper.restoreNode(id)) {
						agentLogger
								.debug("new totally cleared reported: " + id);

					}
				}
					;
					break;
				case WARM_BUILDING: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);
					int fieryness = Integer.parseInt(msg[2]);
					int ttl = Integer.parseInt(msg[3]);

					Building b = (Building) model.getEntity(id);
					if (radioChannels.size() > 0 || ttl == 9)
						Think.addIfNotExists(b, newVoiceBuildingWarm);

					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, fieryness, 0, time,
								BUILDING_LIST.BUILDINGS_WARM);
					} else {
						double[] centroid = { b.getX(), b.getY() };
						c = new FireCluster(new ArrayList<EntityID>(),
								centroid, b, 0, agentLogger);
						c.agentLogger = agentLogger;
						c.addNewTarget(b, fieryness, 0, time,
								BUILDING_LIST.BUILDINGS_WARM);

						c.radius = 0;
						fireGroups.add(c);

					}
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1, MESSAGE_ID.WARM_BUILDING.name()),
							voiceMessages, comparator);
				}
					;
					break;
				case COLLAPSED_BUILDING_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					FireCluster c = FireCluster.getClosestCluster(id,
							fireGroups, model);
					int fieryness = Integer.parseInt(msg[2]);
					int ttl = Integer.parseInt(msg[3]);

					Building b = (Building) model.getEntity(id);
					if (currentFireGroup != null)
						Think.removeIfExists(b.getID(),
								currentFireGroup.buildingsExpectedToBeOnFire);

					if (c != null && c.isCloseToCluster(id, model)) {
						c.addNewTarget(b, fieryness, 0, time,
								BUILDING_LIST.BUILDINGS_COLLAPSED);
					}
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1,
									MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				}
			} catch (NumberFormatException e) {

			} catch (Exception e) {
				e.printStackTrace();
				agentLogger.debug(e.getMessage());
			}
		}
	}

	protected void handleFires(ChangeSet arg1, int time,
			ArrayList<Building> changedBuildings) {

		newBuildingUnburnt = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingUnBurnt);
		if (!newBuildingUnburnt.isEmpty())
			agentLogger.debug("newBuildingUnburnt:"
					+ Think.ArrayListBuildingtoString(newBuildingUnburnt));

		newBuildingWarm = (ArrayList<Building>) Think.filter(changedBuildings,
				FireCluster.predicateBuildingWarm);

		if (!newBuildingWarm.isEmpty())
			agentLogger.debug("newBuildingWarm:"
					+ Think.ArrayListBuildingtoString(newBuildingWarm));

		newBuildingsOnFire = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingOnFire);

		if (!newBuildingsOnFire.isEmpty())
			agentLogger.debug("newBuildingsOnFire:"
					+ Think.ArrayListBuildingtoString(newBuildingsOnFire));

		newCollapsedBuildings = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingCollapsed);
		if (!newCollapsedBuildings.isEmpty())
			agentLogger.debug("newCollapsedBuildings:"
					+ Think.ArrayListBuildingtoString(newCollapsedBuildings));

		newExtinguishedFire = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingExtinguished);
		if (!newExtinguishedFire.isEmpty())
			agentLogger.debug("newExtinguishedFire:"
					+ Think.ArrayListBuildingtoString(newExtinguishedFire));

		for (Building b : newBuildingUnburnt) {
			// FireCluster c = FireCluster.getClosestCluster(b.getID(),
			// fireGroups, model);
			// if (c != null) {
			for (FireCluster fc : fireGroups)
				if (Think.removeIfExists(b.getID(),
						fc.buildingsExpectedToBeOnFire)
						|| Think.removeIfExists(b.getID(), fc.buildingsWarm)) {
					if (fc.addNewTarget(b, b.getFieryness(),
							b.getTemperature(), time,
							BUILDING_LIST.BUILDINGS_UNBURNT))
						;
				}
			// }

		}

		for (Building b : newBuildingWarm) {

			FireCluster c = FireCluster.getClosestCluster(b.getID(),
					fireGroups, model);

			if (c != null && c.isCloseToCluster(b.getID(), model)) {

				if (c.addNewTarget(b, b.getFieryness(), b.getTemperature(),
						time, BUILDING_LIST.BUILDINGS_WARM)) {
					agentLogger.debug("report warm: " + b.getID());
					if (radioChannels.size() > 0)
						reportWarm(b, time, 0);
					else {
						Comm.reportVoice(
								new VoiceMessage(Comm.fireMessageVoice(
										MESSAGE_ID.WARM_BUILDING.ordinal(), b,
										allEntities), 2, TIME_TO_LIVE_VOICE,
										MESSAGE_ID.WARM_BUILDING.name()),
								voiceMessages, comparator);
					}
				}

			} else {
				double[] centroid = { b.getX(), b.getY() };
				c = new FireCluster(new ArrayList<EntityID>(), centroid, b, 0,
						agentLogger);
				c.agentLogger = agentLogger;
				c.addNewTarget(b, b.getFieryness(), b.getTemperature(), time,
						BUILDING_LIST.BUILDINGS_WARM);
				agentLogger.debug("report warm: " + b.getID());
				if (radioChannels.size() > 0)
					reportWarm(b, time, 0);
				else {
					Comm.reportVoice(
							new VoiceMessage(Comm.fireMessageVoice(
									MESSAGE_ID.WARM_BUILDING.ordinal(), b,
									allEntities), 2, TIME_TO_LIVE_VOICE,
									MESSAGE_ID.WARM_BUILDING.name()),
							voiceMessages, comparator);
				}
				c.radius = 0;
				fireGroups.add(c);

			}
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							Comm.MESSAGE_ID.WARM_BUILDING.ordinal(), b,
							allEntities), 2, 1, MESSAGE_ID.WARM_BUILDING.name()),
					voiceMessages, comparator);

		}
		for (Building b : newBuildingsOnFire) {
			FireCluster c = FireCluster.getClosestCluster(b.getID(),
					fireGroups, model);
			if (c != null && c.isCloseToCluster(b.getID(), model)) {
				if (c.addNewTarget(b, b.getFieryness(), b.getTemperature(),
						time, BUILDING_LIST.BUILDINGS_ON_FIRE)) {
					if (radioChannels.size() > 0)
						reportFire(b, time, 0);
					else {
						Comm.reportVoice(
								new VoiceMessage(Comm.fireMessageVoice(
										MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
												.ordinal(), b, allEntities), 2,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
												.name()), voiceMessages,
								comparator);
					}
					agentLogger.debug("report fire: " + b.getID());

				}
			} else {
				double[] centroid = { b.getX(), b.getY() };
				c = new FireCluster(new ArrayList<EntityID>(), centroid, b, 0,
						agentLogger);
				c.agentLogger = agentLogger;
				c.addNewTarget(b, b.getFieryness(), b.getTemperature(), time,
						BUILDING_LIST.BUILDINGS_ON_FIRE);
				agentLogger.debug("report fire: " + b.getID());
				if (radioChannels.size() > 0)
					reportFire(b, time, 0);
				else {
					Comm.reportVoice(
							new VoiceMessage(Comm.fireMessageVoice(
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
											.ordinal(), b, allEntities), 2,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
				c.radius = 0;
				fireGroups.add(c);

			}
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							Comm.MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
									.ordinal(), b, allEntities), 2, 1,
							MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.name()),
					voiceMessages, comparator);
		}
		for (Building b : newExtinguishedFire) {
			FireCluster c = FireCluster.getClosestCluster(b.getID(),
					fireGroups, model);
			if (c != null && c.isCloseToCluster(b.getID(), model)) {
				if (c.addNewTarget(b, b.getFieryness(), b.getTemperature(),
						time, BUILDING_LIST.BUILDINGS_EXTINGUISHED)) {
					if (radioChannels.size() > 0)
						reportExtinguish(b, time, 0);
					{
						Comm.reportVoice(
								new VoiceMessage(Comm.fireMessageVoice(
										MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
												.ordinal(), b, allEntities), 2,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
												.name()), voiceMessages,
								comparator);
					}
					agentLogger.debug("report extinguished: " + b.getID());

				}
			}

			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							Comm.MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
									.ordinal(), b, allEntities), 2, 1,
							MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.name()),
					voiceMessages, comparator);

		}
		for (Building b : newCollapsedBuildings) {

			for (FireCluster fc : fireGroups)
				if (fc.addNewTarget(b, b.getFieryness(), b.getTemperature(),
						time, BUILDING_LIST.BUILDINGS_COLLAPSED)) {
					agentLogger.debug("report collapse: " + b.getID());

					if (radioChannels.size() > 0)
						reportCollapse(b, time, 0);
					else {
						Comm.reportVoice(
								new VoiceMessage(
										Comm.fireMessageVoice(
												MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
														.ordinal(), b,
												allEntities),
										2,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
												.name()), voiceMessages,
								comparator);
					}

				}

			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							Comm.MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
									.ordinal(), b, allEntities), 2, 1,
							MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.name()),
					voiceMessages, comparator);

		}

		if (currentFireGroup != null) {
			Think.removeBuildingsFromEntityIDs(
					currentFireGroup.buildingsExpectedToBeOnFire,
					newBuildingsOnFire);
			Think.removeBuildingsFromEntityIDs(
					currentFireGroup.buildingsExpectedToBeOnFire,
					newBuildingUnburnt);
			Think.removeBuildingsFromEntityIDs(
					currentFireGroup.buildingsExpectedToBeOnFire,
					newBuildingWarm);
			Think.removeBuildingsFromEntityIDs(
					currentFireGroup.buildingsExpectedToBeOnFire,
					newCollapsedBuildings);
			Think.removeBuildingsFromEntityIDs(
					currentFireGroup.buildingsExpectedToBeOnFire,
					newExtinguishedFire);

		}
		// remove building not on fire from ClustersCopy, and collapsedBuildings
		// from allClusters
		removeBuildingsFromClusters(changedBuildings, newCollapsedBuildings,
				CurrentClusterCopy, clusters);
		for (FireCluster fc : fireGroups) {
			fc.updateCentroid(model);
			fc.updateRadius(model);

		}
		agentLogger.debug("number of fires" + fireGroups.size());
		for (int i = 0; i < fireGroups.size(); i++) {
			FireCluster fc = fireGroups.get(i);
			if (fc != null && fc.buildingsOnFireHeating.size() == 0
					&& fc.buildingsOnFireBurning.size() == 0
					&& fc.buildingsOnFireInferno.size() == 0
					&& fc.buildingsWarm.size() == 0
					&& fc.buildingsExpectedToBeOnFire.size() == 0) {
				fireGroups.remove(i--);
				if (fc == currentFireGroup) {
					agentLogger.debug("CURRENT FIRE CLUSTER");
					currentFireGroup = null;
				}
				agentLogger.debug("time: " + time + " FIRE CLUSTER EMPTY"
						+ fc.center.getID());

			}
		}
		if (currentFireGroup == null) {
			FireCluster fc = FireCluster.getClosestCluster(location().getID(),
					fireGroups, model);
			if (fc != null) {
				double threshold = Think.thresholdDistance(
						(int) numberOfAgents / 2, fc.center, clusters);
				double threshold2 = Think.thresholdDistance(5, fc.center,
						clusters);

				boolean far = model.getDistance(fc.center,
						clusters.get(initialClusterIndex).center) > threshold
						&& model.getDistance(fc.center, location()) > threshold2;
				if (!far) {
					agentLogger.debug("Assigned to FireCluster Close"
							+ fc.center.getID());
					currentFireGroup = fc;
				} else {
					agentLogger.debug("FireCluster Far" + fc.center.getID());
				}
			}
		} else {

			FireCluster fc = FireCluster.getClosestCluster(location().getID(),
					fireGroups, model);
			if (fc != currentFireGroup) {
				currentFireGroup = fc;
				agentLogger.debug("founf closer Far" + fc.center.getID());

			}
		}

		for (FireCluster fc : fireGroups) {
			if (fc == currentFireGroup)
				agentLogger.debug("CURRENT FIRE CLUSTER");
			fc.printCluster();
		}
		for (int i = 0; i < fireGroups.size(); i++) {
			for (int j = i + 1; j < fireGroups.size(); j++) {

				if (model.getDistance(fireGroups.get(i).center,
						fireGroups.get(j).center) < Math.max(
						fireGroups.get(i).radius, fireGroups.get(j).radius)) {
					agentLogger.debug("CLUSTERS WILL BE MERGED "
							+ fireGroups.get(i).center + ", "
							+ fireGroups.get(j).center);
					FireCluster.mergeClusters(fireGroups.get(j),
							fireGroups.get(i));
					fireGroups.remove(j--);

					fireGroups.get(j).updateCentroid(model);
					fireGroups.get(j).updateRadius(model);
				}
			}
		}
		for (Building b : newVoiceBuildingWarm) {
			if (!Think.exists(b, newBuildingsOnFire)
					&& !Think.exists(b, newCollapsedBuildings)
					&& !Think.exists(b, newExtinguishedFire))
				Think.addIfNotExists(b, newBuildingWarm);
		}
		newVoiceBuildingWarm.clear();

		for (Building b : newVoiceBuildingsOnFire) {
			if (!Think.exists(b, newBuildingWarm)
					&& !Think.exists(b, newCollapsedBuildings)
					&& !Think.exists(b, newExtinguishedFire))
				Think.addIfNotExists(b, newBuildingsOnFire);
		}
		newVoiceBuildingsOnFire.clear();
	}

	protected void removeBuildingsFromClusters(
			ArrayList<Building> changedBuildings,
			ArrayList<Building> collapsedBuildings, Cluster copy,
			ArrayList<Cluster> permanent) {
		Think.removeBuildingsFromEntityIDs(copy.cluster, changedBuildings);
		Think.removeBuildingsFromEntityIDs(
				permanent.get(initialClusterIndex).cluster, collapsedBuildings);
	}

	protected ArrayList<EntityID> getNeighbouringBuilding(Building building) {
		ArrayList<EntityID> neighbouringBuilding = new ArrayList<EntityID>();
		for (EntityID b : building.getNeighbours()) {
			if (model.getEntity(b) instanceof Building) {
				neighbouringBuilding.add(b);
			}
		}
		return neighbouringBuilding;
	}

	private void reportFire(Building b, int time, int clusterIndex) {

		sendSpeak(time, fireChannel,
				Comm.fireMessage(b, time, clusterIndex, allEntities));
		Think.addIfNotExists(
				new AKSpeak(me().getID(), time, fireChannel, Comm.fireMessage(
						b, time, clusterIndex, allEntities)), dropped);

		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.fireMessage(b, time, clusterIndex, allEntities));

			if (fireChannel != policeChannel
					&& ambulanceChannel != policeChannel) {
				sendSpeak(time, policeChannel,
						Comm.fireMessage(b, time, clusterIndex, allEntities));
			}

		}
	}

	private void reportCollapse(Building b, int time, int clusterIndex) {
		sendSpeak(time, fireChannel,
				Comm.CollapsedMessage(b, time, clusterIndex, allEntities));
		Think.addIfNotExists(
				new AKSpeak(me().getID(), time, fireChannel, Comm
						.CollapsedMessage(b, time, clusterIndex, allEntities)),
				dropped);
		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.CollapsedMessage(b, time, clusterIndex, allEntities));
		}
		if (fireChannel != policeChannel && ambulanceChannel != policeChannel) {
			sendSpeak(time, policeChannel,
					Comm.CollapsedMessage(b, time, clusterIndex, allEntities));
		}
	}

	private void reportExtinguish(Building b, int time, int clusterIndex) {

		sendSpeak(time, fireChannel,
				Comm.extinguishedFire(b, time, clusterIndex, allEntities));
		Think.addIfNotExists(
				new AKSpeak(me().getID(), time, fireChannel, Comm
						.extinguishedFire(b, time, clusterIndex, allEntities)),
				dropped);
		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.extinguishedFire(b, time, clusterIndex, allEntities));
		}
		if (fireChannel != policeChannel && ambulanceChannel != policeChannel) {
			sendSpeak(time, policeChannel,
					Comm.extinguishedFire(b, time, clusterIndex, allEntities));
		}
	}

	private void reportWarm(Building b, int time, int clusterIndex) {

		sendSpeak(time, fireChannel,
				Comm.warmBuilding(b, time, clusterIndex, allEntities));
		Think.addIfNotExists(
				new AKSpeak(me().getID(), time, fireChannel, Comm.warmBuilding(
						b, time, clusterIndex, allEntities)), dropped);
		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.warmBuilding(b, time, clusterIndex, allEntities));
		}
		if (fireChannel != policeChannel && ambulanceChannel != policeChannel) {
			sendSpeak(time, policeChannel,
					Comm.warmBuilding(b, time, clusterIndex, allEntities));
		}
	}

	private void reportBlockade(int time) {
		List<EntityID> otherPath = graphHelper.getSearch().breadthFirstSearch(
				me().getPosition(), previousPath.get(previousPath.size() - 1));
		if (otherPath == null || otherPath.size() == 0) {
			agentLogger.debug("time: " + time + " reporting blockade in road: "
					+ previousPath.get(0));
			if (radioChannels.size() > 0) {
				sendSpeak(time, policeChannel, Comm.blockedRoadWithPriority(
						previousPath.get(0), priorityStuck.ordinal(),
						allEntities));

			} else
				Comm.reportVoice(
						new VoiceMessage(Comm.blockedRoadWithPriority(
								previousPath.get(0), priorityStuck.ordinal(),
								allEntities), 3, TIME_TO_LIVE_VOICE,
								MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.name()),
						voiceMessages, comparator);
		}
	}

	private void reportBlockadetoAll(int time) {
		List<EntityID> otherPath = graphHelper.getSearch().breadthFirstSearch(
				me().getPosition(), previousPath.get(previousPath.size() - 1));
		if (otherPath == null || otherPath.size() == 0) {
			agentLogger.debug("time: " + time + " reporting blockade in road: "
					+ previousPath.get(0));
			if (radioChannels.size() > 0) {
				sendSpeak(time, policeChannel, Comm.blockedRoadWithPriority(
						previousPath.get(0), priorityStuck.ordinal(),
						allEntities));

				sendSpeak(time, ambulanceChannel,
						Comm.blockedRoad(previousPath.get(0), allEntities));
				if (fireChannel != ambulanceChannel)
					sendSpeak(time, fireChannel,
							Comm.blockedRoad(previousPath.get(0), allEntities));

			} else {
				Comm.reportVoice(
						new VoiceMessage(Comm.blockedRoadWithPriority(
								previousPath.get(0), priorityStuck.ordinal(),
								allEntities), 3, TIME_TO_LIVE_VOICE,
								MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.name()),
						voiceMessages, comparator);
				Comm.reportVoice(
						new VoiceMessage(Comm.blockedRoad(previousPath.get(0),
								allEntities), 3, TIME_TO_LIVE_VOICE,
								MESSAGE_ID.BLOCKED_ROAD_MESSAGE_ID.name()),
						voiceMessages, comparator);
			}
		}
	}

	private void reportBlockadeVoice(int time) {
		List<EntityID> otherPath = graphHelper.getSearch().breadthFirstSearch(
				me().getPosition(), previousPath.get(previousPath.size() - 1));
		if (otherPath == null || otherPath.size() == 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.blockedRoadWithPriority(
							previousPath.get(0), priorityStuck.ordinal(),
							allEntities), 3, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.name()),
					voiceMessages, comparator);

		}
	}

	protected void handleEmptyClusterCopy(ChangeSet arg1, int time) {

		if (CurrentClusterCopy.cluster.size() == 0) {
			agentLogger.debug(getID() + " Cluster is empty");

			CurrentClusterCopy = Think.copyCluster(currentCluster);
			if (CurrentClusterCopy.cluster.size() == 0) {
				agentLogger.debug(getID() + " Cluster is still empty");
				int clusterIndex = Clustering
						.getClosestNotEmptyClusterIndexFromClusters(location()
								.getID(), clusters, model);
				if (clusterIndex != -1) {
					currentCluster = clusters.get(clusterIndex);
					CurrentClusterCopy = Think.copyCluster(currentCluster);
					agentLogger.debug(getID() + " new Cluster");
				}
			}
		}
	}

	protected boolean waterTankPart(int time, ChangeSet arg1) {
		// agent has finished filling its tank, therefore agent is not stuck
		if (finishedFillingWater(time)) {
			lastPosition = null;
			previousPath = null;
		}
		if (isOutOfWater()) { // Are we out
			// of
			// water?
			// Head for a refuge
			agentLogger.debug("is out of water");
			path = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), refuges);

			if (path == null || path.size() == 0) {
				System.out.println("restore graph");
				surroundedByBlockades(time, arg1);
				restoreGraph();
				path = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), refuges);

			}
			priorityStuck = STUCK_PRIORITY.GOING_TO_REFUGE;
			previousPath = path;
			agentLogger.debug("going to refuge: " + path.get(path.size() - 1));

			sendMove(time, path);
			return true;
		}

		return isFillingWater(time);
	}

	protected boolean isFillingWater(int time) {
		if ((location() instanceof Refuge) && me().getWater() < maxWater) {
			lastPosition = null;
			previousPath = null;
			agentLogger.debug("is filling water");

			// sendRest(time);
			return true;
		}
		return false;
	}

	protected boolean isOutOfWater() {
		return me().isWaterDefined() && me().getWater() == 0
				&& (!(location() instanceof Refuge));
	}

	protected boolean finishedFillingWater(int time) {
		if ((location() instanceof Refuge) && me().getWater() == maxWater)
			return true;
		return false;
	}

	protected boolean extinguishFire2(ChangeSet arg1, int time) {
		Building sameTarget = null;

		for (Building b : Think.buildingsInRangeBuildings(newBuildingWarm,
				model, (Human) me(), maxDistance)) {

			if (b instanceof GasStation) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing GasStation: " + b.getID());
				break;
			}
		}
		if (sameTarget == null)
			for (Building b : Think.buildingsInRangeBuildings(
					newBuildingsOnFire, model, (Human) me(), maxDistance)) {

				if (b instanceof GasStation) {
					sameTarget = b;
					agentLogger
							.debug("Extinghuishing GasStation: " + b.getID());
					break;
				}
			}
		for (Building b : Think.getBurningBuildings(model, arg1)) {

			if (b.getID().getValue() == location().getID().getValue()) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing Building same location: "
						+ b.getID());
				break;
			}
		}

		if (sameTarget == null)
			for (Building b : Think.buildingsInRangeBuildings(newBuildingWarm,
					model, (Human) me(), maxDistance)) {
				if (b.getFierynessEnum() != Fieryness.UNBURNT)
					continue;
				sameTarget = b;
				agentLogger
						.debug("Extinghuishing Building warm not extinguished: "
								+ b.getID());
				break;
			}

		if (sameTarget == null)
			for (Building b : Think.filter(Think.buildingsInRangeBuildings(
					newBuildingsOnFire, model, (Human) me(), maxDistance),
					FireCluster.predicateBuildingFireynessHeating)) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing Building HEATING : "
						+ b.getID());

				break;
			}
		if (sameTarget == null)
			for (Building b : Think.filter(Think.buildingsInRangeBuildings(
					newBuildingsOnFire, model, (Human) me(), maxDistance),
					FireCluster.predicateBuildingFireynessBurning)) {
				agentLogger.debug("Extinghuishing Building BURNING : "
						+ b.getID());

				sameTarget = b;
				break;
			}

		if (sameTarget == null)
			for (Building b : Think.filter(Think.buildingsInRangeBuildings(
					newBuildingsOnFire, model, (Human) me(), maxDistance),
					FireCluster.predicateBuildingFireynessInferno)) {
				agentLogger.debug("Extinghuishing Building INFERNO : "
						+ b.getID());

				sameTarget = b;
				break;
			}
		if (sameTarget == null)
			for (Building b : Think.buildingsInRangeBuildings(newBuildingWarm,
					model, (Human) me(), maxDistance)) {

				sameTarget = b;
				agentLogger
						.debug("Extinghuishing Building WARM : " + b.getID());

				break;
			}

		if (sameTarget != null) {
			priorityStuck = STUCK_PRIORITY.GOING_TO_FIRE;
			sendExtinguish(time, sameTarget.getID(),
					Math.min(maxPower, me().getWater()));
			for (FireCluster fc : fireGroups) {
				if (fc.belongsToCluster(sameTarget.getID())) {
					if (currentFireGroup != fc) {
						currentFireGroup = fc;
						currentFireGroup.addExpectedFires(model, me(),
								sameTarget);

						agentLogger.debug("changeCluster to center: "
								+ fc.center + " centroid" + fc.centroid[0]
								+ "," + fc.centroid[1]);

						break;
					}

				}
			}
			lastPosition = null;
			previousPath = null;
			return true;
		}
		if (isOutOfWater() || isFillingWater(time))
			return true;
		if (currentFireGroup != null) {
			priorityStuck = STUCK_PRIORITY.GOING_TO_FIRE;

			path = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), currentFireGroup.buildingsWarm);
			agentLogger.debug("Trying to go to warm Building");

			if (path == null || path.size() == 0) {

				path = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentFireGroup.buildingsOnFireHeating);
				agentLogger.debug("Trying to go to HEATING Building");

				if (path == null || path.size() == 0) {
					path = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(),
							currentFireGroup.buildingsOnFireBurning);
					agentLogger.debug("Trying to go to BURNING Building");

					if (path == null || path.size() == 0) {
						path = graphHelper.getSearch().breadthFirstSearch(
								me().getPosition(),
								currentFireGroup.buildingsExpectedToBeOnFire);
						agentLogger.debug("Trying to go to EXPECTED Building");

					}
				}
			}
			if (path != null && path.size() > 1) {

				agentLogger.debug("Going to building: "
						+ path.get(path.size() - 1));

				path.remove(path.size() - 1);
				sendMove(time, path);
				previousPath = path;
				return true;
			} else {
				if (currentFireGroup.buildingsWarm.size() != 0
						|| currentFireGroup.buildingsOnFireHeating.size() != 0
						|| currentFireGroup.buildingsOnFireBurning.size() != 0
						|| currentFireGroup.buildingsOnFireInferno.size() != 0) {
					agentLogger.debug("List is not empty ");

				}
				agentLogger.debug("Failed Going  to building: ");
			}

		}

		return false;

	}

	protected boolean extinguishFire3(ChangeSet arg1, int time) {
		Building sameTarget = null;

		for (Building b : Think.getBurningBuildings(model, arg1)) {

			if (b instanceof GasStation) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing GasStation: " + b.getID());
				break;
			}
		}

		for (Building b : Think.getBurningBuildings(model, arg1)) {

			if (b.getID().getValue() == location().getID().getValue()) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing Building same location: "
						+ b.getID());
				break;
			}
		}

		if (sameTarget == null)
			for (Building b : Think.buildingsInRangeBuildings(newBuildingWarm,
					model, (Human) me(), maxDistance)) {
				if (b.getFierynessEnum() != Fieryness.UNBURNT)
					continue;
				sameTarget = b;
				agentLogger
						.debug("Extinghuishing Building warm not extinguished: "
								+ b.getID());
				break;
			}

		if (sameTarget == null)
			for (Building b : Think.filter(Think.buildingsInRangeBuildings(
					newBuildingsOnFire, model, (Human) me(), maxDistance),
					FireCluster.predicateBuildingFireynessHeating)) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing Building HEATING : "
						+ b.getID());

				break;
			}
		if (sameTarget == null)
			for (Building b : Think.filter(Think.buildingsInRangeBuildings(
					newBuildingsOnFire, model, (Human) me(), maxDistance),
					FireCluster.predicateBuildingFireynessBurning)) {
				agentLogger.debug("Extinghuishing Building BURNING : "
						+ b.getID());

				sameTarget = b;
				break;
			}
		if (!isFillingWater(time)) {
			if (sameTarget == null)
				if (currentFireGroup != null) {
					path = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(),
							currentFireGroup.buildingsOnFireHeating);
					agentLogger.debug("Trying going to heating building ");
					if (path == null || path.size() == 0) {
						path = graphHelper.getSearch().breadthFirstSearch(
								me().getPosition(),
								currentFireGroup.buildingsOnFireBurning);
						agentLogger.debug("Trying going to burning building ");

					}
					if (path != null && path.size() > 0) {
						agentLogger.debug("path to building on fire"
								+ path.get(path.size() - 1));

						path.remove(path.size() - 1);
						sendMove(time, path);
						previousPath = path;
						return true;
					}
				}

			if (sameTarget == null)
				for (Building b : Think.filter(Think.buildingsInRangeBuildings(
						newBuildingsOnFire, model, (Human) me(), maxDistance),
						FireCluster.predicateBuildingFireynessInferno)) {
					agentLogger.debug("Extinghuishing Building INFERNO : "
							+ b.getID());

					sameTarget = b;
					break;
				}
			if (sameTarget == null)
				for (Building b : Think.buildingsInRangeBuildings(
						newBuildingWarm, model, (Human) me(), maxDistance)) {

					sameTarget = b;
					agentLogger.debug("Extinghuishing Building WARM : "
							+ b.getID());

					break;
				}
		}

		if (sameTarget != null) {
			priorityStuck = STUCK_PRIORITY.GOING_TO_FIRE;
			sendExtinguish(time, sameTarget.getID(),
					Math.min(maxPower, me().getWater()));
			for (FireCluster fc : fireGroups) {
				if (fc.belongsToCluster(sameTarget.getID())) {
					fc.addExpectedFires(model, me(), sameTarget);
					if (currentFireGroup != fc) {
						currentFireGroup = fc;

						agentLogger.debug("changeCluster to center: "
								+ fc.center + " centroid" + fc.centroid[0]
								+ "," + fc.centroid[1]);

						break;
					}

				}
			}
			lastPosition = null;
			previousPath = null;
			return true;
		}
		if (isFillingWater(time))
			return true;
		if (currentFireGroup != null) {
			priorityStuck = STUCK_PRIORITY.GOING_TO_FIRE;

			path = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), currentFireGroup.buildingsWarm);
			agentLogger.debug("Trying to go to warm Building");

			if (path == null || path.size() == 0) {

				path = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentFireGroup.buildingsExpectedToBeOnFire);
				agentLogger.debug(currentFireGroup.buildingsExpectedToBeOnFire);

				if (path == null || path.size() == 0) {
					path = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(),
							currentFireGroup.buildingsOnFireInferno);
					agentLogger.debug("Trying to go to INFERNO Building");

				}

			}
			if (path != null && path.size() > 1) {

				agentLogger.debug("Going to building: "
						+ path.get(path.size() - 1));

				path.remove(path.size() - 1);
				sendMove(time, path);
				previousPath = path;
				return true;
			} else {
				if (currentFireGroup.buildingsWarm.size() != 0
						|| currentFireGroup.buildingsOnFireHeating.size() != 0
						|| currentFireGroup.buildingsOnFireBurning.size() != 0
						|| currentFireGroup.buildingsOnFireInferno.size() != 0) {
					agentLogger.debug("List is not empty ");

				}
				agentLogger.debug("Failed Going  to building: ");
			}

		}

		return false;

	}

	protected boolean extinguishFire4(ChangeSet arg1, int time) {
		Building sameTarget = null;

		for (Building b : Think.getBurningBuildings(model, arg1)) {

			if (b instanceof GasStation) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing GasStation: " + b.getID());
				break;
			}
		}

		for (Building b : Think.getBurningBuildings(model, arg1)) {

			if (b.getID().getValue() == location().getID().getValue()) {
				sameTarget = b;
				agentLogger.debug("Extinghuishing Building same location: "
						+ b.getID());
				break;
			}
		}
		ArrayList<Building> buildings = Think.buildingsInRangeBuildings(Think
				.appendBuildingsCopy(Think.getBurningBuildings(model, arg1),
						newBuildingWarm), model, (Human) me(), maxDistance);
		buildings = (ArrayList<Building>) Think.filter(buildings,
				new Predicate<Building>() {

					@Override
					public boolean apply(Building object) {
						if (currentFireGroup != null
								&& currentFireGroup.belongsToCluster(object
										.getID()))
							return true;
						return false;
					}

				});

		Collections.sort(buildings, new Comparator<Building>() {

			@Override
			public int compare(Building o1, Building o2) {
				// TODO Auto-generated method stub
				if (model.getDistance(o1, currentFireGroup.center) < model
						.getDistance(o2, currentFireGroup.center))
					return 1;
				else
					return -1;
			}
		});
		if (sameTarget == null)
			for (Building b : buildings) {
				agentLogger.debug("Extinghuishing far building: " + b.getID());
				sameTarget = b;
				break;
			}

		if (!isFillingWater(time)) {
			if (sameTarget == null)
				if (currentFireGroup != null) {
					path = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(),
							currentFireGroup.buildingsOnFireHeating);
					agentLogger.debug("Trying going to heating building ");
					if (path == null || path.size() == 0) {
						path = graphHelper.getSearch().breadthFirstSearch(
								me().getPosition(),
								currentFireGroup.buildingsOnFireBurning);
						agentLogger.debug("Trying going to burning building ");

					}
					if (path != null && path.size() > 0) {
						agentLogger.debug("path to building on fire"
								+ path.get(path.size() - 1));

						path.remove(path.size() - 1);
						sendMove(time, path);
						previousPath = path;
						return true;
					}
				}

			if (sameTarget == null)
				for (Building b : Think.filter(Think.buildingsInRangeBuildings(
						newBuildingsOnFire, model, (Human) me(), maxDistance),
						FireCluster.predicateBuildingFireynessInferno)) {
					agentLogger.debug("Extinghuishing Building INFERNO : "
							+ b.getID());

					sameTarget = b;
					break;
				}
			if (sameTarget == null)
				for (Building b : Think.buildingsInRangeBuildings(
						newBuildingWarm, model, (Human) me(), maxDistance)) {

					sameTarget = b;
					agentLogger.debug("Extinghuishing Building WARM : "
							+ b.getID());

					break;
				}
		}

		if (sameTarget != null) {
			priorityStuck = STUCK_PRIORITY.GOING_TO_FIRE;
			sendExtinguish(time, sameTarget.getID(),
					Math.min(maxPower, me().getWater()));
			for (FireCluster fc : fireGroups) {
				if (fc.belongsToCluster(sameTarget.getID())) {
					fc.addExpectedFires(model, me(), sameTarget);
					if (currentFireGroup != fc) {
						currentFireGroup = fc;

						agentLogger.debug("changeCluster to center: "
								+ fc.center + " centroid" + fc.centroid[0]
								+ "," + fc.centroid[1]);

						break;
					}

				}
			}
			lastPosition = null;
			previousPath = null;
			return true;
		}
		if (isFillingWater(time))
			return true;
		if (currentFireGroup != null) {
			priorityStuck = STUCK_PRIORITY.GOING_TO_FIRE;

			path = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), currentFireGroup.buildingsWarm);
			agentLogger.debug("Trying to go to warm Building");

			if (path == null || path.size() == 0) {

				path = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentFireGroup.buildingsExpectedToBeOnFire);
				agentLogger.debug(currentFireGroup.buildingsExpectedToBeOnFire);

				if (path == null || path.size() == 0) {
					path = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(),
							currentFireGroup.buildingsOnFireInferno);
					agentLogger.debug("Trying to go to INFERNO Building");

				}

			}
			if (path != null && path.size() > 1) {

				agentLogger.debug("Going to building: "
						+ path.get(path.size() - 1));

				path.remove(path.size() - 1);
				sendMove(time, path);
				previousPath = path;
				return true;
			} else {
				if (currentFireGroup.buildingsWarm.size() != 0
						|| currentFireGroup.buildingsOnFireHeating.size() != 0
						|| currentFireGroup.buildingsOnFireBurning.size() != 0
						|| currentFireGroup.buildingsOnFireInferno.size() != 0) {
					agentLogger.debug("List is not empty ");

				}
				agentLogger.debug("Failed Going  to building: ");
			}

		}

		return false;

	}

	protected void restoreGraph() {
		graphHelper.resetGraph();
		CurrentClusterCopy = Think.copyCluster(currentCluster);
	}

	protected void removeTotallyBlockedRoads(int time, ChangeSet changeSet) {
		// remove totally blocked Roads for the graph
		for (Road r : Think.TotallyBlockedRoads(model, changeSet)) {
			if (previousPath != null && Think.exists(r.getID(), previousPath)
					&& !Think.roadOnlyEntranceToBuilding(r, model))
				if (graphHelper.removeNode(r.getID())) {
					agentLogger.debug("new totally blocked road: " + r.getID());
					reportBlockadetoAll(time);
				}
		}
	}

	protected void addTotallyClearedRoads(int time, ChangeSet changeSet) {
		// add totally cleared Roads to the graph
		ArrayList<Road> clearRoads = Think.getClearedRoadsNew(model, changeSet);
		for (Road r : clearRoads) {
			if (me().getPosition().getValue() != r.getID().getValue()) {
				List<EntityID> pathToClearRoad = graphHelper.getSearch()
						.breadthFirstSearch(me().getPosition(), r.getID());
				if (pathToClearRoad == null)
					continue;
				boolean clear = true;
				pathToClearRoad.add(0, me().getPosition());
				for (EntityID step : pathToClearRoad) {
					if (model.getEntity(step) instanceof Road) {
						if (!Think.exists((Road) model.getEntity(step),
								clearRoads)) {
							clear = false;
							break;
						}
					}
				}
				if (!clear)
					continue;
			}

			for (EntityID e : r.getNeighbours()) {

				if (model.getEntity(e) instanceof Building)
					if (graphHelper.restoreNode(e)) {
						agentLogger
								.debug("new totally cleared building blocked: "
										+ e);
						if (radioChannels.size() > 0) {
							sendSpeak(time, fireChannel,
									Comm.clearedRoad(e, allEntities));
							Think.addIfNotExists(
									new AKSpeak(me().getID(), time,
											fireChannel, Comm.clearedRoad(e,
													allEntities)), dropped);

							if (ambulanceChannel != fireChannel) {
								sendSpeak(time, ambulanceChannel,
										Comm.clearedRoad(e, allEntities));

							}
							if (fireChannel != policeChannel
									&& ambulanceChannel != policeChannel) {
								sendSpeak(time, policeChannel,
										Comm.clearedRoad(e, allEntities));
							}
						} else {

						}
					}
			}

			if (graphHelper.restoreNode(r.getID())) {
				agentLogger.debug("new totally cleared road " + r.getID());
				if (radioChannels.size() > 0) {
					sendSpeak(time, fireChannel,
							Comm.clearedRoad(r.getID(), allEntities));
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, fireChannel, Comm
									.clearedRoad(r.getID(), allEntities)),
							dropped);

					if (ambulanceChannel != fireChannel) {
						sendSpeak(time, ambulanceChannel,
								Comm.clearedRoad(r.getID(), allEntities));

					}
					if (fireChannel != policeChannel
							&& ambulanceChannel != policeChannel) {
						sendSpeak(time, policeChannel,
								Comm.clearedRoad(r.getID(), allEntities));
					}
				} else {

				}
			}
		}
	}

	/**
	 * Noha Khater
	 */
	private void reportLocationOfCivilians(int time, ChangeSet changeset,
			StandardWorldModel model, int channel) {
		ArrayList<EntityID> otherAmbulanceAgents = new ArrayList<EntityID>();
		for (Human c : getSeenTargets(changeset)) {
			if (!Think.exists(c.getPosition(), buriedCiviliansReported)
					&& model.getEntity(c.getPosition()) instanceof Building) {
				otherAmbulanceAgents = Think.AmbulanceAgentsInSameBuilding(
						model, changeset, getID(), c.getPosition());
				if (otherAmbulanceAgents.size() == 0
						|| (otherAmbulanceAgents.size() > 0 && c
								.getBuriedness() / otherAmbulanceAgents.size() <= 5)
						|| c.getHP() >= 2000) {
					Think.addIfNotExists(c.getPosition(),
							buriedCiviliansReported);
					if (radioChannels.size() > 0) {
						Comm.reportVoice(
								new VoiceMessage(Comm.civilianLocationBuried(
										c.getPosition(), allEntities), 1, 1,
										MESSAGE_ID.CIVILIAN_LOCATION_BURIED
												.name()), voiceMessages,
								comparator);
						sendSpeak(time, ambulanceChannel,
								Comm.civilianLocationBuried(c.getPosition(),
										allEntities));
					} else {
						Comm.reportVoice(
								new VoiceMessage(Comm.civilianLocationBuried(
										c.getPosition(), allEntities), 1,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.CIVILIAN_LOCATION_BURIED
												.name()), voiceMessages,
								comparator);
					}
				}
			}
		}
	}

	private List<Human> getSeenTargets(ChangeSet set) {
		List<Human> targets = new ArrayList<Human>();

		for (Human human : getBuriedTargets(set))
			targets.add(human);
		Collections.sort(targets, new Comparator<Human>() {

			@Override
			public int compare(Human arg0, Human arg1) {
				// TODO Auto-generated method stub
				if (arg0.getHP() > arg1.getHP())
					return 1;
				if (arg0.getHP() < arg1.getHP())
					return -1;
				return 0;
			}
		});
		// Collections.sort(targets, new DistanceSorter(location(), model));
		return targets;
	}

	private List<Human> getBuriedTargets(ChangeSet set) {
		List<Human> buriedTargets = new ArrayList<Human>();

		for (Human target : Think.getChangedHumans(model, set)) {

			if (target.getID().getValue() == getID().getValue())
				continue;
			if (target.isHPDefined() && target.isBuriednessDefined()
					&& target.isPositionDefined() && target.getHP() > 0
					&& target.getBuriedness() > 0)
				buriedTargets.add(target);
		}

		Collections.sort(buriedTargets, new DistanceSorter(location(), model));

		return buriedTargets;
	}

	private boolean surroundedByBlockades(int time, ChangeSet changeSet) {
		boolean isStuck = true;
		Cluster closestCluster = Clustering.getClosestClusterFromClusters(me()
				.getPosition(), clusters, model);
		for (Cluster cluster : clusters) {
			if (cluster.center.getID().getValue() == closestCluster.center
					.getID().getValue()) {
				continue;
			}
			List<EntityID> pathToCluster = graphHelper.getSearch()
					.breadthFirstSearch(me().getPosition(),
							cluster.center.getID());
			if (pathToCluster != null && !pathToCluster.isEmpty()) {
				isStuck = false;
				break;
			}
		}
		if (isStuck) {
			Blockade closestBlockade = getclosestBlockade(changeSet);
			if (closestBlockade != null) {
				if (radioChannels.size() > 0) {

					sendSpeak(time, policeChannel, Comm.stuckInsideBlockade(
							me().getPosition(), closestBlockade.getID(),
							allEntities));

				} else {
					Comm.reportVoice(
							new VoiceMessage(Comm.stuckInsideBlockade(me()
									.getPosition(), closestBlockade.getID(),
									allEntities), 1, TIME_TO_LIVE_VOICE,
									MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
							voiceMessages, comparator);
				}
				agentLogger.debug("time: " + time + " agent: " + getID()
						+ " surrounded by blockades");
			}
		}
		return isStuck;
	}

	private Blockade getclosestBlockade(ChangeSet changeSet) {
		ArrayList<Blockade> seenBlockades = Think
				.getBlockades(model, changeSet);
		if (!seenBlockades.isEmpty()) {
			Collections.sort(seenBlockades, new DistanceSorter(location(),
					model));
			return seenBlockades.get(0);
		}
		return null;
	}

}