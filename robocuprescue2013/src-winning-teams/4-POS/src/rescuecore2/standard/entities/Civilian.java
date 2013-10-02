package rescuecore2.standard.entities;

import geometry.Mathematic;

import java.util.HashSet;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * The Civilian object.
 */
public class Civilian extends Human {
	public EntityID nearestRefuge = null;
	public int counterForSay = 0;
	public int deadCounter = 0;

	public int compareTo(Civilian other) {
		if (deadtime > other.deadtime)
			return 1;
		else if (deadtime < other.deadtime)
			return -1;
		return 0;
	}

	public boolean isDeadTimeDefined() {
		if (isDamageDefined() && isBuriednessDefined() && isHPDefined())
			return true;
		return false;
	}

	public void setNearestRefuge(HashSet<Refuge> refuges, int x, int y) {
		nearestRefuge = null;
		if (!(refuges.size() == 0)) {
			double minDist = Integer.MAX_VALUE;
			Refuge nearerRefuge = null;
			for (Refuge refuge : refuges) {
				double d = Mathematic.getDistance(x, y, refuge.getX(),
						refuge.getY());
				if (d < minDist) {
					minDist = d;
					nearerRefuge = refuge;
				}
			}
			nearestRefuge = nearerRefuge.getID();
		}
	}

	/**
	 * Construct a Civilian object with entirely undefined values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	public Civilian(EntityID id) {
		super(id);
	}

	/**
	 * Civilian copy constructor.
	 * 
	 * @param other
	 *            The Civilian to copy.
	 */
	public Civilian(Civilian other) {
		super(other);
	}

	@Override
	protected Entity copyImpl() {
		return new Civilian(getID());
	}

	@Override
	public StandardEntityURN getStandardURN() {
		return StandardEntityURN.CIVILIAN;
	}

	@Override
	protected String getEntityName() {
		return "Civilian";
	}
}
