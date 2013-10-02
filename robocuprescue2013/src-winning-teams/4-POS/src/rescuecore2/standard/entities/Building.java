package rescuecore2.standard.entities;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntProperty;
import rescuecore2.worldmodel.properties.BooleanProperty;

import geometry.Line;
import geometry.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

import agent.FireBrigadeAgent;

/**
 * The Building object.
 */
public class Building extends Area {
	/**
	 * Fieryness levels that indicate burning.
	 */
	public static final EnumSet<StandardEntityConstants.Fieryness> BURNING = EnumSet
			.of(StandardEntityConstants.Fieryness.HEATING,
					StandardEntityConstants.Fieryness.BURNING,
					StandardEntityConstants.Fieryness.INFERNO);

	private IntProperty floors;
	private BooleanProperty ignition;
	private IntProperty fieryness;
	private IntProperty brokenness;
	private IntProperty code;
	private IntProperty attributes;
	private IntProperty groundArea;
	private IntProperty totalArea;
	private IntProperty temperature;
	private IntProperty importance;
	// public ArrayList<Building> nearBuildings = new ArrayList<Building>();
	// public ArrayList<Building> notIgnitedNearBuildings = new
	// ArrayList<Building>();
	// public ArrayList<Building> nearByFireNeighbours = new
	// ArrayList<Building>();
	public HashMap<Road, ArrayList<Point>> visionRoads = null;
	// public ArrayList<Building> visionBuildings = new ArrayList<Building>();
	public HashMap<Building, Point> visionBuildings = new HashMap<Building, Point>();
	public int getLastFieryness = 0;
	public double value = Double.MAX_VALUE;
	public int simulatedValue = 0;
	public boolean haveZone = false;
	public int zoneNumber = -1;
	public int maxDistance = 0;
	public int timesSaid = -1;
	public int lastFireness = 0;
	public boolean IsShpide = false;
	public HashSet<Building> nearForSimulator = new HashSet<Building>();
	public double volume = 0;
	public int ownerForFire = 0;
	public ArrayList<Integer> owners = new ArrayList<Integer>();
	public int hamraheAval = 0;
	public StandardWorldModel model = null;
	public int MAX_DISTANCE_VIEW = 30000, maxDistanceExtingiush = 0;
	public boolean shouldSend = false;
	public int toBeAssigned = 3;
	public int assigned = 0;

	// From 2009
	public ArrayList<Integer> mitooneEzafeShe = new ArrayList<Integer>();

	// Sensetive firyness
	public int stFire = 0;
	public boolean isSimulatedForFire = false;
	public int counterForSimulate = 0;
	public int timeOfStartOfFire = -1;
	public boolean haveOwner = false;

	public boolean isNotIgnitable() {
		return (this instanceof Refuge || this instanceof PoliceOffice
				|| this instanceof FireStation || this instanceof AmbulanceCentre);
	}

	public boolean hasGoodVisionPoint() {
		if (this.getVisionRoads().size() != 0)
			return true;
		for (Building b : this.visionBuildings.keySet()) {
			if (b.stFire == 0 && b.worldGraphArea.isReachable)
				return true;
		}
		return false;
	}

	// Till here

	public HashMap<Road, ArrayList<Point>> getVisionRoads() {
		if (visionRoads != null)
			return visionRoads;
		visionRoads = new HashMap<Road, ArrayList<Point>>();
		for (Road road : nearRoads) {
			ArrayList<Point> visionPoints = FireBrigadeAgent.getVisionPoints(
					model, road, this, maxDistanceExtingiush,
					Math.min(MAX_DISTANCE_VIEW, maxDistanceExtingiush));
			if (visionPoints != null && visionPoints.size() != 0)
				visionRoads.put(road, visionPoints);
		}
		for (EntityID entityId : getNeighbours()) {
			Entity entity = model.getEntity(entityId);
			if (entity instanceof Road) {
				Road r = (Road) entity;
				if (!visionRoads.containsKey(r)) {
					ArrayList<Point> rPoint = new ArrayList<Point>();
					Point point = new Point(r.getX(), r.getY());
					if (Math.hypot(point.getX() - this.getX(), point.getY()
							- this.getY()) < maxDistanceExtingiush)
						rPoint.add(point);
					if (rPoint != null)
						visionRoads.put(r, rPoint);

				}
			}
		}
		return visionRoads;
	}

	/**
	 * Construct a Building object with entirely undefined property values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	public void fixOwners() {
		Collections.sort(owners);
		for (int i = hamraheAval; i < owners.size(); i++) {
			owners.remove(owners.get(i));
			i--;
		}
	}

	public Building(EntityID id) {
		super(id);
		floors = new IntProperty(StandardPropertyURN.FLOORS);
		ignition = new BooleanProperty(StandardPropertyURN.IGNITION);
		fieryness = new IntProperty(StandardPropertyURN.FIERYNESS);
		brokenness = new IntProperty(StandardPropertyURN.BROKENNESS);
		code = new IntProperty(StandardPropertyURN.BUILDING_CODE);
		attributes = new IntProperty(StandardPropertyURN.BUILDING_ATTRIBUTES);
		groundArea = new IntProperty(StandardPropertyURN.BUILDING_AREA_GROUND);
		totalArea = new IntProperty(StandardPropertyURN.BUILDING_AREA_TOTAL);
		temperature = new IntProperty(StandardPropertyURN.TEMPERATURE);
		importance = new IntProperty(StandardPropertyURN.IMPORTANCE);
		registerProperties(floors, ignition, fieryness, brokenness, code,
				attributes, groundArea, totalArea, temperature, importance);
	}

	/**
	 * Building copy constructor.
	 * 
	 * @param other
	 *            The Building to copy.
	 */

	public Building(Building other) {
		super(other);
		floors = new IntProperty(other.floors);
		ignition = new BooleanProperty(other.ignition);
		fieryness = new IntProperty(other.fieryness);
		brokenness = new IntProperty(other.brokenness);
		code = new IntProperty(other.code);
		attributes = new IntProperty(other.attributes);
		groundArea = new IntProperty(other.groundArea);
		totalArea = new IntProperty(other.totalArea);
		temperature = new IntProperty(other.temperature);
		importance = new IntProperty(other.importance);
		registerProperties(floors, ignition, fieryness, brokenness, code,
				attributes, groundArea, totalArea, temperature, importance);
	}

	@Override
	protected Entity copyImpl() {
		return new Building(getID());
	}

	@Override
	public StandardEntityURN getStandardURN() {
		return StandardEntityURN.BUILDING;
	}

	@Override
	protected String getEntityName() {
		return "Building";
	}

	@Override
	public Property getProperty(String urn) {
		StandardPropertyURN type;
		try {
			type = StandardPropertyURN.fromString(urn);
		} catch (IllegalArgumentException e) {
			return super.getProperty(urn);
		}
		switch (type) {
		case FLOORS:
			return floors;
		case IGNITION:
			return ignition;
		case FIERYNESS:
			return fieryness;
		case BROKENNESS:
			return brokenness;
		case BUILDING_CODE:
			return code;
		case BUILDING_ATTRIBUTES:
			return attributes;
		case BUILDING_AREA_GROUND:
			return groundArea;
		case BUILDING_AREA_TOTAL:
			return totalArea;
		case TEMPERATURE:
			return temperature;
		case IMPORTANCE:
			return importance;
		default:
			return super.getProperty(urn);
		}
	}

	/**
	 * Get the floors property.
	 * 
	 * @return The floors property.
	 */
	public IntProperty getFloorsProperty() {
		return floors;
	}

	/**
	 * Get the number of floors in this building.
	 * 
	 * @return The number of floors.
	 */
	public int getFloors() {
		return floors.getValue();
	}

	/**
	 * Set the number of floors in this building.
	 * 
	 * @param floors
	 *            The new number of floors.
	 */
	public void setFloors(int floors) {
		this.floors.setValue(floors);
	}

	/**
	 * Find out if the floors property has been defined.
	 * 
	 * @return True if the floors property has been defined, false otherwise.
	 */
	public boolean isFloorsDefined() {
		return floors.isDefined();
	}

	/**
	 * Undefine the floors property.
	 */
	public void undefineFloors() {
		floors.undefine();
	}

	/**
	 * Get the ignition property.
	 * 
	 * @return The ignition property.
	 */
	public BooleanProperty getIgnitionProperty() {
		return ignition;
	}

	/**
	 * Get the value of the ignition property.
	 * 
	 * @return The ignition property value.
	 */
	public boolean getIgnition() {
		return ignition.getValue();
	}

	/**
	 * Set the ignition property.
	 * 
	 * @param ignition
	 *            The new ignition value.
	 */
	public void setIgnition(boolean ignition) {
		this.ignition.setValue(ignition);
	}

	/**
	 * Find out if the ignition property has been defined.
	 * 
	 * @return True if the ignition property has been defined, false otherwise.
	 */
	public boolean isIgnitionDefined() {
		return ignition.isDefined();
	}

	/**
	 * Undefine the ingition property.
	 */
	public void undefineIgnition() {
		ignition.undefine();
	}

	/**
	 * Get the fieryness property.
	 * 
	 * @return The fieryness property.
	 */
	public IntProperty getFierynessProperty() {
		return fieryness;
	}

	/**
	 * Get the fieryness of this building.
	 * 
	 * @return The fieryness property value.
	 */
	public int getFieryness() {
		return fieryness.getValue();
	}

	/**
	 * Get the fieryness of this building as an enum constant. If fieryness is
	 * not defined then return null.
	 * 
	 * @return The fieryness property value as a Fieryness enum, or null if
	 *         fieryness is undefined.
	 */
	public StandardEntityConstants.Fieryness getFierynessEnum() {
		if (!fieryness.isDefined()) {
			return null;
		}
		return StandardEntityConstants.Fieryness.values()[fieryness.getValue()];
	}

	/**
	 * Set the fieryness of this building.
	 * 
	 * @param fieryness
	 *            The new fieryness value.
	 */
	public void setFieryness(int fieryness) {
		this.fieryness.setValue(fieryness);
	}

	/**
	 * Find out if the fieryness property has been defined.
	 * 
	 * @return True if the fieryness property has been defined, false otherwise.
	 */
	public boolean isFierynessDefined() {
		return fieryness.isDefined();
	}

	/**
	 * Undefine the fieryness property.
	 */
	public void undefineFieryness() {
		fieryness.undefine();
	}

	/**
	 * Get the brokenness property.
	 * 
	 * @return The brokenness property.
	 */
	public IntProperty getBrokennessProperty() {
		return brokenness;
	}

	/**
	 * Get the brokenness of this building.
	 * 
	 * @return The brokenness value.
	 */
	public int getBrokenness() {
		return brokenness.getValue();
	}

	/**
	 * Set the brokenness of this building.
	 * 
	 * @param brokenness
	 *            The new brokenness.
	 */
	public void setBrokenness(int brokenness) {
		this.brokenness.setValue(brokenness);
	}

	/**
	 * Find out if the brokenness property has been defined.
	 * 
	 * @return True if the brokenness property has been defined, false
	 *         otherwise.
	 */
	public boolean isBrokennessDefined() {
		return brokenness.isDefined();
	}

	/**
	 * Undefine the brokenness property.
	 */
	public void undefineBrokenness() {
		brokenness.undefine();
	}

	/**
	 * Get the building code property.
	 * 
	 * @return The building code property.
	 */
	public IntProperty getBuildingCodeProperty() {
		return code;
	}

	/**
	 * Get the building code of this building.
	 * 
	 * @return The building code.
	 */
	public int getBuildingCode() {
		return code.getValue();
	}

	/**
	 * Get the building code of this building as an enum constant. If building
	 * code is not defined then return null.
	 * 
	 * @return The building code property value as a BuildingCode enum, or null
	 *         if building code is undefined.
	 */
	public StandardEntityConstants.BuildingCode getBuildingCodeEnum() {
		if (!code.isDefined()) {
			return null;
		}
		return StandardEntityConstants.BuildingCode.values()[code.getValue()];
	}

	/**
	 * Set the building code of this building.
	 * 
	 * @param newCode
	 *            The new building code.
	 */
	public void setBuildingCode(int newCode) {
		this.code.setValue(newCode);
	}

	/**
	 * Find out if the building code has been defined.
	 * 
	 * @return True if the building code has been defined, false otherwise.
	 */
	public boolean isBuildingCodeDefined() {
		return code.isDefined();
	}

	/**
	 * Undefine the building code.
	 */
	public void undefineBuildingCode() {
		code.undefine();
	}

	/**
	 * Get the building attributes property.
	 * 
	 * @return The building attributes property.
	 */
	public IntProperty getBuildingAttributesProperty() {
		return attributes;
	}

	/**
	 * Get the building attributes of this building.
	 * 
	 * @return The building attributes.
	 */
	public int getBuildingAttributes() {
		return attributes.getValue();
	}

	/**
	 * Set the building attributes of this building.
	 * 
	 * @param newAttributes
	 *            The new building attributes.
	 */
	public void setBuildingAttributes(int newAttributes) {
		this.attributes.setValue(newAttributes);
	}

	/**
	 * Find out if the building attributes property has been defined.
	 * 
	 * @return True if the building attributes property has been defined, false
	 *         otherwise.
	 */
	public boolean isBuildingAttributesDefined() {
		return attributes.isDefined();
	}

	/**
	 * Undefine the building attributes.
	 */
	public void undefineBuildingAttributes() {
		attributes.undefine();
	}

	/**
	 * Get the ground area property.
	 * 
	 * @return The ground area property.
	 */
	public IntProperty getGroundAreaProperty() {
		return groundArea;
	}

	/**
	 * Get the ground area of this building.
	 * 
	 * @return The ground area.
	 */
	public int getGroundArea() {
		return groundArea.getValue();
	}

	/**
	 * Set the ground area of this building.
	 * 
	 * @param ground
	 *            The new ground area.
	 */
	public void setGroundArea(int ground) {
		this.groundArea.setValue(ground);
	}

	/**
	 * Find out if the ground area property has been defined.
	 * 
	 * @return True if the ground area property has been defined, false
	 *         otherwise.
	 */
	public boolean isGroundAreaDefined() {
		return groundArea.isDefined();
	}

	/**
	 * Undefine the ground area.
	 */
	public void undefineGroundArea() {
		groundArea.undefine();
	}

	/**
	 * Get the total area property.
	 * 
	 * @return The total area property.
	 */
	public IntProperty getTotalAreaProperty() {
		return totalArea;
	}

	/**
	 * Get the total area of this building.
	 * 
	 * @return The total area.
	 */
	public int getTotalArea() {
		return totalArea.getValue();
	}

	/**
	 * Set the total area of this building.
	 * 
	 * @param total
	 *            The new total area.
	 */
	public void setTotalArea(int total) {
		this.totalArea.setValue(total);
	}

	/**
	 * Find out if the total area property has been defined.
	 * 
	 * @return True if the total area property has been defined, false
	 *         otherwise.
	 */
	public boolean isTotalAreaDefined() {
		return totalArea.isDefined();
	}

	/**
	 * Undefine the total area property.
	 */
	public void undefineTotalArea() {
		totalArea.undefine();
	}

	/**
	 * Get the temperature property.
	 * 
	 * @return The temperature property.
	 */
	public IntProperty getTemperatureProperty() {
		return temperature;
	}

	/**
	 * Get the temperature of this building.
	 * 
	 * @return The temperature.
	 */
	public int getTemperature() {
		return temperature.getValue();
	}

	/**
	 * Set the temperature of this building.
	 * 
	 * @param temperature
	 *            The new temperature.
	 */
	public void setTemperature(int temperature) {
		this.temperature.setValue(temperature);
	}

	/**
	 * Find out if the temperature property has been defined.
	 * 
	 * @return True if the temperature property has been defined, false
	 *         otherwise.
	 */
	public boolean isTemperatureDefined() {
		return temperature.isDefined();
	}

	/**
	 * Undefine the temperature property.
	 */
	public void undefineTemperature() {
		temperature.undefine();
	}

	/**
	 * Get the building importance property.
	 * 
	 * @return The importance property.
	 */
	public IntProperty getImportanceProperty() {
		return importance;
	}

	/**
	 * Get the importance of this building.
	 * 
	 * @return The importance.
	 */
	public int getImportance() {
		return importance.getValue();
	}

	/**
	 * Set the importance of this building.
	 * 
	 * @param importance
	 *            The new importance.
	 */
	public void setImportance(int importance) {
		this.importance.setValue(importance);
	}

	/**
	 * Find out if the importance property has been defined.
	 * 
	 * @return True if the importance property has been defined, false
	 *         otherwise.
	 */
	public boolean isImportanceDefined() {
		return importance.isDefined();
	}

	/**
	 * Undefine the importance property.
	 */
	public void undefineImportance() {
		importance.undefine();
	}

	/**
	 * Find out if this building is on fire.
	 * 
	 * @return True iff this buildings fieryness indicates that it is burning.
	 */
	public boolean isOnFire() {
		if (!fieryness.isDefined()) {
			return false;
		}
		return BURNING.contains(getFierynessEnum());
		// if (stFire > 0)
		// return true;
		// else
		// return false;
	}

	public boolean hasIntersectWithBuilding(Line line) {
		ArrayList<Point> points = worldGraphArea.points;
		for (int i = 0; i < points.size() - 1; i++)
			if (getEdges().get(i).getNeighbour() == null
					&& Line.isIntersectBetweenLines(points.get(i),
							points.get(i + 1), line.getFirstPoint(),
							line.getSecondPoint()))
				return true;

		return false;
	}
}
