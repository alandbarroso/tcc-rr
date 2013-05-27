package lti.agent.police;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

	protected int lastX;

	protected int lastY;

	protected int currentX;

	protected int currentY;

	private static enum State {
		MOVING_TO_ROAD, RETURNING_TO_SECTOR, MOVING_TO_BLOCKADE, PATROLLING, RANDOM_WALKING, CLEARING, BURIED, DEAD, CLEARING_PATH
	};

	private EntityID obstructingBlockade;

	private int numberOfDivisions;

	@Override
	protected void postConnect() {
		super.postConnect();

		distance = config.getIntValue(DISTANCE_KEY);

		state = State.RANDOM_WALKING;

		obstructingBlockade = null;

		lastX = 0;

		lastY = 0;

		currentX = me().getX();

		currentY = me().getY();

		Set<EntityID> policeForces = new TreeSet<EntityID>(
				new EntityIDComparator());

		for (StandardEntity e : model
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
			policeForces.add(e.getID());
		}

		numberOfDivisions = policeForces.size() / 2;

		List<EntityID> policeForcesList = new ArrayList<EntityID>(policeForces);

		Set<Sector> sectors = sectorize(numberOfDivisions);

		printSectors(sectors);

		createDotFile(sectors, "setor");

		List<Sector> sectorsList = new ArrayList<Sector>(sectors);

		sector = sectorsList.get(policeForcesList.indexOf(me().getID())
				% numberOfDivisions);

		System.out.println("ID " + me().getID() + ", sector " + sector.getIndex());

		/*
		 * placesToCheck = new
		 * ArrayList<EntityID>(sector.getLocations().keySet());
		 */
		// Collections.shuffle(placesToCheck, new
		// Random(me().getID().getValue()));
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		super.think(time, changed, heard);
		lastX = currentX;
		lastY = currentY;
		currentX = me().getX();
		currentY = me().getY();

		if (me().getHP() == 0) {
			state = State.DEAD;

			return;
		}

		/*
		 * if (placesToCheck.isEmpty()) { placesToCheck = new
		 * ArrayList<EntityID>(sector.getLocations() .keySet());
		 * Collections.shuffle(placesToCheck, new Random(me().getID()
		 * .getValue() + time)); }
		 */

		if (me().getBuriedness() != 0) {
			state = State.BURIED;

			return;
		}

		// placesToCheck.remove(currentPosition);

		// Am I stuck?
		// if (amIBlocked(time) && obstructingBlockade == null) {
		if (amIBlocked(time) && state.compareTo(State.PATROLLING) < 0) {
			state = State.CLEARING_PATH;
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

				return;
			}
		}

		// Work on the task, if you have one
		if (target != null) {
			// Is the target visible and inside clearing range?
			if (blockadeInRange(target, changed)) {
				state = State.CLEARING;
				sendClear(time, target);

				logInfo(time, state.toString() + " " + target);
				return;
			}

			logInfo(time, "target " + target + " out of reach");

			List<EntityID> path;
			Blockade targetBlockade = (Blockade) model.getEntity(target);

			if (sector.getLocations().keySet().contains(currentPosition)) {
				path = search.breadthFirstSearch(currentPosition, sector,
						targetBlockade.getPosition());
				logInfo(time, "target " + target + " inside sector");
				logInfo(time, "neighbours of " + currentPosition + ": "
						+ sector.getNeighbours(currentPosition));
			} else {
				path = search.breadthFirstSearch(currentPosition,
						targetBlockade.getPosition());
				logInfo(time, "outside sector");
			}

			if (path != null) {
				state = State.MOVING_TO_BLOCKADE;
				sendMove(time, path, targetBlockade.getX(),
						targetBlockade.getY());

				logInfo(time, state.toString() + " " + target);
				return;
			}

			logInfo(time, "no path to target " + target);
		}

		// Move around the map
		List<EntityID> path;

		if (sector.getLocations().keySet().contains(currentPosition)) {
			path = randomWalk(time);
			state = State.RANDOM_WALKING;
		} else {
			List<EntityID> local = new ArrayList<EntityID>(sector
					.getLocations().keySet());
			path = search.breadthFirstSearch(currentPosition, local.get(0));
			state = State.RETURNING_TO_SECTOR;
		}

		if (path != null) {
			sendMove(time, path);

			logInfo(time, state.toString());
			return;
		}
	}

	/**
	 * Get the blockade with the best socring function.
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
		if (state.compareTo(State.PATROLLING) <= 0
				&& currentPosition.equals(lastPosition) && time > 3) {
			return true;
		}

		return false;
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

	@Override
	protected void dropTask(int time, ChangeSet changed) {
		if (!taskTable.containsKey(target)) {
			taskDropped = target;
			target = null;
		}
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
			lengthDivisions = factors.second();
			heightDivisions = factors.first();
		} else {
			lengthDivisions = factors.first();
			heightDivisions = factors.second();
		}

		// Divide the map into sectors
		Set<Sector> sectors = new TreeSet<Sector>();

		for (int i = 1; i <= heightDivisions; i++) {
			for (int j = 1; j <= lengthDivisions; j++) {
				sectors.add(new Sector(minX + (length * (j - 1))
						/ lengthDivisions, minY + (height * (i - 1))
						/ heightDivisions, minX + (length * j)
						/ lengthDivisions, minY + (height * i)
						/ heightDivisions, lengthDivisions * (i - 1) + j));
			}
		}

		sectors = buildLocations(sectors);

		return sectors;
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
				sector.getBounds().first().first(), sector.getBounds().first()
						.second(), sector.getBounds().second().first(), sector
						.getBounds().second().second());

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

	/**
	 * Factorize a number into the two closest factors possible.
	 * 
	 * @param n
	 *            The number to be factorized.
	 * @return The pair of factors obtained.
	 */
	private Pair<Integer, Integer> factorization(int n) {
		Pair<Integer, Integer> result = null;
		int difference = Integer.MAX_VALUE;

		for (int i = 1; i <= (int) Math.sqrt(n); i++) {
			if (n % i == 0) {
				if ((n - n / i) < difference && (n - n / i) >= 0) {
					result = new Pair<Integer, Integer>(new Integer(i),
							new Integer(n / i));
					difference = n - n / i;
				}
			}
		}

		return result;
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
	
	private void logInfo(int time, String s) {
		System.out.println("PoliceF - Time " + time + " - ID " +
				me().getID() + " - Pos: (" + me().getX() + "," + me().getY() +
				") - " + s);
	}
}

class EntityIDComparator implements Comparator<EntityID> {

	public EntityIDComparator() {
	}

	@Override
	public int compare(EntityID a, EntityID b) {

		if (a.getValue() < b.getValue()) {
			return -1;
		}
		if (a.getValue() > b.getValue()) {
			return 1;
		}
		return 0;
	}
}