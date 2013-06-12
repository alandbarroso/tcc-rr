package lti.agent.police;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import lti.agent.AbstractLTIAgent;
import lti.message.Message;
import lti.utils.EntityIDComparator;
import area.Sector;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class LTIPoliceForce extends AbstractLTIAgent<PoliceForce> {

	private static final String DISTANCE_KEY = "clear.repair.distance";

	private int distance;

	private Sector sector;

	// private List<EntityID> placesToCheck;

	private State state;

	private static enum State {
		RETURNING_TO_SECTOR, MOVING_TO_BLOCKADE, RANDOM_WALKING,
		CLEARING, BURIED, DEAD, CLEARING_PATH
	};

	private EntityID obstructingBlockade;

	private int numberOfDivisions;
	
	private List<EntityID> policeForcesList;

	@Override
	protected void postConnect() {
		super.postConnect();
		currentX = me().getX();
		currentY = me().getY();

		Set<EntityID> policeForces = new TreeSet<EntityID>(
				new EntityIDComparator());

		for (StandardEntity e : model
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
			policeForces.add(e.getID());
		}

		policeForcesList = new ArrayList<EntityID>(policeForces);
		
		internalID = policeForcesList.indexOf(me().getID()) + 1;
		
		distance = config.getIntValue(DISTANCE_KEY);

		changeState(State.RANDOM_WALKING);

		obstructingBlockade = null;
		
		numberOfDivisions = defineNumberOfDivisions();

		Set<Sector> sectors = sectorize(numberOfDivisions);

		printSectors(sectors);

		createDotFile(sectors, "setor");

		sector = defineSector(sectors);
		log("Defined sector: " + sector);

		/*
		 * placesToCheck = new
		 * ArrayList<EntityID>(sector.getLocations().keySet());
		 */
		// Collections.shuffle(placesToCheck, new
		// Random(me().getID().getValue()));
	}

	/**
	 * Try to define the number of divisions which can maximize
	 * the performance of the sectorization
	 * @return numberOfDivisions
	 */
	private int defineNumberOfDivisions() {
		int numberOfDivisions = 1;
		int numberOfPoliceForces = policeForcesList.size();
		
		if (numberOfPoliceForces > 1)
			numberOfDivisions = numberOfPoliceForces / 2;
		
		Pair<Integer, Integer> factors = factorization(numberOfDivisions);
		// Prime numbers bigger than 3 (5, 7, 11, ...)
		if (factors.first() == 1 && factors.second() > 3)
			numberOfDivisions--;
		
		return numberOfDivisions;
	}

	/**
	 * Divides the map into sectors, assigning the "Area" entities contained in
	 * them to each Police Force.
	 * 
	 * @param neighbours
	 *            The table of adjacency of the graph to be sectorized.
	 */
	private Set<Sector> sectorize(int numberOfSectors) {
		Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> bounds = model
				.getWorldBounds();

		// Get the two points that define the map as rectangle.
		int minX = bounds.first().first();
		int minY = bounds.first().second();
		int maxX = bounds.second().first();
		int maxY = bounds.second().second();

		int length = maxX - minX;
		int height = maxY - minY;

		// Get the number of divisions on each dimension.
		Pair<Integer, Integer> factors = factorization(numberOfSectors);

		int lengthDivisions;
		int heightDivisions;

		if (length < height) {
			lengthDivisions = factors.first();
			heightDivisions = factors.second();
		} else {
			lengthDivisions = factors.second();
			heightDivisions = factors.first();
		}

		// Divide the map into sectors
		Set<Sector> sectors = new TreeSet<Sector>();

		for (int i = 0; i < heightDivisions; i++) {
			for (int j = 0; j < lengthDivisions; j++) {
				sectors.add(new Sector(
						minX + (length * j) / lengthDivisions,
						minY + (height * i) / heightDivisions,
						minX + (length * (j + 1)) / lengthDivisions,
						minY + (height * (i + 1)) / heightDivisions,
						lengthDivisions * i + j + 1)
				);
			}
		}

		sectors = buildLocations(sectors);

		return sectors;
	}

	/**
	 * Factorize a number into the two closest factors possible.
	 * 
	 * @param n
	 *            The number to be factorized.
	 * @return The pair of factors obtained.
	 */
	private Pair<Integer, Integer> factorization(int n) {
		for (int i = (int) Math.sqrt(n); i >= 1; i--)
			if (n % i == 0)
				return new Pair<Integer, Integer>(i, n / i);
		return new Pair<Integer, Integer>(1, n);
	}
	
	/**
	 * Builds a set of connected entities to each sector.
	 * 
	 * @param sectors
	 *            The set of sectors the entities should be allocated between.
	 * 
	 * @return The set of sectors received, with the entities allocated.
	 */
	private Set<Sector> buildLocations(Set<Sector> sectors) {
		Map<Integer, Set<Set<EntityID>>> connectedSubgraphs = new HashMap<Integer, Set<Set<EntityID>>>();
		/*
		 * connectedSubgraphs: maps each sector (through its index) to the set
		 * of connected subgraphs it contains
		 */

		// Form the subgraphs of each sector
		for (Sector s : sectors) {
			// Group the entities contained in the sector into subgraphs
			connectedSubgraphs.put(s.getIndex(), group(s));
		}

		// Get the main subgraph (the largest one) of each sector.
		for (Sector s : sectors) {
			int maxSize = 0;
			Set<EntityID> largest = null;

			if (!connectedSubgraphs.get(s.getIndex()).isEmpty()) {
				for (Set<EntityID> next : connectedSubgraphs.get(s.getIndex())) {
					if (next.size() >= maxSize) {
						maxSize = next.size();
						largest = next;
					}
				}

				for (EntityID next : largest) {
					s.addVertex(next);
				}
				connectedSubgraphs.get(s.getIndex()).remove(largest);
			}
		}

		Set<Set<EntityID>> subgraphs = new HashSet<Set<EntityID>>();

		for (Set<Set<EntityID>> next : connectedSubgraphs.values()) {
			subgraphs.addAll(next);
		}

		// Allocate the remaining subgraphs
		while (!subgraphs.isEmpty()) {
			List<Set<EntityID>> allocated = new ArrayList<Set<EntityID>>(
					allocateSubgraphs(subgraphs, sectors));

			/*
			 * Remove the allocated subgraphs from the subgraphs set
			 */
			while (!allocated.isEmpty()) {
				Set<EntityID> subgraph = allocated.get(0);

				if (subgraphs.contains(subgraph)) {
					subgraphs.remove(subgraph);
					allocated.remove(0);
				}
			}
		}

		// Map each entity of each sector to the set of its neighbours contained
		// in the sector
		for (Sector s : sectors) {
			for (EntityID location : s.getLocations().keySet()) {
				for (EntityID neighbour : neighbours.get(location)) {
					if (s.getLocations().containsKey(neighbour)) {
						if (s.getNeighbours(location) == null) {
							s.getLocations().put(location,
									new HashSet<EntityID>());
						}
						s.getNeighbours(location).add(neighbour);
					}
				}
			}
		}
		return sectors;
	}

	/**
	 * Group the entities contained in a sector into connected subgraphs.
	 * 
	 * @param sector
	 *            The sector whose entities should be grouped.
	 * 
	 * @return The set of subgraphs produced
	 */
	private Set<Set<EntityID>> group(Sector sector) {
		/*
		 * Get the entities that are partially or entirely inside the bounds of
		 * the sector
		 */
		Collection<StandardEntity> entities = model.getObjectsInRectangle(
			sector.getBounds().first().first(),
			sector.getBounds().first().second(),
			sector.getBounds().second().first(),
			sector.getBounds().second().second()
		);

		Set<EntityID> locations = new HashSet<EntityID>();

		/*
		 * Determine the entities geographically contained in the sector, so
		 * that each entity belongs to an unique sector
		 */
		for (StandardEntity next : entities) {
			if (next instanceof Area) {
				// Redundante com getObjectsInRectangle?
				if (sector.geographicallyContains((Area) next)) {
					locations.add(next.getID());
				}
			}
		}

		Set<Set<EntityID>> subgraphs = new HashSet<Set<EntityID>>();
		Set<EntityID> visited = new HashSet<EntityID>();
		List<EntityID> nodesLeft = new ArrayList<EntityID>(locations);

		// Group the nodes into connected subgraphs
		while (!nodesLeft.isEmpty()) {
			// connected: the current subgraph being constructed
			Set<EntityID> connected = new HashSet<EntityID>();
			// border: set of nodes to be expanded
			Set<EntityID> border = new HashSet<EntityID>();

			border.add(nodesLeft.remove(0));

			// Expand each subgraph
			while (!border.isEmpty()) {
				Set<EntityID> newBorder = new HashSet<EntityID>();

				for (EntityID e : border) {
					for (EntityID next : neighbours.get(e)) {
						if (locations.contains(next) && !visited.contains(next)
								&& !border.contains(next)) {
							newBorder.add(next);
						}
					}
					visited.add(e);
				}
				connected.addAll(border);
				nodesLeft.removeAll(border);
				border = newBorder;
			}
			subgraphs.add(connected);
		}

		return subgraphs;
	}

	/**
	 * Allocate each subgraph of a sector to the sector with the smallest main
	 * graph, if possible
	 * 
	 * @param subgraphs
	 *            The set of subgraphs to be allocated.
	 * 
	 * @param sectors
	 *            The set of sectors the subgraphs can be allocated to.
	 * 
	 * @return The set of subgraphs successfully allocated.
	 */
	private Set<Set<EntityID>> allocateSubgraphs(Set<Set<EntityID>> subgraphs,
			Set<Sector> sectors) {
		Set<Set<EntityID>> allocated = new HashSet<Set<EntityID>>();

		for (Set<EntityID> subgraph : subgraphs) {
			int smallestSectorSize = Integer.MAX_VALUE;
			Sector smallestSector = null;

			for (EntityID node : subgraph) {
				Set<Sector> evaluatedSectors = new HashSet<Sector>();

				for (EntityID next : neighbours.get(node)) {
					for (Sector s : sectors) {
						if (s.getLocations().containsKey(next)
								&& !evaluatedSectors.contains(s)) {
							if (s.getLocations().size() < smallestSectorSize) {
								smallestSectorSize = s.getLocations().size();
								smallestSector = s;
							}

							evaluatedSectors.add(s);
						}
					}
				}
			}

			if (smallestSector != null) {
				for (EntityID next : subgraph) {
					smallestSector.addVertex(next);
				}
				allocated.add(subgraph);
			}
		}

		return allocated;
	}
	
	private void printSectors(Set<Sector> sectors) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"setores.txt"));

			out.write("Map dimension: "
					+ model.getWorldBounds().first().first() + ","
					+ model.getWorldBounds().first().second() + " "
					+ model.getWorldBounds().second().first() + ","
					+ model.getWorldBounds().second().second());
			out.newLine();
			out.write("Number of Area entities: "
					+ new Integer(buildingIDs.size() + roadIDs.size()));
			out.newLine();
			out.write("Number of divisions: " + numberOfDivisions);
			out.newLine();
			out.newLine();
			out.flush();

			for (Sector next : sectors) {
				out.write("Sector " + next.getIndex());
				out.newLine();
				Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> bounds = next
						.getBounds();
				out.write("Bottom-left: " + bounds.first().first() + ", "
						+ bounds.first().second());
				out.newLine();
				out.write("Top-right: " + bounds.second().first() + ", "
						+ bounds.second().second());
				out.newLine();
				if (next.getLocations() != null) {
					out.write("Number of entities: "
							+ next.getLocations().keySet().size());
					out.newLine();
					out.flush();

					for (EntityID areaID : next.getLocations().keySet()) {
						out.write(areaID.toString());
						out.newLine();
					}

					out.newLine();
				} else {
					out.write("Empty sector");
					out.newLine();
					out.newLine();
					out.flush();
				}
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createDotFile(Set<Sector> sectors, String fileName) {
		BufferedWriter out;
		try {
			for (Sector sector : sectors) {
				out = new BufferedWriter(new FileWriter(fileName
						+ sector.getIndex() + ".dot"));
				out.write("graph sector" + sector.getIndex() + " {\n");

				for (EntityID node : sector.getLocations().keySet()) {
					out.write("\tsubgraph sg_" + node.toString() + "{\n");
					for (EntityID neighbour : sector.getNeighbours(node)) {
						out.write("\t\t" + node.toString() + " -- "
								+ neighbour.toString());
						out.newLine();
					}
					out.write("\t}\n\n");
					out.flush();
				}
				out.write("}");
				out.close();
			}
		} catch (IOException e) {
		}
	}

	/**
	 * @param sectors
	 */
	private Sector defineSector(Set<Sector> sectors) {
		List<Sector> sectorsList = new ArrayList<Sector>(sectors);
		int mypos = internalID;
		int nPolice = policeForcesList.size();

		int nEntitiesTotal = 0;
		for (Sector next: sectorsList)
			nEntitiesTotal += next.getLocations().keySet().size();
		
		
		if (mypos < sectorsList.size())
			return sectorsList.get(mypos);
		
		mypos -= sectorsList.size();
		nPolice -= sectorsList.size();
		int nEntities = 0;
		for (Sector next: sectorsList) {
			nEntities += next.getLocations().keySet().size();
			if (((double)(mypos+1)/nPolice) <=
					((double)nEntities/nEntitiesTotal))
				return next;
		}

		return sectorsList.get(0);
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

		/*
		 * if (placesToCheck.isEmpty()) { placesToCheck = new
		 * ArrayList<EntityID>(sector.getLocations() .keySet());
		 * Collections.shuffle(placesToCheck, new Random(me().getID()
		 * .getValue() + time)); }
		 */

		if (me().getBuriedness() != 0) {
			changeState(State.BURIED);
			return;
		}

		// placesToCheck.remove(currentPosition);

		// Am I stuck?
		// if (amIBlocked(time) && obstructingBlockade == null) {
		if (amIBlocked(time) && isMovingState()) {
			changeState(State.CLEARING_PATH);
			obstructingBlockade = getClosestBlockade();
		}

		// Evaluate task dropping
		if (target != null) {
			dropTask(time, changed);
		}

		// Pick a task to work upon, if you don't have one
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

		// Remove the obstructing blockade, if it exists
		if (state.equals(State.CLEARING_PATH)) {
			if (model.getEntitiesOfType(StandardEntityURN.BLOCKADE).contains(
					model.getEntity(obstructingBlockade))
					&& model.getDistance(me().getID(), obstructingBlockade) < distance) {
				sendClear(time, obstructingBlockade);
				log("Sent clear to remove the obstructing blockade: " + obstructingBlockade);
				return;
			}
		}

		// Work on the task, if you have one
		if (target != null) {
			// Is the target visible and inside clearing range?
			if (blockadeInRange(target, changed)) {
				changeState(State.CLEARING);
				sendClear(time, target);
				log("Sent clear to remove the target: " + target);
				return;
			}

			log("Target " + target + " out of direct reach");

			List<EntityID> path;
			Blockade targetBlockade = (Blockade) model.getEntity(target);

			if (sector.getLocations().keySet().contains(currentPosition)) {
				path = search.breadthFirstSearch(currentPosition, sector,
						targetBlockade.getPosition());
				log("I'm inside my sector");
				log("Neighbours of " + currentPosition + ": "
						+ sector.getNeighbours(currentPosition));
			} else {
				path = search.breadthFirstSearch(currentPosition,
						targetBlockade.getPosition());
				log("I'm outside my sector");
			}

			if (path != null) {
				changeState(State.MOVING_TO_BLOCKADE);
				sendMove(time, path, targetBlockade.getX(),
						targetBlockade.getY());
				log("Found path and sent move to target: " + target);
				return;
			}

			log("No path to target: " + target);
		}

		// Move around the map
		List<EntityID> path;

		if (sector.getLocations().keySet().contains(currentPosition)) {
			path = randomWalk(time);
			changeState(State.RANDOM_WALKING);
		} else {
			List<EntityID> local = new ArrayList<EntityID>(sector
					.getLocations().keySet());
			path = search.breadthFirstSearch(currentPosition, local.get(0));
			changeState(State.RETURNING_TO_SECTOR);
		}

		if (path != null) {
			sendMove(time, path);
			log("Path calculated and sent move");
			return;
		}
	}

	private List<EntityID> randomWalk(int time) {
		List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
		Set<EntityID> seen = new HashSet<EntityID>();
		EntityID current = ((Human) me()).getPosition();

		for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
			result.add(current);
			seen.add(current);
			List<EntityID> possible = new ArrayList<EntityID>();

			for (EntityID next : sector.getNeighbours(current)) {
				if (model.getEntity(next) instanceof Building) {
					if (!((Building) model.getEntity(next)).isOnFire()) {
						possible.add(next);
					}
				} else {
					possible.add(next);
				}
			}

			Collections.shuffle(possible, new Random(me().getID().getValue()
					+ time));
			boolean found = false;

			for (EntityID next : possible) {
				if (!seen.contains(next)) {
					current = next;
					found = true;
					break;
				}
			}
			if (!found) { // We reached a dead-end.
				break;
			}
		}

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
		if (getVisibleEntitiesOfType(StandardEntityURN.BLOCKADE, changed)
				.contains(blockade)
				&& model.getDistance(me().getID(), blockade) < distance) {
			return true;
		}

		return false;
	}
	
	/**
	 * Locates the closest blockade to the agent.
	 * 
	 * @return The closest blockade.
	 */
	private EntityID getClosestBlockade() {
		int dist = Integer.MAX_VALUE;
		EntityID result = null;
		Set<StandardEntity> blockades = new HashSet<StandardEntity>(
				model.getEntitiesOfType(StandardEntityURN.BLOCKADE));

		for (StandardEntity next : blockades) {
			int newDist = model.getDistance(getID(), next.getID());

			if (newDist < dist) {
				result = next.getID();
				dist = newDist;
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

		for (EntityID next : taskTable.keySet()) {
			Blockade blockade = (Blockade) model.getEntity(next);

			int thisDistance = model.getDistance(getID(), blockade.getID());
			/*
			 * int repairCost = blockade.getRepairCost();
			 * 
			 * Set<EntityID> civiliansInSector = new HashSet<EntityID>(); for
			 * (StandardEntity civilian : model
			 * .getEntitiesOfType(StandardEntityURN.CIVILIAN)) { if
			 * (sector.getLocations().keySet() .contains(((Civilian)
			 * civilian).getPosition())) {
			 * civiliansInSector.add(civilian.getID()); } }
			 * 
			 * EntityID blockadePosition = blockade.getPosition(); int
			 * civiliansAround = 0; for (EntityID e : civiliansInSector) { if
			 * (blockadePosition.equals(((Civilian) model.getEntity(e))
			 * .getPosition())) { civiliansAround++; } else if
			 * (sector.getNeighbours(blockadePosition) != null) { if
			 * (sector.getNeighbours(blockadePosition).contains( ((Civilian)
			 * model.getEntity(e)).getPosition())) { civiliansAround++; } } }
			 * 
			 * Set<EntityID> burningBuildings = new HashSet<EntityID>(); for
			 * (EntityID e : buildingIDs) { if
			 * (sector.getLocations().containsKey(e)) { if (((Building)
			 * model.getEntity(e)).isOnFire()) { burningBuildings.add(e); } } }
			 * 
			 * int burningBuildingsAround = 0; for (EntityID building :
			 * burningBuildings) { if (sector.getNeighbours(blockadePosition) !=
			 * null) { if (sector.getNeighbours(blockadePosition).contains(
			 * building)) { burningBuildingsAround++; } else { for (EntityID
			 * neighbour : sector .getNeighbours(blockadePosition)) { if
			 * (sector.getNeighbours(neighbour) != null) { if
			 * (sector.getNeighbours(neighbour).contains( building)) {
			 * burningBuildingsAround++; } } } } } }
			 * 
			 * if (civiliansInSector.isEmpty() && burningBuildings.isEmpty()) {
			 * newPb = 1 / Math.sqrt(distance / 1000) + 1 / repairCost; } else
			 * if (!civiliansInSector.isEmpty()) { newPb = 1 /
			 * Math.sqrt(distance / 1000) + 1 / repairCost + civiliansAround /
			 * civiliansInSector.size(); } else if (!burningBuildings.isEmpty())
			 * { newPb = 1 / Math.sqrt(distance / 1000) + 1 / repairCost +
			 * burningBuildingsAround / burningBuildings.size(); } else { newPb
			 * = 1 / Math.sqrt(distance / 1000) + 1 / repairCost +
			 * civiliansAround / civiliansInSector.size() +
			 * burningBuildingsAround / burningBuildings.size(); }
			 */
			double thisPb = 1 / ((double) thisDistance / 1000);

			if (thisPb > Pb) {
				result = next;
				Pb = thisPb;
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
	protected void refreshWorldModel(ChangeSet changed,
			Collection<Command> heard) {

		// Remove the blockade the agent has finished clearing
		if (state.equals(State.CLEARING)
				&& !getVisibleEntitiesOfType(StandardEntityURN.BLOCKADE,
						changed).contains(target)) {
			model.removeEntity(target);

		} else if (state.equals(State.CLEARING_PATH)
				&& !getVisibleEntitiesOfType(StandardEntityURN.BLOCKADE,
						changed).contains(obstructingBlockade)) {
			model.removeEntity(obstructingBlockade);
			obstructingBlockade = null;
		}

		super.refreshWorldModel(changed, heard);
	}

	@Override
	protected void refreshTaskTable(ChangeSet changed) {
		Set<StandardEntity> blockades = new HashSet<StandardEntity>(
				model.getEntitiesOfType(StandardEntityURN.BLOCKADE));
		Set<EntityID> blockadesIDs = new HashSet<EntityID>();

		for (StandardEntity next : blockades) {
			blockadesIDs.add(next.getID());
		}

		// Discard blockades that do not exist anymore
		taskTable.keySet().retainAll(blockadesIDs);

		// Add new blockades to the task table
		for (StandardEntity blockade : blockades) {
			if (sector.getLocations().keySet()
					.contains(((Blockade) blockade).getPosition())) {
				if (!taskTable.containsKey(blockade.getID())) {
					taskTable.put(blockade.getID(), new HashSet<EntityID>());
				}
			}
		}
	}

	@Override
	protected boolean amIBlocked(int time) {
		return lastPosition.getValue() == currentPosition.getValue()
				&& isMovingState()
				&& time > 3;
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

	private boolean isMovingState() {
		List<State> ss = new ArrayList<State>();
		ss.add(State.RETURNING_TO_SECTOR);
		ss.add(State.MOVING_TO_BLOCKADE);
		ss.add(State.RANDOM_WALKING);

		return ss.contains(state);
	}
	
	private void changeState(State state) {
		this.state = state;
		log("Changed state to: " + this.state);
	}
}
