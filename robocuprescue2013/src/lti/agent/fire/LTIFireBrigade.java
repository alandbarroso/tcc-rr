package lti.agent.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import lti.agent.AbstractLTIAgent;
import lti.message.Message;
import lti.utils.EntityIDComparator;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class LTIFireBrigade extends AbstractLTIAgent<FireBrigade> {

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

	private int maxWater;
	private int maxDistance;
	private int maxPower;
	private List<EntityID> refuges;
	private List<EntityID> fireBrigadesList;
	
	private Set<EntityID> gasStationNeighbours;
	private int dangerousDistance;

	private static enum State {
		MOVING_TO_REFUGE, MOVING_TO_HYDRANT, MOVING_TO_FIRE, 
		MOVING_TO_GAS, MOVING_CLOSE_TO_GAS, RANDOM_WALKING, TAKING_ALTERNATE_ROUTE,
		EXTINGUISHING_FIRE, REFILLING, DEAD, BURIED
	};

	private State state;

	private boolean blocked;

	@Override
	protected void postConnect() {
		super.postConnect();
		currentX = me().getX();
		currentY = me().getY();
		
		Set<EntityID> fireBrigades = new TreeSet<EntityID>(
				new EntityIDComparator());

		for (StandardEntity e : model
				.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
			fireBrigades.add(e.getID());
		}

		fireBrigadesList = new ArrayList<EntityID>(fireBrigades);
		
		internalID = fireBrigadesList.indexOf(me().getID()) + 1;

		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);

		refuges = new ArrayList<EntityID>();
		List<Refuge> ref = getRefuges();

		for (Refuge r : ref) {
			refuges.add(r.getID());
		}
		
		dangerousDistance = 25000;
		
		gasStationNeighbours = calculateGasStationNeighbours();
		
		changeState(State.RANDOM_WALKING);
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		super.think(time, changed, heard);
		currentX = me().getX();
		currentY = me().getY();

		if (me().getHP() == 0) {
			changeState(State.DEAD);
			return;
		}

		if (me().getBuriedness() != 0) {
			changeState(State.BURIED);
			return;
		}

		// Verify if you are blocked
		if (amIBlocked(time)) {
			blocked = true;
			
			log("Blocked! Random walk to escape");
			changeState(State.RANDOM_WALKING);
			List<EntityID> path = randomWalk();
			sendMove(time, path);
			return;
		}

		dropTask(time, changed);

		if (target == null) {
			target = selectTask();
		}

		// Send a message about all the perceptions
		Message msg = composeMessage(changed);

		if (this.channelComm) {
			if (!msg.getParameters().isEmpty() && !channelList.isEmpty()) {
				for (Pair<Integer,Integer> channel : channelList) {
					sendSpeak(time, channel.first(),
							msg.getMessage(channel.second().intValue()));
				}
			}
		}

		// There is no need to stay inside a burning building, right?
		if (location() instanceof Building) {
			if (((Building) location()).isOnFire()) {
				List<EntityID> path = randomWalk();

				sendMove(time, path);
				changeState(State.RANDOM_WALKING);
				log("Leaving a burning building");
				return;
			}
		}

		// Am I at a refuge or a hydrant?
		if ((location() instanceof Refuge || location() instanceof Hydrant) && me().isWaterDefined()
				&& me().getWater() < maxWater) {
			sendRest(time);
			changeState(State.REFILLING);
			return;
		}

		// Am I out of water?
		if (me().isWaterDefined() && me().getWater() == 0) {
			List<EntityID> path = search.breadthFirstSearch(location().getID(),
					refuges);
			changeState(State.MOVING_TO_REFUGE);

			if (path == null) {
				path = randomWalk();
				log("Trying to move to refugee, but couldn't find path");
				changeState(State.RANDOM_WALKING);
			}
			target = path.get(path.size() - 1);
			sendMove(time, path);
			return;
		}

		if (target != null) {
			if (changed.getChangedEntities().contains(target)
					&& model.getDistance(location().getID(), target) < maxDistance) {
				sendExtinguish(time, target, maxPower);
				changeState(State.EXTINGUISHING_FIRE);
				return;
			}

			List<EntityID> path = search.breadthFirstSearch(location().getID(), target);

			if (path != null) {
				path.remove(path.size() - 1);
				sendMove(time, path);
				
				if(model.getEntity(target) instanceof GasStation)
					changeState(State.MOVING_TO_GAS);
				else if(closeToGas(target))
					changeState(State.MOVING_CLOSE_TO_GAS);
				else
					changeState(State.MOVING_TO_FIRE);

				if (!path.isEmpty()) {
					target = path.get(path.size() - 1);
				}

				return;
			}
		}
		
		// If it has nothing to do and water level is below 80%, start finding a place to refill it
		if(me().isWaterDefined() && me().getWater() < 0.8*this.maxWater){
			log("I have nothing to do, i'll reffil, my water level is at " + ((float) me().getWater()/this.maxWater) + "!");
			
			// We analyze first if the level is below 50%
			// If it is, we search for a refuge
			if(me().getWater() < 0.5*this.maxWater){
				log("Finding a refuge");
				List<EntityID> path = search.breadthFirstSearch(location().getID(),
						refuges);
				changeState(State.MOVING_TO_REFUGE);

				if (path == null) {
					path = randomWalk();
					log("Trying to move to refugee, but couldn't find path");
					changeState(State.RANDOM_WALKING);
				}
				target = path.get(path.size() - 1);
				sendMove(time, path);
				return;
			}
			else{
				log("Finding a hydrant");
				List<EntityID> path = search.breadthFirstSearch(location().getID(),
						getHydrants());
				changeState(State.MOVING_TO_HYDRANT);

				if (path == null) {
					path = randomWalk();
					log("Trying to move to hydrant, but couldn't find path");
					changeState(State.RANDOM_WALKING);
				}
				target = path.get(path.size() - 1);
				sendMove(time, path);
				return;
			}
		}

		// FIXME Avaliar a necessidade deste trecho
		/*
		 * if (blocked) { List<EntityID> path =
		 * search.pathFinder(currentPosition, getBlockedRoads(), target);
		 * 
		 * if (path != null) { changeState(State.TAKING_ALTERNATE_ROUTE); } else if
		 * (state.equals(State.MOVING_TO_FIRE)) { List<EntityID> burning =
		 * getBurning(); Collections.shuffle(burning, random);
		 * 
		 * for (EntityID next : burning) { if (!next.equals(target)) { target =
		 * next; path = search.breadthFirstSearch(currentPosition, target);
		 * break; } } } else { Collections.shuffle(refuges, random);
		 * 
		 * for (EntityID next : refuges) { if (!next.equals(target)) { target =
		 * next; path = search.breadthFirstSearch(currentPosition, target);
		 * break; } } } }
		 */

		List<EntityID> path = randomWalk();
		sendMove(time, path);
		changeState(State.RANDOM_WALKING);
		return;
	}
	
	protected List<EntityID> getHydrants(){
		List<EntityID> result = new ArrayList<EntityID>();
		Collection<StandardEntity> b = model.
				getEntitiesOfType(StandardEntityURN.HYDRANT); // List of hydrants
		
		for(StandardEntity next : b){
			if(next instanceof Hydrant){
				result.add(next.getID());;
			}
		}
		
		return result;
	}

	protected List<EntityID> getBurning() {
		List<EntityID> result = new ArrayList<EntityID>();
		Collection<StandardEntity> b = model
				.getEntitiesOfType(StandardEntityURN.BUILDING);

		for (StandardEntity next : b) {
			if (next instanceof Building) {
				if (((Building) next).isOnFire()) {
					result.add(next.getID());
				}
			}
		}

		return result;
	}

	@Override
	protected boolean amIBlocked(int time) {
		return lastPosition.getValue() == currentPosition.getValue()
					&& isMovingState()
					&& time > 3
					&& getBlockedRoads().contains(location().getID());
	}

	@Override
	protected void refreshTaskTable(ChangeSet changed) {
		Set<EntityID> fires = new HashSet<EntityID>();

		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.BUILDING)) {
			if (((Building) next).isOnFire()) {
				fires.add(next.getID());
			}
		}

		taskTable.keySet().retainAll(fires);

		for (EntityID next : fires) {
			if (!taskTable.containsKey(next)) {
				taskTable.put(next, new HashSet<EntityID>());
			}
		}
	}
	
	@Override
	protected EntityID selectTask() {
		int closest = Integer.MAX_VALUE;
		EntityID result = null;
		
		// For all the buildings, we should prioritize those close to gas stations because they can explode
		for (EntityID task : taskTable.keySet()) {
			// If result defined
			if(result != null){
				if(model.getEntity(task) instanceof GasStation) {
					if(model.getEntity(result) instanceof GasStation) {
						if (model.getDistance(me().getID(), task) < closest) {
							closest = model.getDistance(me().getID(), task);
							result = task;
						}
					} else{
						closest = model.getDistance(me().getID(), task);
						result = task;
					}
				}
				else if(closeToGas(task) && !(model.getEntity(result) instanceof GasStation)) {
					if(closeToGas(result)) {
						if (model.getDistance(me().getID(), task) < closest) {
							closest = model.getDistance(me().getID(), task);
							result = task;
						}
					} else{
						closest = model.getDistance(me().getID(), task);
						result = task;
					}
				}
				else if(!(model.getEntity(result) instanceof GasStation) && !closeToGas(result)){
					if (model.getDistance(me().getID(), task) < closest) {
						closest = model.getDistance(me().getID(), task);
						result = task;
					}
				}
			}
			else {
				closest = model.getDistance(me().getID(), task);
				result = task;
			}
		}

		if (result != null) {
			for (Set<EntityID> agents : taskTable.values()) {
				if (agents != null) {
					agents.remove(me().getID());
				}
			}

			taskTable.get(result).add(me().getID());
		}

		return result;
	}
	
	private double getBuildingPriority(EntityID building){
		int distanceToBuilding; // The distace to the target
		int nbSafeNeighbours = 0; // The number of neighbours that haven't start burning
		boolean presenceOfCivilians = false;
		boolean presenceOfAgents = false;
		boolean closeToGas = false;
		boolean nearGas = false;
		
		double result;
		
		Collection<StandardEntity> buildings = model.getEntitiesOfType(StandardEntityURN.BUILDING);
		Collection<StandardEntity> people = model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,
																StandardEntityURN.FIRE_BRIGADE,
																StandardEntityURN.POLICE_FORCE,
																StandardEntityURN.CIVILIAN);
		
		distanceToBuilding = model.getDistance(me().getID(), building);
		
		for(StandardEntity bd : buildings){
			if(bd instanceof Building){
				// If it is in a dangerous distance, that may allow the fire to spread
				// And it's not on fire then give high priority
				if(model.getDistance(building, bd.getID()) < dangerousDistance && !((Building) bd).isOnFire()){
						nbSafeNeighbours++;
				}
				if(gasStationNeighbours.contains(bd.getID())){
					closeToGas = true;
				}
			}
		}
		
		for(StandardEntity person : people){
			// We check if it's a Ambulance, Fire Brigade or Police Force
			if(person instanceof AmbulanceTeam || person instanceof FireBrigade || person instanceof PoliceOffice){
				presenceOfAgents = true;
			}
			// We check if there are civilians in the building
			if(person instanceof Civilian){
				presenceOfCivilians = true;
			}
		}
		
		if(gasStationNeighbours.contains(building)){
			nearGas = true;
		}
		
		result = 0;
		if (distanceToBuilding >= 0)
			result -= distanceToBuilding / maxPower;
		result += nbSafeNeighbours/2*maxPower;
		result += closeToGas ? 3*maxPower : 0;
		result += nearGas ? 5*maxPower : 0;
		result += presenceOfAgents ? 2*maxPower : 0;
		result += presenceOfCivilians ? maxPower : 0;
		
		return result;
	}

	@Override
	protected void dropTask(int time, ChangeSet changed) {
		if (!taskTable.containsKey(target)) {
			taskDropped = target;
			target = null;
		}

		if (amIBlocked(time)) {
			taskDropped = target;
			target = null;
		}

	}

	private boolean isMovingState() {
		List<State> ss = new ArrayList<State>();
		ss.add(State.MOVING_TO_FIRE);
		ss.add(State.MOVING_TO_REFUGE);
		ss.add(State.RANDOM_WALKING);
		ss.add(State.TAKING_ALTERNATE_ROUTE);

		return ss.contains(state);
	}
	
	private void changeState(State state) {
		this.state = state;
		log("Changed state to: " + this.state);
	}
	
	// Used to calculate all the buildings near gas stations
	private Set<EntityID> calculateGasStationNeighbours(){
		Set<EntityID> result = new HashSet<EntityID>();
		
		Collection<StandardEntity> gasStations = model.
				getEntitiesOfType(StandardEntityURN.GAS_STATION); // Getting the list of Gas Stations
		Collection<StandardEntity> buildings = model.
				getEntitiesOfType(StandardEntityURN.BUILDING); // Getting the list of buildings
		for(StandardEntity gs : gasStations){
			if(gs instanceof GasStation){
				log("GasStation: " + gs.getID());
				
				for(StandardEntity bd : buildings){
					if(bd instanceof Building){
						log("Building " + bd.getID() + " distance to gas: " + model.getDistance(gs.getID(), bd.getID()));
						if(model.getDistance(gs.getID(), bd.getID()) < dangerousDistance){
							log("Close building! Adding " + bd.getID() + " to neighbours!");
							result.add(bd.getID());
						}
					}
				}
			}
		}

		
		return result;
	}
	
	// Verify if is a building is close to a gas station
	private boolean closeToGas(EntityID ent){
		if(gasStationNeighbours.contains(ent))
			return true;
		
		return false;
	}
}
