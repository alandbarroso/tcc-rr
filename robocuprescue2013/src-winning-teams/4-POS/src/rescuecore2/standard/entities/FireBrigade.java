package rescuecore2.standard.entities;

import geometry.Mathematic;

import java.util.ArrayList;
import java.util.HashSet;

import agent.FireZone;
import agent.LittleZone;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntProperty;

/**
 * The FireBrigade object.
 */
public class FireBrigade extends Human {
	private IntProperty water;
	public boolean fireTrueOrFalse = false;
	public Building myZoneBuil = null;
	public LittleZone myLastSearchZone = null;
	public ArrayList<Double> distToFire = new ArrayList<Double>();
	public ArrayList<Building> unreachableAndIgnitedBuildings = new ArrayList<Building>();
	public Building aim=null;
	public int maxDistanceExtingiush = -1;
	public boolean IamOnfire = false;
	public FireZone myZone=null;
	public int fgNum = -1;
	public int Water = 1;
	public int timeOfSendingWater = -1;
	public HashSet<FireZone> zonesIcanExt = new HashSet<FireZone>();
	public boolean canExtinguish(Building target) {
		return Mathematic.getDistance(target.getX(), target.getY(),
				getX(), getY()) < maxDistanceExtingiush;
	}

	/**
	 * Construct a FireBrigade object with entirely undefined values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	public FireBrigade(EntityID id) {
		super(id);
		water = new IntProperty(StandardPropertyURN.WATER_QUANTITY);
		registerProperties(water);
	}

	/**
	 * FireBrigade copy constructor.
	 * 
	 * @param other
	 *            The FireBrigade to copy.
	 */
	public FireBrigade(FireBrigade other) {
		super(other);
		water = new IntProperty(other.water);
		registerProperties(water);
	}

	@Override
	protected Entity copyImpl() {
		return new FireBrigade(getID());
	}

	@Override
	public StandardEntityURN getStandardURN() {
		return StandardEntityURN.FIRE_BRIGADE;
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
		case WATER_QUANTITY:
			return water;
		default:
			return super.getProperty(urn);
		}
	}

	/**
	 * Get the water property.
	 * 
	 * @return The water property.
	 */
	public IntProperty getWaterProperty() {
		return water;
	}

	/**
	 * Get the amount of water this fire brigade is carrying.
	 * 
	 * @return The water.
	 */
	public int getWater() {
		return water.getValue();
	}

	/**
	 * Set the amount of water this fire brigade is carrying.
	 * 
	 * @param water
	 *            The new amount of water.
	 */
	public void setWater(int water) {
		this.water.setValue(water);
	}

	/**
	 * Find out if the water property has been defined.
	 * 
	 * @return True if the water property has been defined, false otherwise.
	 */
	public boolean isWaterDefined() {
		return water.isDefined();
	}

	/**
	 * Undefine the water property.
	 */
	public void undefineWater() {
		water.undefine();
	}

	@Override
	protected String getEntityName() {
		return "Fire brigade";
	}
}
