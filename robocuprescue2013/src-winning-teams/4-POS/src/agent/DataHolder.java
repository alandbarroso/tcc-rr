package agent;

import java.io.Serializable;
import java.util.ArrayList;

class DataHolder implements Serializable {
	private static final long serialVersionUID = 2446185739797663854L;

	public ArrayList<Integer> nearRoads = new ArrayList<Integer>();
	public ArrayList<Integer> nearBuildings = new ArrayList<Integer>();
	public ArrayList<Integer> nearAreas50000 = new ArrayList<Integer>();
}
