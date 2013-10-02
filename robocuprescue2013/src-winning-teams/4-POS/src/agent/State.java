package agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;

enum type {
	CV, MOVE, MOVEREF, NULL,
}

public class State {

	private StandardWorldModel model;
	private double fitness = -1.0;
	public int time = 0;
	public HashMap<AmbulanceTeam, Integer> lastSetTime = new HashMap<AmbulanceTeam, Integer>();
	public HashMap<AmbulanceTeam, Area> lastSetPos = new HashMap<AmbulanceTeam, Area>();
	public ArrayList<TaskAssign> tasks = new ArrayList<TaskAssign>();
	private Random random = new Random(50000);

	public State(StandardWorldModel model) {
		this.model = model;
	}

	public double getFitness() {
		if (fitness > 0)
			return fitness;
		else {
			for (TaskAssign t : tasks)
				if (t.human instanceof Civilian)
					fitness += 1;
				else
					fitness += 2;
			return fitness;
		}
	}

	public void generateRandom(int num, ArrayList<Human> humans,
			HashMap<AmbulanceTeam, HashMap<Area, Integer>> map) {
		while (humans.size() > 0) {
			int rnd = random.nextInt(Math.min(10, humans.size()));
			Human h = humans.remove(rnd);
			if (!goodEnough(h, num))
				continue;

			else {
				TaskAssign jadid = new TaskAssign();
				ArrayList<Integer> cTime = new ArrayList<Integer>();
				jadid.human = h;
				for (int j = 0; j < num; j++) {
					int minTime = Integer.MAX_VALUE;
					AmbulanceTeam minTAmb = null;
					for (AmbulanceTeam amb : lastSetTime.keySet()) {
						if (!amb.hasBuriedness
								&& amb.isAlive
								&& !map.get(amb).containsKey(
										(Area) h.getPosition(model))
								&& !jadid.ambs.contains(amb)
								&& h.distance.containsKey(lastSetPos.get(amb))) {
							int currentTime = (int) (lastSetTime.get(amb) + h.distance
									.get(lastSetPos.get(amb)));
							if (currentTime < minTime) {
								minTime = currentTime;
								minTAmb = amb;
							}
						}
					}

					if (minTAmb == null)
						break;
					cTime.add(minTime);
					jadid.ambs.add(minTAmb);
				}

				boolean isValid = false;
				int s = jadid.human.getBuriedness();
				for (int i = 0; i < jadid.ambs.size(); i++) {
					s -= h.deadtime - (cTime.get(i));
					if (s <= 0) {
						isValid = true;
						for (int j = i + 1; j < jadid.ambs.size(); j++) {
							jadid.ambs.remove(j);
							cTime.remove(j);
							j--;
						}
					}
				}

				if (isValid) {
					int b = jadid.human.getBuriedness();
					int size = jadid.ambs.size();
					for (int i = 1; i < size; i++) {
						b -= (cTime.get(i) - cTime.get(i - 1)) * i;
					}

					for (int i = 0; i < size; i++) {
						AmbulanceTeam a = jadid.ambs.get(i);
						if (i < b % size)
							lastSetTime.put(a, lastSetTime.get(a)
									+ (b / size + 1));
						else
							lastSetTime.put(a, lastSetTime.get(a) + (b / size));
						jadid.time.add(lastSetTime.get(a));
						lastSetPos.put(a, (Area) h.getPosition(model));
					}

					if (jadid.human instanceof Civilian) {
						Collections.sort(jadid.ambs, IDcomparator);
						int toRefuge = Integer.MAX_VALUE;
						Refuge minDis = null;
						for (Entity e : h.distance.keySet())
							if (e instanceof Refuge
									&& h.distance.get(e) < toRefuge) {
								toRefuge = h.distance.get(e);
								minDis = (Refuge) e;
							}
						if (toRefuge < Integer.MAX_VALUE) {
							lastSetTime.put(jadid.ambs.get(0),
									lastSetTime.get(jadid.ambs.get(0))
											+ toRefuge);
							lastSetPos.put(jadid.ambs.get(0), minDis);
							jadid.time.set(0,
									lastSetTime.get(jadid.ambs.get(0)));
						}
					}
					tasks.add(jadid);
				}
			}
		}
	}

	private Comparator<Entity> IDcomparator = new Comparator<Entity>() {
		public int compare(Entity a1, Entity a2) {
			if (a1.getID().getValue() > a2.getID().getValue())
				return 1;
			if (a1.getID().getValue() < a2.getID().getValue())
				return -1;
			return 0;
		}
	};

	public boolean goodEnough(Human h, int saverNum) {
		if (saverNum < 1)
			saverNum = 1;
		if ((h.hasBuriedness || h instanceof Civilian)
				&& h.isPositionDefined()
				&& !isOnFire(model.getEntity(h.getPosition()))
				&& (!(h instanceof Civilian) || h.deadtime > time)
				&& (model.getEntity(h.getPosition()) instanceof Building || (model
						.getEntity(h.getPosition()) instanceof Road
						&& h.isDamageDefined() && h.getDamage() != 0))
				&& !(isInRefuge(h))) {
			if (h.deadtime > time + (h.getBuriedness() / (saverNum))) {
				return true;
			}
		}
		return false;
	}

	public boolean isOnFire(Entity building) {
		if (building instanceof Building
				&& ((((Building) building).isFierynessDefined() && ((Building) building)
						.getFieryness() > 0) || ((Building) building).stFire > 0))
			return true;
		return false;
	}

	public boolean isInRefuge(Human h) {
		if (model.getEntityByInt(h.getPosition().getValue()) instanceof Refuge)
			return true;
		return false;
	}

}
