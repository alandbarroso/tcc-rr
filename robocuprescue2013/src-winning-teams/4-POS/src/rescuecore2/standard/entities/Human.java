package rescuecore2.standard.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.properties.EntityRefProperty;
import rescuecore2.worldmodel.properties.IntArrayProperty;
import rescuecore2.worldmodel.properties.IntProperty;
import worldGraph.Enterance;
import worldGraph.WorldGraph;
import agent.GroupForPolice;

/**
 * Abstract base class for Humans.
 */
public abstract class Human extends StandardEntity implements Comparable<Human> {
	private IntProperty x;
	private IntProperty y;
	private EntityRefProperty position;
	private IntArrayProperty positionHistory;
	private IntProperty travelDistance;
	private IntProperty direction;
	private IntProperty stamina;
	private IntProperty hp;
	private IntProperty damage;
	private IntProperty buriedness;
	public ArrayList<Integer> Owners = new ArrayList<Integer>();
	public ArrayList<Integer> newOwners = new ArrayList<Integer>();
	public ArrayList<AmbulanceTeam> PgOwners = new ArrayList<AmbulanceTeam>();
	public int owner = -1;
	public int hisZone = -1;
	public int irancell = 4;
	public int PgPointAg = 0;
	public int PgpointCv = 0;
	public int pogNum = -1;
	public boolean ge = false;
	public boolean hasBuriedness = false;
	public boolean isAlive = false;
	public boolean mark = false;
	public GroupForPolice lastStuckGroup = null;
	public int lastHeardPosFrom = -1;

	// for ambulance State...
	public HashMap<Area, Integer> distance = new HashMap<Area, Integer>();

	/**
	 * Construct a Human object with entirely undefined property values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	protected Human(EntityID id) {
		super(id);
		x = new IntProperty(StandardPropertyURN.X);
		y = new IntProperty(StandardPropertyURN.Y);
		travelDistance = new IntProperty(StandardPropertyURN.TRAVEL_DISTANCE);
		position = new EntityRefProperty(StandardPropertyURN.POSITION);
		positionHistory = new IntArrayProperty(
				StandardPropertyURN.POSITION_HISTORY);
		direction = new IntProperty(StandardPropertyURN.DIRECTION);
		stamina = new IntProperty(StandardPropertyURN.STAMINA);
		hp = new IntProperty(StandardPropertyURN.HP);
		damage = new IntProperty(StandardPropertyURN.DAMAGE);
		buriedness = new IntProperty(StandardPropertyURN.BURIEDNESS);
		registerProperties(x, y, position, positionHistory, travelDistance,
				direction, stamina, hp, damage, buriedness);
	}

	/**
	 * Human copy constructor.
	 * 
	 * @param other
	 *            The Human to copy.
	 */
	public Human(Human other) {
		super(other);
		pgDT();
		x = new IntProperty(other.x);
		y = new IntProperty(other.y);
		travelDistance = new IntProperty(other.travelDistance);
		position = new EntityRefProperty(other.position);
		positionHistory = new IntArrayProperty(other.positionHistory);
		direction = new IntProperty(other.direction);
		stamina = new IntProperty(other.stamina);
		hp = new IntProperty(other.hp);
		damage = new IntProperty(other.damage);
		buriedness = new IntProperty(other.buriedness);
		registerProperties(x, y, position, positionHistory, travelDistance,
				direction, stamina, hp, damage, buriedness);
	}

	public int compareTo(Human other) {
		if (deadtime > other.deadtime)
			return 1;
		else if (deadtime < other.deadtime)
			return -1;
		return 0;
	}

	public int deadtime = 502;
	float hp1 = 0, b = 0, d = 0, k = 0.0258f;
	public int lastDeadTimeSaid = -1;
	// parand:code khodemun
	// public void setDeadtime(int time) {
	// deadtime = time;
	// if (isDamageDefined() && isHPDefined()) {
	// if (getDamage() != 0) {
	// hp1 = getHP();
	// d = getDamage();
	// while (hp1 > 0) {
	// d += ((0.000035 * d * d) + 0.11);
	// hp1 -= d;
	// deadtime++;
	// }
	// } else
	// deadtime = 502;
	// }
	// if (deadtime < time || (isHPDefined() && getHP() < 50))
	// deadtime = 500;
	// }
	// //parand:code dozD:
	// public static int getEasyLifeTime_OLD(int hp, int dmg, int time) {
	// if (dmg <= 0)
	// return Integer.MAX_VALUE;
	// if (hp <= 0)
	// return 0;
	// double alpha = 0;
	// double newAlpha = 0.01;
	// while (java.lang.Math.abs(alpha - newAlpha) > 1E-10) {
	//
	// alpha = newAlpha;
	// double tmp = java.lang.Math.exp(-alpha * time);
	// newAlpha = ((alpha * time + 1) * tmp - 1) /
	// (time * tmp - (double) (10000 - hp) / dmg);
	// }
	//
	// if (alpha > 0)
	// return (int) (java.lang.Math.ceil((7.0 / 8) *
	// java.lang.Math.log(alpha * hp / dmg + 1) / alpha));
	// else
	// return hp / dmg;
	// }
	// /**
	// * a simple method to find life time added by Aramik
	// *
	// * @param hp
	// * @param dmg
	// * @param time
	// * @return
	// */
	// public static int getEasyLifeTime(int hp, int dmg, int time) {
	// return estimatedDeathTime(hp, dmg, time);
	// // return getEasyLifeTime_OLD(hp, dmg, time);
	// }
	// public static int estimatedDeathTime(int hp, double dmg,int updatetime) {
	// int agenttime=1000;
	// int count = agenttime - updatetime;
	// if (count <= 0 || dmg == 0)
	// return hp;
	//
	// double kbury = 0.000035;
	// double kcollapse = 0.00025;
	// double darsadbury = -0.0014 * updatetime + 0.64;
	// double burydamage = dmg * darsadbury;
	// double collapsedamage = dmg - burydamage;
	//
	// while (count > 0) {
	// int time = agenttime - count;
	// //
	// System.out.print("cycle:"+time+" bury:"+burydamage+" collapse:"+collapsedamage+" dmg:"+dmg);
	// burydamage += kbury * burydamage * burydamage + 0.11 ;
	// collapsedamage += kcollapse * collapsedamage * collapsedamage+0.11 ;
	// dmg=burydamage+collapsedamage;
	// count--;
	// hp -= dmg;
	// // System.out.print("cycle:"+time+" darsad:"+darsadbury);
	// // System.out.println(" hp:"+hp+" damge:"+dmg);
	// if (hp <= 0)
	// return time;
	// }
	// return 1000;
	// }
	// public static int getEstimatedDamage(int hp, int time) {
	// return (10000 - hp) / time;
	// }
	// //end of code dozD

	// code e dozD 2
	private int m_dmg_ob;
	private int m_hp_ob;
	private int m_bury;
	private int m_lastUpdate;
	private double[][] m_particles;
	private int m_particles_time;
	public boolean m_particlesNeedResample = false;
	public int[] m_deadTime;
	//
	private boolean m_propertyChanged;
	private int m_time_needRefresh;
	// aramik
	public static int HP_PRECISION;
	public static int DAMAGE_PRECISION;

	public void pgDT() {
		m_dmg_ob = 0;
		m_bury = -1;
		m_hp_ob = 10000;
		m_lastUpdate = -1;
		m_particles = null;
		m_particles_time = 0;
		m_deadTime = new int[60];
		m_propertyChanged = false;
		m_time_needRefresh = 50;

	}

	public int getDeadTime() {
		int dt;
		if (m_bury <= 5)
			dt = 5;
		else if (m_bury >= 55)
			dt = 55;
		else
			dt = m_bury;

		return m_deadTime[dt];
	}

	public void setDmg(int dmg, int time) {
		if (dmg != m_dmg_ob)
			m_propertyChanged = true;
		m_dmg_ob = dmg;
		m_lastUpdate = time;
		this.damage.setValue(dmg);
	}

	public void setHp(int hp, int time) {
		if (hp != m_hp_ob)
			m_propertyChanged = true;
		m_hp_ob = hp;
		m_lastUpdate = time;
		this.hp.setValue(hp);
	}

	public void setBury(int bury) {
		if (m_bury == -1)
			m_bury = bury;
		this.buriedness.setValue(bury);
	}

	private double[][] initTempParticles() {
		double[][] result = new double[60 * DAMAGE_PRECISION][3];
		double[] hpTable = new double[6];
		hpTable[0] = m_hp_ob - ((HP_PRECISION / 2) - 1);
		hpTable[1] = m_hp_ob - (((HP_PRECISION * 3) / 10) - 1);
		hpTable[2] = m_hp_ob - (HP_PRECISION / 5 - 1);
		hpTable[3] = m_hp_ob + (HP_PRECISION / 5 + 1);
		hpTable[4] = m_hp_ob + (((HP_PRECISION * 3) / 10) + 1);
		hpTable[5] = m_hp_ob + ((HP_PRECISION / 2) - 1);

		double[] dmgTable = new double[DAMAGE_PRECISION];
		double step = 9.98 / 9;
		for (int i = 0; i < DAMAGE_PRECISION; i++) {
			dmgTable[i] = (m_dmg_ob) - ((double) DAMAGE_PRECISION / 2 - 0.01)
					+ step * i;
		}
		double[] brokenRateTable = new double[10];
		for (int i = 0; i < 10; i++)
			brokenRateTable[i] = 5 + 10 * i;

		for (int i = 0; i < 60 * DAMAGE_PRECISION; i++) {
			int a = i / (10 * DAMAGE_PRECISION);
			int b = (i % (10 * DAMAGE_PRECISION)) / DAMAGE_PRECISION;
			int c = i % 10;
			m_particles[i][0] = hpTable[a];
			if (m_dmg_ob == 0)
				m_particles[i][1] = DAMAGE_PRECISION / 3;
			else
				m_particles[i][1] = dmgTable[b];
			if (m_bury == 0)
				m_particles[i][2] = 100.0;
			else
				m_particles[i][2] = brokenRateTable[c];
		}

		return result;
	}

	private void initParticles() {
		m_propertyChanged = false;
		m_time_needRefresh = m_lastUpdate;
		m_time_needRefresh += (int) (Math.random() * 20);

		m_time_needRefresh += 30;

		m_particles = new double[60 * DAMAGE_PRECISION][3];
		m_particles_time = m_lastUpdate;

		double[] hpTable = new double[6];
		if (m_hp_ob % HP_PRECISION == 0) {
			hpTable[0] = m_hp_ob - ((HP_PRECISION / 2) - 1);
			hpTable[1] = m_hp_ob - (((HP_PRECISION * 3) / 10) - 1);
			hpTable[2] = m_hp_ob - (HP_PRECISION / 5 - 1);
			hpTable[3] = m_hp_ob + (HP_PRECISION / 5 + 1);
			hpTable[4] = m_hp_ob + (((HP_PRECISION * 3) / 10) + 1);
			hpTable[5] = m_hp_ob + ((HP_PRECISION / 2) - 1);
		} else {
			hpTable[0] = m_hp_ob - 20;
			hpTable[1] = m_hp_ob - 10;
			hpTable[2] = m_hp_ob;
			hpTable[3] = m_hp_ob;
			hpTable[4] = m_hp_ob + 10;
			hpTable[5] = m_hp_ob + 20;
		}

		double[] dmgTable = new double[DAMAGE_PRECISION];
		double step = 9.98 / 9;
		for (int i = 0; i < DAMAGE_PRECISION; i++) {
			dmgTable[i] = (m_dmg_ob) - ((double) DAMAGE_PRECISION / 2 - 0.01)
					+ step * i;
		}

		double[] brokenRateTable = new double[10];
		for (int i = 0; i < 10; i++) {
			brokenRateTable[i] = 5 + 10 * i;
		}

		for (int i = 0; i < 60 * DAMAGE_PRECISION; i++) {
			int a = i / (10 * DAMAGE_PRECISION);
			int b = (i % (10 * DAMAGE_PRECISION)) / DAMAGE_PRECISION;
			int c = i % 10;
			m_particles[i][0] = hpTable[a];
			if (m_dmg_ob == 0)
				m_particles[i][1] = DAMAGE_PRECISION / 3;
			else
				m_particles[i][1] = dmgTable[b];
			if (m_bury == 0)
				m_particles[i][2] = 100.0;
			else
				m_particles[i][2] = brokenRateTable[c];
		}

		m_deadTime = calculateDeathTimeAgent();
		deadtime = getDeadTime();
		// System.err.println("dare por mishe");
	}

	private double[] lifeSpan(double[] status, int time) {
		double[] result = new double[3];
		double hp = status[0];
		double dmg = status[1];
		double brokenDmg = dmg * status[2] / 100.0;
		double buryDmg = dmg - brokenDmg;
		for (int i = 0; i < time; i++) {
			buryDmg += buryDmg * buryDmg * 0.000035;
			buryDmg += 0.01;
			brokenDmg += brokenDmg * brokenDmg * 0.00025;
			brokenDmg += 0.01;
			hp -= buryDmg;
			hp -= brokenDmg;
		}
		result[0] = hp;
		result[1] = buryDmg + brokenDmg;
		result[2] = brokenDmg / result[1] * 100;
		return result;
	}

	private int calculateDeathTime(double[] status) {
		double hp = status[0];
		double dmg = status[1];
		double brokenDmg = dmg * status[2] / 100.0;
		double buryDmg = dmg - brokenDmg;
		int time = 0;
		while (hp > 0) {
			buryDmg += buryDmg * buryDmg * 0.000035;
			buryDmg += 0.01;
			brokenDmg += brokenDmg * brokenDmg * 0.00025;
			brokenDmg += 0.01;
			hp -= buryDmg;
			hp -= brokenDmg;
			time++;
			if (time > 1000)
				return m_particles_time + 1000;
		}
		return m_particles_time + time;
	}

	private int calculateDeathTime(double[] status, int baseTime) {
		double hp = status[0];
		double dmg = status[1];
		double brokenDmg = dmg * status[2] / 100.0;
		double buryDmg = dmg - brokenDmg;
		int time = 0;
		while (hp > 0) {
			buryDmg += buryDmg * buryDmg * 0.000035;
			buryDmg += 0.01;
			brokenDmg += brokenDmg * brokenDmg * 0.00025;
			brokenDmg += 0.01;
			hp -= buryDmg;
			hp -= brokenDmg;
			time++;
			if (time > 1000)
				return baseTime + 1000;
		}
		return baseTime + time;
	}

	private int[] calculateDeathTimeAgent() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < 60; i++) {
			int index = 0;
			index = (int) (Math.random() * 60 * DAMAGE_PRECISION);

			result.add(calculateDeathTime(m_particles[index]));
		}
		Collections.sort(result);
		int[] finalResult = new int[result.size()];
		for (int i = 0; i < result.size(); i++)
			finalResult[i] = result.get(i);
		return finalResult;
	}

	private boolean checkParticle(double[] status) {
		double hp = m_hp_ob;
		double dmg = m_dmg_ob;
		if (m_hp_ob % HP_PRECISION == 0) {
			return (status[0] > hp - (HP_PRECISION / 2)
					&& status[0] < hp + (HP_PRECISION / 2)
					&& status[1] > dmg - (DAMAGE_PRECISION / 2) && status[1] < dmg
					+ (DAMAGE_PRECISION / 2));
		} else {
			return (status[0] > hp - 26 && status[0] < hp + 26
					&& status[1] > dmg - 5 && status[1] < dmg + 5);
		}
	}

	private void updateParticlesAgent(int timeNow) {
		int time = m_lastUpdate - m_particles_time;
		if (time <= 0)
			return;
		boolean propertyChanged = m_propertyChanged;
		m_propertyChanged = false;
		ArrayList<double[]> newParticle = new ArrayList<double[]>();
		for (int i = 0; i < 60 * DAMAGE_PRECISION; i++) {
			double[] newElement = lifeSpan(m_particles[i], time);
			if (checkParticle(newElement))
				newParticle.add(newElement);
		}

		if (newParticle.size() == 0) {
			if (propertyChanged && m_particlesNeedResample) {
				m_particlesNeedResample = false;
				initParticles();
				return;
			}
			// // check if resample is needed ///

			double[][] tmpParticle = initTempParticles();
			ArrayList<Integer> deadTime = new ArrayList<Integer>();
			int total = 0;
			for (int i = 0; i < 60; i++) {
				int index = 0;
				index = (int) (Math.random() * 60 * DAMAGE_PRECISION);
				int t = calculateDeathTime(tmpParticle[index], m_lastUpdate);
				// System.out.println(t);
				deadTime.add(t);
				total += t;
			}
			int death_avg = total / 60;
			total = 0;

			for (int i = 0; i < 60; i++) {
				total += Math.abs(deadTime.get(i) - death_avg);
			}
			int scatterNew = total;
			//
			deadTime = new ArrayList<Integer>();

			total = 0;
			for (int i = 0; i < 60; i++) {
				int index = 0;
				index = (int) (Math.random() * 60 * DAMAGE_PRECISION);

				int t = calculateDeathTime(m_particles[index]);
				deadTime.add(t);
				total += t;
			}
			death_avg = total / 60;
			total = 0;

			for (int i = 0; i < 60; i++) {
				total += Math.abs(deadTime.get(i) - death_avg);
			}
			int scatterOld = total;

			if (scatterNew < scatterOld) {
				initParticles();
				return;
			}

			m_time_needRefresh = timeNow + 15;
			return;
		}

		m_time_needRefresh = timeNow + 50;
		m_particles = new double[60 * DAMAGE_PRECISION][3];
		for (int i = 0; i < newParticle.size(); i++)
			m_particles[i] = newParticle.get(i);
		for (int i = newParticle.size(); i < 60 * DAMAGE_PRECISION; i++) {
			int index = 0;
			index = (int) (Math.random() * (newParticle.size()));
			m_particles[i] = newParticle.get(index);
		}
		m_particles_time = m_lastUpdate;
		m_deadTime = calculateDeathTimeAgent();
		// for (int i = 0; i < m_deadTime.length; i++)
		// System.out.print("m_deadTime: " + m_deadTime[i]);
		deadtime = getDeadTime();
		// System.out.println("555");
	}

	// avalin tabeE ke bara setting deadtime seda mishe :)

	public int cycle(int time) {
		//
		// System.out.println("222 ");
		if (m_particles == null && m_lastUpdate >= 0) {
			// System.out.println("333");
			initParticles();
		}
		if (m_propertyChanged == true || time > m_time_needRefresh) {
			// System.out.println("444");
			updateParticlesAgent(time);
		}
		return m_time_needRefresh;

	}

	// end of code dozD 2

	@Override
	public Property getProperty(String urn) {
		StandardPropertyURN type;
		try {
			type = StandardPropertyURN.fromString(urn);
		} catch (IllegalArgumentException e) {
			return super.getProperty(urn);
		}
		switch (type) {
		case POSITION:
			return position;
		case POSITION_HISTORY:
			return positionHistory;
		case DIRECTION:
			return direction;
		case STAMINA:
			return stamina;
		case HP:
			return hp;
		case X:
			return x;
		case Y:
			return y;
		case DAMAGE:
			return damage;
		case BURIEDNESS:
			return buriedness;
		case TRAVEL_DISTANCE:
			return travelDistance;
		default:
			return super.getProperty(urn);
		}
	}

	@Override
	public Pair<Integer, Integer> getLocation(
			WorldModel<? extends StandardEntity> world) {
		if (x.isDefined() && y.isDefined()) {
			return new Pair<Integer, Integer>(x.getValue(), y.getValue());
		}
		if (position.isDefined()) {
			EntityID pos = getPosition();
			StandardEntity e = world.getEntity(pos);
			return e.getLocation(world);
		}
		return null;
	}

	/**
	 * Get the position property.
	 * 
	 * @return The position property.
	 */
	public EntityRefProperty getPositionProperty() {
		return position;
	}

	/**
	 * Get the position of this human.
	 * 
	 * @return The position.
	 */
	public EntityID getPosition() {
		return position.getValue();
	}

	/**
	 * Set the position of this human.
	 * 
	 * @param position
	 *            The new position.
	 */
	public void setPosition(EntityID position) {
		this.position.setValue(position);
	}

	/**
	 * Find out if the position property has been defined.
	 * 
	 * @return True if the position property has been defined, false otherwise.
	 */
	public boolean isPositionDefined() {
		return position.isDefined();
	}

	/**
	 * Undefine the position property.
	 */
	public void undefinePosition() {
		position.undefine();
	}

	/**
	 * Get the position history property.
	 * 
	 * @return The position history property.
	 */
	public IntArrayProperty getPositionHistoryProperty() {
		return positionHistory;
	}

	/**
	 * Get the position history.
	 * 
	 * @return The position history.
	 */
	public int[] getPositionHistory() {
		return positionHistory.getValue();
	}

	/**
	 * Set the position history.
	 * 
	 * @param history
	 *            The new position history.
	 */
	public void setPositionHistory(int[] history) {
		this.positionHistory.setValue(history);
	}

	/**
	 * Find out if the position history property has been defined.
	 * 
	 * @return True if the position history property has been defined, false
	 *         otherwise.
	 */
	public boolean isPositionHistoryDefined() {
		return positionHistory.isDefined();
	}

	/**
	 * Undefine the position history property.
	 */
	public void undefinePositionHistory() {
		positionHistory.undefine();
	}

	/**
	 * Get the direction property.
	 * 
	 * @return The direction property.
	 */
	public IntProperty getDirectionProperty() {
		return direction;
	}

	/**
	 * Get the direction.
	 * 
	 * @return The direction.
	 */
	public int getDirection() {
		return direction.getValue();
	}

	/**
	 * Set the direction.
	 * 
	 * @param direction
	 *            The new direction.
	 */
	public void setDirection(int direction) {
		this.direction.setValue(direction);
	}

	/**
	 * Find out if the direction property has been defined.
	 * 
	 * @return True if the direction property has been defined, false otherwise.
	 */
	public boolean isDirectionDefined() {
		return direction.isDefined();
	}

	/**
	 * Undefine the direction property.
	 */
	public void undefineDirection() {
		direction.undefine();
	}

	/**
	 * Get the stamina property.
	 * 
	 * @return The stamina property.
	 */
	public IntProperty getStaminaProperty() {
		return stamina;
	}

	/**
	 * Get the stamina of this human.
	 * 
	 * @return The stamina.
	 */
	public int getStamina() {
		return stamina.getValue();
	}

	/**
	 * Set the stamina of this human.
	 * 
	 * @param stamina
	 *            The new stamina.
	 */
	public void setStamina(int stamina) {
		this.stamina.setValue(stamina);
	}

	/**
	 * Find out if the stamina property has been defined.
	 * 
	 * @return True if the stamina property has been defined, false otherwise.
	 */
	public boolean isStaminaDefined() {
		return stamina.isDefined();
	}

	/**
	 * Undefine the stamina property.
	 */
	public void undefineStamina() {
		stamina.undefine();
	}

	/**
	 * Get the hp property.
	 * 
	 * @return The hp property.
	 */
	public IntProperty getHPProperty() {
		return hp;
	}

	/**
	 * Get the hp of this human.
	 * 
	 * @return The hp of this human.
	 */
	public int getHP() {
		return hp.getValue();
	}

	/**
	 * Set the hp of this human.
	 * 
	 * @param newHP
	 *            The new hp.
	 */
	public void setHP(int newHP) {
		this.hp.setValue(newHP);
	}

	/**
	 * Find out if the hp property has been defined.
	 * 
	 * @return True if the hp property has been defined, false otherwise.
	 */
	public boolean isHPDefined() {
		return hp.isDefined();
	}

	/**
	 * Undefine the hp property.
	 */
	public void undefineHP() {
		hp.undefine();
	}

	/**
	 * Get the damage property.
	 * 
	 * @return The damage property.
	 */
	public IntProperty getDamageProperty() {
		return damage;
	}

	/**
	 * Get the damage of this human.
	 * 
	 * @return The damage of this human.
	 */
	public int getDamage() {
		return damage.getValue();
	}

	/**
	 * Set the damage of this human.
	 * 
	 * @param damage
	 *            The new damage.
	 */
	public void setDamage(int damage) {
		this.damage.setValue(damage);
	}

	/**
	 * Find out if the damage property has been defined.
	 * 
	 * @return True if the damage property has been defined, false otherwise.
	 */
	public boolean isDamageDefined() {
		return damage.isDefined();
	}

	/**
	 * Undefine the damage property.
	 */
	public void undefineDamage() {
		damage.undefine();
	}

	/**
	 * Get the buriedness property.
	 * 
	 * @return The buriedness property.
	 */
	public IntProperty getBuriednessProperty() {
		return buriedness;
	}

	/**
	 * Get the buriedness of this human.
	 * 
	 * @return The buriedness of this human.
	 */
	public int getBuriedness() {
		return buriedness.getValue();
	}

	/**
	 * Set the buriedness of this human.
	 * 
	 * @param buriedness
	 *            The new buriedness.
	 */
	public void setBuriedness(int buriedness) {
		this.buriedness.setValue(buriedness);
	}

	/**
	 * Find out if the buriedness property has been defined.
	 * 
	 * @return True if the buriedness property has been defined, false
	 *         otherwise.
	 */
	public boolean isBuriednessDefined() {
		return buriedness.isDefined();
	}

	/**
	 * Undefine the buriedness property.
	 */
	public void undefineBuriedness() {
		buriedness.undefine();
	}

	/**
	 * Get the X property.
	 * 
	 * @return The X property.
	 */
	public IntProperty getXProperty() {
		return x;
	}

	/**
	 * Get the X coordinate of this human.
	 * 
	 * @return The x coordinate of this human.
	 */
	public int getX() {
		return x.getValue();
	}

	/**
	 * Set the X coordinate of this human.
	 * 
	 * @param x
	 *            The new x coordinate.
	 */
	public void setX(int x) {
		this.x.setValue(x);
	}

	/**
	 * Find out if the x property has been defined.
	 * 
	 * @return True if the x property has been defined, false otherwise.
	 */
	public boolean isXDefined() {
		return x.isDefined();
	}

	/**
	 * Undefine the X property.
	 */
	public void undefineX() {
		x.undefine();
	}

	/**
	 * Get the y property.
	 * 
	 * @return The y property.
	 */
	public IntProperty getYProperty() {
		return y;
	}

	/**
	 * Get the y coordinate of this human.
	 * 
	 * @return The y coordinate of this human.
	 */
	public int getY() {
		return y.getValue();
	}

	/**
	 * Set the y coordinate of this human.
	 * 
	 * @param y
	 *            The new y coordinate.
	 */
	public void setY(int y) {
		this.y.setValue(y);
	}

	/**
	 * Find out if the y property has been defined.
	 * 
	 * @return True if the y property has been defined, false otherwise.
	 */
	public boolean isYDefined() {
		return y.isDefined();
	}

	/**
	 * Undefine the y property.
	 */
	public void undefineY() {
		y.undefine();
	}

	/**
	 * Get the travel distance property.
	 * 
	 * @return The travel distance property.
	 */
	public IntProperty getTravelDistanceProperty() {
		return travelDistance;
	}

	/**
	 * Get the travel distance.
	 * 
	 * @return The travel distance.
	 */
	public int getTravelDistance() {
		return travelDistance.getValue();
	}

	/**
	 * Set the travel distance.
	 * 
	 * @param d
	 *            The new travel distance.
	 */
	public void setTravelDistance(int d) {
		this.travelDistance.setValue(d);
	}

	/**
	 * Find out if the travel distance property has been defined.
	 * 
	 * @return True if the travel distance property has been defined, false
	 *         otherwise.
	 */
	public boolean isTravelDistanceDefined() {
		return travelDistance.isDefined();
	}

	/**
	 * Undefine the travel distance property.
	 */
	public void undefineTravelDistance() {
		travelDistance.undefine();
	}

	public void updateDistance(StandardWorldModel model, WorldGraph wg) {
		wg.clearAreas();
		distance = new HashMap<Area, Integer>();
		Area area = (Area) getPosition(model);
		distance.put(area, 0);
		ArrayList<Enterance> layer = new ArrayList<Enterance>();
		ArrayList<Enterance> enterances = area.worldGraphArea.enterances;
		layer.addAll(enterances);
		for (Enterance e : layer)
			e.mark = true;
		while (layer.size() > 0) {
			ArrayList<Enterance> newLayer = new ArrayList<Enterance>();
			for (Enterance enterance : layer) {
				if (enterance.isItConnectedToNeighbour
						&& !enterance.neighbour.mark) {
					enterance.neighbour.mark = true;
					enterance.neighbour.lastEnterance = enterance;
					for (Enterance internal : enterance.neighbour.internalEnterances)
						if (!internal.mark) {
							newLayer.add(internal);
							internal.mark = true;
							internal.lastEnterance = enterance.neighbour;
						}
					if (enterance.neighbour.area.modelArea.checkForAmbForBFS) {
						float dist = 0;
						;
						Enterance copyOfEnterance = enterance.neighbour;
						while (copyOfEnterance.lastEnterance != null) {
							dist += copyOfEnterance.realCenter
									.getDistance(copyOfEnterance.lastEnterance.realCenter);
							copyOfEnterance = copyOfEnterance.lastEnterance;
						}

						distance.put(enterance.neighbour.area.modelArea,
								(int) dist / 30000);
					}
				}
			}
			layer = newLayer;
		}
	}

	/**
	 * Get the entity represented by the position property. The result will be
	 * null if the position property has not been set or if the entity reference
	 * is invalid.
	 * 
	 * @param model
	 *            The WorldModel to look up entity references.
	 * @return The entity represented by the position property.
	 */
	public StandardEntity getPosition(WorldModel<? extends StandardEntity> model) {
		if (!position.isDefined()) {
			return null;
		}
		return model.getEntity(position.getValue());
	}

	/**
	 * Set the position of this human.
	 * 
	 * @param newPosition
	 *            The new position.
	 * @param newX
	 *            The x coordinate of this agent.
	 * @param newY
	 *            The y coordinate if this agent.
	 */
	public void setPosition(EntityID newPosition, int newX, int newY) {
		this.position.setValue(newPosition);
		this.x.setValue(newX);
		this.y.setValue(newY);
	}
}
