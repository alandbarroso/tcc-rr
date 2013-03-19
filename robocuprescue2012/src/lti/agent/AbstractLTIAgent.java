package lti.agent;

//TODO implementar checksum

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lti.message.Message;
import lti.message.Parameter;
import lti.message.Parameter.Operation;
import lti.message.type.BlockadeCleared;
import lti.message.type.BuildingBurnt;
import lti.message.type.Fire;
import lti.message.type.FireExtinguished;
import lti.message.type.TaskDrop;
import lti.message.type.TaskPickup;
import lti.message.type.Victim;
import lti.message.type.VictimDied;
import lti.message.type.VictimRescued;
import lti.utils.Search;

import rescuecore2.Constants;
import rescuecore2.messages.Command;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public abstract class AbstractLTIAgent<E extends StandardEntity> extends
		StandardAgent<E> {

	protected static final int RANDOM_WALK_LENGTH = 50;

	private static final String MAX_SIGHT_KEY = "perception.los.max-distance";

	private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class
			.getName();

	private static final String CHANNEL_COUNT = "comms.channels.count";

	private static final String MAX_CHANNEL_PLATOON = "comms.channels.max.platoon";

	private static final String MAX_CHANNEL_CENTRE = "comms.channels.max.centre";

	protected Search search;

	protected Set<EntityID> buildingIDs;

	protected Set<EntityID> roadIDs;

	protected Map<EntityID, Set<EntityID>> neighbours;

	// Maximum sight distance
	protected int maxSight;

	// Communication channel available
	protected boolean channelComm;

	// Number of channels
	protected int numChannels;

	// Maximum channels for Platoon
	protected int maxChannelPlatoon;

	// Maximum channels for Centre
	protected int maxChannelCentre;

	// Receive channel number
	protected int receiveChannel;

	// Send channel number
	protected int sendChannel;

	protected List<Integer> channelList;

	protected EntityID lastPosition;

	protected EntityID currentPosition;

	protected EntityID target;

	protected Map<EntityID, Set<EntityID>> taskTable;

	// Incêndios já conhecidos
	protected Set<EntityID> buildingsOnFire;

	// Bloqueios já conhecidos
	protected Set<EntityID> knownBlockades;

	// Vítimas já conhecidas
	protected Set<EntityID> knownVictims;

	protected boolean blocked;

	protected EntityID taskDropped;

	@Override
	protected void postConnect() {
		super.postConnect();

		model.indexClass(StandardEntityURN.BUILDING);

		buildingIDs = new HashSet<EntityID>();

		roadIDs = new HashSet<EntityID>();

		for (StandardEntity next : model) {
			if (next instanceof Building) {
				buildingIDs.add(next.getID());
			} else {
				if (next instanceof Road) {
					roadIDs.add(next.getID());
				}
			}
		}

		search = new Search(model);

		neighbours = search.getGraph();

		boolean speakComm = config.getValue(Constants.COMMUNICATION_MODEL_KEY)
				.equals(SPEAK_COMMUNICATION_MODEL);

		this.numChannels = this.config.getIntValue(CHANNEL_COUNT);
		if ((speakComm) && (this.numChannels > 1)) {
			this.channelComm = true;
		} else {
			this.channelComm = false;
		}

		this.maxChannelPlatoon = this.config.getIntValue(MAX_CHANNEL_PLATOON);
		this.maxChannelCentre = this.config.getIntValue(MAX_CHANNEL_CENTRE);

		this.receiveChannel = 0;
		this.sendChannel = 0;

		channelList = new ArrayList<Integer>();

		maxSight = config.getIntValue(MAX_SIGHT_KEY);

		lastPosition = null;

		currentPosition = location().getID();

		target = null;

		taskTable = new HashMap<EntityID, Set<EntityID>>();

		taskDropped = null;

		buildingsOnFire = new HashSet<EntityID>();

		knownBlockades = new HashSet<EntityID>();

		knownVictims = new HashSet<EntityID>();
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		lastPosition = currentPosition;
		currentPosition = location().getID();
		taskDropped = null;

		// Subscribe to a communication channel
		if (time == this.config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {

			if (channelComm) {
				for (int i = 1; i <= numChannels; i++) {
					channelList.add(new Integer(i));
				}
				Collections.shuffle(channelList);
				sendSubscribe(time, channelList.get(0).intValue());
			}
		}

		// Refresh the World Model
		refreshWorldModel(changed, heard);

		// Refresh the Task Table
		refreshTaskTable(changed);
	}

	protected List<EntityID> randomWalk() {
		List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
		Set<EntityID> seen = new HashSet<EntityID>();
		EntityID current = ((Human) me()).getPosition();
		for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
			result.add(current);
			seen.add(current);
			List<EntityID> possible = new ArrayList<EntityID>();
			boolean found = false;

			for (EntityID next : neighbours.get(current)) {
				if (model.getEntity(next) instanceof Building) {
					if (!((Building) model.getEntity(next)).isOnFire()) {
						possible.add(next);
					}
				} else {
					possible.add(next);
				}
			}

			Collections.shuffle(possible, random);

			for (EntityID next : possible) {
				if (seen.contains(next)) {
					continue;
				}
				current = next;
				found = true;
				break;
			}
			if (!found) {
				// We reached a dead-end.
				break;
			}
		}
		return result;
	}

	protected Set<EntityID> getSafeBuildings(ChangeSet changed) {
		Set<EntityID> buildings = getVisibleEntitiesOfType(
				StandardEntityURN.BUILDING, changed);
		Set<EntityID> result = new HashSet<EntityID>();

		for (EntityID next : buildings) {
			StandardEntity e = model.getEntity(next);
			if (!((Building) e).isOnFire()) {
				result.add(next);
			}
		}
		return result;
	}

	protected Set<EntityID> getVisibleEntitiesOfType(StandardEntityURN type,
			ChangeSet changed) {
		Set<EntityID> visibleEntities = changed.getChangedEntities();
		Set<EntityID> result = new HashSet<EntityID>();

		for (EntityID next : visibleEntities) {
			if (model.getEntity(next).getStandardURN().equals(type)) {
				result.add(next);
			}
		}
		return result;
	}

	/*
	 * Não é mais utilizada devido à comunicação, pois requer parâmetros
	 * ("apexes") que não são passados por mensagens
	 */
	protected int findDistanceTo(Blockade b, int x, int y) {
		// Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
		List<Line2D> lines = GeometryTools2D.pointsToLines(
				GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D origin = new Point2D(x, y);
		for (Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next,
					origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			// Logger.debug("Next line: " + next + ", closest point: " + closest
			// + ", distance: " + d);
			if (d < best) {
				best = d;
				// Logger.debug("New best distance");
			}
		}
		return (int) best;
	}

	// TODO verificar se essa função é confiável
	/* Retorna uma lista com todas as estradas que possuem algum bloqueio */
	protected List<EntityID> getBlockedRoads() {
		Collection<StandardEntity> e = model
				.getEntitiesOfType(StandardEntityURN.ROAD);
		List<EntityID> result = new ArrayList<EntityID>();
		for (StandardEntity next : e) {
			Road road = (Road) next;
			if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()) {
				result.add(road.getID());
			}
		}
		return result;
	}

	/* TODO fazer uma boa implementação dela em cada agente */
	/** Avalia se é melhor abandonar uma tarefa */
	protected abstract void dropTask(int time, ChangeSet changed);

	/**
	 * Refresh the task table and the world model of the agent.
	 * 
	 * @param changed
	 *            The agent's ChangeSet for this timestep.
	 */
	protected abstract void refreshTaskTable(ChangeSet changed);

	/**
	 * Atualiza o modelo de mundo do agente de acordo com o que ele enxerga no
	 * momento e as mensagens por ele recebidas
	 */
	protected void refreshWorldModel(ChangeSet changed,
			Collection<Command> heard) {
		Set<StandardEntity> blockades = new HashSet<StandardEntity>(
				model.getEntitiesOfType(StandardEntityURN.BLOCKADE));

		/*
		 * Remove blockades that do not exist anymore (blockades the agent
		 * should see, but does not)
		 */
		for (StandardEntity next : blockades) {
			if (((Blockade) next).getPosition().equals(currentPosition)
					&& !getVisibleEntitiesOfType(StandardEntityURN.BLOCKADE,
							changed).contains(next.getID())) {
				model.removeEntity(next);
			}
		}

		Message speakMsg;
		for (Command cmd : heard) {
			if (cmd instanceof AKSpeak) {
				speakMsg = new Message(((AKSpeak) cmd).getContent());

				/*
				 * Para cada parâmetro da mensagem, verificar qual é o seu tipo
				 * e, caso à entidade à qual a mensagem se refere não esteja
				 * visível, atualizar o modelo de mundo de acordo.
				 */

				// Avalia se foi identificado um novo incêndio
				for (Parameter param : speakMsg.getParameters()) {
					if (param.getOperation().equals(Operation.FIRE)
							&& param instanceof Fire) {
						Fire fire = (Fire) param;
						EntityID buildingID = new EntityID(fire.getBuilding());

						if (!changed.getChangedEntities().contains(buildingID)) {
							StandardEntity entity = model.getEntity(buildingID);
							Building building;

							if (entity != null && entity instanceof Building) {
								building = (Building) entity;
							} else {
								building = new Building(buildingID);
							}

							building.setFieryness(fire.getIntensity());
							building.setGroundArea(fire.getGroundArea());
							building.setFloors(fire.getFloors());

							model.addEntity(building);
							// Esse incêndio passa a ser conhecido
							buildingsOnFire.add(buildingID);
						}
					}

					// Avalia se foi identificado um novo bloqueio
					else if (param.getOperation().equals(Operation.BLOCKADE)
							&& param instanceof lti.message.type.Blockade) {
						lti.message.type.Blockade blockade = (lti.message.type.Blockade) param;
						EntityID blockadeID = new EntityID(
								blockade.getBlockade());

						if (!changed.getChangedEntities().contains(blockadeID)) {
							StandardEntity entity = model.getEntity(blockadeID);
							Blockade block;

							if (entity != null && entity instanceof Blockade) {
								block = (Blockade) entity;
							} else {
								block = new Blockade(blockadeID);
							}

							block.setPosition(new EntityID(blockade.getRoad()));
							block.setX(blockade.getX());
							block.setY(blockade.getY());
							block.setRepairCost(blockade.getCost());

							model.addEntity(block);
							// O bloqueio passa a ser conhecido
							knownBlockades.add(blockadeID);
						}
					}
					// Avalia se foi identificada uam nova vítima
					else if (param.getOperation().equals(Operation.VICTIM)
							&& param instanceof Victim) {
						Victim victim = (Victim) param;
						EntityID victimID = new EntityID(victim.getVictim());

						if (!changed.getChangedEntities().contains(victimID)) {
							StandardEntity entity = model.getEntity(victimID);
							Human human;

							if (entity != null && entity instanceof Human) {
								switch (entity.getStandardURN()) {
								case FIRE_BRIGADE:
									human = (FireBrigade) entity;
									break;

								case POLICE_FORCE:
									human = (PoliceForce) entity;
									break;

								case AMBULANCE_TEAM:
									human = (AmbulanceTeam) entity;
									break;

								default:
									human = (Civilian) entity;
								}
							} else {

								switch (victim.getURN()) {
								case FIRE_BRIGADE:
									human = new FireBrigade(new EntityID(
											victim.getVictim()));
									break;

								case POLICE_FORCE:
									human = new PoliceForce(new EntityID(
											victim.getVictim()));
									break;

								case AMBULANCE_TEAM:
									human = new AmbulanceTeam(new EntityID(
											victim.getVictim()));
									break;

								default:
									human = new Civilian(new EntityID(
											victim.getVictim()));
								}
							}

							human.setPosition(new EntityID(victim.getPosition()));
							human.setHP(victim.getHP());
							human.setDamage(victim.getDamage());
							human.setBuriedness(victim.getBuriedness());

							model.addEntity(human);
							knownVictims.add(victimID);
						}
					}
					// Algum agente abandonou uma tarefa
					else if (param.getOperation().equals(Operation.TASK_DROP)
							&& param instanceof TaskDrop) {
						TaskDrop task = (TaskDrop) param;
						EntityID taskID = new EntityID(task.getTask());

						/*
						 * Remove o ID deste agente do conjunto de agentes
						 * agindo sobre aquela tarefa
						 */
						if (taskTable.containsKey(taskID)) {
							taskTable.get(taskID).remove(cmd.getAgentID());
						}
					}
					// Algum agente passou a atuar em uma tarefa
					else if (param.getOperation().equals(Operation.TASK_PICKUP)
							&& param instanceof TaskPickup) {
						TaskPickup task = (TaskPickup) param;
						EntityID taskID = new EntityID(task.getTask());

						if (taskTable.containsKey(taskID)) {
							if (taskTable.values().contains(cmd.getAgentID())) {
								taskTable.values().remove(cmd.getAgentID());
							}

							taskTable.get(taskID).add(cmd.getAgentID());
						}
					} else if (param.getOperation().equals(
							Operation.BLOCKADE_CLEARED)
							&& param instanceof BlockadeCleared) {
						BlockadeCleared blockade = (BlockadeCleared) param;
						EntityID blockadeID = new EntityID(
								blockade.getBlockade());

						if (model.getEntity(blockadeID) != null
								&& model.getEntity(blockadeID) instanceof Blockade) {
							model.removeEntity(blockadeID);
							knownBlockades.remove(blockadeID);
						}
					} else if (param.getOperation().equals(
							Operation.VICTIM_DIED)
							&& param instanceof VictimDied) {
						VictimDied victim = (VictimDied) param;
						EntityID victimID = new EntityID(victim.getVictim());

						if (!changed.getChangedEntities().contains(victimID)) {
							StandardEntity entity = model.getEntity(victimID);
							Human human;

							if (entity != null && entity instanceof Human) {
								switch (entity.getStandardURN()) {
								case FIRE_BRIGADE:
									human = (FireBrigade) entity;
									break;

								case POLICE_FORCE:
									human = (PoliceForce) entity;
									break;

								case AMBULANCE_TEAM:
									human = (AmbulanceTeam) entity;
									break;

								default:
									human = (Civilian) entity;
								}

								human.setHP(0);

								model.addEntity(human);
								knownVictims.remove(victimID);
							}
						}
					} else if (param.getOperation().equals(
							Operation.VICTIM_RESCUED)
							&& param instanceof VictimRescued) {
						VictimRescued victim = (VictimRescued) param;
						EntityID victimID = new EntityID(victim.getVictim());

						if (!changed.getChangedEntities().contains(victimID)) {
							StandardEntity entity = model.getEntity(victimID);
							Human human;

							if (entity != null && entity instanceof Human) {
								switch (entity.getStandardURN()) {
								case FIRE_BRIGADE:
									human = (FireBrigade) entity;
									break;

								case POLICE_FORCE:
									human = (PoliceForce) entity;
									break;

								case AMBULANCE_TEAM:
									human = (AmbulanceTeam) entity;
									break;

								default:
									human = (Civilian) entity;
								}

								human.setBuriedness(0);

								model.addEntity(human);
								knownVictims.remove(victimID);
							}
						}
					} else if (param.getOperation().equals(
							Operation.FIRE_EXTINGUISHED)
							&& param instanceof FireExtinguished) {
						FireExtinguished fire = (FireExtinguished) param;
						EntityID buildingID = new EntityID(fire.getBuilding());

						if (!changed.getChangedEntities().contains(buildingID)) {
							StandardEntity entity = model.getEntity(buildingID);

							if (entity != null && entity instanceof Building) {
								Building building = (Building) entity;
								building.setFieryness(Fieryness.WATER_DAMAGE
										.ordinal());
								model.addEntity(building);
								buildingsOnFire.remove(buildingID);
							}
						}
					} else if (param.getOperation().equals(
							Operation.BUILDING_BURNT)
							&& param instanceof BuildingBurnt) {
						BuildingBurnt fire = (BuildingBurnt) param;
						EntityID buildingID = new EntityID(fire.getBuilding());

						if (!changed.getChangedEntities().contains(buildingID)) {
							StandardEntity entity = model.getEntity(buildingID);

							if (entity != null && entity instanceof Building) {
								Building building = (Building) entity;
								building.setFieryness(Fieryness.BURNT_OUT
										.ordinal());
								model.addEntity(building);
								buildingsOnFire.remove(buildingID);
							}
						}
					}
				}
			}
		}
	}

	/** Compõe uma mensagem para ser enviada de acordo com o que o agente vê */
	protected Message composeMessage(ChangeSet changed) {
		Message message = new Message();

		for (EntityID buildingID : getVisibleEntitiesOfType(
				StandardEntityURN.BUILDING, changed)) {
			Building building = (Building) model.getEntity(buildingID);

			/*
			 * Se o prédio está em chamas e este incêndio não é conhecido, deve
			 * se enviar uma mensagem falando sobre ele
			 */
			if (building.isOnFire() && !buildingsOnFire.contains(buildingID)) {
				Fire fire = new Fire(buildingID.getValue(),
						building.getGroundArea(), building.getFloors(),
						building.getFieryness());
				message.addParameter(fire);
				// O incêndio passa a ser conhecido
				buildingsOnFire.add(buildingID);

			}
			/*
			 * Se o prédio não está em chamas, mas havia um incêndio nele
			 * anteriormente, há duas opções: 1) O incêndio comsumiu todo o
			 * prédio; 2) O incêndio foi extinguido
			 */
			else if (!building.isOnFire()
					&& buildingsOnFire.contains(buildingID)) {

				if (building.getFierynessEnum().equals(Fieryness.BURNT_OUT)) {
					BuildingBurnt burnt = new BuildingBurnt(
							buildingID.getValue());
					message.addParameter(burnt);

				} else if (building.getFierynessEnum().compareTo(
						Fieryness.INFERNO) > 0) {
					FireExtinguished extinguished = new FireExtinguished(
							buildingID.getValue());
					message.addParameter(extinguished);
				}

				// Não se sabe mais de nenhum incêndio neste prédio
				buildingsOnFire.remove(buildingID);
			}
		}

		/*
		 * Se algum bloqueio visível não é conhecido, deve-se enviar uma
		 * mensagem relatando-o
		 */
		for (EntityID blockadeID : getVisibleEntitiesOfType(
				StandardEntityURN.BLOCKADE, changed)) {
			Blockade blockade = (Blockade) model.getEntity(blockadeID);

			if (!knownBlockades.contains(blockadeID)) {
				lti.message.type.Blockade block = new lti.message.type.Blockade(
						blockadeID.getValue(), blockade.getPosition()
								.getValue(), blockade.getX(), blockade.getY(),
						blockade.getRepairCost());
				message.addParameter(block);
				knownBlockades.add(blockadeID);
			}
		}

		Set<EntityID> toRemove = new HashSet<EntityID>();

		/*
		 * Se algum bloqueio conhecido não se encontra mais em seu lugar, ele
		 * foi removido
		 */
		for (EntityID blockadeID : knownBlockades) {
			if (model.getEntity(blockadeID) == null) {
				BlockadeCleared cleared = new BlockadeCleared(
						blockadeID.getValue());
				message.addParameter(cleared);
				toRemove.add(blockadeID);
			}
		}

		knownBlockades.removeAll(toRemove);

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
			nonRefugeBuildings.removeAll(getRefuges());

			for (EntityID next : getVisibleEntitiesOfType(urns[i], changed)) {
				Human human = (Human) model.getEntity(next);
				if (human.isHPDefined() && human.getHP() != 0) {
					if (urns[i].equals(StandardEntityURN.CIVILIAN)
							&& nonRefugeBuildings.contains(human.getPosition())) {
						victims.add(next);

					} else if (human.isBuriednessDefined()
							&& human.getBuriedness() != 0) {
						victims.add(next);
					}
				}
			}
		}

		/*
		 * Se alguma vítima visível não é conhecida, deve-se enviar uma mensagem
		 * relatando-a
		 */
		for (EntityID next : victims) {
			StandardEntity entity;
			Human human;
			int urn;

			if (!knownVictims.contains(next)) {
				entity = model.getEntity(next);

				if (entity != null && entity instanceof Human) {
					switch (entity.getStandardURN()) {
					case AMBULANCE_TEAM:
						human = (AmbulanceTeam) entity;
						urn = 0;
						break;

					case FIRE_BRIGADE:
						human = (FireBrigade) entity;
						urn = 1;
						break;

					case POLICE_FORCE:
						human = (PoliceForce) entity;
						urn = 2;
						break;

					default:
						human = (Civilian) entity;
						urn = 3;
					}

					Victim victim = new Victim(next.getValue(), human
							.getPosition().getValue(), human.getHP(),
							human.getDamage(), human.getBuriedness(), urn);
					message.addParameter(victim);
					knownVictims.add(next);
				}
			}
		}

		toRemove = new HashSet<EntityID>();

		/*
		 * Se alguma vítima conhecida não está no lugar aonde deveria estar ou
		 * morreu, deve-se enviar uma mensagem relatando a situação
		 */
		for (EntityID victim : knownVictims) {
			StandardEntity entity = model.getEntity(victim);

			if (entity != null && entity instanceof Human) {
				Human human = (Human) entity;

				if (human.getHP() == 0) {
					VictimDied death = new VictimDied(victim.getValue());
					message.addParameter(death);
					toRemove.add(victim);
				} else if (human.getBuriedness() == 0) {
					VictimRescued rescue = new VictimRescued(victim.getValue());
					message.addParameter(rescue);
					toRemove.add(victim);
				}
			}
		}

		knownVictims.removeAll(toRemove);

		/* Se uma tarefa foi abandonada, envia-se uma mensagem */
		if (taskDropped != null) {
			TaskDrop drop = new TaskDrop(taskDropped.getValue());
			message.addParameter(drop);

			/* Se uma tarefa foi escohida, envia-se uma mensagem relatando-a */
			if (target != null) {
				TaskPickup task = new TaskPickup(target.getValue());
				message.addParameter(task);
			}
		}

		return message;
	}

	protected abstract boolean amIBlocked(int time);

	protected EntityID selectTask() {
		return new EntityID(0);
	}
}