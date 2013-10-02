package Think;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.worldmodel.EntityID;

public class BuildingInformation {
	public int fireyness, temperature, time, clusterIndex;
	public Building building;
	public BUILDING_LIST type;

	public static enum BUILDING_LIST {
		BUILDINGS_WARM, BUILDINGS_ON_FIRE, BUILDINGS_EXTINGUISHED, BUILDINGS_COLLAPSED, BUILDINGS_UNBURNT
	};

	public BuildingInformation(Building b, int f, int t, BUILDING_LIST type,
			int time) {
		fireyness = f;
		temperature = t;
		this.time = time;
		this.type = type;
		this.building = b;

	}

	public BuildingInformation(Building b, int f, int t, int time) {
		fireyness = f;
		temperature = t;
		this.time = time;
		this.building = b;

	}

	public BuildingInformation(Building b, int f, int time) {
		fireyness = f;
		this.time = time;
		this.building = b;

	}

	public BuildingInformation(Building b, int f, int temp, int time,
			int clusterIndex) {
		fireyness = f;
		this.time = time;
		this.building = b;
		this.temperature = temp;
		this.clusterIndex = clusterIndex;

	}

	public void setClusterIndex(int clusterIndex) {
		this.clusterIndex = clusterIndex;
	}

	public void setBuilding(Building b) {
		this.building = b;
	}

	public boolean updateProperties(int t, int f, BUILDING_LIST type, int time) {
		if (this.time > time)
			return false;
		fireyness = f;
		temperature = t;
		this.type = type;
		this.time = time;
		return true;
	}

	public boolean updateFireyness(int f, int time) {
		if (this.time > time)
			return false;

		fireyness = f;
		this.time = time;
		return true;
	}

	public boolean updateTemperature(int t, int time) {
		if (this.time > time)
			return false;

		temperature = t;
		this.time = time;
		return true;
	}

	public static BuildingInformation getBuildingInformation(EntityID target,
			List<BuildingInformation> list) {
		for (BuildingInformation b : list) {
			if (b.building.getID().getValue() == target.getValue())
				return b;
		}
		return null;
	}

	public static ArrayList<BuildingInformation> filterFireynessHEATINGorBURNING(
			ArrayList<BuildingInformation> list) {
		return (ArrayList<BuildingInformation>) Think.filter(list,
				new Predicate<BuildingInformation>() {

					@Override
					public boolean apply(BuildingInformation object) {
						// TODO Auto-generated method stub
						return object.fireyness == StandardEntityConstants.Fieryness.HEATING
								.ordinal()
								&& object.fireyness == StandardEntityConstants.Fieryness.BURNING
										.ordinal();
					}
				});
	}

	public String toString() {
		return "" + this.fireyness;
	}
}
