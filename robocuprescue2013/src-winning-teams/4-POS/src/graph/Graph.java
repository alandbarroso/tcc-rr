package graph;

import java.util.ArrayList;

public class Graph {
	public ArrayList<Node> nodes = new ArrayList<Node>();

	public Graph() {
	}

	public void clearNodesTarget() {
		for (Node node : nodes)
			node.isTarget = false;
	}

	public void clearNodesMark() {
		for (Node node : nodes)
			node.mark = false;
	}

	// for Target you should set the isTarget flag true in target nodes
	public boolean isThereWay(ArrayList<Node> startNodes,
			ArrayList<Node> targets) {
		clearNodesTarget();
		for (Node node : targets)
			node.isTarget = true;

		return isThereWay(startNodes);
	}

	public boolean isThereWay(ArrayList<Node> startNodes) {
		ArrayList<Node> layer = new ArrayList<Node>();
		clearNodesMark();
		for (Node node : startNodes) {
			node.mark = true;
			layer.add(node);
		}

		while (layer.size() != 0) {
			ArrayList<Node> newLayer = new ArrayList<Node>();
			for (Node curNode : layer)
				for (Node neighbour : curNode.neighbours)
					if (neighbour.isTarget)
						return true;
					else if (!neighbour.mark) {
						neighbour.mark = true;
						newLayer.add(neighbour);
					}

			layer = newLayer;
		}

		return false;
	}
}
