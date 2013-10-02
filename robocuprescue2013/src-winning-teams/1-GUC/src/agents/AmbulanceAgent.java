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
import java.util.PriorityQueue;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
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
import Think.Think;
import clustering.Cluster;
import clustering.Clustering;
import clustering.Density;
import clustering.FireCluster;

public class AmbulanceAgent extends StandardAgent<AmbulanceTeam> {
	public static final String PLAN_FORMAT = ".GUC_ARTSAPIENCE";
	public static final String PLAN_PREFIX = "AmbulanceTeamPlan";
	public static final String PLAN_DIRECTORY = "precompute/";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";

	private static int counter = 0; // Keep track of how many ambulances were
									// created so

	// Lists of Building IDs and refuge IDs

	private ArrayList<EntityID> refugeIDs; // List if Refuge IDs present in map

	private GraphHelper graphHelper;
	// entity's neighbors as values
	boolean stuck = false; // Indicates whether a Blockade ahead is blocking the
							// ambulance's way

	private ArrayList<Cluster> clusters; // List of all Clusters
	private Cluster currentCluster; // Cluster of Buildings IDs
	// present in agent cluster
	// Current Cluster index in addition to lists of messages in and out of the
	// cluster
	private int clusterIndex;

	ArrayList<EntityID> voiceCiviliansHelp = new ArrayList<EntityID>();
	ArrayList<EntityID> voiceCiviliansOuch = new ArrayList<EntityID>();

	ArrayList<EntityID> reportedHelp = new ArrayList<EntityID>();
	ArrayList<EntityID> reportedOuch = new ArrayList<EntityID>();

	ArrayList<VoiceChannel> voiceChannels = new ArrayList<VoiceChannel>();
	ArrayList<RadioChannel> radioChannels = new ArrayList<RadioChannel>();
	int canSubscribeChannels;

	private List<EntityID> currentPath, previousPath;

	private int ignoreAgentCommand;

	private EntityID lastPosition;

	private int lastPositionX, lastPositionY;

	private Human rescuedTarget;
	private boolean isRescue;

	/*****************************************************************/
	ArrayList<EntityID> reportedTargetsLocation = new ArrayList<EntityID>();
	ArrayList<EntityID> emptyBuildings = new ArrayList<EntityID>();
	ArrayList<Human> previouslySeenTargets = new ArrayList<Human>();
	ArrayList<EntityID> previouslyRescuedTargets = new ArrayList<EntityID>();
	ArrayList<EntityID> collapsedOrBurningBuildings = new ArrayList<EntityID>();

	public static enum STUCK_PRIORITY {
		GOING_TO_TARGET, GOING_TO_REFUGE, NORMAL
	};

	/*****************************************************************/

	private int policeChannel, fireChannel, ambulanceChannel;

	int numberOfAgents;

	private static final int TIME_TO_LIVE_VOICE = 10;

	private Logger agentLogger;

	private ArrayList<AKSpeak> dropped = new ArrayList<AKSpeak>();
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

	ArrayList<StandardEntity> allEntities = new ArrayList<StandardEntity>();

	ArrayList<Building> newBuildingWarm = new ArrayList<Building>();
	ArrayList<Building> newBuildingsOnFire = new ArrayList<Building>();
	ArrayList<Building> newCollapsedBuildings = new ArrayList<Building>();
	ArrayList<Building> newExtinguishedFire = new ArrayList<Building>();
	FireCluster fires;
	STUCK_PRIORITY priorityStuck = STUCK_PRIORITY.NORMAL;
	boolean precompute = false;

	public AmbulanceAgent(int preCompute) {
		super();
		System.out.println("Ambulance Agent " + counter++ + " created!");
		this.precompute = (preCompute == 1);
	}

	@Override
	public void postConnect() {
		agentLogger = Logger.getLogger("Ambulance Agent "
				+ this.getID().toString() + " logger");
		agentLogger.setLevel(Level.DEBUG);
		FileAppender fileAppender;
		try {
			fileAppender = new FileAppender(new PatternLayout(), "log/"
					+ getCurrentTimeStamp() + "Ambulance-"
					+ this.getID().toString() + ".log");
			agentLogger.removeAllAppenders();
			agentLogger.addAppender(fileAppender);
			agentLogger.debug("======================================");
			agentLogger.debug("Ambulance Agent " + this.getID().toString()
					+ " connected");
			agentLogger.debug("======================================");
			ignoreAgentCommand = config
					.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);
			Clustering c = new Clustering();
			// Get all Ambulance Agents and assign and set number of clusters to
			// be
			// equal to number of agents
			agentLogger.debug("PostConnect");

			ArrayList<StandardEntity> ambulanceAgents = PostConnect
					.getAgentsNotInsideBuildings(
							model,
							model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));

			int numberOfAmbulanceAgents = ambulanceAgents.size();
			numberOfAgents = numberOfAmbulanceAgents;
			int numberOfClusters = numberOfAmbulanceAgents;
			graphHelper = new GraphHelper(model);

			File file = new File(PLAN_DIRECTORY);
			file.mkdirs();
			file = new File(PLAN_DIRECTORY + PLAN_PREFIX + PLAN_FORMAT);
			if (file.exists() && !precompute) {
				System.out.println("File found");
				clusters = PostConnect.readAllTasks(model, PLAN_DIRECTORY
						+ PLAN_PREFIX + PLAN_FORMAT);
			} else {
				System.out.println("File not found");

				clusters = c.KMeansPlusPlus(numberOfClusters, model, 50,
						model.getEntitiesOfType(StandardEntityURN.BUILDING));

				Density.assignAgentsToClusters2(model, clusters,
						PostConnect.getIDs(ambulanceAgents), PLAN_DIRECTORY
								+ PLAN_PREFIX + PLAN_FORMAT, false, true);
			}

			int initialClusterIndex = 0;
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
			if (initialClusterIndex == clusters.size()) {
				System.out.println("not found");

				double shortestDistance = Double.POSITIVE_INFINITY;
				initialClusterIndex = 0;
				int counter = 0;
				for (Cluster cl : clusters) {
					if (Think.getEuclidianDistance(me().getX(), me().getY(),
							cl.centroid[0], cl.centroid[1]) < shortestDistance) {
						shortestDistance = Think.getEuclidianDistance(me()
								.getX(), me().getY(), cl.centroid[0],
								cl.centroid[1]);
						initialClusterIndex = counter;
					}
					counter++;
				}
			}

			clusterIndex = initialClusterIndex;
			currentCluster = clusters.get(clusterIndex); // Get all IDs of
															// buildings in this
															// agent's cluster
			agentLogger.debug("clusterIndex: " + clusterIndex);

			refugeIDs = PostConnect.getIDs(new ArrayList<StandardEntity>(model
					.getEntitiesOfType(StandardEntityURN.REFUGE))); // Get IDs
																	// of
																	// all
																	// refuges
																	// in map

			Comm.discoverChannels(voiceChannels, radioChannels, config);
			String comm = rescuecore2.standard.kernel.comms.ChannelCommunicationModel.PREFIX;
			canSubscribeChannels = config.getIntValue(comm + "max.platoon");
			lastPositionX = me().getX();
			lastPositionY = me().getY();

			fires = new FireCluster(null, null, location(), 0, agentLogger);

			Collection<StandardEntity> tempAll = model.getEntitiesOfType(
					StandardEntityURN.BUILDING, StandardEntityURN.ROAD,
					StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
					StandardEntityURN.AMBULANCE_CENTRE,
					StandardEntityURN.POLICE_OFFICE,
					StandardEntityURN.FIRE_STATION,
					StandardEntityURN.GAS_STATION);
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
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

	protected void thinkWithCommunication(int time, ChangeSet changeSet,
			Collection<Command> heard) {
		model.merge(changeSet); // Update the changed Objects

		if (time < ignoreAgentCommand)
			return;

		if (time == ignoreAgentCommand) {
			if (radioChannels.size() != 0 && canSubscribeChannels > 0) {
				fireChannel = Comm.decideRadioChannel(radioChannels, 'f');
				ambulanceChannel = Comm.decideRadioChannel(radioChannels, 'a');
				policeChannel = Comm.decideRadioChannel(radioChannels, 'p');
				// System.out.println("Will subscribe to: " + ambulanceChannel);
				sendSubscribe(time, ambulanceChannel);
				if (Think.stuckInBlockade(model, changeSet, lastPositionX,
						lastPositionY)) {
					agentLogger
							.debug("time: " + time + " ,stuckInsideBlockade");
					agentLogger.debug("time: " + time
							+ " send stuckInBlockade to policeChannel");

					sendSpeak(time, policeChannel, Comm.stuckInsideBlockade(
							location().getID(), getclosestBlockade(changeSet)
									.getID(), allEntities));

				}
				if (me().getBuriedness() > 0) {
					agentLogger.debug("time: " + time + " ,Buried");
					agentLogger.debug("time: " + time
							+ " send buried to policeChannel");

					sendSpeak(time, ambulanceChannel,
							Comm.buriedAgent(location().getID(), allEntities));
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, ambulanceChannel,
									Comm.buriedAgent(location().getID(),
											allEntities)), dropped);
				}
			} else
				policeChannel = fireChannel = ambulanceChannel = 1;

		} else {
			handleMessages(time, heard);
			resendMessages(time, heard);
		}

		// update the list of burning or collapsed buildings insight
		ArrayList<EntityID> currentCollapsedOrBurningBuildings = Think
				.getCollapsedOrBurningEntityIDs(model, changeSet);

		for (EntityID b : currentCollapsedOrBurningBuildings) {
			if (Think.addIfNotExists(b, collapsedOrBurningBuildings)) {
				for (Cluster cluster : clusters)
					Think.removeIfExists(b, cluster.cluster);
				// remove collapsed or buildings seen from the list of buildings
				// of
				// their corresponding clusters, remove them form the list of
				// reported
				// and previously seen targets

				Think.removeIfExists(b, reportedTargetsLocation);

				for (int i = 0; i < previouslySeenTargets.size(); i++) {
					if (b.getValue() == previouslySeenTargets.get(i)
							.getPosition().getValue()) {
						previouslySeenTargets.remove(i--);
					}
				}
			}
		}

		handleFires(changeSet, time,
				Think.getChangedBuildings(model, changeSet));
		if (currentPath != null)
			agentLogger.debug("time: " + time + " current path: "
					+ Think.ArrayListEntityIDtoString(currentPath));

		agentLogger.debug("time: " + time + " removed nodes: "
				+ graphHelper.removedNodesToString());

		removeTotallyBlockedRoads(time, changeSet);
		addTotallyClearedRoads(time, changeSet);
    
		if (me().getBuriedness() > 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.buriedAgent(location().getID(),
							allEntities), 1, 1,
							MESSAGE_ID.BURIED_AGENT_LOCATION.name()),
					voiceMessages, comparator);
			agentLogger.debug("time: " + time + " ,sendRest");
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));

			sendRest(time);
			return;
		}
		if (Think.stuckInBlockade(model, changeSet, lastPositionX,
				lastPositionY)) {

			agentLogger.debug("time: " + time + " ,sendRest");
			Comm.reportVoice(
					new VoiceMessage(Comm.stuckInsideBlockade(location()
							.getID(), getclosestBlockade(changeSet).getID(),
							allEntities), 1, 1,
							MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
					voiceMessages, comparator);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));

			sendRest(time);
			return;
		}

		// If agent is in a building, and no targets are inside that building
		// remove it from the list
		// because there will be no need to visit it again in the future

		if (location() instanceof Building)
			if (BuildingIsEmpty(location().getID(), changeSet, time)) {
				removeEmptyBuildingFromClusterAndTargetLists(time, location()
						.getID());

				if (isRescue) {
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedBuilding(location()
									.getID(), allEntities), 1, 1,
									MESSAGE_ID.CLEARED_BUILDING.name()),
							voiceMessages, comparator);
					sendSpeak(time, ambulanceChannel, Comm.clearedBuilding(
							location().getID(), allEntities));
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, ambulanceChannel,
									Comm.clearedBuilding(location().getID(),
											allEntities)), dropped);
					Think.addIfNotExists(location().getID(), emptyBuildings);
					isRescue = false;
					agentLogger.debug("Reporting building "
							+ location().getID() + "is empty");
					agentLogger.debug(Think
							.ArrayListEntityIDtoString(emptyBuildings));

				}
			}

		for (EntityID b : getNeighbouringBuilding((Area) location())) {
			if (BuildingIsEmpty(b, changeSet, time)) {

				removeEmptyBuildingFromClusterAndTargetLists(time, b);
				if (isRescue) {
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedBuilding(location()
									.getID(), allEntities), 1,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.CLEARED_BUILDING.name()),
							voiceMessages, comparator);
					sendSpeak(time, ambulanceChannel,
							Comm.clearedBuilding(b, allEntities));
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, ambulanceChannel,
									Comm.clearedBuilding(b, allEntities)),
							dropped);
					Think.addIfNotExists(b, emptyBuildings);
					isRescue = false;
					agentLogger.debug("Reporting neighbouring building "
							+ location().getID() + "is empty");
					agentLogger.debug(Think
							.ArrayListEntityIDtoString(emptyBuildings));

				}

			}
		}

		// If agent finished cluster, move to another cluster
		if (currentCluster.cluster.size() == 0) {
			sendSpeak(time, ambulanceChannel,
					Comm.finishedClusterAmbulance(clusterIndex));
			Think.addIfNotExists(
					new AKSpeak(me().getID(), time, ambulanceChannel, Comm
							.finishedClusterAmbulance(clusterIndex)), dropped);

			int tempCIndex = Clustering
					.getClosestNotEmptyClusterIndexFromClusters(me()
							.getPosition(), clusters, model);

			Comm.finishedClusterAmbulance(clusterIndex);

			if (tempCIndex != -1) {
				currentCluster = clusters.get(tempCIndex);
				agentLogger.debug("time: " + time + " ," + getID()
						+ ", finished cluster: " + clusterIndex + " going to "
						+ tempCIndex);

				System.out.println("time: " + time + " ," + getID()
						+ ", finished cluster: " + clusterIndex + " going to "
						+ tempCIndex);

				clusterIndex = tempCIndex;

			} else {

				agentLogger.debug("time: " + time + " ," + getID()
						+ ", empty cluster");
			}
		}

		// Declare agent as stuck if it hasn't significantly moved since
		// last
		// time step
		if (lastPosition != null
				&& lastPosition.getValue() == me().getPosition().getValue()
				&& Math.hypot(Math.abs(me().getX() - lastPositionX),
						Math.abs(me().getY() - lastPositionY)) < 8000) {

			stuck = true;
		} else {

			stuck = false;
			lastPosition = me().getPosition();

		}

		lastPositionX = me().getX();
		lastPositionY = me().getY();

		if (stuck) {

			if (previousPath != null) {
				Blockade b = getclosestBlockade(changeSet);
				if (model.getEntity(lastPosition) instanceof Building) {

					if (previousPath.size() > 2
							&& (b == null || (b.getPosition().getValue() != previousPath
									.get(0).getValue() && b.getPosition()
									.getValue() != previousPath.get(1)
									.getValue()))) {

						EntityID firstStep = previousPath.get(0);
						EntityID secondStep = previousPath.get(1);
						previousPath.clear();
						previousPath.add(firstStep);
						previousPath.add(secondStep);
						agentLogger
								.debug("time: "
										+ time
										+ " stuck in "
										+ lastPosition.getValue()
										+ " and moving 2 steps in path: "
										+ Think.ArrayListEntityIDtoString(previousPath));

						lastPosition = me().getPosition();
						currentPath = previousPath;
						sendMove(time, previousPath);
						return;
					}
				}

			}
			if (graphHelper.removeNode(previousPath.get(0))) {
				agentLogger.debug("time: " + time
						+ " agent stuck and previous path removed first node"
						+ Think.ArrayListEntityIDtoString(previousPath));

				reportBlockade(time);
			}

			else
				agentLogger.debug("agent stuck and previous path is null");
		}

		/**
		 * -If someone on board: -If at Refuge, "Unload", else plan path and
		 * move to Refuge. -If nobody on board: - If standing at the same
		 * position as target, "Load", else plan path and move to nearest target
		 */
		if (someoneOnBoardTask(time, model, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		// If agent is damaged go to refuge
		if (me().getDamage() > 0) {
			if (!(location() instanceof Refuge)) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), refugeIDs);
				priorityStuck = STUCK_PRIORITY.GOING_TO_REFUGE;

				if (currentPath == null || currentPath.size() == 0) {
					agentLogger.debug("time: " + time
							+ " graph recreated from damaged part");
					surroundedByBlockades(time, changeSet);
					graphHelper.resetGraph();
					removeTotallyBlockedRoads(time, changeSet);

					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), refugeIDs);

					agentLogger.debug("Agent going to refuge");

				}
				previousPath = currentPath;
				sendMove(time, currentPath);
			}
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		// To add all the seen targets before taking any action
		List<Human> currentlySeenTargets = getSeenTargets(changeSet, time);

		agentLogger.debug("Agent's seen targets "
				+ Think.ArrayListHumanstoString(currentlySeenTargets));

		// check if rescuing target is finished then send load
		if (!(location() instanceof Refuge) && location() instanceof Building) {
			ArrayList<EntityID> otherAmbulanceAgents = Think
					.AmbulanceAgentsInSameBuilding(model, changeSet, getID(),
							location().getID());

			if (rescuedTarget != null
					&& rescuedTarget instanceof Civilian
					&& rescuedTarget.getBuriedness() == 0
					&& rescuedTarget.getPosition().getValue() == location()
							.getID().getValue()) {

				boolean isSmallest = true;
				for (EntityID ambulanceAgentID : otherAmbulanceAgents) {
					if (ambulanceAgentID.getValue() < getID().getValue()) {
						isSmallest = false;
						break;
					}
				}

				Think.removeIfExists(
						(Human) model.getEntity(rescuedTarget.getID()),
						previouslySeenTargets);
				Think.addIfNotExists(rescuedTarget.getID(),
						previouslyRescuedTargets);

				if (isSmallest) {
					sendLoad(time, rescuedTarget.getID());
					agentLogger.debug("Agent is loading target  "
							+ rescuedTarget.getID());
					lastPosition = null;
					previousPath = null;
					isRescue = true;
					rescuedTarget = null;
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				rescuedTarget = null;

			}

			for (Human civilian : Think.getChangedCivilians(model, changeSet))
				if (!Think.exists(civilian.getID(), previouslyRescuedTargets)
						&& rescuedTarget != null) {
					if ((Think.exists(civilian.getID(), voiceCiviliansOuch) || civilian
							.getHP() < 10000)) {
						if (civilian.getPosition().getValue() != location()
								.getID().getValue())
							continue;
						if (civilian.getBuriedness() != 0)
							continue;

						boolean isSmallest = true;
						for (EntityID ambulanceAgentID : otherAmbulanceAgents) {
							if (ambulanceAgentID.getValue() < getID()
									.getValue()) {
								isSmallest = false;
								break;
							}
						}
						Think.removeIfExists(
								(Human) model.getEntity(civilian.getID()),
								previouslySeenTargets);
						Think.addIfNotExists(rescuedTarget.getID(),
								previouslyRescuedTargets);

						if (isSmallest) {
							agentLogger
									.debug("Agent is loading target based on voice  "
											+ rescuedTarget.getID());
							sendLoad(time, civilian.getID());
							lastPosition = null;
							previousPath = null;
							isRescue = true;
							rescuedTarget = null;
							sendSpeak(time, 0, Comm.reportAllVoiceMessages(
									voiceChannels.get(0), voiceMessages));
							return;
						}

						rescuedTarget = null;
					}
				}

			if (rescuedTarget != null
					&& !(rescuedTarget instanceof Civilian)
					&& rescuedTarget.getBuriedness() == 0
					&& rescuedTarget.getPosition().getValue() == location()
							.getID().getValue()) {
				boolean isSmallest = true;
				for (EntityID ambulanceAgentID : otherAmbulanceAgents) {
					if (ambulanceAgentID.getValue() < getID().getValue()) {
						isSmallest = false;
						break;
					}
				}

				Think.removeIfExists(
						(Human) model.getEntity(rescuedTarget.getID()),
						previouslySeenTargets);

				if (buildingIsEmptyExceptOneTarget(location().getID(),
						rescuedTarget.getID(), currentlySeenTargets)) {
					Think.addIfNotExists(location().getID(), emptyBuildings);
					agentLogger.debug(Think
							.ArrayListEntityIDtoString(emptyBuildings));

				}
				Think.addIfNotExists(rescuedTarget.getID(),
						previouslyRescuedTargets);

				if (isSmallest) {
					agentLogger
							.debug("Agent reporting that he finished loading target "
									+ rescuedTarget.getID());
					lastPosition = null;
					previousPath = null;
					isRescue = true;
					rescuedTarget = null;
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				rescuedTarget = null;

			}

		}

		updateReportedAndPreviouslySeenTargets(time, currentlySeenTargets,
				changeSet);

		agentLogger.debug("Agent's previously seen targets"
				+ Think.ArrayListHumanstoString(previouslySeenTargets));
		agentLogger.debug("Agent's reported targets locations  "
				+ Think.ArrayListEntityIDtoString(reportedTargetsLocation));
		agentLogger.debug("Agent's empty buildings locations  "
				+ Think.ArrayListEntityIDtoString(emptyBuildings));

		if (currenltySeenTask(time, currentlySeenTargets, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		if (previouslySeenTask(time, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		if (reportedLocationTask(time, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		// ////////////////////////////////////////////////////////////////////////////
		/**
		 * If nothing specific to do now, keep visiting buildings in cluster
		 * until no more need to be visited, then move to the next cluster
		 */
		if (currentClusterTask(time, changeSet))
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
		return;

	}

	protected void thinkWithVoice(int time, ChangeSet changeSet,
			Collection<Command> heard) {
		model.merge(changeSet); // Update the changed Objects

		if (time < ignoreAgentCommand)
			return;

		if (time == ignoreAgentCommand) {
			policeChannel = fireChannel = ambulanceChannel = 0;

		} else {
			handleMessages(time, heard);
		}

		// update the list of burning or collapsed buildings insight
		ArrayList<EntityID> currentCollapsedOrBurningBuildings = Think
				.getCollapsedOrBurningEntityIDs(model, changeSet);

		for (EntityID b : currentCollapsedOrBurningBuildings) {
			if (Think.addIfNotExists(b, collapsedOrBurningBuildings)) {
				for (Cluster cluster : clusters)
					Think.removeIfExists(b, cluster.cluster);
				// remove collapsed or buildings seen from the list of buildings
				// of
				// their corresponding clusters, remove them form the list of
				// reported
				// and previously seen targets

				Think.removeIfExists(b, reportedTargetsLocation);

				for (int i = 0; i < previouslySeenTargets.size(); i++) {
					if (b.getValue() == previouslySeenTargets.get(i)
							.getPosition().getValue()) {
						previouslySeenTargets.remove(i--);
					}
				}
			}
		}

		handleFires(changeSet, time,
				Think.getChangedBuildings(model, changeSet));
		if (currentPath != null)
			agentLogger.debug("time: " + time + " current path: "
					+ Think.ArrayListEntityIDtoString(currentPath));

		agentLogger.debug("time: " + time + " removed nodes: "
				+ graphHelper.removedNodesToString());

		removeTotallyBlockedRoads(time, changeSet);
		addTotallyClearedRoads(time, changeSet);

		if (me().getBuriedness() > 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.buriedAgent(location().getID(),
							allEntities), 1, 1,
							MESSAGE_ID.BURIED_AGENT_LOCATION.name()),
					voiceMessages, comparator);
			agentLogger.debug("time: " + time + " ,sendRest");
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));

			sendRest(time);
			return;
		}
		if (Think.stuckInBlockade(model, changeSet, lastPositionX,
				lastPositionY)) {

			agentLogger.debug("time: " + time + " ,sendRest");
			Comm.reportVoice(
					new VoiceMessage(Comm.stuckInsideBlockade(location()
							.getID(), getclosestBlockade(changeSet).getID(),
							allEntities), 1, 1,
							MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
					voiceMessages, comparator);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));

			sendRest(time);
			return;
		}

		// If agent is in a building, and no targets are inside that building
		// remove it from the list
		// because there will be no need to visit it again in the future

		if (location() instanceof Building)
			if (BuildingIsEmpty(location().getID(), changeSet, time)) {
				removeEmptyBuildingFromClusterAndTargetLists(time, location()
						.getID());

				if (isRescue) {

					Comm.reportVoice(
							new VoiceMessage(Comm.clearedBuilding(location()
									.getID(), allEntities), 1,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.CLEARED_BUILDING.name()),
							voiceMessages, comparator);
					Think.addIfNotExists(location().getID(), emptyBuildings);
					isRescue = false;
					agentLogger.debug("Reporting building "
							+ location().getID() + "is empty");
					agentLogger.debug(Think
							.ArrayListEntityIDtoString(emptyBuildings));

				}
			}

		for (EntityID b : getNeighbouringBuilding((Area) location())) {
			if (BuildingIsEmpty(b, changeSet, time)) {

				removeEmptyBuildingFromClusterAndTargetLists(time, b);
				if (isRescue) {
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedBuilding(location()
									.getID(), allEntities), 1,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.CLEARED_BUILDING.name()),
							voiceMessages, comparator);
					Think.addIfNotExists(b, emptyBuildings);
					isRescue = false;
					agentLogger.debug("Reporting neighbouring building "
							+ location().getID() + "is empty");
					agentLogger.debug(Think
							.ArrayListEntityIDtoString(emptyBuildings));

				}

			}
		}

		// If agent finished cluster, move to another cluster
		if (currentCluster.cluster.size() == 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm
							.finishedClusterAmbulance(clusterIndex), 3,
							TIME_TO_LIVE_VOICE,
							MESSAGE_ID.FINISHED_CLUSTER_AMBULANCE_MESSAGE_ID
									.name()), voiceMessages, comparator);
			int tempCIndex = Clustering
					.getClosestNotEmptyClusterIndexFromClusters(me()
							.getPosition(), clusters, model);

			Comm.finishedClusterAmbulance(clusterIndex);

			if (tempCIndex != -1) {
				currentCluster = clusters.get(tempCIndex);
				agentLogger.debug("time: " + time + " ," + getID()
						+ ", finished cluster: " + clusterIndex + " going to "
						+ tempCIndex);

				System.out.println("time: " + time + " ," + getID()
						+ ", finished cluster: " + clusterIndex + " going to "
						+ tempCIndex);

				clusterIndex = tempCIndex;

			} else {

				agentLogger.debug("time: " + time + " ," + getID()
						+ ", empty cluster");
			}
		}

		// Declare agent as stuck if it hasn't significantly moved since
		// last
		// time step
		if (lastPosition != null
				&& lastPosition.getValue() == me().getPosition().getValue()
				&& Math.hypot(Math.abs(me().getX() - lastPositionX),
						Math.abs(me().getY() - lastPositionY)) < 8000) {

			stuck = true;
		} else {

			stuck = false;
			lastPosition = me().getPosition();

		}

		lastPositionX = me().getX();
		lastPositionY = me().getY();

		if (stuck) {

			if (previousPath != null) {
				Blockade b = getclosestBlockade(changeSet);
				if (model.getEntity(lastPosition) instanceof Building) {

					if (previousPath.size() > 2
							&& (b == null || (b.getPosition().getValue() != previousPath
									.get(0).getValue() && b.getPosition()
									.getValue() != previousPath.get(1)
									.getValue()))) {

						EntityID firstStep = previousPath.get(0);
						EntityID secondStep = previousPath.get(1);
						previousPath.clear();
						previousPath.add(firstStep);
						previousPath.add(secondStep);
						agentLogger
								.debug("time: "
										+ time
										+ " stuck in "
										+ lastPosition.getValue()
										+ " and moving 2 steps in path: "
										+ Think.ArrayListEntityIDtoString(previousPath));

						lastPosition = me().getPosition();
						currentPath = previousPath;
						sendMove(time, previousPath);

						sendSpeak(
								time,
								0,
								Comm.reportAllVoiceMessages(
										voiceChannels.get(0), voiceMessages));
						return;
					}
				}

			}
			if (graphHelper.removeNode(previousPath.get(0))) {
				agentLogger.debug("time: " + time
						+ " agent stuck and previous path removed first node"
						+ Think.ArrayListEntityIDtoString(previousPath));
				reportBlockade(time);

			}

			else
				agentLogger.debug("agent stuck and previous path is null");
		}

		/**
		 * -If someone on board: -If at Refuge, "Unload", else plan path and
		 * move to Refuge. -If nobody on board: - If standing at the same
		 * position as target, "Load", else plan path and move to nearest target
		 */

		if (someoneOnBoardTask(time, model, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		// If damaged go to refuge
		if (me().getDamage() > 0) {
			if (!(location() instanceof Refuge)) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), refugeIDs);
				priorityStuck = STUCK_PRIORITY.GOING_TO_REFUGE;

				if (currentPath == null || currentPath.size() == 0) {
					agentLogger.debug("time: " + time
							+ " graph recreated from damaged part");
					surroundedByBlockades(time, changeSet);
					graphHelper.resetGraph();
					removeTotallyBlockedRoads(time, changeSet);

					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), refugeIDs);

					agentLogger.debug("Agent going to refuge");

				}
				previousPath = currentPath;
				sendMove(time, currentPath);
			}
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		// To add all the seen targets before taking any action
		List<Human> currentlySeenTargets = getSeenTargets(changeSet, time);

		agentLogger.debug("Agent's seen targets "
				+ Think.ArrayListHumanstoString(currentlySeenTargets));

		// check if rescuing target is finished then send load
		if (!(location() instanceof Refuge) && location() instanceof Building) {
			ArrayList<EntityID> otherAmbulanceAgents = Think
					.AmbulanceAgentsInSameBuilding(model, changeSet, getID(),
							location().getID());

			if (rescuedTarget != null
					&& rescuedTarget instanceof Civilian
					&& rescuedTarget.getBuriedness() == 0
					&& rescuedTarget.getPosition().getValue() == location()
							.getID().getValue()) {

				boolean isSmallest = true;
				for (EntityID ambulanceAgentID : otherAmbulanceAgents) {
					if (ambulanceAgentID.getValue() < getID().getValue()) {
						isSmallest = false;
						break;
					}
				}

				Think.removeIfExists(
						(Human) model.getEntity(rescuedTarget.getID()),
						previouslySeenTargets);
				Think.addIfNotExists(rescuedTarget.getID(),
						previouslyRescuedTargets);

				if (isSmallest) {
					sendLoad(time, rescuedTarget.getID());
					agentLogger.debug("Agent is loading target  "
							+ rescuedTarget.getID());
					lastPosition = null;
					previousPath = null;
					isRescue = true;
					rescuedTarget = null;

					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				rescuedTarget = null;

			}

			for (Human civilian : Think.getChangedCivilians(model, changeSet))
				if (!Think.exists(civilian.getID(), previouslyRescuedTargets)
						&& rescuedTarget != null) {
					if ((Think.exists(civilian.getID(), voiceCiviliansOuch) || civilian
							.getHP() < 10000)) {
						if (civilian.getPosition().getValue() != location()
								.getID().getValue())
							continue;
						if (civilian.getBuriedness() != 0)
							continue;

						boolean isSmallest = true;
						for (EntityID ambulanceAgentID : otherAmbulanceAgents) {
							if (ambulanceAgentID.getValue() < getID()
									.getValue()) {
								isSmallest = false;
								break;
							}
						}
						Think.removeIfExists(
								(Human) model.getEntity(civilian.getID()),
								previouslySeenTargets);
						Think.addIfNotExists(rescuedTarget.getID(),
								previouslyRescuedTargets);

						if (isSmallest) {
							agentLogger
									.debug("Agent is loading target based on voice  "
											+ rescuedTarget.getID());
							sendLoad(time, civilian.getID());
							lastPosition = null;
							previousPath = null;
							isRescue = true;
							rescuedTarget = null;

							sendSpeak(time, 0, Comm.reportAllVoiceMessages(
									voiceChannels.get(0), voiceMessages));
							return;
						}

						rescuedTarget = null;
					}
				}

			if (rescuedTarget != null
					&& !(rescuedTarget instanceof Civilian)
					&& rescuedTarget.getBuriedness() == 0
					&& rescuedTarget.getPosition().getValue() == location()
							.getID().getValue()) {
				boolean isSmallest = true;
				for (EntityID ambulanceAgentID : otherAmbulanceAgents) {
					if (ambulanceAgentID.getValue() < getID().getValue()) {
						isSmallest = false;
						break;
					}
				}

				Think.removeIfExists(
						(Human) model.getEntity(rescuedTarget.getID()),
						previouslySeenTargets);

				if (buildingIsEmptyExceptOneTarget(location().getID(),
						rescuedTarget.getID(), currentlySeenTargets)) {
					Think.addIfNotExists(location().getID(), emptyBuildings);
					agentLogger.debug(Think
							.ArrayListEntityIDtoString(emptyBuildings));

				}
				Think.addIfNotExists(rescuedTarget.getID(),
						previouslyRescuedTargets);

				if (isSmallest) {
					agentLogger
							.debug("Agent reporting that he finished loading target "
									+ rescuedTarget.getID());
					lastPosition = null;
					previousPath = null;
					isRescue = true;
					rescuedTarget = null;

					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				rescuedTarget = null;

			}

		}

		updateReportedAndPreviouslySeenTargets(time, currentlySeenTargets,
				changeSet);

		agentLogger.debug("Agent's previously seen targets"
				+ Think.ArrayListHumanstoString(previouslySeenTargets));
		agentLogger.debug("Agent's reported targets locations  "
				+ Think.ArrayListEntityIDtoString(reportedTargetsLocation));
		agentLogger.debug("Agent's empty buildings locations  "
				+ Think.ArrayListEntityIDtoString(emptyBuildings));

		if (currenltySeenTask(time, currentlySeenTargets, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		if (previouslySeenTask(time, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		if (reportedLocationTask(time, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		;

		// ////////////////////////////////////////////////////////////////////////////
		/**
		 * If nothing specific to do now, keep visiting buildings in cluster
		 * until no more need to be visited, then move to the next cluster
		 */
		if (currentClusterTask(time, changeSet)) {
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

	}

	protected void updateReportedAndPreviouslySeenTargets(int time,
			List<Human> currentlySeenTargets, ChangeSet changeSet) {

		for (Human target : currentlySeenTargets) {

			Think.removeIfExists(target.getPosition(), reportedTargetsLocation);
			if (!Think.exists(target, previouslySeenTargets)
					&& !Think.exists(target.getPosition(),
							collapsedOrBurningBuildings)) {

				if (radioChannels.size() > 0) {
					sendSpeak(time, ambulanceChannel,
							Comm.civilianLocationBuried(target.getPosition(),
									allEntities));
					Comm.reportVoice(
							new VoiceMessage(Comm.civilianLocationBuried(
									target.getPosition(), allEntities), 1, 1,
									MESSAGE_ID.CIVILIAN_LOCATION_BURIED.name()),
							voiceMessages, comparator);
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, ambulanceChannel,
									Comm.civilianLocationBuried(
											target.getPosition(), allEntities)),
							dropped);
				} else
					Comm.reportVoice(
							new VoiceMessage(Comm.civilianLocationBuried(
									target.getPosition(), allEntities), 1,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.CIVILIAN_LOCATION_BURIED.name()),
							voiceMessages, comparator);
				if (!Think.exists(target.getPosition(), emptyBuildings)) {
					ArrayList<EntityID> otherAmbulanceAgents = Think
							.AmbulanceAgentsInSameBuilding(model, changeSet,
									getID(), location().getID());

					if ((otherAmbulanceAgents.size() > 0 && target
							.getBuriedness() / otherAmbulanceAgents.size() <= 5)
							|| target.getHP() >= 2000)
						Think.addIfNotExists(target, previouslySeenTargets);

					else {
						if (buildingIsEmptyExceptOneTarget(location().getID(),
								target.getID(), currentlySeenTargets)) {

							Think.addIfNotExists(location().getID(),
									emptyBuildings);
							reportEmptyBuilding(time, changeSet);
							Think.addIfNotExists(target.getID(),
									previouslyRescuedTargets);

						}
					}
				}
			}

		}
	}

	private void reportBlockade(int time) {
		List<EntityID> otherPath = graphHelper.getSearch().breadthFirstSearch(
				me().getPosition(), previousPath.get(previousPath.size() - 1));
		if (otherPath == null || otherPath.size() == 0) {
			agentLogger.debug("time: " + time + " reporting blockade in road: "
					+ previousPath.get(0));
			if (radioChannels.size() > 0) {
				Comm.reportVoice(
						new VoiceMessage(Comm.blockedRoadWithPriority(
								previousPath.get(0), priorityStuck.ordinal(),
								allEntities), 3, 1,
								MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.name()),
						voiceMessages, comparator);
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
				Comm.reportVoice(
						new VoiceMessage(Comm.blockedRoadWithPriority(
								previousPath.get(0), priorityStuck.ordinal(),
								allEntities), 3, 1,
								MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.name()),
						voiceMessages, comparator);
				Comm.reportVoice(
						new VoiceMessage(Comm.blockedRoad(previousPath.get(0),
								allEntities), 3, 1,
								MESSAGE_ID.BLOCKED_ROAD_MESSAGE_ID.name()),
						voiceMessages, comparator);
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

	// Unload civilians at the refuge and start looking for new targets
	protected boolean someoneOnBoardTask(int time, StandardWorldModel model,
			ChangeSet changeSet) {
		if (someoneOnBoard(model, changeSet)) {
			if (location() instanceof Refuge) {
				sendUnload(time);
				previousPath = null;
				lastPosition = null;
			} else {
				if (getCivilianOnBoard(model, changeSet).getHP() > 0) {
					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), refugeIDs);
					priorityStuck = STUCK_PRIORITY.GOING_TO_REFUGE;
					if (currentPath == null || currentPath.size() == 0) {
						agentLogger
								.debug("time: "
										+ time
										+ " graph recreated from someone on board task");
						surroundedByBlockades(time, changeSet);
						graphHelper.resetGraph();
						removeTotallyBlockedRoads(time, changeSet);

						priorityStuck = STUCK_PRIORITY.GOING_TO_REFUGE;

						currentPath = graphHelper.getSearch()
								.breadthFirstSearch(me().getPosition(),
										refugeIDs);

					}
					if (currentPath != null) {
						previousPath = currentPath;
						sendMove(time, currentPath);
					} else
						sendRest(time);
					return true;
				} else {
					sendUnload(time);
					previousPath = null;
					lastPosition = null;

				}
			}
		}

		return false;
	}

	// Rescue the agent that is currently in the scene
	protected boolean currenltySeenTask(int time,
			List<Human> currentlySeenTargets, ChangeSet changeSet) {
		ArrayList<EntityID> otherAmbulanceAgents = Think
				.AmbulanceAgentsInSameBuilding(model, changeSet, getID(),
						location().getID());
		Think.addIfNotExists(getID(), otherAmbulanceAgents);

		Collections.sort(otherAmbulanceAgents, new Comparator<EntityID>() {

			@Override
			public int compare(EntityID arg0, EntityID arg1) {
				// TODO Auto-generated method stub
				if (arg0.getValue() > arg1.getValue())
					return 1;
				if (arg0.getValue() < arg1.getValue())
					return -1;
				return 0;
			}
		});

		for (Human target : currentlySeenTargets) {

			if (target.getPosition().getValue() == location().getID()
					.getValue()) {

				if (target.getBuriedness() > 0) {
					int agentIndex = Think.indexOfAgentID(otherAmbulanceAgents,
							getID());
					int numberOfTargets = getNumberOfSeenTargets(currentlySeenTargets);
					if (agentIndex < target.getBuriedness()
							&& agentIndex + 1 <= (numberOfAgents / 5)+ numberOfTargets
							&& agentIndex < (5 + numberOfTargets)) {

						if ((target.getBuriedness()
								/ otherAmbulanceAgents.size() <= 5)
								|| target.getHP() >= 2000) {
							rescuedTarget = target;
							sendRescue(time, target.getID());
							agentLogger.debug("Agent is rescuing target  "
									+ rescuedTarget.getID());
							Think.removeIfExists(target, previouslySeenTargets);
							lastPosition = null;
							previousPath = null;
							if (agentIndex + 1 == numberOfAgents / 5
									|| agentIndex == 4) {

								if (buildingIsEmptyExceptOneTarget(location()
										.getID(), target.getID(),
										currentlySeenTargets)) {

									Think.addIfNotExists(location().getID(),
											emptyBuildings);
									reportEmptyBuilding(time, changeSet);
									agentLogger
											.debug("Agent last in limit is reporting empty building "
													+ location().getID());
									agentLogger
											.debug(Think
													.ArrayListEntityIDtoString(emptyBuildings));

								}
							}
							return true;
						}
					}

					agentLogger.debug("Agent is done rescuing target  "
							+ target.getID());

					if (buildingIsEmptyExceptOneTarget(location().getID(),
							target.getID(), currentlySeenTargets)) {

						Think.addIfNotExists(location().getID(), emptyBuildings);
						reportEmptyBuilding(time, changeSet);
						agentLogger.debug("Agent is reporting empty building "
								+ location().getID());
						agentLogger.debug(Think
								.ArrayListEntityIDtoString(emptyBuildings));

					}
					rescuedTarget = null;
					Think.addIfNotExists(target.getID(),
							previouslyRescuedTargets);

					updateReportedAndPreviouslySeenTargets(time,
							currentlySeenTargets, changeSet);

					Think.removeIfExists(target, previouslySeenTargets);
					agentLogger
							.debug("Agent's previously rescued targets "
									+ Think.ArrayListEntityIDtoString(previouslyRescuedTargets));
					agentLogger
							.debug("Agent's reported targets locations "
									+ Think.ArrayListEntityIDtoString(reportedTargetsLocation));
					agentLogger
							.debug("Agent's previously seen targets "
									+ Think.ArrayListHumanstoString(previouslySeenTargets));

					continue;
				}

			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), target.getPosition());

				if (currentPath != null && currentPath.size() != 0) {
					priorityStuck = STUCK_PRIORITY.GOING_TO_TARGET;
					previousPath = currentPath;
					sendMove(time, currentPath);
					return true;

				}
			}
		}
		return false;
	}

	// If no targets seen, go to previously seen targets
	protected boolean previouslySeenTask(int time, ChangeSet changeSet) {

		agentLogger.debug("Agent's sorted previously seen targets "
				+ Think.ArrayListHumanstoString(previouslySeenTargets));

		if (previouslySeenTargets.size() > 0) {
			ArrayList<EntityID> seenTargetsLocations = new ArrayList<EntityID>();
			for (int i = 0; i < previouslySeenTargets.size(); i++) {
				Human human = previouslySeenTargets.get(i);
				if (human.getBuriedness() > 0)
					Think.addIfNotExists(human.getPosition(),
							seenTargetsLocations);
				else {
					previouslySeenTargets.remove(i--);
					Think.removeIfExists(human.getPosition(),
							reportedTargetsLocation);
					Think.removeIfExists(human.getPosition(),
							seenTargetsLocations);
					Think.addIfNotExists(human.getID(),
							previouslyRescuedTargets);
				}
			}

			currentPath = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), seenTargetsLocations);
			priorityStuck = STUCK_PRIORITY.GOING_TO_TARGET;

			if (currentPath != null && currentPath.size() != 0) {
				if (currentPath.size() > 1) {
					StandardEntity previousEntity = model.getEntity(currentPath
							.get(currentPath.size() - 1));
					if (previousEntity instanceof Building
							&& !Think
									.exists((Building) previousEntity, Think
											.getChangedBuildings(model,
													changeSet)))
						currentPath.remove(currentPath.size() - 1);

				}
				previousPath = currentPath;
				sendMove(time, currentPath);
				return true;
			}
		}
		return false;
	}

	// If no targets seen or previously seen go to reported targets
	protected boolean reportedLocationTask(int time, ChangeSet changeSet) {

		if (reportedTargetsLocation.size() > 0) {
			currentPath = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), reportedTargetsLocation);
			priorityStuck = STUCK_PRIORITY.GOING_TO_TARGET;

			// move to reported target only if it's in the same cluster
			if (currentPath != null) {
				if (currentPath.size() > 1) {
					StandardEntity reportedEntity = model.getEntity(currentPath
							.get(currentPath.size() - 1));
					if (reportedEntity instanceof Building
							&& !Think
									.exists((Building) reportedEntity, Think
											.getChangedBuildings(model,
													changeSet)))
						currentPath.remove(currentPath.size() - 1);
				}
				previousPath = currentPath;
				isRescue = true;
				sendMove(time, currentPath);
				return true;
			}
		}
		return false;
	}

	//Checks if a building is empty from buried targets except for one specific target.
	protected boolean buildingIsEmptyExceptOneTarget(EntityID building,
			EntityID targetID, List<Human> currentlySeenTargets) {
		boolean isEmpty = true;
		for (Human target : currentlySeenTargets) {
			if (target.getPosition() == building
					&& target.getID().getValue() != targetID.getValue())
				isEmpty = false;
		}
		return isEmpty;
	}

	//reports fires, collapsed and extinguished buildings to fires brigades agents 
	protected void handleFires(ChangeSet arg1, int time,
			ArrayList<Building> changedBuildings) {

		newBuildingWarm = (ArrayList<Building>) Think.filter(changedBuildings,
				FireCluster.predicateBuildingWarm);
		newBuildingsOnFire = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingOnFire);
		newCollapsedBuildings = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingCollapsed);
		newExtinguishedFire = (ArrayList<Building>) Think.filter(
				changedBuildings, FireCluster.predicateBuildingExtinguished);

		for (Building b : newBuildingWarm) {

			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_WARM))
				if (radioChannels.size() > 0)
					reportWarm(b, time, 0);
				else
					Comm.reportVoice(
							new VoiceMessage(Comm.fireMessageVoice(
									MESSAGE_ID.WARM_BUILDING.ordinal(), b,
									allEntities), 2, TIME_TO_LIVE_VOICE,
									MESSAGE_ID.WARM_BUILDING.name()),
							voiceMessages, comparator);

		}
		for (Building b : newExtinguishedFire) {

			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_EXTINGUISHED))
				if (radioChannels.size() > 0)
					reportExtinguished(b, time, 0);
				else
					Comm.reportVoice(
							new VoiceMessage(Comm.fireMessageVoice(
									MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
											.ordinal(), b, allEntities), 2,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);

		}
		for (Building b : newBuildingsOnFire) {

			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_ON_FIRE))
				if (radioChannels.size() > 0)
					reportFire(b, time, 0);
				else
					Comm.reportVoice(
							new VoiceMessage(Comm.fireMessageVoice(
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
											.ordinal(), b, allEntities), 2,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);

		}
		for (Building b : newCollapsedBuildings) {
			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_COLLAPSED))
				if (radioChannels.size() > 0)
					reportCollapse(b, time, 0);
				else
					Comm.reportVoice(
							new VoiceMessage(Comm.fireMessageVoice(
									MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
											.ordinal(), b, allEntities), 2,
									TIME_TO_LIVE_VOICE,
									MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
											.name()), voiceMessages, comparator);
		}

	}

	//checks for tasks to perform in each agent's corresponding cluster.
	protected boolean currentClusterTask(int time, ChangeSet changeSet) {
		currentPath = graphHelper.getSearch().breadthFirstSearch(
				me().getPosition(), currentCluster.cluster);

		if (currentPath == null || currentPath.size() == 0) {

			surroundedByBlockades(time, changeSet);
			graphHelper.resetGraph();
			agentLogger.debug("time: " + time
					+ " graph recreated from current cluster task");
			removeTotallyBlockedRoads(time, changeSet);

			currentPath = graphHelper.getSearch().breadthFirstSearch(
					me().getPosition(), currentCluster.cluster);

			if (currentPath == null || currentPath.size() == 0) {

				int tempCIndex = Clustering
						.getClosestNotEmptyDiffClusterIndexFromClusters(me()
								.getPosition(), clusters, model, clusterIndex);
				if (tempCIndex != -1) {
					currentCluster = clusters.get(tempCIndex);
					clusterIndex = tempCIndex;
					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), currentCluster.cluster);
				} else {
					System.out.println("time: " + time + " ," + getID()
							+ ", empty cluster");
				}
			}
		}

		if (currentPath != null && currentPath.size() > 0) {
			if (currentPath.size() > 1) {
				currentPath.remove(currentPath.size() - 1);
			}
			previousPath = currentPath;
			priorityStuck = STUCK_PRIORITY.NORMAL;
			sendMove(time, currentPath);
			return true;
		}
		return false;
	}
    
	//reports if the agents is surrounded by blockades.
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
					Comm.reportVoice(
							new VoiceMessage(Comm.stuckInsideBlockade(me()
									.getPosition(), closestBlockade.getID(),
									allEntities), 1, 1,
									MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
							voiceMessages, comparator);
					sendSpeak(time, policeChannel, Comm.stuckInsideBlockade(
							me().getPosition(), closestBlockade.getID(),
							allEntities));

					agentLogger.debug("time: " + time + " agent: " + getID()
							+ " surrounded by blockades");
				} else
					Comm.reportVoice(
							new VoiceMessage(Comm.stuckInsideBlockade(me()
									.getPosition(), closestBlockade.getID(),
									allEntities), 1, TIME_TO_LIVE_VOICE,
									MESSAGE_ID.STUCK_INSIDE_BLOCKADE.name()),
							voiceMessages, comparator);
			}

		}
		return isStuck;
	}

	private Blockade getclosestBlockade(ChangeSet changeSet) {
		ArrayList<Blockade> seenBlockades = Think.getBlockades(model,
				changeSet);
		if (!seenBlockades.isEmpty()) {
			Collections.sort(seenBlockades, new DistanceSorter(location(),
					model));
			for (Blockade blockade : seenBlockades) {
				if (currentPath != null
						&& (blockade.getPosition().getValue() == me()
								.getPosition().getValue() || Think.exists(
								blockade.getPosition(), currentPath)))
					return blockade;
			}
			return seenBlockades.get(0);
		}
		return null;
	}

	//reports if a building is empty and no longer has any buried targets in it.
	protected void reportEmptyBuilding(int time, ChangeSet changeSet) {
		if (location() instanceof Building)
			if (BuildingIsEmpty(location().getID(), changeSet, time)) {
				if (removeEmptyBuildingFromClusterAndTargetLists(time,
						location().getID())) {
					if (radioChannels.size() > 0) {
						Comm.reportVoice(
								new VoiceMessage(Comm.clearedBuilding(
										location().getID(), allEntities), 1,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.CLEARED_BUILDING.name()),
								voiceMessages, comparator);
						sendSpeak(time, ambulanceChannel, Comm.clearedBuilding(
								location().getID(), allEntities));
						Think.addIfNotExists(
								new AKSpeak(me().getID(), time,
										ambulanceChannel, Comm
												.clearedBuilding(location()
														.getID(), allEntities)),
								dropped);
					} else
						Comm.reportVoice(
								new VoiceMessage(Comm.clearedBuilding(
										location().getID(), allEntities), 1,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.CLEARED_BUILDING.name()),
								voiceMessages, comparator);
				}

			}

		for (EntityID b : getNeighbouringBuilding((Area) location())) {
			if (BuildingIsEmpty(b, changeSet, time)) {

				if (removeEmptyBuildingFromClusterAndTargetLists(time, b)) {
					if (radioChannels.size() > 0) {
						sendSpeak(time, ambulanceChannel, Comm.clearedBuilding(
								location().getID(), allEntities));
						Think.addIfNotExists(
								new AKSpeak(me().getID(), time,
										ambulanceChannel, Comm
												.clearedBuilding(location()
														.getID(), allEntities)),
								dropped);
					} else
						Comm.reportVoice(
								new VoiceMessage(Comm.clearedBuilding(
										location().getID(), allEntities), 1,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.CLEARED_BUILDING.name()),
								voiceMessages, comparator);
				}
			}
		}
	}

	protected ArrayList<EntityID> getNeighbouringBuilding(Area area) {
		ArrayList<EntityID> neighbouringBuilding = new ArrayList<EntityID>();
		for (EntityID a : area.getNeighbours()) {
			if (model.getEntity(a) instanceof Building) {
				neighbouringBuilding.add(a);
			}
		}
		return neighbouringBuilding;
	}

	// remove totally blocked Roads for the graph
	protected void removeTotallyBlockedRoads(int time, ChangeSet changeSet) {
		
		for (Road e : Think.TotallyBlockedRoads(model, changeSet)) {

			if (previousPath != null && Think.exists(e.getID(), previousPath)
					&& graphHelper.removeNode(e.getID())) {
				reportBlockadetoAll(time);
			}
		}
	}

	// add totally cleared Roads to the graph
	protected void addTotallyClearedRoads(int time, ChangeSet changeSet) {

		ArrayList<Road> clearRoads = Think.getClearedRoadsNew(model, changeSet);
		agentLogger.debug("time: " + time + " clear roads: "
				+ Think.ArrayListRoadtoString(clearRoads));
		for (Road r : clearRoads) {

			if (me().getPosition().getValue() != r.getID().getValue()) {
				List<EntityID> pathToClearRoad = graphHelper.getSearch()
						.breadthFirstSearch(me().getPosition(), r.getID());
				if (pathToClearRoad == null)
					continue;
				boolean clear = true;
				pathToClearRoad.add(0, me().getPosition());
				agentLogger.debug("time: " + time + " path to clear road: "
						+ Think.ArrayListEntityIDtoString(pathToClearRoad));
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
			agentLogger.debug("time: " + time + " road: " + r
					+ " and neighbouring buildings will be restored");

			for (EntityID e : r.getNeighbours()) {
				if (model.getEntity(e) instanceof Building)
					if (graphHelper.restoreNode(e)) {
						if (radioChannels.size() > 0) {
							sendSpeak(time, ambulanceChannel,
									Comm.clearedRoad(e, allEntities));
							Think.addIfNotExists(
									new AKSpeak(me().getID(), time,
											ambulanceChannel, Comm.clearedRoad(
													e, allEntities)), dropped);

							if (ambulanceChannel != policeChannel) {
								sendSpeak(time, policeChannel,
										Comm.clearedRoad(e, allEntities));

							}
							Comm.reportVoice(
									new VoiceMessage(Comm.clearedRoad(e,
											allEntities), 3, 1,
											MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID
													.name()), voiceMessages,
									comparator);
						} else {
							Comm.reportVoice(
									new VoiceMessage(Comm.clearedRoad(e,
											allEntities), 3,
											TIME_TO_LIVE_VOICE,
											MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID
													.name()), voiceMessages,
									comparator);
						}
					}
			}

			if (graphHelper.restoreNode(r.getID())) {

				if (radioChannels.size() > 0) {
					;
					sendSpeak(time, ambulanceChannel,
							Comm.clearedRoad(r.getID(), allEntities));
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, ambulanceChannel,
									Comm.clearedRoad(r.getID(), allEntities)),
							dropped);
					if (policeChannel != ambulanceChannel) {
						sendSpeak(time, policeChannel,
								Comm.clearedRoad(r.getID(), allEntities));

					}
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedRoad(r.getID(),
									allEntities), 3, 1,
									MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID.name()),
							voiceMessages, comparator);
				} else {
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedRoad(r.getID(),
									allEntities), 3, TIME_TO_LIVE_VOICE,
									MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID.name()),
							voiceMessages, comparator);

				}
			}
		}

	}

	private List<Human> getSeenTargets(ChangeSet set, int time) {
		List<Human> targets = new ArrayList<Human>();

		for (Human human : getBuriedTargets(set)) {
			if (!Think.exists(human.getID(), previouslyRescuedTargets)) {
				targets.add(human);
				agentLogger.debug("Civilian " + human.getID() + " HP "
						+ human.getHP() + " Time: " + time);
				Comm.reportVoice(
						new VoiceMessage(Comm.civilianLocationBuried(
								human.getPosition(), allEntities), 1, 1,
								MESSAGE_ID.CIVILIAN_LOCATION_BURIED.name()),
						voiceMessages, comparator);
			}
		}
		Collections.sort(targets, new DistanceSorter(location(), model));
		agentLogger.debug("Sorted seen targets "
				+ Think.ArrayListHumanstoString(targets));
		return targets;
	}

	private List<Human> getBuriedTargets(ChangeSet set) {
		List<Human> buriedTargets = new ArrayList<Human>();

		for (Human target : Think.getChangedHumans(model, set)) {

			if (target.getID().getValue() == getID().getValue())
				continue;
			if ((model.getEntity(target.getPosition()) instanceof Refuge)) {
				Think.removeIfExists(target, previouslySeenTargets);
				Think.removeIfExists(target.getID(), reportedTargetsLocation);
				continue;
			}
			// In order not to enter a burning building
			if (Think.exists(target.getPosition(), collapsedOrBurningBuildings)) {

				continue;
			}
			if (target.isHPDefined() && target.isBuriednessDefined()
					&& target.isPositionDefined() && target.getHP() > 0
					&& target.getBuriedness() > 0)
				buriedTargets.add(target);
		}

		return buriedTargets;
	}

	private boolean BuildingIsEmpty(EntityID b, ChangeSet set, int time) {

		for (Human target : getSeenTargets(set, time)) {
			if (target.getPosition().getValue() == b.getValue()) {
				return false;
			}
		}
		return true;

	}

	private boolean someoneOnBoard(StandardWorldModel model, ChangeSet changeSet) {
		for (Human c : Think.getChangedCivilians(model, changeSet)) {
			if (c.getPosition().getValue() == getID().getValue()) {
				// Logger.debug(c + " is on board");
				return true;
			}
		}
		return false;
	}

	private int getNumberOfSeenTargets(List<Human> currentlySeenTargets){
		 int number = 0; 
		for (Human target : currentlySeenTargets) {
			if (target.getPosition().getValue() == location().getID()
					.getValue()) 
		     number++;	
		}
		return number;
	}
	private Human getCivilianOnBoard(StandardWorldModel model,
			ChangeSet changeSet) {
		for (Human c : Think.getChangedCivilians(model, changeSet)) {
			if (c.getPosition().getValue() == getID().getValue()) {
				// Logger.debug(c + " is on board");
				return c;
			}
		}
		return null;
	}

	private boolean removeEmptyBuildingFromClusterAndTargetLists(int time,
			EntityID building) {
		Think.removeIfExists(building, currentCluster.cluster);
		boolean toReport = false;
		for (int i = 0; i < previouslySeenTargets.size(); i++) {
			if (previouslySeenTargets.get(i).getPosition().getValue() == building
					.getValue()) {
				previouslySeenTargets.remove(i--);
				toReport = true;
			}
		}

		if (Think.removeIfExists(building, reportedTargetsLocation))
			toReport = true;

		return toReport;
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
						dropped.remove(i);
						droppedMsg = false;
						break;
					}
				}
				// }
				if (droppedMsg) {
					sendSpeak(time, dropped.get(i).getChannel(), dropped.get(i)
							.getContent());
					agentLogger.debug("time: " + time + ", agent "
							+ me().getID().getValue()
							+ " resending dropped msg");
				}
			}
			// dropped.clear();
		} catch (Exception e) {
			e.printStackTrace();
			agentLogger.debug(e.getMessage());
		}
	}

	private void handleMessages(int time, Collection<Command> heard) {

		for (Command next : heard) {
			AKSpeak message = (AKSpeak) next;
			String m = new String(message.getContent());
			String[] msg = m.split(",");

			try {

				if (message.getChannel() == 0) {
					String[] splitted = m.split("-");
					handleVoiceMessages(splitted, message.getTime());
					continue;
				}

				if (message.getAgentID().getValue() == getID().getValue()) {
					Think.removeIfExists(message, dropped);
					continue;
				}
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
					// ADD THE CODE HERE
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
					if (!Think.exists(id, collapsedOrBurningBuildings)) {
						collapsedOrBurningBuildings.add(id);
					}
					for (StandardEntity e : model.getObjectsInRange(id,
							config.getIntValue(MAX_DISTANCE_KEY) / 2)) {
						if (e instanceof Building)
							Think.addIfNotExists(e.getID(),
									collapsedOrBurningBuildings);
					}
					graphHelper.removeNode(id);
					handleReportedFire(id, message.getTime());

				}
					;
					break;
				case BLOCKED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					agentLogger.debug("time: " + time + "Road " + id
							+ " Blocked from: " + message.getAgentID());

					graphHelper.removeNode(id);
				}
					;
					break;
				case EXTINGUISHED_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					Think.addIfNotExists(id, collapsedOrBurningBuildings);
					for (StandardEntity e : model.getObjectsInRange(id,
							config.getIntValue(MAX_DISTANCE_KEY) / 2)) {
						if (e instanceof Building)
							Think.addIfNotExists(e.getID(),
									collapsedOrBurningBuildings);
					}
					graphHelper.restoreNode(id);
					agentLogger.debug("time: " + time + " restoring : " + id
							+ " from extinguished fire message");
					handleReportedExtinguished(id, message.getTime());
				}
					;
					break;
				case COLLAPSED_BUILDING_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					Think.addIfNotExists(id, collapsedOrBurningBuildings);
					for (StandardEntity e : model.getObjectsInRange(id,
							config.getIntValue(MAX_DISTANCE_KEY) / 2)) {
						if (e instanceof Building)
							Think.addIfNotExists(e.getID(),
									collapsedOrBurningBuildings);
					}
					handleReportedCollapsed(id, message.getTime());
				}
					;
					break;
				case WARM_BUILDING: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					handleReportedWarm(id, message.getTime());
				}
					;
					break;
				case CIVILIAN_LOCATION_BURIED: {

					EntityID civilianLocation = allEntities.get(
							Integer.parseInt(msg[1])).getID();
					agentLogger.debug("time: " + time + "Civilian "
							+ civilianLocation.getValue() + " from: "
							+ message.getAgentID());

					if (model.getEntity(civilianLocation) instanceof Refuge) {
						System.out.println("reported refuge by"
								+ model.getEntity(message.getAgentID())
										.getURN());
						continue;
					}
					if (!Think
							.exists(civilianLocation, reportedTargetsLocation)
							&& !Think.existsHumanInLocation(civilianLocation,
									previouslySeenTargets)
							&& !Think.exists(civilianLocation,
									collapsedOrBurningBuildings)) {

						double threshold = Think.thresholdDistance(
								numberOfAgents / 4,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						double threshold2 = Think.thresholdDistance(3,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						if (model.getDistance(civilianLocation,
								clusters.get(clusterIndex).center.getID()) > threshold
								&& model.getDistance(civilianLocation,
										location().getID()) > threshold2)
							continue;
						agentLogger.debug("time: " + time + "Civilian "
								+ civilianLocation.getValue() + "added ");
						if (!Think.exists(civilianLocation, emptyBuildings)
								&& model.getEntity(civilianLocation) instanceof Building)
							reportedTargetsLocation.add(civilianLocation);
					}
				}
					;
					break;
				case BURIED_AGENT_LOCATION: {

					EntityID civilianLocation = allEntities.get(
							Integer.parseInt(msg[1])).getID();

					agentLogger.debug("time: " + time + "agent "
							+ civilianLocation.getValue() + " from: "
							+ message.getAgentID());

					if (model.getEntity(civilianLocation) instanceof Refuge) {
						System.out.println("reported refuge by"
								+ model.getEntity(message.getAgentID())
										.getURN());
						continue;
					}
					if (!Think
							.exists(civilianLocation, reportedTargetsLocation)
							&& !Think.existsHumanInLocation(civilianLocation,
									previouslySeenTargets)
							&& !Think.exists(civilianLocation,
									collapsedOrBurningBuildings)) {
						double threshold = Think.thresholdDistance(
								numberOfAgents / 4,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						double threshold2 = Think.thresholdDistance(3,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						if (model.getDistance(civilianLocation,
								clusters.get(clusterIndex).center.getID()) > threshold
								&& model.getDistance(civilianLocation,
										location().getID()) > threshold2)
							continue;
						agentLogger.debug("time: " + time + "agent  "
								+ civilianLocation.getValue() + "added ");
						if (!Think.exists(civilianLocation, emptyBuildings)
								&& model.getEntity(civilianLocation) instanceof Building)
							reportedTargetsLocation.add(civilianLocation);
					}
				}
					;
					break;
				case AGENT_LOCATION_HEARD_CIVILIAN: {

				}
					;
					break;
				case CLEARED_BUILDING: {
					EntityID buildingID = allEntities.get(
							Integer.parseInt(msg[1])).getID();

					agentLogger.debug("time: " + time + "Building "
							+ buildingID.getValue() + " Cleared from: "
							+ message.getAgentID());

					Think.removeIfExists(buildingID, reportedTargetsLocation);
					Think.removeHumansInLocation(buildingID,
							previouslySeenTargets);
					Think.removeIfExists(buildingID, currentCluster.cluster);
					Think.addIfNotExists(buildingID, emptyBuildings);

				}
					;
					break;
				case CLEARED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
			
					graphHelper.restoreNode(id);
					agentLogger.debug("time: " + time + " restoring : " + id
							+ " from cleared road message from "
							+ message.getAgentID());
				}
					;
					break;
				case FINISHED_CLUSTER_AMBULANCE_MESSAGE_ID: {
					int cId = Integer.parseInt(msg[1]);
					Think.removeEntityIDs(reportedTargetsLocation,
							clusters.get(cId).cluster);

					for (int i = 0; i < previouslySeenTargets.size(); i++) {
						if (Think.exists(previouslySeenTargets.get(i)
								.getPosition(), clusters.get(cId).cluster))
							previouslySeenTargets.remove(i--);
					}
					clusters.get(cId).cluster.clear();
					if (cId == clusterIndex) {
						int tempCIndex = Clustering
								.getClosestNotEmptyClusterIndexFromClusters(
										me().getPosition(), clusters, model);

						if (tempCIndex != -1) {
							currentCluster = clusters.get(tempCIndex);

							clusterIndex = tempCIndex;

						}
					}
				}
					;
					break;
				}

			} catch (NumberFormatException e) {
				EntityID id = message.getAgentID();
				if (m.equalsIgnoreCase("help")) {
					if (!Think.exists(id, voiceCiviliansHelp)) {
						voiceCiviliansHelp.add(id);
					}
				} else if (m.equalsIgnoreCase("ouch")) {
					if (!Think.exists(id, voiceCiviliansOuch)) {
						voiceCiviliansOuch.add(id);
					}
				}
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
					int ttl = Integer.parseInt(msg[3]);

					if (!Think.exists(id, collapsedOrBurningBuildings)) {
						collapsedOrBurningBuildings.add(id);
					}
					for (StandardEntity e : model.getObjectsInRange(id,
							config.getIntValue(MAX_DISTANCE_KEY) / 2)) {
						if (e instanceof Building)
							Think.addIfNotExists(e.getID(),
									collapsedOrBurningBuildings);
					}
					graphHelper.removeNode(id);
					handleReportedFire(id, time);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1,
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
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
					graphHelper.removeNode(id);
				}
					;
					break;
				case EXTINGUISHED_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[3]);

					Think.addIfNotExists(id, collapsedOrBurningBuildings);
					graphHelper.restoreNode(id);
					agentLogger.debug("time: " + time + " restoring : " + id
							+ " from extinguished fire message");
					handleReportedExtinguished(id, time);

					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1,
									MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case COLLAPSED_BUILDING_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[3]);

					Think.addIfNotExists(id, collapsedOrBurningBuildings);
					for (StandardEntity e : model.getObjectsInRange(id,
							config.getIntValue(MAX_DISTANCE_KEY) / 2)) {
						if (e instanceof Building)
							Think.addIfNotExists(e.getID(),
									collapsedOrBurningBuildings);
					}
					handleReportedCollapsed(id, time);

					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1,
									MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case WARM_BUILDING: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[3]);

					handleReportedWarm(id, time);

					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1, MESSAGE_ID.WARM_BUILDING.name()),
							voiceMessages, comparator);
				}
					;
					break;
				case CIVILIAN_LOCATION_BURIED: {

					EntityID civilianLocation = allEntities.get(
							Integer.parseInt(msg[1])).getID();
					int ttl = Integer.parseInt(msg[2]);
					if (model.getEntity(civilianLocation) instanceof Refuge) {
						continue;
					}
					if (!Think
							.exists(civilianLocation, reportedTargetsLocation)
							&& !Think.existsHumanInLocation(civilianLocation,
									previouslySeenTargets)
							&& !Think.exists(civilianLocation,
									collapsedOrBurningBuildings)) {

						double threshold = Think.thresholdDistance(
								numberOfAgents / 4,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						double threshold2 = Think.thresholdDistance(3,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						if (model.getDistance(civilianLocation,
								clusters.get(clusterIndex).center.getID()) > threshold
								&& model.getDistance(civilianLocation,
										location().getID()) > threshold2)
							continue;
						agentLogger.debug("time: " + time + "Civilian "
								+ civilianLocation.getValue() + "added ");
						if (!Think.exists(civilianLocation, emptyBuildings)
								&& model.getEntity(civilianLocation) instanceof Building)
							reportedTargetsLocation.add(civilianLocation);
					}
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1,
									MESSAGE_ID.CIVILIAN_LOCATION_BURIED.name()),
							voiceMessages, comparator);
				}
					;
					break;
				case BURIED_AGENT_LOCATION: {

					EntityID civilianLocation = allEntities.get(
							Integer.parseInt(msg[1])).getID();
					int ttl = Integer.parseInt(msg[2]);
					if (model.getEntity(civilianLocation) instanceof Refuge) {
						continue;
					}
					if (!Think
							.exists(civilianLocation, reportedTargetsLocation)
							&& !Think.existsHumanInLocation(civilianLocation,
									previouslySeenTargets)
							&& !Think.exists(civilianLocation,
									collapsedOrBurningBuildings)) {
						double threshold = Think.thresholdDistance(
								numberOfAgents / 4,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						double threshold2 = Think.thresholdDistance(3,
								(Area) model.getEntity(civilianLocation),
								clusters) - 1;
						if (model.getDistance(civilianLocation,
								clusters.get(clusterIndex).center.getID()) > threshold
								&& model.getDistance(civilianLocation,
										location().getID()) > threshold2)
							continue;
						agentLogger.debug("time: " + time + "agent  "
								+ civilianLocation.getValue() + "added ");
						if (!Think.exists(civilianLocation, emptyBuildings)
								&& model.getEntity(civilianLocation) instanceof Building)
							reportedTargetsLocation.add(civilianLocation);
					}

					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1, MESSAGE_ID.BURIED_AGENT_LOCATION
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case CLEARED_BUILDING: {
					EntityID buildingID = allEntities.get(
							Integer.parseInt(msg[1])).getID();
					int ttl = Integer.parseInt(msg[2]);

					Think.removeIfExists(buildingID, reportedTargetsLocation);
					Think.removeHumansInLocation(buildingID,
							previouslySeenTargets);
					Think.removeIfExists(buildingID, currentCluster.cluster);
					Think.addIfNotExists(buildingID, emptyBuildings);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1, MESSAGE_ID.CLEARED_BUILDING.name()),
							voiceMessages, comparator);
				}
					;
					break;
				case CLEARED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[2]);
					graphHelper.restoreNode(id);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1, MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case FINISHED_CLUSTER_AMBULANCE_MESSAGE_ID: {
					int cId = Integer.parseInt(msg[1]);
					Think.removeEntityIDs(reportedTargetsLocation,
							clusters.get(cId).cluster);

					for (int i = 0; i < previouslySeenTargets.size(); i++) {
						if (Think.exists(previouslySeenTargets.get(i)
								.getPosition(), clusters.get(cId).cluster))
							previouslySeenTargets.remove(i--);
					}
					clusters.get(cId).cluster.clear();
					if (cId == clusterIndex) {
						int tempCIndex = Clustering
								.getClosestNotEmptyClusterIndexFromClusters(
										me().getPosition(), clusters, model);

						if (tempCIndex != -1) {
							currentCluster = clusters.get(tempCIndex);

							clusterIndex = tempCIndex;

						}
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

	private void handleReportedFire(EntityID id, int messageTime) {

		Building b = (Building) model.getEntity(id);

		fires.addNewTarget(b, 0, 1, messageTime,
				BUILDING_LIST.BUILDINGS_ON_FIRE);
	}

	private void handleReportedExtinguished(EntityID id, int messageTime) {

		Building b = (Building) model.getEntity(id);

		fires.addNewTarget(b, 0, 1, messageTime,
				BUILDING_LIST.BUILDINGS_EXTINGUISHED);
	}

	private void handleReportedCollapsed(EntityID id, int messageTime) {

		Building b = (Building) model.getEntity(id);

		fires.addNewTarget(b, 0, 1, messageTime,
				BUILDING_LIST.BUILDINGS_COLLAPSED);
	}

	private void handleReportedWarm(EntityID id, int messageTime) {

		Building b = (Building) model.getEntity(id);

		fires.addNewTarget(b, 0, 1, messageTime, BUILDING_LIST.BUILDINGS_WARM);
	}

	private void reportWarm(Building b, int time, int clusterIndex) {

		Comm.reportVoice(
				new VoiceMessage(Comm.fireMessageVoice(
						MESSAGE_ID.WARM_BUILDING.ordinal(), b, allEntities), 2,
						1, MESSAGE_ID.WARM_BUILDING.name()), voiceMessages,
				comparator);
		sendSpeak(time, fireChannel,
				Comm.warmBuilding(b, time, clusterIndex, allEntities));

		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.warmBuilding(b, time, clusterIndex, allEntities));
			Think.addIfNotExists(
					new AKSpeak(me().getID(), time, ambulanceChannel, Comm
							.warmBuilding(b, time, clusterIndex, allEntities)),
					dropped);

		}
		if (fireChannel != policeChannel && ambulanceChannel != policeChannel) {
			sendSpeak(time, policeChannel,
					Comm.warmBuilding(b, time, clusterIndex, allEntities));

		}

	}

	private void reportFire(Building b, int time, int clusterIndex) {

		sendSpeak(time, fireChannel,
				Comm.fireMessage(b, time, clusterIndex, allEntities));

		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.fireMessage(b, time, clusterIndex, allEntities));
			Think.addIfNotExists(
					new AKSpeak(me().getID(), time, ambulanceChannel, Comm
							.fireMessage(b, time, clusterIndex, allEntities)),
					dropped);

			if (fireChannel != policeChannel
					&& ambulanceChannel != policeChannel) {
				sendSpeak(time, policeChannel,
						Comm.fireMessage(b, time, clusterIndex, allEntities));

			}

		}
		Comm.reportVoice(
				new VoiceMessage(Comm.fireMessageVoice(
						MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.ordinal(), b,
						allEntities), 2, 1,
						MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.name()),
				voiceMessages, comparator);
	}

	private void reportCollapse(Building b, int time, int clusterIndex) {
		Comm.reportVoice(
				new VoiceMessage(Comm.fireMessageVoice(
						MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.ordinal(), b,
						allEntities), 2, 1,
						MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.name()),
				voiceMessages, comparator);
		sendSpeak(time, fireChannel,
				Comm.CollapsedMessage(b, time, clusterIndex, allEntities));

		if (fireChannel != ambulanceChannel) {
			sendSpeak(time, ambulanceChannel,
					Comm.CollapsedMessage(b, time, clusterIndex, allEntities));
			Think.addIfNotExists(
					new AKSpeak(me().getID(), time, ambulanceChannel, Comm
							.CollapsedMessage(b, time, clusterIndex,
									allEntities)), dropped);
		}
		if (fireChannel != policeChannel && ambulanceChannel != policeChannel) {
			sendSpeak(time, policeChannel,
					Comm.CollapsedMessage(b, time, clusterIndex, allEntities));

		}

	}

	private void reportExtinguished(Building b, int time, int clusterIndex) {

		Comm.reportVoice(
				new VoiceMessage(Comm.fireMessageVoice(
						MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.ordinal(), b,
						allEntities), 2, 1,
						MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.name()),
				voiceMessages, comparator);
		sendSpeak(time, fireChannel,
				Comm.extinguishedFire(b, time, clusterIndex, allEntities));
		if (radioChannels.size() > 0) {
			if (fireChannel != ambulanceChannel) {
				sendSpeak(time, ambulanceChannel, Comm.extinguishedFire(b,
						time, clusterIndex, allEntities));
				Think.addIfNotExists(
						new AKSpeak(me().getID(), time, ambulanceChannel, Comm
								.extinguishedFire(b, time, clusterIndex,
										allEntities)), dropped);

			}
			if (fireChannel != policeChannel
					&& ambulanceChannel != policeChannel) {
				sendSpeak(time, policeChannel, Comm.extinguishedFire(b, time,
						clusterIndex, allEntities));

			}
		}

	}
	
	/*
	 * public static void main(String[] args) {
	 * 
	 * Registry.SYSTEM_REGISTRY
	 * .registerEntityFactory(StandardEntityFactory.INSTANCE);
	 * Registry.SYSTEM_REGISTRY
	 * .registerMessageFactory(StandardMessageFactory.INSTANCE);
	 * Registry.SYSTEM_REGISTRY
	 * .registerPropertyFactory(StandardPropertyFactory.INSTANCE);
	 * 
	 * Config config = new Config(); int port =
	 * config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
	 * Constants.DEFAULT_KERNEL_PORT_NUMBER); String host =
	 * config.getValue(Constants.KERNEL_HOST_NAME_KEY,
	 * Constants.DEFAULT_KERNEL_HOST_NAME);
	 * 
	 * System.out.println("port: " + port + "\nhost: " + host);
	 * ComponentLauncher launcher = new TCPComponentLauncher(host, port,
	 * config); while (true) try { launcher.connect(new AmbulanceAgent()); }
	 * catch (Exception e) { e.printStackTrace(); break; } }
	 */
}