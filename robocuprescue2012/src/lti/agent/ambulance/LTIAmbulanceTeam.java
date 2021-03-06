package lti.agent.ambulance;

/*
 * TODO
 * - desenvolver método para encontrar rotas alternativas em
 *   caso de bloqueios.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lti.agent.AbstractLTIAgent;
import lti.message.Message;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class LTIAmbulanceTeam extends AbstractLTIAgent<AmbulanceTeam> {

	private List<EntityID> buildingsToCheck;

	private List<EntityID> refuges;

	private static enum State {
		CARRYING_CIVILIAN, PATROLLING, TAKING_ALTERNATE_ROUTE, MOVING_TO_TARGET, MOVING_TO_REFUGE, RESCUEING, RANDOM_WALKING, DEAD, BURIED
	};

	private State state;

	private Set<EntityID> safeBuildings;

	@Override
	protected void postConnect() {
		super.postConnect();

		refuges = new ArrayList<EntityID>();
		Collection<Refuge> ref = getRefuges();

		for (Refuge next : ref) {
			refuges.add(next.getID());
		}

		buildingsToCheck = new ArrayList<EntityID>();

		for (EntityID next : buildingIDs) {
			if (!refuges.contains(next)) {
				buildingsToCheck.add(next);
			}
		}

		safeBuildings = new HashSet<EntityID>();

		state = State.RANDOM_WALKING;
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	}

	@Override
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

		// Evaluate task dropping
		if (target != null) {
			dropTask(time, changed);
		}

		if (buildingIDs.contains(currentPosition)) {
			// Will check the building for victims
			if (checkBuilding(changed)) {
				buildingsToCheck.remove(currentPosition);
			}
		}

		// Pick a task to work upon, if you don't have one
		if (target == null) {
			/*
			 * Choose a victim to rescue from the task table, in the following
			 * priority order: ambulances, fire brigades, civilians and police
			 * forces
			 */
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

		// Work on the task, if you have one
		if (target != null) {
			// Am I carrying a civilian?
			if (targetOnBoard()) {
				state = State.CARRYING_CIVILIAN;
				// Am I at a refuge?
				if (refuges.contains(currentPosition)) {
					sendUnload(time);
					return;
				}
				// No? I need to get to one, then.
				List<EntityID> path = search.breadthFirstSearch(
						currentPosition, refuges);
				state = State.MOVING_TO_REFUGE;

				if (path == null) {
					path = randomWalk();
					state = State.RANDOM_WALKING;
				}
				sendMove(time, path);
				return;
			} else {
				Human victim = (Human) model.getEntity(target);

				if (victim.getPosition().equals(currentPosition)) {
					if (victim.getBuriedness() != 0) {
						sendRescue(time, target);
						state = State.RESCUEING;
					} else if (victim instanceof Civilian) {
						sendLoad(time, target);
					}
					return;
				} else {
					List<EntityID> path = search.breadthFirstSearch(
							currentPosition, victim.getPosition());
					if (path != null) {
						state = State.MOVING_TO_TARGET;
						sendMove(time, path);
						return;
					}
				}
			}
		}

		// Move around the map
		// Nothing to do here. Moving on.
		Set<EntityID> safeBuildings = getSafeBuildings(changed);
		List<EntityID> path;

		getSafeBuildings();

		for (EntityID next : buildingsToCheck) {
			// I need to check if it's safe to go inside this building.
			if (safeBuildings.contains(next)) {
				path = search.breadthFirstSearch(currentPosition, next);
				if (path != null) {
					sendMove(time, path);
					state = State.PATROLLING;
					return;
				}
			}
		}

		path = randomWalk();
		sendMove(time, path);
		state = State.RANDOM_WALKING;
		return;
	}

	/**
	 * Check if this ambulance is carrying a civilian.
	 * 
	 * @return true if the ambulance is carrying a civilian, false otherwise.
	 */
	private boolean targetOnBoard() {
		// FIXME deu null pointer aqui
		if (((Human) model.getEntity(target)).isPositionDefined()) {
			if (((Human) model.getEntity(target)).getPosition().equals(
					this.getID())) {
				return true;
			}
			return false;
		}

		System.out.println(getID() + ": posicao nao definida");
		return false;
	}

	@SuppressWarnings("unused")
	private Set<EntityID> getVictimsOfType(StandardEntityURN type) {
		Set<EntityID> result = new HashSet<EntityID>();

		for (EntityID next : taskTable.keySet()) {
			if (model.getEntity(next).getStandardURN().equals(type)) {
				result.add(next);
			}
		}
		return result;
	}

	@Override
	protected boolean amIBlocked(int time) {
		if (currentPosition.equals(lastPosition) && time > 3
				&& state.compareTo(State.MOVING_TO_REFUGE) < 0
				&& getBlockedRoads().contains(currentPosition)) {
			return true;
		}
		return false;
	}

	/**
	 * Choose a vicitm from the Task Table to rescue according to the following
	 * pritority: ambulances, fire brigades, civilians and police forces.
	 * 
	 * @return The victim's ID.
	 */
	@Override
	protected EntityID selectTask() {
		if (!taskTable.isEmpty()) {
			Map<EntityID, Set<EntityID>> taskTableCopy = new HashMap<EntityID, Set<EntityID>>(
					taskTable);
			Map<EntityID, Set<EntityID>> narrowedVictims = new HashMap<EntityID, Set<EntityID>>();
			StandardEntityURN[] urns = { StandardEntityURN.AMBULANCE_TEAM,
					StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.CIVILIAN,
					StandardEntityURN.POLICE_FORCE };
			Human victim;

			// FIXME Ordenação das vítimas pelo seu URN está causando bizarrices
			for (int i = 0; i < 4; i++) {
				taskTableCopy.keySet().removeAll(narrowedVictims.keySet());
				narrowedVictims.clear();

				for (EntityID next : taskTableCopy.keySet()) {
					if (model.getEntity(next).getStandardURN().equals(urns[i])) {
						narrowedVictims.put(next, taskTableCopy.get(next));
					}
				}
				victim = pickVictim(narrowedVictims);

				if (victim != null) {
					for (Set<EntityID> agents : taskTable.values()) {
						if (agents != null) {
							agents.remove(me().getID());
						}
					}

					taskTable.get(victim.getID()).add(me().getID());

					return victim.getID();
				}
			}
		}

		return null;
	}

	/**
	 * Choose the victim with the best chance of being succesfully rescued.
	 * 
	 * @param victims
	 *            A map containing the victims and the ambulances engaged in
	 *            rescueing each one.
	 * @return The Human entity to be rescued.
	 */
	private Human pickVictim(Map<EntityID, Set<EntityID>> victims) {
		int finalDistance = Integer.MAX_VALUE;
		Human result = null;

		for (EntityID next : victims.keySet()) {
			if (!next.equals(taskDropped)) {
				Human victim = (Human) model.getEntity(next);
				int distanceFromAT = model.getDistance(currentPosition,
						victim.getPosition());
				int distanceToRefuge = Integer.MAX_VALUE;

				for (EntityID ref : refuges) {
					int dist = model.getDistance(victim.getPosition(), ref);
					if (dist < distanceToRefuge) {
						distanceToRefuge = dist;
					}
				}

				if (distanceFromAT + distanceToRefuge < finalDistance) {
					finalDistance = distanceFromAT + distanceToRefuge;
					result = victim;
				}
			}
		}

		return result;
	}

	@Override
	protected void refreshTaskTable(ChangeSet changed) {
		Set<EntityID> victims = new HashSet<EntityID>();
		StandardEntityURN[] urns = { StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.CIVILIAN,
				StandardEntityURN.POLICE_FORCE };
		/*
		 * Victims: - AT, FB and PF who are alive and buried; - Civilians who
		 * are alive and are either buried or inside a building that is not a
		 * refuge.
		 */
		for (int i = 0; i < 4; i++) {
			Set<EntityID> nonRefugeBuildings = new HashSet<EntityID>(
					buildingIDs);
			nonRefugeBuildings.removeAll(refuges);

			for (StandardEntity next : model.getEntitiesOfType(urns[i])) {
				if (((Human) next).isHPDefined() && ((Human) next).getHP() != 0) {
					if (urns[i].equals(StandardEntityURN.CIVILIAN)
							&& nonRefugeBuildings.contains(((Human) next)
									.getPosition())) {
						victims.add(next.getID());

					} else if (((Human) next).isBuriednessDefined()
							&& ((Human) next).getBuriedness() != 0
							&& !next.getID().equals(getID())) {
						victims.add(next.getID());
					}
				}
			}
		}

		Set<EntityID> toRemove = new HashSet<EntityID>();

		/*
		 * Remove victims who are not found in the position they were thought to
		 * be; they have already been rescued.
		 */
		for (EntityID next : victims) {
			if (((Human) model.getEntity(next)).getPosition().equals(
					currentPosition)
					&& !changed.getChangedEntities().contains(next)) {
				Human exVictim = (Human) model.getEntity(next);
				exVictim.undefineBuriedness();
				exVictim.undefinePosition();
				toRemove.add(next);
			}
		}

		victims.removeAll(toRemove);
		taskTable.keySet().retainAll(victims);
		victims.removeAll(taskTable.keySet());

		for (EntityID next : victims) {
			taskTable.put(next, new HashSet<EntityID>());
		}
	}

	/**
	 * Check the building to see if there are victims inside it.
	 * 
	 * @param changed
	 *            The ChangeSet of the ambulance.
	 * @return false, if there is still a victim inside the building, otherwise
	 *         returns true.
	 */
	private boolean checkBuilding(ChangeSet changed) {
		Set<EntityID> visible = changed.getChangedEntities();
		visible.retainAll(taskTable.keySet());

		for (EntityID next : visible) {
			if (((Human) model.getEntity(next)).getPosition().equals(
					currentPosition)) {
				return false;
			}
		}

		return true;
	}

	private void getSafeBuildings() {
		Set<EntityID> safe = new HashSet<EntityID>();

		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.BUILDING)) {
			if (!((Building) next).isOnFire()) {
				safe.add(next.getID());
			}
		}

		safe.removeAll(refuges);
		safeBuildings.retainAll(safe);
		safe.removeAll(safeBuildings);
		safeBuildings.addAll(safe);
	}

	@Override
	protected void dropTask(int time, ChangeSet changed) {
		if (!taskTable.keySet().contains(target) && !targetOnBoard()) {
			taskDropped = target;
			target = null;
		} else if (amIBlocked(time) && !targetOnBoard()) {
			taskDropped = target;
			target = null;
		}

		// TODO tempo limite estourará?

		// TODO existe tarefa mais vantajosa?
	}
}