package clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import Think.BuildingInformation;
import Think.BuildingInformation.BUILDING_LIST;
import Think.Predicate;
import Think.Think;

public class FireCluster {
	static final int WARM_TEMPERATURE = 25;

	public static final Predicate<Building> predicateBuildingUnBurnt = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getFierynessEnum() == StandardEntityConstants.Fieryness.UNBURNT
					&& object.getTemperature() <= WARM_TEMPERATURE;
		}

	};
	public static final Predicate<Building> predicateBuildingOnFire = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getFierynessEnum() == StandardEntityConstants.Fieryness.BURNING
					|| object.getFierynessEnum() == StandardEntityConstants.Fieryness.HEATING
					|| object.getFierynessEnum() == StandardEntityConstants.Fieryness.INFERNO;

		}

	};
	public static final Predicate<Building> predicateBuildingWarm = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getTemperature() > WARM_TEMPERATURE
					&& !object.isOnFire()
					&& object.getFierynessEnum() != StandardEntityConstants.Fieryness.BURNT_OUT
					&& (!(object instanceof Refuge))
					&& (!(object instanceof AmbulanceCentre))
					&& (!(object instanceof FireStation))
					&& (!(object instanceof PoliceOffice));

		}

	};
	public static final Predicate<Building> predicateBuildingExtinguished = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return (object.getTemperature() <= WARM_TEMPERATURE && ((object
					.getFierynessEnum() == StandardEntityConstants.Fieryness.WATER_DAMAGE)
					|| (object.getFierynessEnum() == StandardEntityConstants.Fieryness.MINOR_DAMAGE)
					|| (object.getFierynessEnum() == StandardEntityConstants.Fieryness.MODERATE_DAMAGE) || (object
					.getFierynessEnum() == StandardEntityConstants.Fieryness.SEVERE_DAMAGE)));
		}

	};
	public static final Predicate<Building> predicateBuildingCollapsed = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getFierynessEnum() == StandardEntityConstants.Fieryness.BURNT_OUT;
		}

	};
	public static final Predicate<Building> predicateBuildingFireynessBurning = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getFierynessEnum() == StandardEntityConstants.Fieryness.BURNING;
		}

	};
	public static final Predicate<Building> predicateBuildingFireynessHeating = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getFierynessEnum() == StandardEntityConstants.Fieryness.HEATING;
		}

	};
	public static final Predicate<Building> predicateBuildingFireynessInferno = new Predicate<Building>() {

		@Override
		public boolean apply(Building object) {

			return object.getFierynessEnum() == StandardEntityConstants.Fieryness.INFERNO;
		}

	};

	// the list of targets in the cluster, it could be a list of buildings,
	// roads, or Humans

	//
	public double[] centroid;
	// the target whose center is the closest to the cluster's centroid
	public Area center;

	public ArrayList<BuildingInformation> allBuildings = new ArrayList<BuildingInformation>();
	public ArrayList<EntityID> buildingsUnburnt = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsWarm = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsOnFireBurning = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsOnFireHeating = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsOnFireInferno = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsExtinguished = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsCollapsed = new ArrayList<EntityID>();
	public ArrayList<EntityID> buildingsExpectedToBeOnFire = new ArrayList<EntityID>();
	public double radius;
	public Logger agentLogger;

	public FireCluster(ArrayList<EntityID> cluster, double[] centroid,
			StandardEntity center, double perc, Logger ag) {
		this.centroid = centroid;
		this.center = (Area) center;
		if (centroid == null) {
			this.centroid = new double[2];
			this.centroid[0] = this.center.getX();
			this.centroid[1] = this.center.getY();
		}
		agentLogger = ag;
		agentLogger.debug("new Cluster assgined center: " + center.getID());

		// System.out.println("new Cluster Created with center " +
		// center.getID());

	}

	public boolean isCloseToCluster(EntityID target, StandardWorldModel model) {
		return model.getDistance(center.getID(), target) < (radius + 100000);
	}

	public boolean addNewTarget(Building b, int fireyness, int temperature,
			int time, BUILDING_LIST type) {
		BuildingInformation bI = BuildingInformation.getBuildingInformation(
				b.getID(), allBuildings);
		if (bI != null && bI.time > time)
			return false;
		switch (type) {
		case BUILDINGS_WARM:
			return addWarmBuilding(bI, fireyness, temperature, b, time);

		case BUILDINGS_ON_FIRE:
			return addFireBuilding(bI, fireyness, temperature, b, time);

		case BUILDINGS_EXTINGUISHED:
			return addExtinuishedBuilding(bI, fireyness, temperature, b, time);
		case BUILDINGS_COLLAPSED:
			return addCollapsedBuilding(bI, fireyness, temperature, b, time);
		case BUILDINGS_UNBURNT:
			return addUnBurntBuilding(bI, fireyness, temperature, b, time);

		}
		return false;

	}

	public void updateRadius(StandardWorldModel model) {

		double biggest = 0;
		for (BuildingInformation bI : allBuildings) {
			if (bI.type == BUILDING_LIST.BUILDINGS_UNBURNT)
				continue;
			if (bI.type == BUILDING_LIST.BUILDINGS_COLLAPSED)
				continue;
			if (Think.getEuclidianDistance(bI.building.getX(),
					bI.building.getY(), centroid[0], centroid[1]) > biggest) {
				biggest = Think.getEuclidianDistance(bI.building.getX(),
						bI.building.getY(), centroid[0], centroid[1]);
			}
		}
		agentLogger.debug("new radius is " + radius);

		radius = biggest;
	}

	public void updateCentroid(StandardWorldModel model) {
		double x = 0, y = 0;
		for (BuildingInformation bI : allBuildings) {
			if (bI.type == BUILDING_LIST.BUILDINGS_UNBURNT)
				continue;
			if (bI.type == BUILDING_LIST.BUILDINGS_COLLAPSED)
				continue;
			x += bI.building.getX();
			y += bI.building.getY();
		}
		if (allBuildings.size() == 0)
			agentLogger.debug("cluster is empty");
		double[] point = {
				x
						/ (allBuildings.size() - buildingsUnburnt.size() - buildingsCollapsed
								.size()),
				y
						/ (allBuildings.size() - buildingsUnburnt.size() - buildingsCollapsed
								.size()) };
		if (point[0] == Double.NaN)
			point[0] = -1000;
		if (point[1] == Double.NaN)
			point[1] = -1000;
		centroid = point;
		Area c = (Area) getClosestEntity(model, allBuildings, centroid[0],
				centroid[1]);

		if (c != null) {
			center = c;
			agentLogger.debug("new center is " + c.getID() + ", x:" + c.getX()
					+ ", y:" + c.getY());
		}
		{
			agentLogger.debug("same center");
		}
	}

	public StandardEntity getClosestEntity(StandardWorldModel model,
			Collection<BuildingInformation> targets, double x, double y) {
		StandardEntity result = null;
		double shortestDistance = Double.POSITIVE_INFINITY, currentDistance = 0;

		for (BuildingInformation bI : targets) {
			double currentX, currentY;
			currentX = bI.building.getX();
			currentY = bI.building.getY();
			currentDistance = Math.sqrt(Math.pow(x - currentX, 2)
					+ Math.pow(y - currentY, 2));
			if (currentDistance < shortestDistance) {
				shortestDistance = currentDistance;
				result = bI.building;
			}
		}
		return result;
	}

	public boolean addWarmBuilding(BuildingInformation bI, int fireyness,
			int temperature, Building b, int time) {

		boolean report = false;
		if (bI != null) {
			switch (bI.type) {
			case BUILDINGS_UNBURNT: {
				Think.addIfNotExists(bI.building.getID(), buildingsWarm);

				Think.removeIfExists(bI.building.getID(), buildingsUnburnt);
			}
				break;

			case BUILDINGS_WARM: {
			}
				break;
			case BUILDINGS_ON_FIRE: {
				agentLogger.debug("time: " + time
						+ ", moved from fire to warm: " + b.getID());

				Think.addIfNotExists(bI.building.getID(), buildingsWarm);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireBurning);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireHeating);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireInferno);

				report = true;
			}
				break;
			case BUILDINGS_EXTINGUISHED: {

				agentLogger.debug("time: " + time
						+ ", moved from extinguished to warm: " + b.getID());

				Think.addIfNotExists(bI.building.getID(), buildingsWarm);

				Think.removeIfExists(bI.building.getID(), buildingsExtinguished);

				report = true;
			}
				break;
			case BUILDINGS_COLLAPSED: {

				// Think.addIfNotExists(bI.building.getID(), buildingsWarm);
				// Think.removeIfExists(bI.building.getID(),
				// buildingsCollapsed);
				return false;
			}
			// break;
			default:
				break;
			}
			bI.updateProperties(temperature, fireyness,
					BUILDING_LIST.BUILDINGS_WARM, time);
		} else {
			bI = new BuildingInformation(b, fireyness, temperature,
					BUILDING_LIST.BUILDINGS_WARM, time);
			agentLogger.debug("time: " + time + ", new Warm " + b.getID());
			report = true;
			buildingsWarm.add(bI.building.getID());
			allBuildings.add(bI);

		}
		return report;
	}

	public boolean addFireBuilding(BuildingInformation bI, int fireyness,
			int temperature, Building b, int time) {
		boolean report = false;
		if (bI != null) {
			switch (bI.type) {
			case BUILDINGS_UNBURNT: {
				switch (StandardEntityConstants.Fieryness.values()[fireyness]) {
				case BURNING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					;
					break;
				case HEATING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					break;
				case INFERNO: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireInferno);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);

				}
					break;
				}
				Think.removeIfExists(bI.building.getID(), buildingsUnburnt);
			}
				break;
			case BUILDINGS_WARM: {
				report = true;
				agentLogger.debug("time: " + time
						+ ", moved from warm to fire: " + b.getID());
				switch (StandardEntityConstants.Fieryness.values()[fireyness]) {
				case BURNING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					;
					break;
				case HEATING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					break;
				case INFERNO: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireInferno);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);

				}
					break;
				}
				Think.removeIfExists(bI.building.getID(), buildingsWarm);

			}
				break;
			case BUILDINGS_ON_FIRE: {
				// agentLogger.debug("time: " + time
				// + ", moved from fire to fire: " + b.getID());
				switch (StandardEntityConstants.Fieryness.values()[fireyness]) {
				case BURNING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					;
					break;
				case HEATING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					break;
				case INFERNO: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireInferno);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);

				}
					break;
				}
			}
				break;
			case BUILDINGS_EXTINGUISHED: {
				report = true;
				agentLogger.debug("time: " + time
						+ ", moved from extinguised to fire: " + b.getID());
				switch (StandardEntityConstants.Fieryness.values()[fireyness]) {
				case BURNING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					;
					break;
				case HEATING: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireInferno);

				}
					break;
				case INFERNO: {
					Think.addIfNotExists(bI.building.getID(),
							buildingsOnFireInferno);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireHeating);
					Think.removeIfExists(bI.building.getID(),
							buildingsOnFireBurning);

				}
					break;
				}
				Think.removeIfExists(bI.building.getID(), buildingsExtinguished);

			}
				break;
			case BUILDINGS_COLLAPSED: {
				// agentLogger.debug("time: " + time
				// + ", moved from collapsed to fire: " + b.getID());
				// Think.addIfNotExists(bI.building.getID(), buildingsOnFire);
				// Think.removeIfExists(bI.building.getID(),
				// buildingsCollapsed);
				return false;
			}
			// break;
			default:
				break;
			}
			if (bI.fireyness != fireyness)
				report = true;
			bI.updateProperties(temperature, fireyness,
					BUILDING_LIST.BUILDINGS_ON_FIRE, time);
		} else {
			bI = new BuildingInformation(b, fireyness, temperature,
					BUILDING_LIST.BUILDINGS_ON_FIRE, time);

			report = true;
			agentLogger.debug("time: " + time + ", new Fire " + b.getID());
			switch (StandardEntityConstants.Fieryness.values()[fireyness]) {
			case BURNING: {
				Think.addIfNotExists(bI.building.getID(),
						buildingsOnFireBurning);

			}
				;
				break;
			case HEATING: {
				Think.addIfNotExists(bI.building.getID(),
						buildingsOnFireHeating);

			}
				break;
			case INFERNO: {
				Think.addIfNotExists(bI.building.getID(),
						buildingsOnFireInferno);

			}
				break;
			}
			allBuildings.add(bI);

		}
		return report;
	}

	public boolean addUnBurntBuilding(BuildingInformation bI, int fireyness,
			int temperature, Building b, int time) {
		boolean report = false;
		if (bI != null) {
			switch (bI.type) {
			case BUILDINGS_WARM: {
				report = true;
				agentLogger.debug("time: " + time
						+ ", moved from warm to unburnt: " + b.getID());
				Think.addIfNotExists(bI.building.getID(), buildingsUnburnt);
				Think.removeIfExists(bI.building.getID(), buildingsWarm);
			}
				break;
			case BUILDINGS_ON_FIRE: {
				// agentLogger.debug("time: " + time
				// + ", moved from fire to fire: " + b.getID());

			}
				break;
			case BUILDINGS_EXTINGUISHED: {
				report = true;

			}
				break;
			case BUILDINGS_COLLAPSED: {
				// agentLogger.debug("time: " + time
				// + ", moved from collapsed to fire: " + b.getID());
				// Think.addIfNotExists(bI.building.getID(), buildingsOnFire);
				// Think.removeIfExists(bI.building.getID(),
				// buildingsCollapsed);
				return false;
			}
			// break;
			default:
				break;
			}
			bI.updateProperties(temperature, fireyness,
					BUILDING_LIST.BUILDINGS_UNBURNT, time);
		} else {
			bI = new BuildingInformation(b, fireyness, temperature,
					BUILDING_LIST.BUILDINGS_UNBURNT, time);

			report = true;
			agentLogger.debug("time: " + time + ", new Unburnt " + b.getID());
			Think.addIfNotExists(bI.building.getID(), buildingsUnburnt);
			allBuildings.add(bI);

		}
		return report;
	}

	public boolean addExtinuishedBuilding(BuildingInformation bI,
			int fireyness, int temperature, Building b, int time) {
		boolean report = true;
		if (bI != null) {
			switch (bI.type) {
			case BUILDINGS_UNBURNT: {
				Think.addIfNotExists(bI.building.getID(), buildingsExtinguished);

				Think.removeIfExists(bI.building.getID(), buildingsUnburnt);
			}
				break;
			case BUILDINGS_WARM: {
				report = true;
				agentLogger.debug("time: " + time
						+ ", moved from warm to extinguished: " + b.getID());

				Think.addIfNotExists(bI.building.getID(), buildingsExtinguished);

				Think.removeIfExists(bI.building.getID(), buildingsWarm);

			}
				break;
			case BUILDINGS_ON_FIRE: {
				report = true;

				agentLogger.debug("time: " + time
						+ ", moved from fire to extinguished: " + b.getID());
				Think.addIfNotExists(bI.building.getID(), buildingsExtinguished);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireBurning);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireHeating);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireInferno);
			}
				break;
			case BUILDINGS_EXTINGUISHED: {
				// agentLogger.debug("time: " + time
				// + ", moved from extinguished to extinguished: "
				// + b.getID());

			}
				break;
			case BUILDINGS_COLLAPSED: {
				// reportExtinguish(time, b.getID(), fireChannel);
				// agentLogger.debug("time: " + time
				// + ", moved from collapsed to extinguished: "
				// + b.getID());
				//
				// // Think.addIfNotExists(bI.building.getID(),
				// buildingsExtingiushed);
				// // Think.removeIfExists(bI.building.getID(),
				// buildingsCollapsed);
				return false;
			}
			// break;
			default:
				break;
			}
			bI.updateProperties(temperature, fireyness,
					BUILDING_LIST.BUILDINGS_EXTINGUISHED, time);
		} else {
			report = true;

			bI = new BuildingInformation(b, fireyness, temperature,
					BUILDING_LIST.BUILDINGS_EXTINGUISHED, time);

			Think.addIfNotExists(bI.building.getID(), buildingsExtinguished);
			agentLogger.debug("time: " + time + ", new Extinguished "
					+ b.getID());

			allBuildings.add(bI);
		}
		return report;
	}

	public boolean addCollapsedBuilding(BuildingInformation bI, int fireyness,
			int temperature, Building b, int time) {
		boolean report = false;
		if (bI != null) {
			switch (bI.type) {
			case BUILDINGS_UNBURNT: {
				agentLogger.debug("time: " + time
						+ ", moved from unburnt to collapsed: " + b.getID());

				Think.addIfNotExists(bI.building.getID(), buildingsCollapsed);

				Think.removeIfExists(bI.building.getID(), buildingsUnburnt);
			}
				break;
			case BUILDINGS_WARM: {
				agentLogger.debug("time: " + time
						+ ", moved from warm to collapsed: " + b.getID());
				report = true;
				Think.addIfNotExists(bI.building.getID(), buildingsCollapsed);

				Think.removeIfExists(bI.building.getID(), buildingsWarm);
			}
				break;
			case BUILDINGS_ON_FIRE: {
				agentLogger.debug("time: " + time
						+ ", moved from on fire to collapsed: " + b.getID());
				report = true;
				Think.addIfNotExists(bI.building.getID(), buildingsCollapsed);

				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireBurning);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireHeating);
				Think.removeIfExists(bI.building.getID(),
						buildingsOnFireInferno);
			}
				break;
			case BUILDINGS_EXTINGUISHED: {
				agentLogger
						.debug("time: " + time
								+ ", moved from extinguised to collapsed: "
								+ b.getID());
				report = true;
				Think.addIfNotExists(bI.building.getID(), buildingsCollapsed);
				Think.removeIfExists(bI.building.getID(), buildingsExtinguished);
			}
				break;
			case BUILDINGS_COLLAPSED: {
				// agentLogger.debug("time: " + time
				// + ", moved from collapsed to collapsed: " + b.getID());

			}
				break;
			default:
				break;
			}
			bI.updateProperties(temperature, fireyness,
					BUILDING_LIST.BUILDINGS_COLLAPSED, time);
		} else {
			report = true;
			bI = new BuildingInformation(b, fireyness, temperature,
					BUILDING_LIST.BUILDINGS_COLLAPSED, time);

			Think.addIfNotExists(bI.building.getID(), buildingsCollapsed);
			agentLogger.debug("time: " + time + ", new Collapsed " + b.getID());

			allBuildings.add(bI);
		}
		return report;
	}

	public void addExpectedFires(StandardWorldModel model,
			Human currentLocation, Building target) {
		agentLogger.debug("adding expected buildings on fire");

		agentLogger.debug(center.getID());
		agentLogger.debug(center.getX() + ", " + center.getY());
		agentLogger.debug(target.getX() + ", " + target.getY());
		agentLogger.debug(currentLocation.getX() + ", "
				+ currentLocation.getY());
		agentLogger.debug((2 * target.getX() - currentLocation.getX()) + ","
				+ (int) (2 * target.getY() - currentLocation.getY()));

		if (buildingsOnFireBurning.size() == 0
				&& buildingsOnFireInferno.size() == 0
				&& buildingsOnFireHeating.size() == 0
				&& buildingsWarm.size() == 0) {
			agentLogger.debug("fakes new fires");
			return;
		}
		for (StandardEntity e : model.getObjectsInRange(
				(int) (2 * target.getX() - currentLocation.getX()),
				(int) (2 * target.getY() - currentLocation.getY()), (int) 5000)) {

			if (e instanceof Building && !(e instanceof Refuge)
					&& !(e instanceof PoliceOffice)
					&& !(e instanceof AmbulanceCentre)
					&& !(e instanceof FireStation))
				if (!belongsToCluster(e.getID())
						&& Think.addIfNotExists(e.getID(),
								buildingsExpectedToBeOnFire)) {
				}
		}
	}

	public boolean belongsToCluster(EntityID target) {
		return Think.existsBuildingInformation(target, allBuildings);
	}

	public static FireCluster getClosestCluster(EntityID target,
			ArrayList<FireCluster> lists, StandardWorldModel model) {
		double smallestDistance = Double.POSITIVE_INFINITY;
		FireCluster closest = null;
		for (FireCluster list : lists) {
			if (list.belongsToCluster(target)) {
				closest = list;
				break;
			}
			if (model.getDistance(list.center.getID(), target) < smallestDistance) {
				closest = list;
				smallestDistance = model.getDistance(list.center.getID(),
						target);
			}
		}
		return closest;
	}

	public static FireCluster mergeClusters(FireCluster fc1, FireCluster fc2) {
		for (BuildingInformation bI : fc1.allBuildings) {

			if (Think.addIfNotExists(bI, fc2.allBuildings)) {
				Think.removeIfExists(bI.building.getID(), fc2.buildingsUnburnt);
				switch (bI.type) {
				case BUILDINGS_ON_FIRE: {
					switch (StandardEntityConstants.Fieryness.values()[bI.fireyness]) {
					case BURNING: {
						Think.addIfNotExists(bI.building.getID(),
								fc2.buildingsOnFireBurning);
					}
						;
						break;
					case HEATING: {
						Think.addIfNotExists(bI.building.getID(),
								fc2.buildingsOnFireHeating);

					}
						break;
					case INFERNO: {
						Think.addIfNotExists(bI.building.getID(),
								fc2.buildingsOnFireInferno);

					}
						break;
					}

				}
					break;
				case BUILDINGS_COLLAPSED: {
					Think.addIfNotExists(bI.building.getID(),
							fc2.buildingsCollapsed);
				}
					break;
				case BUILDINGS_WARM: {
					Think.addIfNotExists(bI.building.getID(), fc2.buildingsWarm);
				}
					break;
				case BUILDINGS_EXTINGUISHED: {
					Think.addIfNotExists(bI.building.getID(),
							fc2.buildingsExtinguished);
				}
					break;
				case BUILDINGS_UNBURNT: {
					Think.addIfNotExists(bI.building.getID(),
							fc2.buildingsUnburnt);
				}
					break;
				}

			}
		}
		return fc2;
	}

	public BuildingInformation closestTargetToLocation(final Area location,
			final StandardWorldModel model) {

		Collections.sort(allBuildings, new Comparator<BuildingInformation>() {

			@Override
			public int compare(BuildingInformation arg0,
					BuildingInformation arg1) {
				// TODO Auto-generated method stub
				if (model.getDistance(arg0.building, location) > model
						.getDistance(arg1.building, location))
					return 1;
				if (model.getDistance(arg0.building, location) < model
						.getDistance(arg1.building, location))
					return 1;
				return 0;
			}
		});
		for (BuildingInformation bI : allBuildings) {
			if (bI.type == BUILDING_LIST.BUILDINGS_UNBURNT)
				continue;
			if (bI.type == BUILDING_LIST.BUILDINGS_COLLAPSED)
				continue;
			if (bI.type == BUILDING_LIST.BUILDINGS_EXTINGUISHED)
				continue;

			return bI;
		}
		return null;
	}

	public void printCluster() {

		agentLogger.debug("----------FIRE CLUSTER---------------");

		if (buildingsWarm.size() != 0)
			agentLogger.debug("warm"
					+ Think.ArrayListEntityIDtoString(buildingsWarm));
		if (buildingsOnFireBurning.size() != 0)

			agentLogger.debug("fireBurning"
					+ Think.ArrayListEntityIDtoString(buildingsOnFireBurning));
		if (buildingsOnFireHeating.size() != 0)

			agentLogger.debug("fireHeating"
					+ Think.ArrayListEntityIDtoString(buildingsOnFireHeating));

		if (buildingsOnFireInferno.size() != 0)

			agentLogger.debug("fireInferno"
					+ Think.ArrayListEntityIDtoString(buildingsOnFireInferno));

		if (buildingsExtinguished.size() != 0)

			agentLogger.debug("extinguished"
					+ Think.ArrayListEntityIDtoString(buildingsExtinguished));
		if (buildingsCollapsed.size() != 0)

			agentLogger.debug("collapsed"
					+ Think.ArrayListEntityIDtoString(buildingsCollapsed));
		if (buildingsUnburnt.size() != 0)

			agentLogger.debug("unburnt"
					+ Think.ArrayListEntityIDtoString(buildingsUnburnt));
		if (buildingsExpectedToBeOnFire.size() != 0)

			agentLogger
					.debug("expected"
							+ Think.ArrayListEntityIDtoString(buildingsExpectedToBeOnFire));

		agentLogger.debug("-------------------------");

	}
}