package agent;

import java.util.ArrayList;

import rescuecore2.standard.entities.Human;

public class PoliceNode {
	public boolean mark = false;
	public Human stuckedAgent;
	public ArrayList<PoliceNode> neighbours = new ArrayList<PoliceNode>();

	public PoliceNode(Human h) {
		this.stuckedAgent = h;
	}

	public int getX() {
		return stuckedAgent.getX();
	}

	public int getY() {
		return stuckedAgent.getY();
	}

	public void mark() {
		this.mark = true;
	}

	public String toString() {
		return "" + stuckedAgent.getPosition().getValue();
	}
}
