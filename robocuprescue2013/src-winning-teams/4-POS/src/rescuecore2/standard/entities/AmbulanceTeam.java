package rescuecore2.standard.entities;

import java.util.HashMap;

import agent.LittleZone;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * The AmbulanceTeam object.
 */
public class AmbulanceTeam extends Human {
	public int target = -1;
	public Human aim = null;
	public LittleZone myLastSearchZone = null;
	public Human PgAim = null;
	public Human lastSetAim = null;
	public int setAimTime = 0;
	public boolean isReachable = true;
	public boolean feedback = true;
	public boolean isMozakhraf = false;
	public boolean isFree = true;
	public HashMap<AmbulanceTeam, Integer> assignIndex = new HashMap<AmbulanceTeam, Integer>();

	/**
	 * Construct a AmbulanceTeam object with entirely undefined values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	public AmbulanceTeam(EntityID id) {
		super(id);
	}

	/**
	 * AmbulanceTeam copy constructor.
	 * 
	 * @param other
	 *            The AmbulanceTeam to copy.
	 */
	public AmbulanceTeam(AmbulanceTeam other) {
		super(other);
	}

	@Override
	protected Entity copyImpl() {
		return new AmbulanceTeam(getID());
	}

	@Override
	public StandardEntityURN getStandardURN() {
		return StandardEntityURN.AMBULANCE_TEAM;
	}

	@Override
	protected String getEntityName() {
		return "Ambulance team";
	}
}
