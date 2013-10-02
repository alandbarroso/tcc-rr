package graph;

import java.util.ArrayList;

import worldGraph.Enterance;

import geometry.Point;

public class Node {
	private int id = -1;
	private Point point = null;
	public ArrayList<Node> neighbours = new ArrayList<Node>();
	public boolean mark = false;
	public boolean isTarget = false;
	public Enterance enterance = null;

	public Node() {
	}

	public Node(int id, Point point) {
		this.id = id;
		this.point = point;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}
}
