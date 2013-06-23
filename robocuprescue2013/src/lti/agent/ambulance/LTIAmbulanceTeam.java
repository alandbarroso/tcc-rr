package lti.agent.ambulance;

/*
 * TODO
 * - desenvolver método para encontrar rotas alternativas em
 *   caso de bloqueios.
 */

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
		CARRYING_CIVILIAN, PATROLLING, TAKING_ALTERNATE_ROUTE,
		MOVING_TO_TARGET, MOVING_TO_REFUGE, RANDOM_WALKING, RESCUEING, DEAD, BURIED
	};

	private State state;

	private Set<EntityID> safeBuildings;
	
	private List<EntityID> ambulanceTeamsList;

	@Override
	protected void postConnect() {
		super.postConnect();
		currentX = me().getX();
		currentY = me().getY();

		Set<EntityID> ambulanceTeams = new TreeSet<EntityID>(
				new EntityIDComparator());

		for (StandardEntity e : model
				.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)) {
			ambulanceTeams.add(e.getID());
		}

		ambulanceTeamsList = new ArrayList<EntityID>(ambulanceTeams);
		
		internalID = ambulanceTeamsList.indexOf(me().getID()) + 1;
		
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

		changeState(State.RANDOM_WALKING);
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	}

	@Override
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
				for (Pair<Integer,Integer> channel : channelList) {
					sendSpeak(time, channel.first(),
							msg.getMessage(channel.second().intValue()));
				}
			}
		}

		// Work on the task, if you have one
		if (target != null) {
			// Am I carrying a civilian?
			if (targetOnBoard(time)) {
				changeState(State.CARRYING_CIVILIAN);
				// Am I at a refuge?
				if (refuges.contains(currentPosition)) {
					sendUnload(time);
					log("I'm at a refuge, so unloading");
					return;
				}
				// No? I need to get to one, then.
				List<EntityID> path = search.breadthFirstSearch(
						currentPosition, refuges);
				changeState(State.MOVING_TO_REFUGE);

				if (path == null) {
					path = randomWalk();
					log("Trying to move to refuge, but couldn't find path");
					changeState(State.RANDOM_WALKING);
				}
				sendMove(time, path);
				return;
			} else {
				Human victim = (Human) model.getEntity(target);

				if (victim.getPosition().equals(currentPosition)) {
					if (victim.getBuriedness() != 0) {
						sendRescue(time, target);
						changeState(State.RESCUEING);
					} else if (victim instanceof Civilian) {
						sendLoad(time, target);
						log("Loading civilian");
					}
					return;
				} else {
					List<EntityID> path = search.breadthFirstSearch(
							currentPosition, victim.getPosition());
					if (path != null) {
						changeState(State.MOVING_TO_TARGET);
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
					changeState(State.PATROLLING);
					return;
				}
			}
		}

		path = randomWalk();
		sendMove(time, path);
		changeState(State.RANDOM_WALKING);
		return;
	}

	/**
	 * Check if this ambulance is carrying a civilian.
	 * 
	 * @return true if the ambulance is carrying a civilian, false otherwise.
	 */
	private boolean targetOnBoard(int time) {
		// FIXME deu null pointer aqui
		if (((Human) model.getEntity(target)).isPositionDefined()) {
			if (((Human) model.getEntity(target)).getPosition().equals(
					this.getID())) {
				return true;
			}
			return false;
		}

		log("Position of the human target not defined. Can't say if it's on board");
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
		return lastPosition.getValue() == currentPosition.getValue()
				&& isMovingState()
				&& time > 3
				&& getBlockedRoads().contains(currentPosition);
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

			Human victim = pickVictim();

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

		return null;
	}
	
	
	private Human pickVictim() {
		int finalDistance = Integer.MAX_VALUE;
		Human result = null;

		for (EntityID next : taskTable.keySet()) {
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

				//TODO:Update final code
				if (victim.getID() != taskDropped) {
					if (distanceFromAT + distanceToRefuge < finalDistance) {
						// Se nao ha nenhuma outra entidade salvando a vitima
						if (taskTable.keySet().contains(victim.getID())
								&& taskTable.get(victim.getID()).isEmpty()) {
							if (isSavable(victim, distanceFromAT
									+ distanceToRefuge)) {
								finalDistance = distanceFromAT
										+ distanceToRefuge;
								result = victim;
								log("Going to save victim "+ victim);
							} else {
								log("Not possible to save victim "+ victim);
							}
						} else {
							log("Victim already being rescued, picking another one "+ victim);
						}

					}
				}
			}
		}

		return result;
	}
	
	
	//TODO:Update final code
		private boolean isSavable(Human victim, int totalDistance){
			
			//Calcula quantos ciclos a vitima tem de vida
			int remainingCycles = 0;
			if(victim.isDamageDefined() && victim.getDamage()!=0)
				remainingCycles = victim.getHP()/victim.getDamage();
			else
				return true;
			
			//Calcula quantos ciclos precisa para salvar a vitima
			//TODO: considerar tempo de rescue e load
			int necessaryCycles = 0;

			if(maxDistanceTraveledPerCycle!=0)
				necessaryCycles = totalDistance/maxDistanceTraveledPerCycle;
			
			if(necessaryCycles>remainingCycles)
				return false;
			
			return true;
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
		if (!taskTable.keySet().contains(target) && !targetOnBoard(time)) {
			taskDropped = target;
			target = null;
		} else if (amIBlocked(time) && !targetOnBoard(time)) {
			taskDropped = target;
			target = null;
		}		
	}
	
	private boolean isMovingState() {
		List<State> ss = new ArrayList<State>();
		ss.add(State.PATROLLING);
		ss.add(State.TAKING_ALTERNATE_ROUTE);
		ss.add(State.MOVING_TO_TARGET);
		ss.add(State.MOVING_TO_REFUGE);
		ss.add(State.RANDOM_WALKING);

		return ss.contains(state);
	}
	
	private void changeState(State state) {
		this.state = state;
		log("Changed state to: " + this.state);
	}
}