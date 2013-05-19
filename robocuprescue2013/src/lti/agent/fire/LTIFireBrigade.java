package lti.agent.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lti.agent.AbstractLTIAgent;
import lti.message.Message;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
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

	private static enum State {
		MOVING_TO_REFUGEE, MOVING_TO_FIRE, EXTINGUISHING_FIRE, REFILLING, RANDOM_WALKING, TAKING_ALTERNATE_ROUTE, DEAD, BURIED
	};

	private State state;

	@Override
	protected void postConnect() {
		super.postConnect();

		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);

		refuges = new ArrayList<EntityID>();
		List<Refuge> ref = getRefuges();

		for (Refuge r : ref) {
			refuges.add(r.getID());
		}

		state = State.RANDOM_WALKING;
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		super.think(time, changed, heard);

		if (me().getHP() == 0) {
			state = State.DEAD;

			return;
		}

		if (me().getBuriedness() != 0) {
			state = State.BURIED;

			return;
		}

		// Verify if you are blocked
		if (amIBlocked(time)) {
			blocked = true;
		}

		dropTask(time, changed);

		if (target == null) {
			target = selectTask();
		}

		// Send a message about all the perceptions
		Message msg = composeMessage(changed);

		if (this.channelComm) {
			if (!msg.getParameters().isEmpty() && !channelList.isEmpty()) {
				for (Integer channel : channelList) {
					sendSpeak(time, channel.intValue(), msg.getMessage());
				}
			}
		}

		// There is no need to stay inside a burning building, right?
		if (location() instanceof Building) {
			if (((Building) location()).isOnFire()) {
				List<EntityID> path = randomWalk();

				sendMove(time, path);
				state = State.RANDOM_WALKING;

				return;
			}
		}

		// Am I at a refuge?
		if (location() instanceof Refuge && me().isWaterDefined()
				&& me().getWater() < maxWater) {
			sendRest(time);
			state = State.REFILLING;

			return;
		}

		// Am I out of water?
		if (me().isWaterDefined() && me().getWater() == 0) {
			List<EntityID> path = search.breadthFirstSearch(location().getID(),
					refuges);
			state = State.MOVING_TO_REFUGEE;

			if (path == null) {
				path = randomWalk();
				state = State.RANDOM_WALKING;
			}
			target = path.get(path.size() - 1);
			sendMove(time, path);

			return;
		}

		if (target != null) {
			if (changed.getChangedEntities().contains(target)
					&& model.getDistance(location().getID(), target) < maxDistance) {
				sendExtinguish(time, target, maxPower);
				state = State.EXTINGUISHING_FIRE;

				return;
			}

			List<EntityID> path = search.breadthFirstSearch(location().getID(),
					target);

			if (path != null) {
				path.remove(path.size() - 1);
				sendMove(time, path);
				state = State.MOVING_TO_FIRE;

				if (!path.isEmpty()) {
					target = path.get(path.size() - 1);
				}

				return;
			}
		}

		// FIXME Avaliar a necessidade deste trecho
		/*
		 * if (blocked) { List<EntityID> path =
		 * search.pathFinder(currentPosition, getBlockedRoads(), target);
		 * 
		 * if (path != null) { state = State.TAKING_ALTERNATE_ROUTE; } else if
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
		state = State.RANDOM_WALKING;
		return;
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
		if (lastPosition.getValue() == currentPosition.getValue()
				&& (state.equals(State.MOVING_TO_FIRE) || state
						.equals(State.MOVING_TO_REFUGEE))
				&& getBlockedRoads().contains(location().getID())) {
			return true;
		}
		return false;
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

		for (EntityID task : taskTable.keySet()) {
			if (model.getDistance(me().getID(), task) < closest) {
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
}