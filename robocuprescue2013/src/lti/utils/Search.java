package lti.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import area.Sector;

import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class Search {
	private Map<EntityID, Set<EntityID>> graph;
	private StandardWorldModel model;

	/**
	 * Construct a new SampleSearch.
	 * 
	 * @param world
	 *            The world model to construct the neighbourhood graph from.
	 */
	public Search(StandardWorldModel world) {
		this.model = world;
		
		Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
			@Override
			public Set<EntityID> createValue() {
				return new HashSet<EntityID>();
			}
		};
		for (Entity next : world) {
			if (next instanceof Area) {
				Collection<EntityID> areaNeighbours = ((Area) next)
						.getNeighbours();
				neighbours.get(next.getID()).addAll(areaNeighbours);
			}
		}
		setGraph(neighbours);
	}

	/**
	 * Construct a new ConnectionGraphSearch.
	 * 
	 * @param graph
	 *            The connection graph in the form of a map from EntityID to the
	 *            set of neighbouring EntityIDs.
	 */
	public Search(Map<EntityID, Set<EntityID>> graph) {
		setGraph(graph);
	}

	/**
	 * Set the neighbourhood graph.
	 * 
	 * @param newGraph
	 *            The new neighbourhood graph.
	 */
	public void setGraph(Map<EntityID, Set<EntityID>> newGraph) {
		this.graph = newGraph;
	}

	/**
	 * Get the neighbourhood graph.
	 * 
	 * @return The neighbourhood graph.
	 */
	public Map<EntityID, Set<EntityID>> getGraph() {
		return graph;
	}

	/**
	 * Do a breadth first search from one location to the closest (in terms of
	 * number of nodes) of a set of goals.
	 * 
	 * @param start
	 *            The location we start at.
	 * @param goals
	 *            The set of possible goals.
	 * @return The path from start to one of the goals, or null if no path can
	 *         be found.
	 */
	public List<EntityID> breadthFirstSearch(EntityID start, EntityID... goals) {
		return breadthFirstSearch(start, Arrays.asList(goals));
	}
	
	public List<EntityID> aStarSearch(EntityID start, EntityID... goals) {
		return aStarSearch(start, Arrays.asList(goals));
	}
	
	public List<EntityID> breadthFirstSearchAvoidingBlockedRoads(
			EntityID start,
			Set<Pair<EntityID, EntityID>> transitionsBlocked,
			EntityID... goals) {
		return breadthFirstSearchAvoidingBlockedRoads(start, transitionsBlocked, Arrays.asList(goals));
	}
	
	public List<EntityID> aStarSearchAvoidingBlockedRoads(
			EntityID start,
			Set<Pair<EntityID, EntityID>> transitionsBlocked,
			EntityID... goals) {
		return aStarSearchAvoidingBlockedRoads(start, transitionsBlocked, Arrays.asList(goals));
	}

	/**
	 * Do a breadth first search from one location to the closest (in terms of
	 * number of nodes) of a set of goals.
	 * 
	 * @param start
	 *            The location we start at.
	 * @param goals
	 *            The set of possible goals.
	 * @return The path from start to one of the goals, or null if no path can
	 *         be found.
	 */
	public List<EntityID> breadthFirstSearch(EntityID start,
			Collection<EntityID> goals) {
		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);
		do {
			next = open.remove(0);
			if (isGoal(next, goals)) {
				found = true;
				break;
			}
			List<EntityID> neighbours = new ArrayList<EntityID>(graph.get(next));
			Collections.shuffle(neighbours);
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (isGoal(neighbour, goals)) {
					ancestors.put(neighbour, next);
					next = neighbour;
					found = true;
					break;
				} else {
					if (!ancestors.containsKey(neighbour)) {
						open.add(neighbour);
						ancestors.put(neighbour, next);
					}
				}
			}
		} while (!found && !open.isEmpty());
		if (!found) {
			// No path
			return null;
		}
		// Walk back from goal to start
		EntityID current = next;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, current);
			current = ancestors.get(current);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (current != start);
		return path;
	}
	
	public List<EntityID> aStarSearch(EntityID start, Collection<EntityID> goals){
		List<AStarEntityWrapper> open = new LinkedList<AStarEntityWrapper>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		
		open.add(new AStarEntityWrapper(start, goals));
		ancestors.put(start, start);
		
		AStarEntityWrapper current = null;
		boolean found = false;
		
		do {			
			current = open.remove(0);
			if (isGoal(current.id, goals)) {
				found = true;
				break;
			}
			List<EntityID> neighbours = new ArrayList<EntityID>(graph.get(current.id));
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (!ancestors.containsKey(neighbour)) {
					AStarEntityWrapper next = new AStarEntityWrapper(neighbour, current, goals);
					
					int index;
					for(index = 0; index < open.size() && (next.heuristic + next.pathCost >= open.get(index).heuristic + open.get(index).pathCost); index++);
					
					if(index < open.size())
					{
						open.add(index, next);
					}
					else
					{
						open.add(next);
					}
					ancestors.put(neighbour, current.id);
				}
			}
		} while (!found && !open.isEmpty());
		
		if (!found) {
			// No path
			return null;
		}
		
		// Walk back from goal to start
		EntityID pathEntity = current.id;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, pathEntity);
			pathEntity = ancestors.get(pathEntity);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (pathEntity != start);
		
		return path;
	}
	
	public class AStarEntityWrapper{
		public EntityID id;
		public Integer heuristic;
		public Integer pathCost;
		
		public AStarEntityWrapper(EntityID next, Collection<EntityID> goals){
			this.id = next;
			this.pathCost = 0;
			this.getHeuristic(goals);
		}
		
		public AStarEntityWrapper(EntityID next, AStarEntityWrapper current, Collection<EntityID> goals){
			this.id = next;
			this.pathCost = current.pathCost + model.getDistance(next, current.id);
			this.getHeuristic(goals);
		}
		
		private void getHeuristic(Collection<EntityID> goals)
		{
			this.heuristic = Integer.MAX_VALUE;
			
			for(EntityID id : goals){
				Integer aux = model.getDistance(this.id, id);
				if(aux < this.heuristic)
				{
					this.heuristic = aux;
				}
			}
		}
		
		public void print(int i){
			System.out.println("A* Node" + i + ":");
			System.out.println("Id:" + this.id + " cost:" + this.pathCost + " heuristic:" + this.heuristic);
		}
	}
	
	public List<EntityID> breadthFirstSearchAvoidingBlockedRoads(
			EntityID start,
			Set<Pair<EntityID, EntityID>> transitionsBlocked,
			Collection<EntityID> goals) {
		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);
		do {
			next = open.remove(0);
			if (isGoal(next, goals)) {
				found = true;
				break;
			}
			List<EntityID> neighbours = new ArrayList<EntityID>(graph.get(next));
			Collections.shuffle(neighbours);
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (isGoal(neighbour, goals)) {
					ancestors.put(neighbour, next);
					next = neighbour;
					found = true;
					break;
				} else {
					Pair<EntityID, EntityID> pair =
							new Pair<EntityID, EntityID>(next, neighbour);
					if (!ancestors.containsKey(neighbour) &&
							!transitionsBlocked.contains(pair)) {
						open.add(neighbour);
						ancestors.put(neighbour, next);
					}
				}
			}
		} while (!found && !open.isEmpty());
		if (!found) {
			// No path
			return null;
		}
		// Walk back from goal to start
		EntityID current = next;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, current);
			current = ancestors.get(current);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (current != start);
		return path;
	}
	
	public List<EntityID> aStarSearchAvoidingBlockedRoads(
			EntityID start,
			Set<Pair<EntityID, EntityID>> transitionsBlocked,
			Collection<EntityID> goals){
		List<AStarEntityWrapper> open = new LinkedList<AStarEntityWrapper>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		
		open.add(new AStarEntityWrapper(start, goals));
		ancestors.put(start, start);
		
		AStarEntityWrapper current = null;
		boolean found = false;
		
		do {			
			current = open.remove(0);
			if (isGoal(current.id, goals)) {
				found = true;
				break;
			}
			List<EntityID> neighbours = new ArrayList<EntityID>(graph.get(current.id));
			
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				Pair<EntityID, EntityID> pair =
						new Pair<EntityID, EntityID>(current.id, neighbour);
				if (!ancestors.containsKey(neighbour) && !transitionsBlocked.contains(pair)) {
					AStarEntityWrapper next = new AStarEntityWrapper(neighbour, current, goals);
					
					int index;
					for(index = 0; index < open.size() && (next.heuristic + next.pathCost >= open.get(index).heuristic + open.get(index).pathCost); index++);
					
					if(index < open.size())
					{
						open.add(index, next);
					}
					else
					{
						open.add(next);
					}
					ancestors.put(neighbour, current.id);
				}
			}
		} while (!found && !open.isEmpty());
		
		if (!found) {
			// No path
			return null;
		}
		
		// Walk back from goal to start
		EntityID pathEntity = current.id;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, pathEntity);
			pathEntity = ancestors.get(pathEntity);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (pathEntity != start);
		
		return path;
	}

	private boolean isGoal(EntityID e, Collection<EntityID> test) {
		return test.contains(e);
	}

	public List<EntityID> breadthFirstSearch(EntityID start, Sector sector,
			EntityID... goals) {
		return breadthFirstSearch(start, sector, Arrays.asList(goals));
	}

	/*
	 * FIXME Policiais não encontram um caminho até certos bloqueios contidos em
	 * seu setor
	 */
	public List<EntityID> breadthFirstSearch(EntityID start, Sector sector,
			Collection<EntityID> goals) {
		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);
		do {
			next = open.remove(0);
			if (isGoal(next, goals)) {
				found = true;
				break;
			}
			List<EntityID> neighbours = new ArrayList<EntityID>(
					sector.getNeighbours(next));
			Collections.shuffle(neighbours);
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (isGoal(neighbour, goals)) {
					ancestors.put(neighbour, next);
					next = neighbour;
					found = true;
					break;
				} else {
					if (!ancestors.containsKey(neighbour)) {
						open.add(neighbour);
						ancestors.put(neighbour, next);
					}
				}
			}
		} while (!found && !open.isEmpty());
		if (!found) {
			// No path
			return null;
		}
		// Walk back from goal to start
		EntityID current = next;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, current);
			current = ancestors.get(current);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (current != start);
		return path;
	}

	public List<EntityID> pathFinder(EntityID start,
			Collection<EntityID> blockedRoads, EntityID... goals) {
		return pathFinder(start, blockedRoads, Arrays.asList(goals));
	}

	public List<EntityID> pathFinder(EntityID start,
			Collection<EntityID> blockedRoads, Collection<EntityID> goals) {
		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);

		do {
			next = open.remove(0);
			if (isGoal(next, goals)) {
				found = true;
				break;
			}
			Collection<EntityID> neighbours = graph.get(next);
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (isGoal(neighbour, goals)) {
					ancestors.put(neighbour, next);
					next = neighbour;
					found = true;
					break;
				} else {
					if (!ancestors.containsKey(neighbour)
							&& !blockedRoads.contains(neighbour)) {
						open.add(neighbour);
						ancestors.put(neighbour, next);
					}
				}
			}
		} while (!found && !open.isEmpty());
		if (!found) {
			// No path
			return null;
		}
		// Walk back from goal to start
		EntityID current = next;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, current);
			current = ancestors.get(current);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (current != start);

		if (path.size() <= 2) {
			return null;
		}
		return path;
	}
}