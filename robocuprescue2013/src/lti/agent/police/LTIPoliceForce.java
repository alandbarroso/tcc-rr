package lti.agent.police;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import lti.agent.AbstractLTIAgent;
import lti.message.Message;
import lti.message.type.BuildingEntranceCleared;
import lti.utils.EntityIDComparator;
import area.Sector;
import area.Sectorization;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class LTIPoliceForce extends AbstractLTIAgent<PoliceForce> {

	private static final String DISTANCE_KEY = "clear.repair.distance";

	private static final String REPAIR_RATE_KEY = "clear.repair.rate";

	private int minClearDistance;

	private int repairRate;

	private Sectorization sectorization;
	
	private Sector sector;
	
	private List<Sector> sectorsLeftToSearch;

	private State state = null;

	private static enum State {
		RETURNING_TO_SECTOR, MOVING_TO_BLOCKADE,
		MOVING_TO_ENTRANCE_BUILDING, RANDOM_WALKING, MOVING_TO_UNBLOCK,
		CLEARING, BURIED, DEAD, CLEARING_PATH
	};

	private EntityID obstructingBlockade;

	private List<EntityID> policeForcesList;

	private Set<EntityID> buildingEntrancesToBeCleared;

	private boolean clearEntranceTask;

	private EntityID buildingEntranceTarget;

	private int lastRepairCost;

	private State lastState;

	private EntityID lastTarget;

	private List<EntityID> path;
	
	private boolean enablePreventiveClearing = false;
	private Set<StandardEntity> preventiveSavedVictims;
	private boolean amIPreventiveClearing = false;
	private boolean amIGoingToRefuge = false;
	
	private List<EntityID> refuges;

	@Override
	protected void postConnect() {
		super.postConnect();

		inicializaVariaveis();

		changeState(State.RANDOM_WALKING);

		defineSectorRelatedVariables();

		buildingEntrancesToBeCleared = getBuildingEntrancesToBeCleared(this.sector);
		
		refuges = new ArrayList<EntityID>();
		
		for(Refuge refuge: getRefuges()){
			refuges.add(refuge.getID());
		}
	}

	/**
	 * Define the number of divisions, sectorize the world, print the sectors
	 * into a file, define the working sector of this instance of the agent and
	 * keep the list of the sectors that can be used during the simulation as a
	 * working sector
	 */
	private void defineSectorRelatedVariables() {
		sectorization = new Sectorization(model, neighbours,
				policeForcesList.size(), VERBOSE);

		sector = sectorization.getSector(internalID);

		sectorsLeftToSearch = sectorization.getSectorsAsList();
		sectorsLeftToSearch.remove(sector);

		log("Defined sector: " + sector);
	}

	/**
	 * Inicializa as variáveis utilizadas pelo agente
	 */
	private void inicializaVariaveis() {
		currentX = me().getX();
		currentY = me().getY();
		clearEntranceTask = true;
		buildingEntranceTarget = null;
		path = null;
		lastRepairCost = -1;

		Set<EntityID> policeForces = new TreeSet<EntityID>(
				new EntityIDComparator());

		for (StandardEntity e : model
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
			policeForces.add(e.getID());
		}

		policeForcesList = new ArrayList<EntityID>(policeForces);

		internalID = policeForcesList.indexOf(me().getID()) + 1;

		minClearDistance = config.getIntValue(DISTANCE_KEY);

		repairRate = config.getIntValue(REPAIR_RATE_KEY);

		obstructingBlockade = null;
		
		preventiveSavedVictims = new HashSet<StandardEntity>();
	}

	private Set<EntityID> getBuildingEntrancesToBeCleared(Sector s) {
		Set<EntityID> buildingEntrances = new HashSet<EntityID>();
		if (clearEntranceTask) {
			for (EntityID buildingID : buildingIDs) {
				if (s.getLocations().keySet().contains(buildingID)) {
					Building building = (Building) model.getEntity(buildingID);
					if (building != null)
						for (EntityID neighbourID : building.getNeighbours())
							if (model.getEntity(neighbourID) instanceof Road)
								buildingEntrances.add(neighbourID);
				}
			}
			log("There are " + buildingEntrances.size()
					+ " building entrances to be cleared");
		}

		return buildingEntrances;
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		super.think(time, changed, heard);

		recalculaVariaveisCiclo();

		if (me().getHP() == 0) {
			changeState(State.DEAD);
			return;
		}

		if (me().getBuriedness() != 0) {
			changeState(State.BURIED);
			return;
		}

		evaluateTaskDroppingAndSelection(changed);

		sendMessageAboutPerceptions(changed);

		/**
		 * If I'm blocked it's probably because there's an obstructing blockade
		 */
		if (amIBlocked(time)) {
			obstructingBlockade = getBestClosestBlockadeToClear();
			if (obstructingBlockade != null)
				clearObstructingBlockade();
			else if (path.size() > 0)
				movingToUnblock();
			return;
		}
		
		if (enablePreventiveClearing)
			if(treatPreventiveClearing())
				return;

		// Work on the task, if you have one
		if (target != null) {
			// Is the target visible and inside clearing range?
			if (blockadeInRange(target, changed)) {
				clearBlockade();
				return;
			}

			log("Target " + target + " out of direct reach");
			Blockade targetBlockade = (Blockade) model.getEntity(target);

			path = getPathToTarget(targetBlockade);
			if (path != null) {
				moveToBlockade(targetBlockade);
				return;
			}

			log("No path to target: " + target + ", dropping this task");
			target = null;
		}

		// Move around the map

		if (clearEntranceTask)
			path = getPathToEntranceTarget();
		else
			path = randomWalk();

		if (path != null) {
			sendMove(time, path);
			log("Path calculated and sent move: " + path);
			return;
		}
	}

	private boolean treatPreventiveClearing() {
		boolean executouAcao = false;
		StandardEntity victimForPreventiveClearing;
		
		if (amIPreventiveClearing && target != null) {
			
			victimForPreventiveClearing = model.getEntity(target);
			log("Preventive Clearing: Started for " + victimForPreventiveClearing);
			if (!amIGoingToRefuge) {
				EntityID victimLoc = ((Human)victimForPreventiveClearing).getPosition();
				if (!currentPosition.equals(victimLoc)) {
					path = search.breadthFirstSearch(currentPosition, victimLoc);
					log("Preventive Clearing: Going to Victim");						
				} else {						
					path = search.breadthFirstSearch(currentPosition, refuges);
					log("Preventive Clearing: Arrived At Victim");
					amIGoingToRefuge = true;
				}
				sendMove(currentTime, path);
				executouAcao = true;
			} else {
				if (!refuges.contains(currentPosition)) {
					path = search.breadthFirstSearch(currentPosition, refuges);
					log("Preventive Clearing: Going to refuge");
					sendMove(currentTime, path);						
					executouAcao = true;
				} else {
					log("Preventive Clearing: Finished for " + victimForPreventiveClearing);
					if (!preventiveSavedVictims.contains(victimForPreventiveClearing))
						preventiveSavedVictims.add(victimForPreventiveClearing);						
					amIGoingToRefuge = false;
					target = null;
					amIPreventiveClearing = false;
				}
			}

		}
		
		return executouAcao;
	}

	private void movingToUnblock() {
		Rectangle2D rect = ((Road) model.getEntity(path.get(0)))
				.getShape().getBounds2D();
		Random rdn = new Random();
		int x = (int) (rect.getMinX() + rdn.nextDouble()
				* (rect.getMaxX() - rect.getMinX()));
		int y = (int) (rect.getMinY() + rdn.nextDouble()
				* (rect.getMaxY() - rect.getMinY()));

		if (rect.contains(x, y)) {
			EntityID e = path.get(0);
			path = new ArrayList<EntityID>();
			path.add(e);
			sendMove(currentTime, path, x, y);
			changeState(State.MOVING_TO_UNBLOCK);
			log("Found path: " + path + " and sent move to dest: " + x
					+ "," + y);
		} else {
			path = randomWalk();
			sendMove(currentTime, path);
			log("Path calculated to unblock and sent move: " + path);
		}
	}

	private List<EntityID> getPathToEntranceTarget() {
		List<EntityID> path;

		if (buildingEntranceTarget == null) {
			path = search.breadthFirstSearch(currentPosition,
					buildingEntrancesToBeCleared);
			if (path != null && path.size() > 0)
				buildingEntranceTarget = path.get(path.size() - 1);
		} else {
			path = search.breadthFirstSearch(currentPosition,
					buildingEntranceTarget);
		}
		if (path != null && path.size() > 0)
			changeState(State.MOVING_TO_ENTRANCE_BUILDING);
		else
			path = randomWalk();
		return path;
	}

	/**
	 * @param time
	 * @param targetBlockade
	 */
	private void moveToBlockade(Blockade targetBlockade) {
		changeState(State.MOVING_TO_BLOCKADE);
		sendMove(currentTime, path, targetBlockade.getX(),
				targetBlockade.getY());
		log("Found path: " + path + " and sent move to target: " + target);
	}

	/**
	 * @param targetBlockade
	 */
	private List<EntityID> getPathToTarget(Blockade targetBlockade) {
		List<EntityID> path;

		if (sector.getLocations().keySet().contains(currentPosition)) {
			path = search.breadthFirstSearch(currentPosition, sector,
					targetBlockade.getPosition());
			log("I'm inside my sector");
		} else {
			path = search.breadthFirstSearch(currentPosition,
					targetBlockade.getPosition());
			log("I'm outside my sector");
		}
		return path;
	}

	/**
	 * @param time
	 */
	private void clearBlockade() {
		changeState(State.CLEARING);
		sendClearArea(currentTime, target);

		int repairCost = ((Blockade) model.getEntity(target)).getRepairCost();
		lastRepairCost = repairCost;
		log("Sent clear to remove " + repairRate + "/" + repairCost
				+ " of the target: " + target);
	}

	/**
	 * If I'm blocked it's probably because there's an obstructing blockade
	 * 
	 * @param time
	 */
	private void clearObstructingBlockade() {
		changeState(State.CLEARING_PATH);
		sendClearArea(currentTime, obstructingBlockade);
		int repairCost = ((Blockade) model.getEntity(obstructingBlockade))
				.getRepairCost();
		lastRepairCost = repairCost;
		log("Sent clear to remove " + repairRate + "/" + repairCost
				+ " of the obstructing blockade: " + obstructingBlockade);
	}

	/**
	 * @param time
	 * @param changed
	 */
	private void sendMessageAboutPerceptions(ChangeSet changed) {
		// Send a message about all the perceptions
		Message msg = composeMessage(changed);
		msg = verifyBuildingEntrancesToBeCleared(msg);
		if (this.channelComm) {
			if (!msg.getParameters().isEmpty() && !channelList.isEmpty()) {
				for (Pair<Integer, Integer> channel : channelList) {
					sendSpeak(currentTime, channel.first(),
							msg.getMessage(channel.second().intValue()));
				}
			}
		}
	}

	private void evaluateTaskDroppingAndSelection(ChangeSet changed) {
		// Evaluate task dropping
		if (target != null) {
			dropTask(currentTime, changed);
			if (target == null)
				log("Dropped task: " + taskDropped);
		}

		// Pick a task to work upon, if you don't have one
		if (target == null) {
			target = selectTask();
			if (target != null)
				log("Selected task: " + target);
		}
	}

	/**
	 * Verify if I just visited another building entrance and if I'm done
	 * looking for building entrances in this sector, I can help in other
	 * sectors. If there are no other sectors to visit, I'm done.
	 */
	private Message verifyBuildingEntrancesToBeCleared(Message msg) {
		int size_before = buildingEntrancesToBeCleared.size();
		buildingEntrancesToBeCleared.removeAll(buildingEntrancesCleared);
		int size_after = buildingEntrancesToBeCleared.size();
		if (size_after < size_before)
			log("Other have cleared some entrances. Yet " + size_after
					+ " to come");

		if (currentPosition.equals(buildingEntranceTarget))
			buildingEntranceTarget = null;
		if (buildingEntrancesToBeCleared.contains(currentPosition)) {
			msg.addParameter(new BuildingEntranceCleared(currentPosition
					.getValue()));
			buildingEntrancesToBeCleared.remove(currentPosition);
			log("Just cleared one more entrance. Yet "
					+ buildingEntrancesToBeCleared.size()
					+ " building entrances to come");
		}

		if (buildingEntrancesToBeCleared.size() == 0) {
			if (sectorsLeftToSearch.size() > 0) {
				Collections.shuffle(sectorsLeftToSearch);
				Sector s = sectorsLeftToSearch.get(0);
				sectorsLeftToSearch.remove(s);
				buildingEntrancesToBeCleared = getBuildingEntrancesToBeCleared(s);
				buildingEntrancesToBeCleared
						.removeAll(buildingEntrancesCleared);
			} else {
				clearEntranceTask = false;
			}
		}

		return msg;
	}

	/**
	 * 
	 */
	private void recalculaVariaveisCiclo() {
		currentX = me().getX();
		currentY = me().getY();
		lastTarget = target;
	}

	protected List<EntityID> randomWalk() {
		List<EntityID> result = new ArrayList<EntityID>();
		EntityID current = currentPosition;
		
		if (!sector.getLocations().keySet().contains(currentPosition)) {
			List<EntityID> local = new ArrayList<EntityID>(sector
					.getLocations().keySet());
			changeState(State.RETURNING_TO_SECTOR);
			return search.breadthFirstSearch(currentPosition, local);
		}

		for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
			result.add(current);
			List<EntityID> possible = new ArrayList<EntityID>();

			for (EntityID next : sector.getNeighbours(current))
				if (model.getEntity(next) instanceof Road)
					possible.add(next);

			Collections.shuffle(possible, new Random(me().getID().getValue()
					+ currentTime));
			boolean found = false;

			for (EntityID next : possible) {
				if (!result.contains(next)) {
					current = next;
					found = true;
					break;
				}
			}
			if (!found)
				break; // We reached a dead-end.
		}

		result.remove(0); // Remove actual position from path
		changeState(State.RANDOM_WALKING);
		return result;
	}

	/**
	 * Determines if the target blockade can be cleared in this turn.
	 * 
	 * @param changed
	 *            The agent's Change Set of the current turn.
	 * @return True, if the blockade can be cleared in this turn; false
	 *         otherwise.
	 */
	private boolean blockadeInRange(EntityID blockade, ChangeSet changed) {
		int repairCost = ((Blockade) model.getEntity(blockade)).getRepairCost();
		boolean stuck = lastState == state && lastTarget == blockade
				&& lastRepairCost == repairCost;
		if (stuck)
			log("Last time clearing blockade was ineffective, so need to get closer");

		return getVisibleEntitiesOfType(StandardEntityURN.BLOCKADE, changed)
				.contains(blockade)
				&& model.getDistance(me().getID(), blockade) < 3.0 * minClearDistance / 4
				&& !stuck;
	}

	/**
	 * Locates the closest blockade to the agent.
	 * 
	 * @return The closest blockade.
	 */
	private EntityID getBestClosestBlockadeToClear() {
		int maxRepairCost = 0, dist, repairCost;
		EntityID result = null;
		Set<StandardEntity> blockades = new HashSet<StandardEntity>(
				model.getEntitiesOfType(StandardEntityURN.BLOCKADE));

		for (StandardEntity next : blockades) {
			dist = model.getDistance(getID(), next.getID());
			repairCost = ((Blockade) next).getRepairCost();

			if (dist <= minClearDistance && repairCost >= maxRepairCost) {
				result = next.getID();
				maxRepairCost = repairCost;
			}
		}

		return result;
	}

	/**
	 * Get the blockade with the best scoring function.
	 * 
	 * @param blockades
	 *            The set of blockades known.
	 * @return The chosen blockade.
	 */
	@Override
	protected EntityID selectTask() {
		EntityID result = null;
		double Pb = Double.NEGATIVE_INFINITY;

		double importance;

		log("taskTable: " + taskTable);
		log("knownVictims: " + knownVictims);
		log("knownBlockades: " + knownBlockades);
		for (EntityID next : taskTable.keySet()) {			
			StandardEntity taskEntity = model.getEntity(next);
			
			//Se a tarefa for do tipo human, eh uma vitima para desbloqueio preventivo
			if(taskEntity instanceof Human) {
				if (enablePreventiveClearing)
					if (foundVictimToUnblock(taskEntity)) {
						result = next;
						break;
					}
			}			
			else if(taskEntity instanceof Blockade) {
				importance = calculateImportanceTask(taskEntity);
				
				if (importance > Pb) {
					result = next;
					Pb = importance;
				}
			}
		}
		
		if (result != null) {
			for (Set<EntityID> agents : taskTable.values())
				if (agents != null)
					agents.remove(me().getID());
			if (!taskTable.containsKey(result))
				taskTable.put(result, new HashSet<EntityID>());
			taskTable.get(result).add(me().getID());
		}

		return result;
	}

	private double calculateImportanceTask(StandardEntity taskEntity) {
		double thisDistance, repairCost, importance;
		boolean samePosition, isPositionEntrance, isInSector;
		Blockade blockade = (Blockade) taskEntity;

		thisDistance = model.getDistance(getID(), blockade.getID());
		repairCost = blockade.getRepairCost();

		EntityID pos = blockade.getPosition();
		samePosition = (pos == currentPosition);
		isPositionEntrance = false;
		if (model.getEntity(pos) instanceof Road) {
			Road areaEntity = (Road) model.getEntity(pos);
			for (EntityID e : areaEntity.getNeighbours())
				if (model.getEntity(e) instanceof Building) {
					isPositionEntrance = true;
					break;
				}
		}
		
		isInSector = sector.getLocations().keySet().contains(pos);
		
		importance = repairCost;
		importance -= thisDistance / 500;
		importance += isPositionEntrance ? 4 * repairRate : 0;
		importance += samePosition ? repairRate : 0;
		importance += samePosition && isPositionEntrance ? 10 * repairRate : 0;
		importance += isInSector ? repairRate / 2 : 0;
		
		return importance;
	}

	private boolean foundVictimToUnblock(StandardEntity taskEntity) {
		Human humanEntity = (Human) taskEntity;
		
		//se o desbloqueio preventivo ainda nao foi feito para essa vitima
		if (!preventiveSavedVictims.contains(taskEntity)) {
			//se a vitima encontra-se no meu setor
			if (sector.getLocations().keySet()
					.contains(humanEntity.getPosition())) {
				if (!humanEntity.getID().equals(me().getID())
						&& humanEntity.isBuriednessDefined()
						&& humanEntity.getBuriedness() > 0) {
					amIPreventiveClearing = true;
					log("Selected task preventive clearing for " + humanEntity);
					
					//Escolheu a vitima para desbloqueio preventivo, sai do loop
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void refreshWorldModel(ChangeSet changed,
			Collection<Command> heard) {
		Set<EntityID> visibleBlockades = getVisibleEntitiesOfType(
				StandardEntityURN.BLOCKADE, changed);

		// Remove the blockade the agent has finished clearing
		if (state.equals(State.CLEARING) && !visibleBlockades.contains(target)) {
			model.removeEntity(target);
		} else if (state.equals(State.CLEARING_PATH)
				&& !visibleBlockades.contains(obstructingBlockade)) {
			model.removeEntity(obstructingBlockade);
			obstructingBlockade = null;
		}

		super.refreshWorldModel(changed, heard);
	}

	@Override
	protected void refreshTaskTable(ChangeSet changed) {
		Set<StandardEntity> blockades = new HashSet<StandardEntity>(
				model.getEntitiesOfType(StandardEntityURN.BLOCKADE));
		Set<EntityID> remainingIDs = new HashSet<EntityID>();

		for (StandardEntity next : blockades)
			remainingIDs.add(next.getID());
		
		if (enablePreventiveClearing)
			remainingIDs = addHumansPossiblyNotYetSaved(remainingIDs);

		// Discard blockades and humans that do not exist anymore
		taskTable.keySet().retainAll(remainingIDs);

		// Add new blockades to the task table
		for (StandardEntity blockade : blockades) {
			Blockade block = (Blockade) blockade;
			EntityID blockID = block.getID();

			if (!taskTable.containsKey(blockID))
				taskTable.put(blockID, new HashSet<EntityID>());
		}
	}

	private Set<EntityID> addHumansPossiblyNotYetSaved(Set<EntityID> remainingIDs) {
		Set<StandardEntity> humans = new HashSet<StandardEntity>();
		humans.addAll(model.getEntitiesOfType(
				StandardEntityURN.CIVILIAN,
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.FIRE_BRIGADE
		));

		// Descarta as vitimas para quem ja foi feito o desbloqueio preventivo
		humans.removeAll(preventiveSavedVictims);
		
		for (StandardEntity next : humans)
			remainingIDs.add(next.getID());
		
		return remainingIDs;
	}

	@Override
	protected boolean amIBlocked(int time) {
		return lastPosition.getValue() == currentPosition.getValue()
				&& isMovingState() && time > 3;
	}

	@Override
	protected void dropTask(int time, ChangeSet changed) {
		if (!taskTable.containsKey(target)) {
			taskDropped = target;
			target = null;
		}
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	private void sendClearArea(int time, EntityID target) {
		Blockade block = (Blockade) model.getEntity(target);

		long moduloVetorDirecao = minClearDistance;
		long deltaX = block.getX() - currentX, deltaY = block.getY() - currentY;
		double deltaDirecao = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		double alpha = moduloVetorDirecao / deltaDirecao;
		int xDestino = currentX + (int) (alpha * deltaX);
		int yDestino = currentY + (int) (alpha * deltaY);

		sendClear(time, xDestino, yDestino);
	}

	private boolean isMovingState() {
		List<State> ss = new ArrayList<State>();
		ss.add(State.RETURNING_TO_SECTOR);
		ss.add(State.MOVING_TO_BLOCKADE);
		ss.add(State.MOVING_TO_ENTRANCE_BUILDING);
		ss.add(State.RANDOM_WALKING);
		ss.add(State.MOVING_TO_UNBLOCK);

		return ss.contains(state);
	}

	private void changeState(State state) {
		lastState = this.state;
		this.state = state;
		log("Changed state to: " + this.state);
	}
}
