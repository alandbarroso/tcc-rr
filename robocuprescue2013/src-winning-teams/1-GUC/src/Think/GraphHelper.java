package Think;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class GraphHelper {
	private Search search;

	private Map<EntityID, Set<EntityID>> graph, graphCopy;
	ArrayList<Pair<EntityID, ArrayList<EntityID>>> removedNodes = new ArrayList<Pair<EntityID, ArrayList<EntityID>>>();
	StandardWorldModel model;

	public GraphHelper(StandardWorldModel model) {
		this.model = model;
		search = new Search(model);
		graphCopy = search.getGraph();
		graph = (new Search(model)).getGraph(); // Acquire graph
	}

	public Search getSearch() {
		return search;
	}

	public Map<EntityID, Set<EntityID>> getGraph() {
		return graphCopy;
	}

	public void resetGraph() {
		search = new Search(model);
		graphCopy = search.getGraph();
		removedNodes.clear();
	}

	public boolean removeNode(EntityID r) {
		if (!Think.existsPairNode(r, removedNodes)) {
			Pair<EntityID, ArrayList<EntityID>> node = removeNodeFromNeighbours(
					r, model, graphCopy, graph);
			removedNodes.add(node);
			return true;
		}
		return false;
	}

	public boolean restoreNode(EntityID r) {
		if (Think.existsPairNode(r, removedNodes)) {
			return restoreNode(r, model, graphCopy, removedNodes);
		}
		return false;
	}

	private static Pair<EntityID, ArrayList<EntityID>> removeNodeFromNeighbours(
			EntityID node, StandardWorldModel model,
			Map<EntityID, Set<EntityID>> graphCopy,
			Map<EntityID, Set<EntityID>> graphOriginal) {
		if (node == null)
			return null;
		Pair<EntityID, ArrayList<EntityID>> set = new Pair<EntityID, ArrayList<EntityID>>(
				node, new ArrayList<EntityID>());
		// System.out.println(((Area) model.getEntity(node)).getNeighbours());
		// System.out.println(graph.get(node));

		for (EntityID ne : graphOriginal.get(node)) {
			// System.out.println(ne);
			boolean found = false;
			while (graphCopy.get(ne).remove(node))
				found = true;
			if (found)
				set.second().add(ne);
		}
		return set;
	}

	public static boolean restoreNode(EntityID node, StandardWorldModel model,
			Map<EntityID, Set<EntityID>> graph,
			ArrayList<Pair<EntityID, ArrayList<EntityID>>> removedNodes) {
		Pair<EntityID, ArrayList<EntityID>> currentNode = null;
		for (Pair<EntityID, ArrayList<EntityID>> current : removedNodes)
			if (current.first().getValue() == node.getValue()) {
				currentNode = current;
				break;
			}
		if (currentNode == null)
			return false;
		for (EntityID ne : currentNode.second())
			graph.get(ne).add(node);
		removedNodes.remove(currentNode);
		return true;
	}

	public String removedNodesToString() {
		String toString = "";
		for (Pair<EntityID, ArrayList<EntityID>> e : removedNodes) {
			toString = toString.concat(e.first() + ", ");
		}
		return toString;
	}
}
