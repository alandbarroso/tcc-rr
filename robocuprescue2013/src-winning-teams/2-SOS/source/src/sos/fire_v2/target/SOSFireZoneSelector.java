package sos.fire_v2.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import rescuecore2.misc.Pair;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.message.structure.MessageConstants;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.HungarianAlgorithm;
import sos.fire_v2.base.AbstractFireBrigadeAgent;
import sos.fire_v2.base.tools.FireStarCluster;
import sos.fire_v2.base.tools.FireStarZone;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.tools.cluster.MapClusterType;
import sos.tools.Utils;

public class SOSFireZoneSelector extends SOSSelectTarget<SOSEstimatedFireZone> {

	public FireStarCluster starCluster;

	public HashMap<FireBrigade, Task> fireBrigade_Task;

	public HashMap<Integer, ArrayList<FireBrigade>> zone_FireBrigade;

	public SOSFireZoneSelector(@SuppressWarnings("rawtypes") SOSAgent agent, MapClusterType<FireBrigade> cluster) {
		super(agent, cluster);
	}

	@Override
	public SOSEstimatedFireZone getBestTarget(List<SOSEstimatedFireZone> validTarget) {

		double max = Integer.MIN_VALUE;

		SOSEstimatedFireZone best = null;

		for (SOSEstimatedFireZone e : validTarget) {
			if (e.score != 0 && e.score > max) {
				best = e;
				max = e.score;
			}
		}

		log.info("get best Target " + best);

		return best;
	}

	@Override
	public void reset(List<SOSEstimatedFireZone> validTarget) {
		for (SOSEstimatedFireZone e : validTarget) {
			e.score = 0;
		}
	}

	@Override
	public void setPriority(List<SOSEstimatedFireZone> validTarget) {
		log.info("Set Priority");
		HashMap<Integer, ArrayList<SOSEstimatedFireZone>> fireByLocation = null;
		if (strategy != SelectStrategy.NONE) {
			fireByLocation = Tools.getFireSiteByLocation(new ArrayList<SOSEstimatedFireZone>(validTarget), starCluster);
		}
		for (SOSEstimatedFireZone fz : validTarget) {
			boolean temp = needMe(fz, fireByLocation);
			log.info("Priority For " + fz + "       Added " + temp);
			if (temp) {
				distanceScore(fz);
				ExableScore(fz);
				mapCenterScore(fz);
				mapSideScore(fz);
				rechaibilityScore(fz);
				smallZoneScore(fz);
			}
		}
	}

	private void smallZoneScore(SOSEstimatedFireZone fz) {
		if (strategy == SelectStrategy.NONE)
			return;
		if (Tools.isBigFire(fz))
			return;
		ArrayList<Integer> x = Tools.getFireSiteLocation(fz, starCluster);
		int coef = 1;
		if (Tools.isSmallFire(fz))
			coef = 3;
		Task myTask = fireBrigade_Task.get(agent.me());
		if (x.contains(myTask.zoneIndex))
		{
			coef *= 3;
			fz.score += coef * 50;
			log.info("small Score " + " Coef= " + coef + "  score" + coef * 50);
		}
	}

	private void rechaibilityScore(SOSEstimatedFireZone fz) {
		// TODO Auto-generated method stub

	}

	private boolean needMe(SOSEstimatedFireZone fz, HashMap<Integer, ArrayList<SOSEstimatedFireZone>> fireByLocation) {
		Task myTask = fireBrigade_Task.get(agent.me());
		log.info("myTask " + myTask);

		//TODO in blockade

		if (agent.messageSystem.type == Type.NoComunication ) {
			if (!agent.getMyClusterData().isCoverer()) {
				if (fz.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY()) < Math.max(2 * AbstractFireBrigadeAgent.maxDistance, 1d / 8d * Math.max(agent.model().getBounds().getWidth(), agent.model().getBounds().getHeight()))) {
					log.info("Fire is In EX distance or near me Added");
					return true;
				}

				for (Building b : fz.getAllBuildings())
					if (agent.getMyClusterData().getBuildings().contains(b))
					{
						log.info("Fire is In My Cluster  Added");
						return true;
					}

				log.info("Fire Nocomm not coverer Rejected");
				return false;

			}
			log.info("Fire Nocomm  coverer Added");

			return true;//Nocomm , isCoverer
		}

		if (fz.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY()) < Math.max(2 * AbstractFireBrigadeAgent.maxDistance, 1d / 5d * Math.max(agent.model().getBounds().getWidth(), agent.model().getBounds().getHeight()))) {
			log.info("Fire is In EX distance or near me Added");
			return true;
		}
		if (strategy == SelectStrategy.CLUSTER_GOOD) {
			log.info("Going to Good Strategy Chooser");
			return goodStrategyChooser(fz, myTask, fireByLocation);
		}
		else if (strategy == SelectStrategy.CLUSTER_NORMAL) {
			log.info("Going to Normal Strategy Chooser");
			return normalStrategyChooser(fz, myTask, fireByLocation);
		} else {//None ::> NoComm
			log.info("None Strategy");
			return true;
		}
	}

	private boolean goodStrategyChooser(SOSEstimatedFireZone fz, Task myTask, HashMap<Integer, ArrayList<SOSEstimatedFireZone>> fireByLocation) {

		TaskType type = myTask.type;

		if (fireByLocation.get(myTask.getZoneIndex()).contains(fz)) {
			log.info("Fire is in my location Added");
			return true;
		}

		if (type == TaskType.SEARCHER) {
			log.info("I Am Searcher");

			if (isSmallFire(fz)) {
				log.info("\t\tSmall fire unable to add this zone Rejected");
				return false;
			}
			if (isBigFire(fz)) {
				log.info("\t\tbig fire added");
				return true;
			}
			if (isMediumFire(fz) && isNearMyCluster(fz, myTask)) {
				log.info("\t\tMedium fire and near my cluster Added");
				return true;
			}
			return false;
		}
		else if (type == TaskType.EXTINGUSHER) {
			log.info("I Am Extinguisher");
			if (isSmallFire(fz)) {
				log.info("\t\tSmall fire Rejected");
				return false;
			}
			if (isInMyNeighbour(fz, myTask)) {
				log.info("\t\t in my neighbour Added");
				return true;
			}
			return false;
		}
		else if (type == TaskType.FREE) {
			log.info("I Am Free");
			if (isSmallFire(fz)) {
				log.info("\t\t Small fire Rejected");
				return false;
			}
			return true;
		}

		return false;
	}

	private boolean normalStrategyChooser(SOSEstimatedFireZone fz, Task myTask, HashMap<Integer, ArrayList<SOSEstimatedFireZone>> fireByLocation) {

		TaskType type = myTask.type;

		if (fireByLocation.get(myTask.getZoneIndex()).contains(fz)) {
			log.info("Fire is in my location Added");
			return true;
		}

		if (type == TaskType.SEARCHER) {
			log.info("I Am Searcher");

			if (isSmallFire(fz)) {
				log.info("\t\t Samll Fire Rejected");
				return true;
			}
			if (isBigFire(fz)) {
				log.info("\t\t Big Fire Added");
				return true;
			}
			if (isMediumFire(fz) && isNearMyCluster(fz, myTask)) {
				log.info("\t\t Medium Fire and Near My Cluster ");
				return true;
			}
			return false;
		}
		else if (type == TaskType.EXTINGUSHER) {
			log.info("I Am Extinguisher");
			if (isSmallFire(fz)) {
				log.info("\t\tSmall Fire Reject");
				return false;
			}
			if (isInMyNeighbour(fz, myTask)) {
				log.info("\t\t In My Neighbour");
				return true;
			}
			return false;
		}
		else if (type == TaskType.FREE) {
			log.info("I Am Free");
			if (isSmallFire(fz) && !isInMyNeighbour(fz, myTask)) {
				log.info("\t\t isSmall Fire and isNot InMyNeighbour");
				return false;
			}
			return true;
		}

		return false;
	}

	private boolean isInMyNeighbour(SOSEstimatedFireZone fz, Task myTask) {
		int[] arr = getNeigh(myTask.getZoneIndex());
		ArrayList<Integer> loc = Tools.getFireSiteLocation(fz, starCluster);
		if (!SOSConstant.IS_CHALLENGE_RUNNING) {
			String s = "";
			for (int i : arr) {
				s += i + "\t";
			}
			log.info("My Task's Neighbour::> " + s);
			log.info("FireZone Location::> " + loc);

		}
		for (int i : loc) {
			for (int j : arr) {
				if (i == j)
					return true;
			}
		}
		return false;
	}

	private int[] getNeigh(int index) {
		if (index == 0) {
			int[] arr = new int[starCluster.getStarZones().length];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = i;
			}
			return arr;
		}
		int index1 = (index + starCluster.getStarZones().length - 1) % starCluster.getStarZones().length;
		int index2 = (index + starCluster.getStarZones().length + 1) % starCluster.getStarZones().length;
		if (index1 == 0)
			index1 = starCluster.getStarZones().length - 1;
		if (index2 == 0)
			index2 = 1;
		return new int[] { index1, index2, 0 };

	}

	private boolean isNearMyCluster(SOSEstimatedFireZone fz, Task myTask) {
		if (!isInMyNeighbour(fz, myTask)) {
			log.debug("is not in my neighbour cluster  ::> isNearmycluster=false");
			return false;
		}

		FireStarZone mycluster = starCluster.getStarZones()[myTask.getZoneIndex()];

		for (Building b : fz.getAllBuildings()) {
			for (Building n : b.realNeighbors_Building()) {
				if (mycluster.getZoneBuildings().contains(n)) {
					log.debug(" Building=" + " in my cluster ::> isNearmycluster=true");
					return true;
				}
			}
		}
		log.debug(" isNearmycluster=false");

		return false;
	}

	private boolean isSmallFire(SOSEstimatedFireZone fz) {
		if (fz.getOuter().size() < 8)
			return true;
		return false;
	}

	private boolean isMediumFire(SOSEstimatedFireZone fz) {
		if (fz.getOuter().size() < 20 && !isSmallFire(fz))
			return true;
		return false;
	}

	private boolean isBigFire(SOSEstimatedFireZone fz) {
		if (!isMediumFire(fz) && !isSmallFire(fz))
			return true;
		return false;
	}

	private void mapSideScore(SOSEstimatedFireZone validTarget) {
		//		for (SOSEstimatedFireZone fz : fireZones) {
		//			for (Building b : fz.getOuter()) {
		//				if (canExtinguish(b)) {
		//					int coef = 1;
		//					if (type == TaskType.FREE)
		//						coef = exableCoeff;
		//					fz.score += exableScore * coef;
		//					log.info("Score fire Exable" + fz + "  " + exableScore * coef);
		//					break;
		//				}
		//			}
		//		}

	}

	private void mapCenterScore(SOSEstimatedFireZone fz) {
		int dis = fz.distance((int) agent.model().getBounds().getWidth() / 2, (int) agent.model().getBounds().getHeight() / 2);
		fz.score += -1 * dis;
		log.info("Map Cenetr Score " + fz + "  " + -1 * dis / 100);

	}

	static int exableScore = 300;
	static int exableCoeff = 2;

	private void ExableScore(SOSEstimatedFireZone fz) {
		Task myTask = fireBrigade_Task.get(agent.me());
		TaskType type = myTask.type;

		for (Building b : fz.getOuter()) {
			if (canExtinguish(b)) {
				int coef = 1;
				if (type == TaskType.FREE)
					coef = exableCoeff;
				fz.score += exableScore * coef;
				log.info("Score fire Exable" + fz + "  " + exableScore * coef);
				break;
			}
		}

	}

	private boolean canExtinguish(Building b) {
		return Utils.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY(), b.getX(), b.getY()) < AbstractFireBrigadeAgent.maxDistance;
	}

	private void distanceScore(SOSEstimatedFireZone fz) {
		int dis = fz.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY());
		fz.score += -1 * dis;
		log.info("Distance Score " + fz + "  " + -1 * dis / 1000);

	}

	private void searcherScoring(List<SOSEstimatedFireZone> validTarget) {
		log.info("\n\nFire Zones By Location in Star ");
		HashMap<Integer, ArrayList<SOSEstimatedFireZone>> fireByLocation = Tools.getFireSiteByLocation(new ArrayList<SOSEstimatedFireZone>(validTarget), starCluster);
		for (Entry<Integer, ArrayList<SOSEstimatedFireZone>> e : fireByLocation.entrySet())
			log.info("\t\t" + e);

		log.info("Searcher scoring");

		for (Entry<Integer, ArrayList<SOSEstimatedFireZone>> es : fireByLocation.entrySet()) {
			int loc = es.getKey();
			ArrayList<SOSEstimatedFireZone> fireZones = es.getValue();
			if (loc == fireBrigade_Task.get(agent.me()).getZoneIndex())
				for (SOSEstimatedFireZone fz : fireZones) {
					fz.score = fz.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY());
					log.info("Score " + fz + "  " + fz.score);
				}
		}
		log.info("finished");
	}

	private void extinguisherScoring() {
		// TODO Auto-generated method stub

	}

	private void freeScoring() {

	}

	private void sampleScoring(List<SOSEstimatedFireZone> validTarget) {
		log.info("\n\nFire Zones By Location in Star ");
		HashMap<Integer, ArrayList<SOSEstimatedFireZone>> fireByLocation = Tools.getFireSiteByLocation(new ArrayList<SOSEstimatedFireZone>(validTarget), starCluster);

		for (Entry<Integer, ArrayList<SOSEstimatedFireZone>> e : fireByLocation.entrySet())
			log.info("\t\t" + e);

		for (Entry<Integer, ArrayList<SOSEstimatedFireZone>> es : fireByLocation.entrySet()) {
			int loc = es.getKey();
			ArrayList<SOSEstimatedFireZone> fireZones = es.getValue();
			if (loc == fireBrigade_Task.get(agent.me()).getZoneIndex())
				for (SOSEstimatedFireZone fz : fireZones) {
					fz.score = fz.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY());
					log.info("Score " + fz + "  " + fz.score);
				}
		}
		log.info("finished");
	}

	@Override
	public void preCompute() {
		int regSize = 6;//getNumberOfRegions();
		if (agent.model().fireBrigades().size() > 10 && agent.messageSystem.type != MessageConstants.Type.NoComunication) {
			starCluster = new FireStarCluster(agent);
			starCluster.startClustering(regSize);
			fireBrigade_Task = new HashMap<FireBrigade, Task>();
			zone_FireBrigade = new HashMap<Integer, ArrayList<FireBrigade>>();
			preComputeAssign();
		} else {
			strategy = SelectStrategy.NONE;
			fireBrigade_Task = new HashMap<FireBrigade, Task>();
			zone_FireBrigade = new HashMap<Integer, ArrayList<FireBrigade>>();
			for (FireBrigade fb : agent.model().fireBrigades()) {
				log.info(fb + "   " + fb.getFireIndex() + " assigned to " + -1 + "     FREE ");
				fireBrigade_Task.put(fb, new Task(-1, TaskType.FREE));
				ArrayList<FireBrigade> arr = zone_FireBrigade.get(0);
				if (arr == null) {
					arr = new ArrayList<FireBrigade>();
					zone_FireBrigade.put(0, arr);
				}
				arr.add(fb);

			}

		}
	}

	private int getNumberOfRegions() {
		log.info("Number Of Region");

		int buildingSize = agent.model().buildings().size();
		int fireSize = agent.model().fireBrigades().size();
		int clusterSize = (int) Math.ceil(buildingSize / fireSize); // number of buildings that each cluster has
		int db = (int) (Math.max(2d, 150d / clusterSize) * clusterSize);//number of buildings that one agent can search very fast [2*clusterSize,100]

		log.info("DB = " + db);

		int best = -1;
		for (int i = fireSize; i >= 5; i--) {
			log.info("INDEX = " + i + "\t\t" + "ClusterSize = " + clusterSize + "\t\t NEW = " + Math.ceil(buildingSize / i));
			if (Math.ceil(buildingSize / i) > db) {
				if (best == -1)
					best = i;
				log.info("YES\t" + i);
			}
		}

		log.info("Number Of Region = " + best);

		return best = 5;
	}

	@Override
	public List<SOSEstimatedFireZone> getValidTask(Object link) {
		log.info("Valid Target ");

		ArrayList<SOSEstimatedFireZone> firezones = new ArrayList<SOSEstimatedFireZone>();
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> x : agent.fireSiteManager.getFireSites()) {
			log.info("\tChecking  " + x.second());
			if (x.second().isDisable()) {
				log.info("\t\t Fire Zone is Disable ");
				continue;
			}
			if (!x.second().isExtinguishable()) {
				log.info("\t\t Fire Zone is unExtinguishable  can i Rejetct ?");
				//				if (iAmSearcher(x.second()) || isNoComm()) {
				//					log.info("\t\t\tI am searcher or Nocomm ::> Added");
				//				} else {
				//					if (isNearMe(x.second()) && isSearcherAbsent(x.second())) {
				//						log.info("\t\t\tSearcher is Absent ::> Added");
				//					}
				//					else {
				//						log.info("\t\t\t Rejected");
				//						continue;
				//					}
				//				}

				if (fireBrigade_Task.get(agent.me()).getType() != TaskType.SEARCHER) {

					log.info("\t\t\t Yes Rejected !! map is not noComm and i am not searcher");
					continue;
				}
				//
			}
			firezones.add(x.second());
		}

		log.info("finished validTarget ::> " + firezones);
		return firezones;
	}

	private boolean isNearMe(SOSEstimatedFireZone second) {
		if (((FireBrigade) agent.me()).distance(second) < Math.max(2 * AbstractFireBrigadeAgent.maxDistance, 1 / 5d * agent.model().getBounds().getWidth()))
			return true;
		return false;
	}

	private boolean iAmSearcher(SOSEstimatedFireZone sosEstimatedFireZone) {//TODO
		return fireBrigade_Task.get(agent.me()).getType() == TaskType.SEARCHER;
	}

	private boolean isSearcherAbsent(SOSEstimatedFireZone sosEstimatedFireZone) {
		ArrayList<Integer> x = Tools.getFireSiteLocation(sosEstimatedFireZone, starCluster);
		for (int i : x) {
			try {
				if (zone_FireBrigade.get(i).get(0).distance(sosEstimatedFireZone) > Math.max(200000, 1 / 4d * agent.model().getBounds().getWidth()))
					return true;
			} catch (Exception e) {
			}
		}
		return false;
	}

	private boolean isNoComm() {
		return agent.messageSystem.type == Type.NoComunication;
	}

	private void preComputeAssign() {

		int[][] simi = computeClusterSimiliarity();

		ArrayList<FireBrigade> availableFire = new ArrayList<FireBrigade>(agent.model().fireBrigades());

		ArrayList<FireBrigade> assigned = assignSearcher(simi, availableFire);

		availableFire.removeAll(assigned);

		int need = getNumberOfAgentNeed();

		int insideBuilding = Tools.getNumberOfAgentInSideBuilding(availableFire);

		int available = availableFire.size();
		log.info(" need =" + need + " insideBuilding =" + insideBuilding + "   " + "   available =" + available);

		if (need <= (available - insideBuilding))
		{
			strategy = SelectStrategy.CLUSTER_GOOD;

			log.info("good situation");

			print(simi);

			log.info("assign center ");

			assigned = completeAssign(availableFire, simi);

			availableFire.removeAll(assigned);

			assignRemainedFire(availableFire, simi);

			availableFire.clear();

		} else {
			strategy = SelectStrategy.CLUSTER_NORMAL;
			log.info("normal situation");
			assignRemainedFire(availableFire, simi);
		}

		for (FireBrigade f : agent.model().fireBrigades()) {
			try {
				Task x = fireBrigade_Task.get(f);
				log.info("FireBrigade=" + f + " Index=" + f.getFireIndex() + " Task=" + x);
			} catch (Exception e) {
				log.info("FireBrigade=" + f + " Index=" + f.getFireIndex() + " Task=Exception" + e.getMessage());
			}
		}

	}

	private void assignRemainedFire(ArrayList<FireBrigade> availableFire, int[][] simi) {

		for (FireBrigade fb : availableFire) {
			int best = 0;
			for (int j = 0; j < simi[fb.getFireIndex()].length; j++) {
				if (simi[fb.getFireIndex()][j] > 0) {
					if (simi[fb.getFireIndex()][j] > simi[fb.getFireIndex()][best])
						best = j;
				}
			}
			if (best != -1) {
				log.info(fb + "   " + fb.getFireIndex() + " assigned to " + best + "     FREE ");
				fireBrigade_Task.put(fb, new Task(best, TaskType.FREE));//TODO
				zone_FireBrigade.get(best).add(fb);
			} else
				fireBrigade_Task.put(fb, new Task(best, TaskType.FREE));//TODO

		}

	}

	private ArrayList<FireBrigade> completeAssign(ArrayList<FireBrigade> availableFire, int[][] simi) {
		ArrayList<FireBrigade> fire = Tools.getOutSideFire(availableFire);

		double[][] cost;
		int need = getNumberOfAgentNeed();

		cost = new double[fire.size()][need];
		int[] jobArr = new int[need];

		for (int i = 0; i < fire.size(); i++) {
			FireBrigade fb = fire.get(i);
			int colIndex = 0;
			for (int j = 0; j < starCluster.getStarZones().length; j++) {
				for (int k = 0; k < starCluster.getStarZones()[j].getNumberOfAgentNeed(); k++) {
					cost[i][colIndex] = 101 - simi[fb.getFireIndex()][j];
					jobArr[colIndex] = j;
					colIndex++;
				}
			}
		}

		HungarianAlgorithm ha = new HungarianAlgorithm(cost);

		int[] res = ha.execute();

		ArrayList<FireBrigade> assigned = new ArrayList<FireBrigade>();
		for (int i = 0; i < res.length; i++) {
			FireBrigade fb = fire.get(i);
			if (res[i] == -1)
				continue;
			assigned.add(fb);
			log.info(fb + "   " + fb.getFireIndex() + " assigned to " + jobArr[res[i]] + "     EXTINGUSHER ");
			fireBrigade_Task.put(fb, new Task(jobArr[res[i]], TaskType.EXTINGUSHER));
			zone_FireBrigade.get(jobArr[res[i]]).add(fb);
		}
		return assigned;

	}

	private ArrayList<FireBrigade> assignSearcher(int[][] simi, ArrayList<FireBrigade> availableFire) {//TODO CHANGE CLUSTER

		ArrayList<FireBrigade> fire = new ArrayList<FireBrigade>(availableFire);
		int inside = Tools.getNumberOfAgentInSideBuilding(fire);
		double[][] cost;

		if (agent.model().fireBrigades().size() - inside >= starCluster.getStarZones().length)
			fire = Tools.getOutSideFire(fire);

		cost = new double[fire.size()][starCluster.getStarZones().length];

		for (int i = 0; i < fire.size(); i++) {
			FireBrigade fb = fire.get(i);
			for (int j = 0; j < starCluster.getStarZones().length; j++) {
				cost[i][j] = 101 - simi[fb.getFireIndex()][j];

			}
		}

		HungarianAlgorithm ha = new HungarianAlgorithm(cost);
		int[] res = ha.execute();
		ArrayList<FireBrigade> assigned = new ArrayList<FireBrigade>();
		for (int i = 0; i < res.length; i++) {
			FireBrigade fb = fire.get(i);
			if (res[i] == -1)
				continue;
			assigned.add(fb);
			log.info(fb + "   " + fb.getFireIndex() + " assigned to " + res[i] + "     SearchTask ");
			fireBrigade_Task.put(fb, new Task(res[i], TaskType.SEARCHER));
			ArrayList<FireBrigade> fbarr = new ArrayList<FireBrigade>();
			fbarr.add(fb);
			zone_FireBrigade.put(res[i], fbarr);
		}
		return assigned;

	}

	private void print(int[][] simi) {
	}

	private int[][] computeClusterSimiliarity() {
		int[][] simi = new int[agent.model().fireBrigades().size()][starCluster.getStarZones().length];
		for (int i = 0; i < simi.length; i++) {
			for (int j = 0; j < simi[0].length; j++) {
				//				if (agent instanceof FireBrigadeAgent)
				//					simi[i][j] = getSimiliraity(agent.newSearch.getSearchWorld().getClusterData(agent.model().fireBrigades().get(i)), starCluster.getStarZones()[j]);
				//				else
				simi[i][j] = getSimiliraity(cluster.getCluster(agent.model().fireBrigades().get(i)), starCluster.getStarZones()[j]);

			}
		}
		return simi;
	}

	private int getSimiliraity(ClusterData clusterData, FireStarZone starZone) {
		int num = 0;
		for (Building b : starZone.getZoneBuildings()) {
			if (clusterData.getBuildings().contains(b)) {
				num++;
			}
		}
		return (int) (num / (double) starZone.getZoneBuildings().size() * 100d);
	}

	private int getNumberOfAgentNeed() {
		int sum = 0;
		for (int i = 0; i < starCluster.getStarZones().length; i++)
			sum += starCluster.getStarZones()[i].getNumberOfAgentNeed();//TODO
		return sum;
	}

	public enum TaskType {
		SEARCHER, EXTINGUSHER, FREE
	}

	public class Task {

		private TaskType type;
		private int zoneIndex;

		public Task(int zoneIndex, TaskType type) {
			this.setZoneIndex(zoneIndex);
			this.setType(type);
		}

		public TaskType getType() {
			return type;
		}

		public void setType(TaskType type) {
			this.type = type;
		}

		public int getZoneIndex() {
			return zoneIndex;
		}

		public void setZoneIndex(int zoneIndex) {
			this.zoneIndex = zoneIndex;
		}

		@Override
		public String toString() {
			return "Task(Zone=" + getZoneIndex() + ",Type=" + type + ")";
		}
	}

	public TaskType getMyTaskType(FireBrigade fb) {
		return fireBrigade_Task.get(fb).getType();
	}
}
