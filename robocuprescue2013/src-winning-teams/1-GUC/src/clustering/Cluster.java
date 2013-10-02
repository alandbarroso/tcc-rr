package clustering;

import java.util.ArrayList;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class Cluster implements Comparable<Cluster> {

	// the list of targets in the cluster, it could be a list of buildings,
	// roads, or Humans
	public ArrayList<EntityID> cluster;
	//
	public double[] centroid;
	// the target whose center is the closest to the cluster's centroid
	public Area center;
	public double perc;
	// list of agents assigned to this cluster
	public ArrayList<EntityID> agents;

	public Cluster(ArrayList<EntityID> cluster, double[] centroid,
			StandardEntity center, double perc) {
		this.cluster = cluster;
		if (cluster == null)
			this.cluster = new ArrayList<EntityID>();
		this.centroid = centroid;
		this.center = (Area) center;
		if (centroid == null && center != null) {
			this.centroid = new double[2];
			this.centroid[0] = this.center.getX();
			this.centroid[1] = this.center.getY();
		}

		this.perc = perc;

		agents = new ArrayList<EntityID>();
	}

	@Override
	public int compareTo(Cluster c) {

		if (perc > c.perc)
			return 1;

		if (perc < c.perc)
			return -1;

		return 0;
	}

}