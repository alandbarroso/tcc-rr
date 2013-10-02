package agents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.swing.text.StyledEditorKit.BoldAction;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import rescuecore2.Constants;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.registry.Registry;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.StandardMessageFactory;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;
import sample.SampleSearch;
import Communication.Comm;
import Communication.RadioChannel;
import Communication.VoiceChannel;
import Communication.Comm.MESSAGE_ID;
import Communication.VoiceMessage;
import PostConnect.PostConnect;
import Think.GraphHelper;
import Think.Think;
import Think.BuildingInformation.BUILDING_LIST;
import clustering.Cluster;
import clustering.Clustering;
import clustering.Density;
import clustering.FireCluster;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FirstPoliceAgent extends StandardAgent<PoliceForce> {
	static int AgentsPostConnectedCounter = 0;

	private static final String DISTANCE_KEY = "clear.repair.distance";
	// private static final String DISTANCE_KEY = "clear.repair.rad";
	int currentTaskNumber = 0;
	int currentClusterNumber = 0;
	private int localID = 0;
	public static final String PLAN_FORMAT = ".GUC_ARTSAPIENCE";
	public static final String PLAN_PREFIX = "PoliceForcePlan";
	public static final String PLAN_DIRECTORY = "precompute/";

	protected ArrayList<Cluster> clusters = null;
	protected Cluster currentCluster;
	protected Cluster originalCluster;

	// Stuck agents and refuge
	ArrayList<EntityID> agentsStuck = new ArrayList<EntityID>();
	ArrayList<EntityID> highPriorityFires = new ArrayList<EntityID>();
	ArrayList<EntityID> highPriorityCivilians = new ArrayList<EntityID>();
	ArrayList<EntityID> mediumPriority = new ArrayList<EntityID>();
	ArrayList<EntityID> lowPriority = new ArrayList<EntityID>();
	ArrayList<EntityID> buildingsEntrance = new ArrayList<EntityID>();

	ArrayList<EntityID> refuges = new ArrayList<EntityID>();

	ArrayList<EntityID> criticalBlockades = new ArrayList<EntityID>();

	ArrayList<Cluster> clustersOriginal = new ArrayList<Cluster>();

	ArrayList<VoiceChannel> voiceChannels = new ArrayList<VoiceChannel>();
	ArrayList<RadioChannel> radioChannels = new ArrayList<RadioChannel>();
	ArrayList<EntityID> buriedCiviliansReported = new ArrayList<EntityID>();
	ArrayList<EntityID> burningOrCollapsedBuildings = new ArrayList<EntityID>();

	int canSubscribeChannels;

	private int distance;

	GraphHelper graphHelper;

	int availableFreeAgents = 0;

	protected EntityID lastPosition = null;
	protected EntityID[] previousPositions = new EntityID[2];
	protected int lastPositionX, lastPositionY;
	int ignoreAgentCommand;

	int policeChannel;
	int fireChannel;
	int ambulanceChannel;
	Blockade currentBlockade = null;
	List<EntityID> currentPath;

	private Logger agentLogger;

	private static final int TIME_TO_LIVE_VOICE = 10;

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

	ArrayList<Building> newBuildingWarm = new ArrayList<Building>();
	ArrayList<Building> newBuildingsOnFire = new ArrayList<Building>();
	ArrayList<Building> newCollapsedBuildings = new ArrayList<Building>();
	ArrayList<Building> newExtinguishedFire = new ArrayList<Building>();

	FireCluster fires;
	boolean precompute = false;

	public FirstPoliceAgent(int preCompute) {
		super();
		localID = AgentsPostConnectedCounter;
		System.out.println("FirstPoliceAgent " + AgentsPostConnectedCounter++
				+ " created!\n");
		this.precompute = (preCompute == 1);
	}

	// never mind about this method, it's only called internally, it tells
	// what's the type of this agent
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	public void postConnect() {
		agentLogger = Logger.getLogger("Police Agent "
				+ this.getID().toString() + " logger");
		agentLogger.setLevel(Level.DEBUG);
		FileAppender fileAppender;
		try {
			fileAppender = new FileAppender(new PatternLayout(), "log/"
					+ getCurrentTimeStamp() + "Police-"
					+ this.getID().toString() + ".log");
			agentLogger.removeAllAppenders();
			agentLogger.addAppender(fileAppender);
			agentLogger.debug("======================================");
			agentLogger.debug("Police Agent " + this.getID().toString()
					+ " connected");
			agentLogger.debug("======================================");
			Clustering c = new Clustering();

			// number of time steps before the simulator responds to agent's
			// commands
			ignoreAgentCommand = config
					.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);
			graphHelper = new GraphHelper(model);
			// Communication part "Noha"
			Comm.discoverChannels(voiceChannels, radioChannels, config);
			String comm = rescuecore2.standard.kernel.comms.ChannelCommunicationModel.PREFIX;
			canSubscribeChannels = config.getIntValue(comm + "max.platoon");

			// I assume that all agents not just police agents inside buildings
			// are
			// buried, so they are r not given tasks
			ArrayList<StandardEntity> nAgentsFree = PostConnect
					.getAgentsNotInsideBuildings(model, model
							.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));

			// ////////////////////////////////////////////////////////////////////////////

			ArrayList<StandardEntity> roads = new ArrayList<StandardEntity>(
					model.getEntitiesOfType(StandardEntityURN.ROAD));

			availableFreeAgents = nAgentsFree.size();

			File file = new File(PLAN_DIRECTORY);
			file.mkdirs();
			file = new File(PLAN_DIRECTORY + PLAN_PREFIX + PLAN_FORMAT);

			if (file.exists() && !precompute) {
				System.out.println("File found");
				clusters = PostConnect.readAllTasks(model, PLAN_DIRECTORY
						+ PLAN_PREFIX + PLAN_FORMAT);
			} else {
				System.out.println("File not found");
				clusters = c.KMeansPlusPlus(nAgentsFree.size(), model, 50,
						roads);
				ArrayList<EntityID> agentsIDs = PostConnect.getIDs(nAgentsFree);

				Density.assignAgentsToClusters2(model, clusters, agentsIDs,
						PLAN_DIRECTORY + PLAN_PREFIX + PLAN_FORMAT, false, true);
			}
			// sets the agents that are assigned to each cluster
			int initialClusterIndex = 0;

			// agent looks for itself in all clusters
			for (Cluster cl : clusters) {
				boolean found1 = false;
				for (EntityID ag : cl.agents) {

					if (ag.getValue() == this.getID().getValue()) {
						found1 = true;
						break;

					}
				}
				if (found1)
					break;
				initialClusterIndex++;
			}
			// in case agent doesn't find itself in a cluster (meaning it was in
			// a building)
			// it assigns itself to the nearest cluster
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

			// /////////////////////////////////////////////////////////

			// first task of police agent in case no comm : go to refuge &
			// agents in buildings only
			// in case of comm: go to refuge

			Collection<StandardEntity> task = null;
			Collection<StandardEntity> agents = null;
			if (radioChannels.size() != 0) {
				task = model.getEntitiesOfType(StandardEntityURN.REFUGE);
				for (StandardEntity e : task) {
					refuges.add(e.getID());
				}
				task.addAll(model
						.getEntitiesOfType(StandardEntityURN.GAS_STATION));
			} else {
				task = model.getEntitiesOfType(StandardEntityURN.REFUGE);
				for (StandardEntity e : task) {
					refuges.add(e.getID());
				}
				task.addAll(model
						.getEntitiesOfType(StandardEntityURN.GAS_STATION));
				agents = (model
						.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
				agents.addAll(model
						.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
				agents.addAll(model
						.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
			}

			currentClusterNumber = initialClusterIndex;
			currentCluster = clusters.get(currentClusterNumber);
			originalCluster = currentCluster;

			// only important roads are selected as targets which have buildings
			// as neighbours
			buildingsEntrance = Think.getClusterBuildingsEntrance(model,
					currentCluster);

			for (StandardEntity target : PostConnect.formTask(model, task,
					graphHelper.getSearch(), (Human) me())) {
				double threshold = Think.thresholdDistance(3, (Area) target,
						clusters) - 1;
				double threshold2 = Think.thresholdDistance(2, (Area) target,
						clusters) - 1;

				if (!Think.exists(target.getID(), currentCluster.cluster)
						&& !Think.exists(target.getID(),
								originalCluster.cluster)
						&& model.getDistance(target.getID(), clusters
								.get(currentClusterNumber).center.getID()) > threshold
						&& model.getDistance(target.getID(), location().getID()) > threshold2)
					continue;
				Think.addIfNotExists(target.getID(), highPriorityFires);
				Think.removeIfExists(target.getID(), buildingsEntrance);
				// Think.removeIfExists(target.getID(), currentCluster.cluster);
			}
			if (agents != null)
				for (StandardEntity target : PostConnect.formTask(model,
						agents, graphHelper.getSearch(), (Human) me())) {

					if (!Think.exists(target.getID(), currentCluster.cluster)
							&& !Think.exists(target.getID(),
									originalCluster.cluster))
						continue;
					Think.addIfNotExists(target.getID(), agentsStuck);
					Think.removeIfExists(target.getID(), buildingsEntrance);
					// Think.removeIfExists(target.getID(),
					// currentCluster.cluster);
				}

			distance = config.getIntValue(DISTANCE_KEY);

			lastPositionX = me().getX();
			lastPositionY = me().getY();
			for (Cluster cluster : clusters) {
				clustersOriginal.add(Think.copyCluster(cluster));
			}

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

	protected void think(int time, ChangeSet arg1, Collection<Command> heard) {
		if (radioChannels.size() != 0)
			thinkWithCommunication(time, arg1, heard);
		else
			thinkWithVoice(time, arg1, heard);

	}

	protected void thinkWithCommunication(int time, ChangeSet changeSet,
			Collection<Command> heard) {

		model.merge(changeSet);

		if (time < ignoreAgentCommand)
			return;

		if (time == ignoreAgentCommand) {
			if (radioChannels.size() != 0 && canSubscribeChannels > 0) {
				fireChannel = Comm.decideRadioChannel(radioChannels, 'f');
				ambulanceChannel = Comm.decideRadioChannel(radioChannels, 'a');
				policeChannel = Comm.decideRadioChannel(radioChannels, 'p');
				sendSubscribe(time, policeChannel);
			}

			if (me().getBuriedness() > 0) {
				sendSpeak(time, ambulanceChannel,
						Comm.buriedAgent(me().getPosition(), allEntities));
			}
		} else {
			handleMessagesRadio(time, heard);
			resendMessages(time, heard);
		}

		handleFires(changeSet, time,
				Think.getChangedBuildings(model, changeSet));

		reportLocationOfBuriedHumans(time, changeSet, model, ambulanceChannel);

		if (me().getBuriedness() > 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.buriedAgent(me().getPosition(),
							allEntities), 1, 1,
							MESSAGE_ID.BURIED_AGENT_LOCATION.name()),
					voiceMessages, comparator);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			sendRest(time);
			return;
		}
		if (lastPosition != null
				&& currentPath != null
				&& lastPosition.getValue() == me().getPosition().getValue()
				&& Math.hypot(Math.abs(me().getX() - lastPositionX),
						Math.abs(me().getY() - lastPositionY)) < 8000) {
			if (currentPath.size() > 2
					&& model.getEntity(lastPosition) instanceof Building) {
				EntityID firstStep = currentPath.get(0);
				EntityID secondStep = currentPath.get(1);
				currentPath.clear();
				currentPath.add(firstStep);
				currentPath.add(secondStep);
				agentLogger.debug("time: " + time + " stuck in "
						+ lastPosition.getValue()
						+ " and moving 2 steps in path: "
						+ Think.ArrayListEntityIDtoString(currentPath));
				previousPositions[1] = previousPositions[0];
				previousPositions[0] = lastPosition;
				lastPosition = me().getPosition();
				lastPositionX = me().getX();
				lastPositionY = me().getY();
				sendMove(time, currentPath);
				sendSpeak(time, 0, Comm.reportAllVoiceMessages(
						voiceChannels.get(0), voiceMessages));
				return;
			} else {
				if (clearClosestBlockade(time, changeSet)) {
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				Blockade closestBlockade = getclosestBlockade(changeSet);
				if (currentPath.size() == 1) {
					if (closestBlockade != null) {
						List<EntityID> path = new ArrayList<EntityID>();
						path.add(location().getID());
						agentLogger.debug("time: " + time
								+ " moving to blockade blocking movement");
						previousPositions[1] = previousPositions[0];
						previousPositions[0] = lastPosition;
						lastPosition = me().getPosition();
						lastPositionX = me().getX();
						lastPositionY = me().getY();
						criticalBlockades.add(closestBlockade.getID());
						sendMove(time, path, closestBlockade.getX(),
								closestBlockade.getY());
						sendSpeak(
								time,
								0,
								Comm.reportAllVoiceMessages(
										voiceChannels.get(0), voiceMessages));
						return;
					}
				} else {
					EntityID firstStep = currentPath.get(0);
					currentPath.clear();
					currentPath.add(firstStep);
					agentLogger.debug("time: " + time + " stuck in "
							+ lastPosition.getValue()
							+ " and moving to next step in the path: "
							+ Think.ArrayListEntityIDtoString(currentPath));
				}
			}
			previousPositions[1] = previousPositions[0];
			previousPositions[0] = lastPosition;
			lastPosition = me().getPosition();
			lastPositionX = me().getX();
			lastPositionY = me().getY();
			sendMove(time, currentPath);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		if (lastPosition != null && previousPositions[0] != null
				&& previousPositions[1] != null) {
			if (me().getPosition().getValue() == previousPositions[0]
					.getValue()
					&& lastPosition.getValue() == previousPositions[0]
							.getValue()) {
				if (clearClosestBlockade(time, changeSet)) {
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				Blockade closestBlockade = getclosestBlockade(changeSet);
				if (closestBlockade != null) {
					List<EntityID> path = new ArrayList<EntityID>();
					path.add(location().getID());
					agentLogger.debug("time: " + time
							+ " moving to blockade blocking movement");
					previousPositions[1] = previousPositions[0];
					previousPositions[0] = lastPosition;
					lastPosition = me().getPosition();
					lastPositionX = me().getX();
					lastPositionY = me().getY();
					criticalBlockades.add(closestBlockade.getID());
					sendMove(time, path, closestBlockade.getX(),
							closestBlockade.getY());
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
			}
		}

		updateClearRoads(time, changeSet);

		if (me().getDamage() > 0) {
			if (!(location() instanceof Refuge)) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), refuges);
				if (currentPath == null) {
					graphHelper.resetGraph();
					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), refuges);
				}

				for (Blockade b : Think.getBlockades(model, changeSet)) {
					if (blockadeOnPath(b)) {
						if (Think.findDistanceTo(b, me().getX(), me().getY()) < distance) {
							lastPosition = null;
							sendClear(time, b.getID());
							sendSpeak(time, 0, Comm.reportAllVoiceMessages(
									voiceChannels.get(0), voiceMessages));
							return;
						} else {
							if (moveToBlockade(time)) {
								sendSpeak(time, 0, Comm.reportAllVoiceMessages(
										voiceChannels.get(0), voiceMessages));
								return;
							}
						}
					}
				}
				previousPositions[1] = previousPositions[0];
				previousPositions[0] = lastPosition;
				lastPosition = me().getPosition();
				lastPositionX = me().getX();
				lastPositionY = me().getY();
				sendMove(time, currentPath);
			}
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		if (me().getDamage() == 0 && location() instanceof Refuge) {
			currentPath = null;
		}
		agentLogger.debug("time: " + time + " high priority fires: "
				+ Think.ArrayListEntityIDtoString(highPriorityFires));

		if (!highPriorityFires.isEmpty()) {
			currentTaskNumber = 0;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), highPriorityFires);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}

		agentLogger.debug("time: " + time + " first task: "
				+ Think.ArrayListEntityIDtoString(agentsStuck));

		if (!agentsStuck.isEmpty()) {
			currentTaskNumber = 1;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), agentsStuck);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		agentLogger.debug("time: " + time + " high priority civilians: "
				+ Think.ArrayListEntityIDtoString(highPriorityCivilians));

		if (!highPriorityCivilians.isEmpty()) {
			currentTaskNumber = 1;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), highPriorityCivilians);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		agentLogger.debug("time: " + time + " medium priority: "
				+ Think.ArrayListEntityIDtoString(mediumPriority));

		if (!mediumPriority.isEmpty()) {
			currentTaskNumber = 2;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), mediumPriority);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;

		}
		agentLogger.debug("time: " + time + " low priority: "
				+ Think.ArrayListEntityIDtoString(lowPriority));

		if (!lowPriority.isEmpty()) {
			currentTaskNumber = 3;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), lowPriority);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;

		}
		agentLogger.debug("time: " + time + " building entrances: "
				+ Think.ArrayListEntityIDtoString(buildingsEntrance));

		currentTaskNumber = 4;
		if (currentPath == null) {
			if (!buildingsEntrance.isEmpty()) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), buildingsEntrance);
			} else {
				if (changeCluster(time)) {
					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), buildingsEntrance);
				}
			}
		} else {
			currentPath = graphHelper.getSearch()
					.breadthFirstSearch(me().getPosition(),
							currentPath.get(currentPath.size() - 1));
		}
		handleBlockades(time, changeSet);
		sendSpeak(time, 0, Comm.reportAllVoiceMessages(voiceChannels.get(0),
				voiceMessages));
	}

	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	private void updateClearRoads(int time, ChangeSet changeSet) {
		ArrayList<Road> clearRoads = Think.getClearedRoadsNew(model, changeSet);
		for (Road r : clearRoads) {
			List<EntityID> roadBlockades = r.getBlockades();
			if (roadBlockades != null && !roadBlockades.isEmpty()) {
				if (Think.isBuildingEntrance(model, r))
					continue;
				boolean clear = true;
				for (EntityID bID : roadBlockades)
					if (Think.exists(bID, criticalBlockades)) {
						clear = false;
						break;
					}
				if (!clear)
					continue;
			}

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
				if (model.getEntity(e) instanceof Building) {
					if (currentPath != null
							&& currentPath.get(currentPath.size() - 1)
									.getValue() == e.getValue()) {
						agentLogger.debug("time: " + time
								+ "nullify current path 1");
						currentPath = null;
					}
					if (Think.removeIfExists(e, agentsStuck)
							|| Think.removeIfExists(e, highPriorityFires)
							|| Think.removeIfExists(e, highPriorityCivilians)
							|| Think.removeIfExists(e, mediumPriority)
							|| Think.removeIfExists(e, lowPriority)) {
						if (radioChannels.size() > 0) {
							sendSpeak(time, ambulanceChannel,
									Comm.clearedRoad(e, allEntities));
							Comm.reportVoice(
									new VoiceMessage(Comm.clearedRoad(e,
											allEntities), 3, 1,
											MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID
													.name()), voiceMessages,
									comparator);

							if (ambulanceChannel != policeChannel) {
								sendSpeak(time, policeChannel,
										Comm.clearedRoad(e, allEntities));
								Think.addIfNotExists(
										new AKSpeak(me().getID(), time,
												policeChannel, Comm
														.clearedRoad(e,
																allEntities)),
										dropped);
							}

							if (fireChannel != policeChannel
									&& ambulanceChannel != fireChannel) {
								sendSpeak(time, fireChannel,
										Comm.clearedRoad(e, allEntities));
							}
						} else
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

			if (currentPath != null
					&& currentPath.get(currentPath.size() - 1).getValue() == r
							.getID().getValue()) {
				agentLogger.debug("time: " + time + "nullify current path 2");
				currentPath = null;
			}
			if (Think.removeIfExists(r.getID(), agentsStuck)
					|| Think.removeIfExists(r.getID(), highPriorityFires)
					|| Think.removeIfExists(r.getID(), highPriorityCivilians)
					|| Think.removeIfExists(r.getID(), mediumPriority)
					|| Think.removeIfExists(r.getID(), lowPriority)) {
				if (radioChannels.size() > 0) {
					sendSpeak(time, ambulanceChannel,
							Comm.clearedRoad(r.getID(), allEntities));
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedRoad(r.getID(),
									allEntities), 3, 1,
									MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID.name()),
							voiceMessages, comparator);

					if (ambulanceChannel != policeChannel) {
						sendSpeak(time, policeChannel,
								Comm.clearedRoad(r.getID(), allEntities));
						Think.addIfNotExists(
								new AKSpeak(
										me().getID(),
										time,
										policeChannel,
										Comm.clearedRoad(r.getID(), allEntities)),
								dropped);
					}
					if (fireChannel != policeChannel
							&& ambulanceChannel != fireChannel) {
						sendSpeak(time, fireChannel,
								Comm.clearedRoad(r.getID(), allEntities));
					}
				} else
					Comm.reportVoice(
							new VoiceMessage(Comm.clearedRoad(r.getID(),
									allEntities), 3, TIME_TO_LIVE_VOICE,
									MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID.name()),
							voiceMessages, comparator);
			}
			// Think.removeIfExists(r.getID(), currentCluster.cluster);
			Think.removeIfExists(r.getID(), buildingsEntrance);
		}
	}

	private boolean clearClosestBlockade(int time, ChangeSet changeSet) {
		ArrayList<Blockade> seenBlockades = Think
				.getBlockades(model, changeSet);
		Collections.sort(seenBlockades, new DistanceSorter(location(), model));
		for (Blockade b : seenBlockades) {
			if (Think.findDistanceTo(b, me().getX(), me().getY()) < distance) {
				agentLogger.debug("time: " + time + " blockade: " + b.getID()
						+ " blocking movement will be cleared");
				lastPosition = null;
				sendClear(time, b.getID());
				return true;
			}
		}
		return false;
	}

	private void handleBlockades(int time, ChangeSet changeSet) {
		ArrayList<Blockade> seenBlockades = Think
				.getBlockades(model, changeSet);
		Collections.sort(seenBlockades, new DistanceSorter(location(), model));
		agentLogger.debug("time: " + time + " current path: "
				+ Think.ArrayListEntityIDtoString(currentPath));

		for (Blockade b : seenBlockades) {
			if (Think.findDistanceTo(b, me().getX(), me().getY()) < distance) {
				if (Think.exists(b.getID(), criticalBlockades)) {
					lastPosition = null;
					agentLogger.debug("time: " + time + " clearing blockade: "
							+ b.getID() + " on road: " + b.getPosition()
							+ "from 1");
					sendClear(time, b.getID());
					return;
				}
				if (Think.isHumanStuckInBlockade(model, changeSet, b)) {
					criticalBlockades.add(b.getID());
					lastPosition = null;
					agentLogger.debug("time: " + time + " clearing blockade: "
							+ b.getID() + " on road: " + b.getPosition()
							+ " from 2");
					sendClear(time, b.getID());
					return;
				}
				Road blockedRoad = (Road) model.getEntity(b.getPosition());
				if (blockadeOnPath(b)) {
					if (Think.isBuildingEntrance(model, blockedRoad)
							|| Think.isBlockingRoad(model, b)) {
						lastPosition = null;
						agentLogger.debug("time: " + time
								+ " clearing blockade: " + b.getID()
								+ " on road: " + b.getPosition() + " from 3");
						sendClear(time, b.getID());
						return;
					}
				} else {
					if (currentTaskNumber > 0
							&& (entranceToCriticalBuilding(blockedRoad) || (neighbourToCriticalBuildingEntrance(blockedRoad) && Think
									.isBlockingRoad(model, b)))) {
						lastPosition = null;
						agentLogger.debug("time: " + time
								+ " clearing blockade: " + b.getID()
								+ " on road: " + b.getPosition() + " from 4");
						sendClear(time, b.getID());
						return;
					}
					if (currentTaskNumber > 2
							&& (isNonBurningBuildingEntrance(model, blockedRoad) || (isNeighbourToNonBurningBuildingEntrance(
									model, blockedRoad) && Think
									.isBlockingRoad(model, b)))) {
						lastPosition = null;
						agentLogger.debug("time: " + time
								+ " clearing blockade: " + b.getID()
								+ " on road: " + b.getPosition() + " from 5");
						sendClear(time, b.getID());
						return;
					}
				}
			}
		}

		if (moveToBlockade(time)) {
			return;
		}
		if (currentPath != null) {
			agentLogger.debug("time: " + time + " moving to target");
			previousPositions[1] = previousPositions[0];
			previousPositions[0] = lastPosition;
			lastPosition = me().getPosition();
			lastPositionX = me().getX();
			lastPositionY = me().getY();
			sendMove(time, currentPath);
		} else {
			agentLogger.debug("time: " + time + " random walk");
			sendMove(time, Think.randomWalk(me().getPosition(),
					graphHelper.getGraph(), random));
		}
	}

	public boolean isNonBurningBuildingEntrance(StandardWorldModel model,
			Road road) {
		for (EntityID e : road.getNeighbours()) {
			StandardEntity b = model.getEntity(e);
			if (b != null && b instanceof Building
					&& !Think.exists(e, burningOrCollapsedBuildings)) {
				return true;
			}
		}
		return false;
	}

	public boolean isNeighbourToNonBurningBuildingEntrance(
			StandardWorldModel model, Road road) {
		for (EntityID e : road.getNeighbours()) {
			StandardEntity r = model.getEntity(e);
			if (r != null && r instanceof Road
					&& isNonBurningBuildingEntrance(model, (Road) r)) {
				return true;
			}
		}
		return false;
	}

	private boolean changeCluster(int time) {
		// currentCluster.cluster.clear();
		if (radioChannels.size() > 0) {
			sendSpeak(time, policeChannel,
					Comm.finishedClusterPolice(currentClusterNumber));
			Comm.reportVoice(
					new VoiceMessage(Comm
							.finishedClusterPolice(currentClusterNumber), 3, 1,
							MESSAGE_ID.FINISHED_CLUSTER_POLICE_MESSAGE_ID
									.name()), voiceMessages, comparator);
		} else
			Comm.reportVoice(
					new VoiceMessage(Comm
							.finishedClusterPolice(currentClusterNumber), 3,
							TIME_TO_LIVE_VOICE,
							MESSAGE_ID.FINISHED_CLUSTER_POLICE_MESSAGE_ID
									.name()), voiceMessages, comparator);
		int tempCIndex = Clustering.getClosestNotEmptyDiffClusterPolice(me()
				.getPosition(), clusters, model, currentClusterNumber);

		if (tempCIndex != -1) {
			currentCluster = clusters.get(tempCIndex);
			buildingsEntrance = Think.getClusterBuildingsEntrance(model,
					currentCluster);
			currentClusterNumber = tempCIndex;
			return true;
		}
		return false;
	}

	protected void thinkWithVoice(int time, ChangeSet changeSet,
			Collection<Command> heard) {

		model.merge(changeSet);

		if (time < ignoreAgentCommand)
			return;

		if (time == ignoreAgentCommand) {
			policeChannel = fireChannel = ambulanceChannel = 0;
		} else {
			handleMessagesRadio(time, heard);
		}

		if (me().getBuriedness() > 0) {
			Comm.reportVoice(
					new VoiceMessage(Comm.buriedAgent(me().getPosition(),
							allEntities), 1, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.BURIED_AGENT_LOCATION.name()),
					voiceMessages, comparator);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		handleFires(changeSet, time,
				Think.getChangedBuildings(model, changeSet));

		reportLocationOfBuriedHumans(time, changeSet, model, ambulanceChannel);

		if (lastPosition != null
				&& currentPath != null
				&& lastPosition.getValue() == me().getPosition().getValue()
				&& Math.hypot(Math.abs(me().getX() - lastPositionX),
						Math.abs(me().getY() - lastPositionY)) < 8000) {
			if (currentPath.size() > 2
					&& model.getEntity(lastPosition) instanceof Building) {
				EntityID firstStep = currentPath.get(0);
				EntityID secondStep = currentPath.get(1);
				currentPath.clear();
				currentPath.add(firstStep);
				currentPath.add(secondStep);
				agentLogger.debug("time: " + time + " stuck in "
						+ lastPosition.getValue()
						+ " and moving 2 steps in path: "
						+ Think.ArrayListEntityIDtoString(currentPath));
				previousPositions[1] = previousPositions[0];
				previousPositions[0] = lastPosition;
				lastPosition = me().getPosition();
				lastPositionX = me().getX();
				lastPositionY = me().getY();
				sendMove(time, currentPath);
				sendSpeak(time, 0, Comm.reportAllVoiceMessages(
						voiceChannels.get(0), voiceMessages));
				return;
			} else {
				if (clearClosestBlockade(time, changeSet)) {
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				Blockade closestBlockade = getclosestBlockade(changeSet);
				if (currentPath.size() == 1) {
					if (closestBlockade != null) {
						List<EntityID> path = new ArrayList<EntityID>();
						path.add(location().getID());
						agentLogger.debug("time: " + time
								+ " moving to blockade blocking movement");
						previousPositions[1] = previousPositions[0];
						previousPositions[0] = lastPosition;
						lastPosition = me().getPosition();
						lastPositionX = me().getX();
						lastPositionY = me().getY();
						criticalBlockades.add(closestBlockade.getID());
						sendMove(time, path, closestBlockade.getX(),
								closestBlockade.getY());
						sendSpeak(
								time,
								0,
								Comm.reportAllVoiceMessages(
										voiceChannels.get(0), voiceMessages));
						return;
					}
				} else {
					EntityID firstStep = currentPath.get(0);
					currentPath.clear();
					currentPath.add(firstStep);
					agentLogger.debug("time: " + time + " stuck in "
							+ lastPosition.getValue()
							+ " and moving to next step in the path: "
							+ Think.ArrayListEntityIDtoString(currentPath));
				}
			}
			previousPositions[1] = previousPositions[0];
			previousPositions[0] = lastPosition;
			lastPosition = me().getPosition();
			lastPositionX = me().getX();
			lastPositionY = me().getY();
			sendMove(time, currentPath);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		if (lastPosition != null && previousPositions[0] != null
				&& previousPositions[1] != null) {
			if (me().getPosition().getValue() == previousPositions[0]
					.getValue()
					&& lastPosition.getValue() == previousPositions[0]
							.getValue()) {
				if (clearClosestBlockade(time, changeSet)) {
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
				Blockade closestBlockade = getclosestBlockade(changeSet);
				if (closestBlockade != null) {
					List<EntityID> path = new ArrayList<EntityID>();
					path.add(location().getID());
					agentLogger.debug("time: " + time
							+ " moving to blockade blocking movement");
					previousPositions[1] = previousPositions[0];
					previousPositions[0] = lastPosition;
					lastPosition = me().getPosition();
					lastPositionX = me().getX();
					lastPositionY = me().getY();
					criticalBlockades.add(closestBlockade.getID());
					sendMove(time, path, closestBlockade.getX(),
							closestBlockade.getY());
					sendSpeak(time, 0, Comm.reportAllVoiceMessages(
							voiceChannels.get(0), voiceMessages));
					return;
				}
			}
		}

		updateClearRoads(time, changeSet);

		if (me().getDamage() > 0) {
			if (!(location() instanceof Refuge)) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), refuges);
				if (currentPath == null) {
					graphHelper.resetGraph();
					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), refuges);
				}

				for (Blockade b : Think.getBlockades(model, changeSet)) {
					if (blockadeOnPath(b)) {
						if (Think.findDistanceTo(b, me().getX(), me().getY()) < distance) {
							lastPosition = null;
							sendClear(time, b.getID());
							sendSpeak(time, 0, Comm.reportAllVoiceMessages(
									voiceChannels.get(0), voiceMessages));
							return;
						} else {
							if (moveToBlockade(time)) {
								sendSpeak(time, 0, Comm.reportAllVoiceMessages(
										voiceChannels.get(0), voiceMessages));
								return;
							}
						}
					}
				}
				previousPositions[1] = previousPositions[0];
				previousPositions[0] = lastPosition;
				lastPosition = me().getPosition();
				lastPositionX = me().getX();
				lastPositionY = me().getY();
				sendMove(time, currentPath);
			}
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		if (me().getDamage() == 0 && location() instanceof Refuge) {
			currentPath = null;
		}

		agentLogger.debug("time: " + time + " high priority fires: "
				+ Think.ArrayListEntityIDtoString(highPriorityFires));

		if (!highPriorityFires.isEmpty()) {
			currentTaskNumber = 0;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), highPriorityFires);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;

		}
		agentLogger.debug("time: " + time + " first task: "
				+ Think.ArrayListEntityIDtoString(agentsStuck));

		if (!agentsStuck.isEmpty()) {
			currentTaskNumber = 1;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), agentsStuck);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;

		}

		agentLogger.debug("time: " + time + " high priority civilians: "
				+ Think.ArrayListEntityIDtoString(highPriorityCivilians));

		if (!highPriorityCivilians.isEmpty()) {
			currentTaskNumber = 1;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), highPriorityCivilians);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;
		}
		agentLogger.debug("time: " + time + " medium priority: "
				+ Think.ArrayListEntityIDtoString(mediumPriority));

		if (!mediumPriority.isEmpty()) {
			currentTaskNumber = 2;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), mediumPriority);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;

		}
		agentLogger.debug("time: " + time + " low priority: "
				+ Think.ArrayListEntityIDtoString(lowPriority));

		if (!lowPriority.isEmpty()) {
			currentTaskNumber = 3;
			if (currentPath == null) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), lowPriority);
			} else {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(),
						currentPath.get(currentPath.size() - 1));
			}
			handleBlockades(time, changeSet);
			sendSpeak(time, 0, Comm.reportAllVoiceMessages(
					voiceChannels.get(0), voiceMessages));
			return;

		}
		agentLogger.debug("time: " + time + " building entrances: "
				+ Think.ArrayListEntityIDtoString(buildingsEntrance));

		currentTaskNumber = 4;
		if (currentPath == null) {
			if (!buildingsEntrance.isEmpty()) {
				currentPath = graphHelper.getSearch().breadthFirstSearch(
						me().getPosition(), buildingsEntrance);
			} else {
				if (changeCluster(time)) {
					currentPath = graphHelper.getSearch().breadthFirstSearch(
							me().getPosition(), buildingsEntrance);
				}
			}
		} else {
			currentPath = graphHelper.getSearch()
					.breadthFirstSearch(me().getPosition(),
							currentPath.get(currentPath.size() - 1));
		}
		handleBlockades(time, changeSet);
		sendSpeak(time, 0, Comm.reportAllVoiceMessages(voiceChannels.get(0),
				voiceMessages));
	}

	private boolean neighbourToCriticalBuildingEntrance(Road road) {
		for (EntityID e : road.getNeighbours()) {
			StandardEntity entity = model.getEntity(e);
			if (entity instanceof Road
					&& (entranceToCriticalBuilding((Road) entity) || entranceToBurningBuilding((Road) entity))) {
				return true;
			}
		}
		return false;
	}

	private boolean entranceToCriticalBuilding(Road road) {
		for (EntityID e : road.getNeighbours()) {
			if (Think.exists(e, buriedCiviliansReported)
					&& !Think.exists(e, burningOrCollapsedBuildings)) {
				return true;
			}
		}
		return false;
	}

	private boolean entranceToBurningBuilding(Road road) {
		for (EntityID e : road.getNeighbours()) {
			if (model.getEntity(e) instanceof Building
					&& Think.exists(e, burningOrCollapsedBuildings))
				return true;
		}
		return false;
	}

	// returns the position of the blockade in the agent's location
	private int[] BlockadePosition(int time) {
		Area location = (Area) location();
		List<EntityID> result = location.getBlockades();
		if (result == null || result.size() == 0)
			return null;
		for (EntityID bID : result) {
			StandardEntity b = model.getEntity(bID);
			if (b != null && Think.isBlockingRoad(model, (Blockade) b)) {
				int x = ((Blockade) b).getX();
				int y = ((Blockade) b).getY();
				int[] position = new int[2];
				position[0] = x;
				position[1] = y;
				return position;
			}
		}
		return null;
	}

	// checks if the given blockade is in the agents current location or on his path to the current target
	private boolean blockadeOnPath(Blockade blockade) {
		if (blockade.getPosition().getValue() == location().getID().getValue()) {
			return true;
		}
		if (currentPath != null) {
			for (EntityID pathID : currentPath) {
				if (blockade.getPosition().getValue() == pathID.getValue()) {
					return true;
				}
			}
		}
		return false;
	}
	
	// returns the closest blockade to the agent on his path
	private Blockade getclosestBlockade(ChangeSet changeSet) {
		ArrayList<Blockade> seenBlockades = Think
				.getBlockades(model, changeSet);
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
			// return seenBlockades.get(0);
		}
		return null;
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

	private void handleMessagesRadio(int time, Collection<Command> heard) {
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

				case BLOCKED_ROAD_PRIORITIZED: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int priority = Integer.parseInt(msg[2]);
					double threshold = Think.thresholdDistance(3,
							(Area) model.getEntity(id), clusters) - 1;
					double threshold2 = Think.thresholdDistance(2,
							(Area) model.getEntity(id), clusters) - 1;
					// if (Think.exists(id, agentsStuck))
					// continue;
					if (priority == 3) {
						if (!Think.exists(id, currentCluster.cluster)
								&& !Think.exists(id, originalCluster.cluster)
								&& model.getDistance(id, clusters
										.get(currentClusterNumber).center
										.getID()) > threshold
								&& model.getDistance(id, location().getID()) > threshold2)
							continue;
						Think.addIfNotExists(id, highPriorityFires);
						Think.removeIfExists(id, agentsStuck);
						Think.removeIfExists(id, highPriorityCivilians);
						Think.removeIfExists(id, mediumPriority);
						Think.removeIfExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						// Think.removeIfExists(id, currentCluster.cluster);
						continue;
					}
					if (!Think.exists(id, currentCluster.cluster)
							&& !Think.exists(id, originalCluster.cluster))
						continue;

					if (priority == 2 && !Think.exists(id, highPriorityFires)
							&& !Think.exists(id, agentsStuck)) {
						Think.addIfNotExists(id, highPriorityCivilians);
						Think.removeIfExists(id, mediumPriority);
						Think.removeIfExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						// Think.removeIfExists(id, currentCluster.cluster);
						continue;
					}
					if (priority == 1 && !Think.exists(id, highPriorityFires)
							&& !Think.exists(id, agentsStuck)
							&& !Think.exists(id, highPriorityCivilians)) {
						Think.addIfNotExists(id, mediumPriority);
						Think.removeIfExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						// Think.removeIfExists(id, currentCluster.cluster);
						continue;
					}
					if (priority == 0 && !Think.exists(id, highPriorityFires)
							&& !Think.exists(id, agentsStuck)
							&& !Think.exists(id, highPriorityCivilians)
							&& !Think.exists(id, mediumPriority)) {
						Think.addIfNotExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						// Think.removeIfExists(id, currentCluster.cluster);
						continue;
					}
				}
					;
					break;

				case CLEARED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();

					Think.removeIfExists(id, agentsStuck);
					Think.removeIfExists(id, highPriorityFires);
					Think.removeIfExists(id, highPriorityCivilians);
					Think.removeIfExists(id, mediumPriority);
					Think.removeIfExists(id, lowPriority);
					Think.removeIfExists(id, buildingsEntrance);
					// Think.removeIfExists(id, currentCluster.cluster);
				}
					;
					break;
				case FINISHED_CLUSTER_POLICE_MESSAGE_ID: {
					int cId = Integer.parseInt(msg[1]);
					Think.removeEntityIDs(agentsStuck,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(highPriorityFires,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(highPriorityCivilians,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(mediumPriority,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(lowPriority,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(buildingsEntrance,
							clusters.get(cId).cluster);

					// clusters.get(cId).cluster.clear();
					if (cId == currentClusterNumber && currentTaskNumber == 4) {
						changeCluster(time);
					}
				}
					;
					break;
				case STUCK_INSIDE_BLOCKADE: {
					EntityID location = allEntities.get(
							Integer.parseInt(msg[1])).getID();
					int bId = Integer.parseInt(msg[2]);
					EntityID blockadeID = new EntityID(bId);

					if (!Think.exists(location, currentCluster.cluster)
							&& !Think.exists(location, originalCluster.cluster))
						continue;
					Think.addIfNotExists(blockadeID, criticalBlockades);

					if (Think.exists(location, highPriorityFires))
						continue;
					Think.addIfNotExists(location, agentsStuck);
					// Think.removeIfExists(location, highPriorityFires);
					Think.removeIfExists(location, highPriorityCivilians);
					Think.removeIfExists(location, mediumPriority);
					Think.removeIfExists(location, lowPriority);
					Think.removeIfExists(location, buildingsEntrance);
					// Think.removeIfExists(id, currentCluster.cluster);

				}
					;
					break;
				case BUILDING_ON_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					handleReportedFire(id, message.getTime());
				}
					;
					break;
				case COLLAPSED_BUILDING_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					handleReportedCollapsed(id, message.getTime());
				}
					;
					break;
				case EXTINGUISHED_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					handleReportedExtinguished(id, message.getTime());
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
				}
			} catch (NumberFormatException e) {
				/*
				 * EntityID id = message.getAgentID(); if
				 * (m.equalsIgnoreCase("help")) { System.out.print("help"); //
				 * sendSpeak(time, 1, (4 + "," + id).getBytes());
				 *//**********************************************************/
				/*
				 * sendSpeak(time, ambulanceChannel,
				 * CommMsg.agentLocation(me().getPosition()));
				 *//**********************************************************/
				/*
				 * } else if (m.equalsIgnoreCase("ouch")) {
				 * System.out.print("ouch");
				 *//**********************************************************/
				/*
				 * sendSpeak(time, ambulanceChannel,
				 * CommMsg.agentLocation(me().getPosition())); sendSpeak(time,
				 * fireChannel, CommMsg.agentLocation(me().getPosition()));
				 *//**********************************************************/
				/*
				 * // sendSpeak(time, 1, (5 + "," + id).getBytes()); }
				 */
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

				case BLOCKED_ROAD_PRIORITIZED: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int priority = Integer.parseInt(msg[2]);
					int ttl = Integer.parseInt(msg[3]);

					double threshold = Think.thresholdDistance(3,
							(Area) model.getEntity(id), clusters) - 1;
					double threshold2 = Think.thresholdDistance(2,
							(Area) model.getEntity(id), clusters) - 1;
					if (priority == 3) {
						if (!Think.exists(id, currentCluster.cluster)
								&& !Think.exists(id, originalCluster.cluster)
								&& model.getDistance(id, clusters
										.get(currentClusterNumber).center
										.getID()) > threshold
								&& model.getDistance(id, location().getID()) > threshold2)
							continue;
						Think.addIfNotExists(id, highPriorityFires);
						Think.removeIfExists(id, agentsStuck);
						Think.removeIfExists(id, highPriorityCivilians);
						Think.removeIfExists(id, mediumPriority);
						Think.removeIfExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						continue;
					}
					if (!Think.exists(id, currentCluster.cluster)
							&& !Think.exists(id, originalCluster.cluster))
						continue;

					if (priority == 2 && !Think.exists(id, highPriorityFires)
							&& !Think.exists(id, agentsStuck)) {
						Think.addIfNotExists(id, highPriorityCivilians);
						Think.removeIfExists(id, mediumPriority);
						Think.removeIfExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						continue;
					}
					if (priority == 1 && !Think.exists(id, highPriorityFires)
							&& !Think.exists(id, agentsStuck)
							&& !Think.exists(id, highPriorityCivilians)) {
						Think.addIfNotExists(id, mediumPriority);
						Think.removeIfExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						continue;
					}
					if (priority == 0 && !Think.exists(id, highPriorityFires)
							&& !Think.exists(id, agentsStuck)
							&& !Think.exists(id, highPriorityCivilians)
							&& !Think.exists(id, mediumPriority)) {
						Think.addIfNotExists(id, lowPriority);
						Think.removeIfExists(id, buildingsEntrance);
						continue;
					}
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 3,
									ttl - 1,
									MESSAGE_ID.BLOCKED_ROAD_PRIORITIZED.name()),
							voiceMessages, comparator);

				}
					;
					break;

				case CLEARED_ROAD_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					Think.removeIfExists(id, agentsStuck);
					Think.removeIfExists(id, highPriorityFires);
					Think.removeIfExists(id, highPriorityCivilians);
					Think.removeIfExists(id, mediumPriority);
					Think.removeIfExists(id, lowPriority);
					Think.removeIfExists(id, buildingsEntrance);
					int ttl = Integer.parseInt(msg[2]);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 3,
									ttl - 1, MESSAGE_ID.CLEARED_ROAD_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case FINISHED_CLUSTER_POLICE_MESSAGE_ID: {
					int cId = Integer.parseInt(msg[1]);
					Think.removeEntityIDs(agentsStuck,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(highPriorityFires,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(highPriorityCivilians,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(mediumPriority,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(lowPriority,
							clusters.get(cId).cluster);
					Think.removeEntityIDs(buildingsEntrance,
							clusters.get(cId).cluster);

					if (cId == currentClusterNumber && currentTaskNumber == 4) {
						changeCluster(time);
					}
				}
					;
					break;
				case STUCK_INSIDE_BLOCKADE: {
					EntityID location = allEntities.get(
							Integer.parseInt(msg[1])).getID();
					int bId = Integer.parseInt(msg[2]);
					int ttl = Integer.parseInt(msg[3]);
					EntityID blockadeID = new EntityID(bId);
					if (!Think.exists(location, currentCluster.cluster)
							&& !Think.exists(location, originalCluster.cluster))
						continue;
					Think.addIfNotExists(blockadeID, criticalBlockades);

					if (Think.exists(location, highPriorityFires))
						continue;
					Think.addIfNotExists(location, agentsStuck);
					Think.removeIfExists(location, highPriorityCivilians);
					Think.removeIfExists(location, mediumPriority);
					Think.removeIfExists(location, lowPriority);
					Think.removeIfExists(location, buildingsEntrance);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1, MESSAGE_ID.STUCK_INSIDE_BLOCKADE
											.name()), voiceMessages, comparator);

				}
					;
					break;

				case BUILDING_ON_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[3]);
					handleReportedFire(id, time);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 1,
									ttl - 1,
									MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case COLLAPSED_BUILDING_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[3]);
					handleReportedCollapsed(id, time);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1,
									MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
											.name()), voiceMessages, comparator);
				}
					;
					break;
				case EXTINGUISHED_FIRE_MESSAGE_ID: {
					EntityID id = allEntities.get(Integer.parseInt(msg[1]))
							.getID();
					int ttl = Integer.parseInt(msg[3]);
					handleReportedExtinguished(id, time);
					Comm.reportVoice(
							new VoiceMessage(Think.extractMessage(message), 2,
									ttl - 1,
									MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
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
		for (EntityID road : Think.getBuildingEntrance(model, b))
			Think.removeIfExists(road, buildingsEntrance);

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
		for (EntityID road : Think.getBuildingEntrance(model, b))
			Think.removeIfExists(road, buildingsEntrance);

		fires.addNewTarget(b, 0, 1, messageTime,
				BUILDING_LIST.BUILDINGS_COLLAPSED);
	}

	private void handleReportedWarm(EntityID id, int messageTime) {

		Building b = (Building) model.getEntity(id);

		fires.addNewTarget(b, 0, 1, messageTime, BUILDING_LIST.BUILDINGS_WARM);
	}

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
					time, BUILDING_LIST.BUILDINGS_WARM)) {
				graphHelper.removeNode(b.getID());
				reportWarm(b, time, 0);
			}

		}
		for (Building b : newExtinguishedFire) {

			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_EXTINGUISHED)) {
				graphHelper.restoreNode(b.getID());
				reportExtinguished(b, time, 0);
			}
		}
		for (Building b : newBuildingsOnFire) {

			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_ON_FIRE)) {
				graphHelper.removeNode(b.getID());
				Think.addIfNotExists(b.getID(), burningOrCollapsedBuildings);
				for (EntityID road : Think.getBuildingEntrance(model, b))
					Think.removeIfExists(road, buildingsEntrance);

				reportFire(b, time, 0);
			}
		}
		for (Building b : newCollapsedBuildings) {
			if (fires.addNewTarget(b, b.getFieryness(), b.getTemperature(),
					time, BUILDING_LIST.BUILDINGS_COLLAPSED)) {
				graphHelper.restoreNode(b.getID());
				Think.addIfNotExists(b.getID(), burningOrCollapsedBuildings);
				for (EntityID road : Think.getBuildingEntrance(model, b))
					Think.removeIfExists(road, buildingsEntrance);

				reportCollapse(b, time, 0);
			}
		}

	}

	/**
	 * Noha Khater
	 */
	private void reportLocationOfBuriedHumans(int time, ChangeSet changeset,
			StandardWorldModel model, int channel) {
		List<Human> targets = getBuriedTargets(changeset);
		ArrayList<EntityID> otherAmbulanceAgents = new ArrayList<EntityID>();

		for (Human h : targets) {
			if (!Think.exists(h.getPosition(), buriedCiviliansReported)
					&& model.getEntity(h.getPosition()) instanceof Building) {
				otherAmbulanceAgents = Think.AmbulanceAgentsInSameBuilding(
						model, changeset, getID(), h.getPosition());
				if (otherAmbulanceAgents.size() == 0
						|| (otherAmbulanceAgents.size() > 0 && h
								.getBuriedness() / otherAmbulanceAgents.size() <= 5)
						|| h.getHP() >= 2000) {
					Think.addIfNotExists(h.getPosition(),
							buriedCiviliansReported);

					if (radioChannels.size() > 0) {
						Comm.reportVoice(
								new VoiceMessage(Comm.civilianLocationBuried(
										h.getPosition(), allEntities), 1, 1,
										MESSAGE_ID.CIVILIAN_LOCATION_BURIED
												.name()), voiceMessages,
								comparator);
						sendSpeak(time, ambulanceChannel,
								Comm.civilianLocationBuried(h.getPosition(),
										allEntities));

					} else

						Comm.reportVoice(
								new VoiceMessage(Comm.civilianLocationBuried(
										h.getPosition(), allEntities), 1,
										TIME_TO_LIVE_VOICE,
										MESSAGE_ID.CIVILIAN_LOCATION_BURIED
												.name()), voiceMessages,
								comparator);
				}

			}
		}
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

		// Collections.sort(buriedTargets, new DistanceSorter(location(),
		// model));

		Collections.sort(buriedTargets, new Comparator<Human>() {

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
		return buriedTargets;
	}

	private boolean moveToBlockade(int time) {
		int[] position = BlockadePosition(time);
		if (position != null) {
			List<EntityID> path = new ArrayList<EntityID>();
			path.add(location().getID());
			agentLogger.debug("time: " + time + " moving to blockade");
			previousPositions[1] = previousPositions[0];
			previousPositions[0] = lastPosition;
			lastPosition = me().getPosition();
			lastPositionX = me().getX();
			lastPositionY = me().getY();
			sendMove(time, path, position[0], position[1]);
			return true;
		}
		return false;
	}

	private void reportWarm(Building b, int time, int clusterIndex) {
		if (radioChannels.size() > 0) {
			sendSpeak(time, fireChannel,
					Comm.warmBuilding(b, time, clusterIndex, allEntities));
			Comm.reportVoice(
					new VoiceMessage(
							Comm.fireMessageVoice(
									MESSAGE_ID.WARM_BUILDING.ordinal(), b,
									allEntities), 2, 1,
							MESSAGE_ID.WARM_BUILDING.name()), voiceMessages,
					comparator);

			if (fireChannel != ambulanceChannel) {
				sendSpeak(time, ambulanceChannel,
						Comm.warmBuilding(b, time, clusterIndex, allEntities));
			}
			if (fireChannel != policeChannel
					&& ambulanceChannel != policeChannel) {
				sendSpeak(time, policeChannel,
						Comm.warmBuilding(b, time, clusterIndex, allEntities));
				Think.addIfNotExists(
						new AKSpeak(me().getID(), time, policeChannel, Comm
								.warmBuilding(b, time, clusterIndex,
										allEntities)), dropped);
			}
		} else
			Comm.reportVoice(
					new VoiceMessage(
							Comm.fireMessageVoice(
									MESSAGE_ID.WARM_BUILDING.ordinal(), b,
									allEntities), 2, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.WARM_BUILDING.name()), voiceMessages,
					comparator);
	}

	private void reportFire(Building b, int time, int clusterIndex) {
		if (radioChannels.size() > 0) {
			sendSpeak(time, fireChannel,
					Comm.fireMessage(b, time, clusterIndex, allEntities));
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.ordinal(),
							b, allEntities), 1, 1,
							MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.name()),
					voiceMessages, comparator);

			if (fireChannel != ambulanceChannel) {
				sendSpeak(time, ambulanceChannel,
						Comm.fireMessage(b, time, clusterIndex, allEntities));

				if (fireChannel != policeChannel
						&& ambulanceChannel != policeChannel) {
					sendSpeak(time, policeChannel, Comm.fireMessage(b, time,
							clusterIndex, allEntities));
					Think.addIfNotExists(
							new AKSpeak(me().getID(), time, policeChannel, Comm
									.fireMessage(b, time, clusterIndex,
											allEntities)), dropped);
				}

			}
		} else
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.ordinal(),
							b, allEntities), 1, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.name()),
					voiceMessages, comparator);
	}

	private void reportCollapse(Building b, int time, int clusterIndex) {
		if (radioChannels.size() > 0) {
			sendSpeak(time, fireChannel,
					Comm.CollapsedMessage(b, time, clusterIndex, allEntities));
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.ordinal(),
							b, allEntities), 2, 1,
							MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.name()),
					voiceMessages, comparator);

			if (fireChannel != ambulanceChannel) {
				sendSpeak(time, ambulanceChannel, Comm.CollapsedMessage(b,
						time, clusterIndex, allEntities));

			}
			if (fireChannel != policeChannel
					&& ambulanceChannel != policeChannel) {
				sendSpeak(time, policeChannel, Comm.CollapsedMessage(b, time,
						clusterIndex, allEntities));
				Think.addIfNotExists(
						new AKSpeak(me().getID(), time, policeChannel, Comm
								.CollapsedMessage(b, time, clusterIndex,
										allEntities)), dropped);
			}
		} else
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.ordinal(),
							b, allEntities), 2, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.name()),
					voiceMessages, comparator);
	}

	private void reportExtinguished(Building b, int time, int clusterIndex) {
		if (radioChannels.size() > 0) {
			sendSpeak(time, fireChannel,
					Comm.extinguishedFire(b, time, clusterIndex, allEntities));
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.ordinal(),
							b, allEntities), 2, 1,
							MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.name()),
					voiceMessages, comparator);

			if (fireChannel != ambulanceChannel) {
				sendSpeak(time, ambulanceChannel, Comm.extinguishedFire(b,
						time, clusterIndex, allEntities));
			}
			if (fireChannel != policeChannel
					&& ambulanceChannel != policeChannel) {
				sendSpeak(time, policeChannel, Comm.extinguishedFire(b, time,
						clusterIndex, allEntities));
				Think.addIfNotExists(
						new AKSpeak(me().getID(), time, policeChannel, Comm
								.extinguishedFire(b, time, clusterIndex,
										allEntities)), dropped);
			}
		} else
			Comm.reportVoice(
					new VoiceMessage(Comm.fireMessageVoice(
							MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.ordinal(),
							b, allEntities), 2, TIME_TO_LIVE_VOICE,
							MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID.name()),
					voiceMessages, comparator);
	}
	/*
	 * public static void main(String[] args) { Registry.SYSTEM_REGISTRY
	 * .registerEntityFactory(StandardEntityFactory.INSTANCE);
	 * Registry.SYSTEM_REGISTRY
	 * .registerMessageFactory(StandardMessageFactory.INSTANCE);
	 * Registry.SYSTEM_REGISTRY
	 * .registerPropertyFactory(StandardPropertyFactory.INSTANCE); Config config
	 * = new Config(); int port =
	 * config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
	 * Constants.DEFAULT_KERNEL_PORT_NUMBER); String host =
	 * config.getValue(Constants.KERNEL_HOST_NAME_KEY,
	 * Constants.DEFAULT_KERNEL_HOST_NAME);
	 * 
	 * ComponentLauncher launcher = new TCPComponentLauncher(host, port,
	 * config); System.out.println("port: " + port + ", host: " + host); while
	 * (true) try { launcher.connect(new FirstPoliceAgent()); } catch (Exception
	 * e) { e.printStackTrace(); break; } }
	 */

}