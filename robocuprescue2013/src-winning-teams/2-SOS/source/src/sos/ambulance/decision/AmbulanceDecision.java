package sos.ambulance.decision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import rescuecore2.geometry.Point2D;
import rescuecore2.geometry.Vector2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.AmbulanceConstants;
import sos.ambulance_v2.base.AmbulanceConstants.CivilianState;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.ambulance_v2.decision.controller.AmbulanceDream;
import sos.ambulance_v2.decision.controller.ImaginationShot;
import sos.ambulance_v2.tools.FireDeathTime;
import sos.ambulance_v2.tools.GraphUsage;
import sos.ambulance_v2.tools.MultiDitinctSourceCostInMM;
import sos.ambulance_v2.tools.SimpleDeathTime;
import sos.base.CenterAgent;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.blocks.MessageBlock;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSGeometryTools;
import sos.base.util.blockadeEstimator.AliGeometryTools;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.search_v2.tools.cluster.ClusterData;

/**
 * Created by IntelliJ IDEA.
 * User: ara
 * To change this template use File | Settings | File Templates.
 */
public class AmbulanceDecision {

	//------------------------------Agents References
	AmbulanceTeamAgent self = null;
	SOSAgent<? extends StandardEntity> agent = null;

	//------------------------------Useful tools
	FireDeathTime fireEstimator;
	public GraphUsage gu;
	public final SOSLoggerSystem dclog;

	//------------------------------Needed Lists
	ArrayList<Human>[] demandMatrix;
	ArrayList<AmbulanceTeam> ambulances;
	ArrayList<Human> targets;
	//------------------------------Assigning map
	HashMap<AmbulanceTeam, ImaginationShot[]> assign;

	//------------------------------minCostFor AmbulanceTeam
	int lastMinCostTime = 0;
	int lastHearsingTime = 0;
	int lastTimeUpdated = 0;
	int lastMakeListTime = 0;
	PriorityQueue<Pair<Human, Long>> minCostTargets = new PriorityQueue<Pair<Human, Long>>(30, new CostComparator());
	PriorityQueue<Pair<Human, Long>> hearsingTargets = new PriorityQueue<Pair<Human, Long>>(30, new CostComparator());

	public static int AVERAGE_MOVE_TO_TARGET = 4;
	public boolean hugeMap = false;
	private SOSLoggerSystem humanUpdateLog;
	public MultiDitinctSourceCostInMM costTable;

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Constructors >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public AmbulanceDecision(AmbulanceTeamAgent agent) {
		self = agent;
		this.agent = agent;
		AVERAGE_MOVE_TO_TARGET = (agent.model().roads().size() < 1200 ? 6 : (agent.model().roads().size() < 2000 ? 9 : 12));
		dclog = new SOSLoggerSystem(agent.me(), "Agent/AmbulanceDecision", true, OutputType.File);
		dclog.setFullLoggingLevel();
		agent.sosLogger.addToAllLogType(dclog);

		humanUpdateLog = new SOSLoggerSystem(agent.me(), "Agent/ATHumanUpdate", true, OutputType.File, true);
		agent.sosLogger.addToAllLogType(humanUpdateLog);

		gu = new GraphUsage(agent);
		if (agent.getMapInfo().isBigMap()) {
			hugeMap = true;
		}
		dclog.logln("AVERAGE_MOVE_TO_TARGET=" + AVERAGE_MOVE_TO_TARGET + "  hugeMap=" + hugeMap);
	}

	public AmbulanceDecision(SOSAgent<? extends StandardEntity> center) {
		this.agent = center;
		AVERAGE_MOVE_TO_TARGET = agent.getMapInfo().isBigMap() ? 12 : agent.getMapInfo().isMediumMap() ? 9 : 6;
		dclog = new SOSLoggerSystem(agent.me(), "Agent/AmbulanceDecision", true, OutputType.File);
		dclog.setFullLoggingLevel();
		agent.sosLogger.addToAllLogType(dclog);

		humanUpdateLog = new SOSLoggerSystem(agent.me(), "Agent/AmbulanceDecision/HumanUpdate", true, OutputType.File, true);
		agent.sosLogger.addToAllLogType(humanUpdateLog);

		gu = new GraphUsage(center);
		if (agent.getMapInfo().isBigMap()) {
			hugeMap = true;
		}
		dclog.logln("AVERAGE_MOVE_TO_TARGET=" + AVERAGE_MOVE_TO_TARGET + "  hugeMap=" + hugeMap);
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * @r@mik updating death and critical and average times and time to transport to nearest open refuge
	 */
	public void updateHumansInfo() {
		dclog.info("IN UPDATE_HUMANS_INFO .....time=" + agent.time() + " to more information take a look at human update log");
		if (lastTimeUpdated == agent.time())
			return;
		ArrayList<Area> srcs = new ArrayList<Area>();

		if (agent.getCenterActivities().isEmpty())
			srcs.add(agent.me().getAreaPosition());
		else {
			for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
				srcs.add(at.getAreaPosition());
			}
		}
		for (Refuge ref : agent.model().refuges()) {
			srcs.add(ref);
		}
		costTable = new MultiDitinctSourceCostInMM(agent.model(), srcs);
		lastTimeUpdated = agent.time();

		for (Human hm : agent.model().humans()) {
			boolean result = updateHuman(hm);
			if (!result)
				continue;
			//setting refuge infos
			//			if (hm.getRescueInfo().getBestRefuge() == null && ((hm.getID().getValue() % 10) + agent.time()) % 5 == 0) { // update Humans which do'nt have refuge in every 3 cycle
			calculateRefugeInformation(hm);
			//			} else if (agent.time() - hm.getRescueInfo().getRefugeCalculatedTime() % 7 == 0) // update Humans which have refuge in every 7 cycles
			//				calculateRefugeInformation(hm);
			humanUpdateLog.log("[ref=" + hm.getRescueInfo().getBestRefuge() + " \t refTime=" + hm.getRescueInfo().getTimeToRefuge() + "], ");
		}

	}

	public boolean updateHuman(Human hm) {
		humanUpdateLog.log(hm);
		//initial checks
		if (hm == null) {
			humanUpdateLog.logln(" --> NULL Human!");
			return false;
		}
		if (!hm.isBuriednessDefined()) {
			humanUpdateLog.logln(" --> Unknown State!");
			return false;
		}
		if (!(hm instanceof Civilian) && hm.getBuriedness() == 0) {
			humanUpdateLog.logln(" --> Not burried Agent!");
			return false;
		}
		if (!hm.isPositionDefined()) {
			humanUpdateLog.logln(" --> No position!");
			return false;
		}

		if (hm.getPosition() instanceof Refuge) {
			humanUpdateLog.logln(" --> in refuge!");
			return false;
		}
		if (hm.getPosition() instanceof AmbulanceTeam) {
			humanUpdateLog.logln(" --> in AT!");
			return false;
		}
		if (hm instanceof Civilian && hm.getPosition() instanceof Road && hm.getDamage() == 0 && hm.getHP() == 10000) {
			humanUpdateLog.logln(" --> in road Healthy Civilian");
			return false;
		}
		// locking free situations
		if (hm.getRescueInfo().isM_lifeTimeLocker()) {
			if (!(hm.getPosition() instanceof Building))
				return false;
			Building b = (Building) hm.getPosition();
			if (b.isOnFire())
				return false;
			hm.getRescueInfo().setM_lifeTimeLocker(false);
//			hm.getRescueInfo().getPartileFilter().m_particlesNeedResample = true;
		}
//		hm.getRescueInfo().getPartileFilter().cycle(agent.time());
		int index;
		if (!agent.model().refuges().isEmpty()) {
			index = 60 - hm.getBuriedness();
			if (index < 5)
				index = 5;
			else if (index > 55)
				index = 55;
		} else {
			index = 55;
		}
//		int deathtime = hm.getRescueInfo().getPartileFilter().getDeadTime()[index];
		int deathtime2 = SimpleDeathTime.getEasyLifeTime(hm.getHP(), hm.getDamage(), hm.updatedtime());
//		String humanInfoLog = hm + " (hp:" + hm.getHP() + ",damage:" + hm.getDamage() + ",time:" + hm.updatedtime() + "), buried=" + hm.getBuriedness() + ",PartileDT:" + deathtime + ",EasyDT:" + deathtime2;

		//		int tmpDamge = Math.max((hm.getDamage()*2),hm.getDamage()+agent.getConfig().getIntValue("perception.los.precision.damage")/2);

//		if (deathtime < deathtime2 - 100 || deathtime > deathtime2 + 50) {
//			humanUpdateLog.info("Estimated death time for " + hm + " seem to not be currect! using easy death time");
//			deathtime = deathtime2;
//		}

		if (AmbulanceUtils.isBurning(hm)) {
			hm.getRescueInfo().setM_lifeTimeLocker(true);
		}
		if (agent.model().refuges().isEmpty()) {
			hm.getRescueInfo().setTimeToRefuge(1);
			hm.getRescueInfo().setBestRefuge(null);
			humanUpdateLog.logln("");
			return false;
		}
		if (hm.getRescueInfo().getTimeToRefuge() == 0) {
			hm.getRescueInfo().setTimeToRefuge(AVERAGE_MOVE_TO_TARGET);
		}
		hm.getRescueInfo().setATneedToBeRescued(getATNeedNow(hm, hm.getRescueInfo().getDeathTime()));
		hm.getRescueInfo().setLongLife(hm instanceof AmbulanceTeam || hm instanceof Civilian && hm.getRescueInfo().getDeathTime() >= AmbulanceConstants.LONG_TIME);
		//TODO why setlonglife =true foe ambulances?????
		return true;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * @param hm
	 * @r@mik update nearest reachable refuge of Human and time to reaching that refuge
	 */

	public void calculateRefugeInformation_old(Human hm) {
		if (!hm.isPositionDefined() || hm.getPosition() instanceof Refuge)
			return;
		if (!(hm instanceof Civilian))
			return;
		if (agent.model().refuges().isEmpty() || hm.getRescueInfo().getRefugeCalculatedTime() == agent.time())
			return;

		ArrayList<Pair<? extends Area, Point2D>> Positions = new ArrayList<Pair<? extends Area, Point2D>>(agent.model().ambulanceTeams().size());
		for (Refuge rf : agent.model().refuges())
			Positions.add(new Pair<Area, Point2D>(rf, new Point2D(rf.getX(), rf.getY())));
		int times[] = movingTime(hm.getPositionPair(), Positions);
		setMinTimeRefuge(hm, times);
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * @param hm
	 * @r@mik update nearest reachable refuge of Human and time to reaching that refuge
	 */

	public void calculateRefugeInformation(Human hm) {
		if (!hm.isPositionDefined() || hm.getPosition() instanceof Refuge)
			return;
		if (!(hm instanceof Civilian))
			return;
		if (agent.model().refuges().isEmpty() || hm.getRescueInfo().getRefugeCalculatedTime() == agent.time())
			return;

		long min = Long.MAX_VALUE;
		Refuge best = null;
		for (Refuge refuge : agent.model().refuges()) {
			long tmp = costTable.getCostFromTo(refuge, hm.getAreaPosition());
			if (tmp < min) {
				best = refuge;
				min = tmp;
			}

		}
		if (best != null && min < MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING) {
			hm.getRescueInfo().setBestRefuge(best);
			hm.getRescueInfo().setTimeToRefuge(gu.getFoolMoveTime(min));
		}

		hm.getRescueInfo().setRefugeCalculatedTime(agent.time());
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * preparing target list in both center and agent usage
	 *
	 * @param isDecidingForCenter
	 */
	public void MakeLists(boolean isDecidingForCenter) {
		makeLists(agent.model().humans(), isDecidingForCenter);
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	protected void makeLists(ArrayList<Human> decideArrayList, boolean isDecidingForCenter) {
		dclog.info("IN MAKELISTS ...  time=" + agent.time());
		if (lastMakeListTime == agent.time())
			return;
		lastMakeListTime = agent.time();
		if (lastMinCostTime == agent.time())
			return;
		ambulances = new ArrayList<AmbulanceTeam>(agent.model().ambulanceTeams().size());
		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.isReadyToAct())
				ambulances.add(at);
		}
		dclog.debug("Ready Ambulances=" + ambulances);
		targets = new ArrayList<Human>();

		for (Human hm : decideArrayList) {

			if (hm.getRescueInfo().getIgnoredUntil() <= agent.time())
				hm.getRescueInfo().setNotIgnored();

			if (isDecidingForCenter) {
				if (AmbulanceUtils.isValidToDecideForCenter(hm, humanUpdateLog))
					targets.add(hm);
			} else {
				if (AmbulanceUtils.isValidToDecide(hm, humanUpdateLog))
					if (hm instanceof Civilian && agent.time() - ((Civilian) hm).getFoundTime() <= 3)
						humanUpdateLog.debug(hm + " is invalid for agent because it is new sense and still 3 cycle doesn't pass");
					else
						targets.add(hm);
			}

		}
		//---------------------------------No refuge changes to targets
		if (agent.model().refuges().isEmpty()) {
			ArrayList<Human> removes = new ArrayList<Human>();
			for (Human hmn : targets) {
				if (hmn instanceof Civilian) {
					if (AmbulanceUtils.isHumaninFireBuilding(hmn) || hmn.getRescueInfo().getInjuryDeathTime() < AmbulanceConstants.VALID_DEATH_TIME_FOR_NO_REFUGE_MAP || !(hmn.getPosition() instanceof Building)) {
						removes.add(hmn);
					}
				}
			}
			dclog.debug("remove list because it is no refuge map=>" + removes);
			targets.removeAll(removes);
		}
		//-------------------------------------------------------------
		dclog.logln("targets = ");
		for (Human hmn : targets) {
			hmn.getRescueInfo().setATneedToBeRescued(getATNeedNow(hmn, hmn.getRescueInfo().getDeathTime()));
			dclog.logln("    " + hmn + "--> b=" + hmn.getBuriedness() + " hp:" + hmn.getHP() + " d=" + hmn.getDamage() + " ATneedToBeRescued=" + hmn.getRescueInfo().getATneedToBeRescued() + "    working=" + hmn.getRescueInfo().getNowWorkingOnMe() + "  dTime=" + hmn.getRescueInfo().getDeathTime() + " " + hmn.getRescueInfo().getBestRefuge() + "  refTime=" + hmn.getRescueInfo().getTimeToRefuge() + " longLife=" + hmn.getRescueInfo().longLife() + " Reachable:" + hmn.isReallyReachable(true));

		}
		dclog.logln("");

	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * using only in AmbulanceTeamAgent
	 *
	 * @return target
	 */
	public Human getMinCostTarget() {
		dclog.info("getMinCostTarget time=" + agent.time());
		if (lastMinCostTime == agent.time() && !minCostTargets.isEmpty())
			return minCostTargets.poll().first();
		else if (lastMinCostTime == agent.time() && minCostTargets.isEmpty())
			return null;

		lastMinCostTime = agent.time();
		minCostTargets = new PriorityQueue<Pair<Human, Long>>(targets.size() + 5, new CostComparator());

		for (int i = 0; i < targets.size(); i++) {
			Human hum = targets.get(i);
			Human etarget = hum;

			if (!hum.isReallyReachable(true)) {
				dclog.logln(hum + " is not reachable!");
				continue;
			}
			if (targets.size() > ambulances.size() / 3 && hum.getRescueInfo().getATneedToBeRescued() - hum.getRescueInfo().getNowWorkingOnMe().size() <= 0) {
				dclog.logln(hum + " has Enough At!");
				continue;
			}
			int duration = taskPerformingDurationForMe(hum, true);
			long ecost;
			try {
				ecost = hum.getRescueInfo().getRescuePriority() / (duration * 100 * (self.me().getAmbIndex() + 1) + hum.getRescueInfo().getDeathTime());
			} catch (ArithmeticException ae) { //divided by zero
				ecost = hum.getRescueInfo().getRescuePriority() / (hum.getBuriedness() + 2 + 2 * AVERAGE_MOVE_TO_TARGET);
				System.err.println("Division by zero in AmbulanceDecision.getMinCostTarget()");
			}
			Pair<Human, Long> e = new Pair<Human, Long>(etarget, ecost);
			dclog.logln("target=" + hum + " --> cost=" + e.second());
			minCostTargets.offer(e);
		}
		dclog.logln("");
		Pair<Human, Long> el = minCostTargets.poll();
		if (targets.isEmpty() || el == null)
			return null;
		dclog.logln("MinCostTarget result = " + el.first() + "    cost=" + el.second());
		return el.first();
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public void removeUnrescueableTargets() {
		if (targets.isEmpty())
			return;
		dclog.info("removing UnrescueableTargets time=" + agent.time());
		if (lastMinCostTime == agent.time())
			return;
		ArrayList<Human> remove = new ArrayList<Human>(5);
		//*************************************remove physically can't help civilians
		for (Human hmn : targets) {
			if (hmn.getRescueInfo().getATneedToBeRescued() > ambulances.size()) {
				dclog.logln(hmn + " needed AT bigger than ambulance sizes!");
				remove.add(hmn);
			} else if (AmbulanceUtils.taskAssigningExpireTime(hmn, hmn.getRescueInfo().getDeathTime()) - agent.time() < 0) { // too late to help from death
				dclog.logln(hmn + " task AssignningExpireTime passed!");
				remove.add(hmn);
			}

		}
		dclog.debug("physically cant help list=" + remove);
		targets.removeAll(remove);
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * only for center assigning usage
	 */
	public void imagining() {
		abstractlog().logln("targets = ");
		for (Human hmn : targets) {
			hmn.getRescueInfo().setATneedToBeRescued(getATNeedNow(hmn, hmn.getRescueInfo().getDeathTime()));
			abstractlog().logln("    " + hmn + "--> b=" + hmn.getBuriedness() + " d=" + hmn.getDamage() + " ATneedToBeRescued=" + hmn.getRescueInfo().getATneedToBeRescued() + "    working=" + hmn.getRescueInfo().getNowWorkingOnMe() + "  dTime=" + hmn.getRescueInfo().getDeathTime() + " " + hmn.getRescueInfo().getBestRefuge() + "  refTime=" + hmn.getRescueInfo().getTimeToRefuge() + " Reachable:" + hmn.isReallyReachable(false) + " longLife=" + hmn.getRescueInfo().longLife());

		}
		abstractlog().logln("");

		if (targets.isEmpty())
			return;

		dclog.info("IMAGINING START......");
		int costTimes[][] = new int[ambulances.size()][targets.size()];
		//        TreeSet<Integer> ignoredTargets = new TreeSet<Integer>();
		boolean[] ignoredTargets = new boolean[targets.size()];
		Arrays.fill(ignoredTargets, false);
		dclog.debug("Targets:" + targets);

		PriorityQueue<ImaginationShot> imaginationQueue = new PriorityQueue<ImaginationShot>(targets.size() + 3, new ImaginationShotPriorityComparator());
		ArrayList<ImaginationShot> listOfImages = new ArrayList<ImaginationShot>();
		ArrayList<AmbulanceDream> helpingDreams = new ArrayList<AmbulanceDream>();
		dclog.debug("Current Helping Dreams(Ready Ambulances)==>");
		abstractlog().debug("Current Helping Dreams(Ready Ambulances)==>");
		for (AmbulanceTeam at : ambulances) {
			AmbulanceDream hd = new AmbulanceDream(at, at.getWork().getNextFreeTime());
			helpingDreams.add(hd);
			dclog.logln("\t" + hd);
			abstractlog().logln("\t" + hd);
		}

		for (int i = 0; i < targets.size(); i++) {
			Human hm = targets.get(i);
			ImaginationShot img = new ImaginationShot(hm, getTargetDesireState(hm, getTargetCurrentState(hm)), i);
			listOfImages.add(img);
			imaginationQueue.offer(img);
		}
		dclog.debug("Current Imagination shots=" + listOfImages);
		HashMap<Long, Integer> hm_MovingTime = new HashMap<Long, Integer>();
		while (true) {
			boolean first = true;
////////////////////////////////////////////////////////////////////////////////////////////
			while (!imaginationQueue.isEmpty()) {

				ImaginationShot currentImage = imaginationQueue.poll();
				dclog.logln("");
				dclog.log("\tcurrent ImaginationShot = " + currentImage);
				if (ignoredTargets[currentImage.index]) { //not enough reachable AT to target or make penalty
					dclog.logln("-->ignored");
					continue;
				}
				dclog.logln("");

				//finding in position cost for AT
/********************* set TimeCost for ATs to move to current target ****************************/
				if (first) {
					//TODO it is not correct
					ArrayList<Area> srcs = new ArrayList<Area>(agent.model().ambulanceTeams().size());
					for (int j = 0; j < helpingDreams.size(); j++)
						srcs.add(helpingDreams.get(j).at.getAreaPosition());
					for (Refuge rf : agent.model().refuges()) {
						srcs.add(rf);
					}
					int times[] = getMovingTime(srcs, currentImage.target.getAreaPosition());

					for (int i = 0; i < times.length; i++) {
						if (i < helpingDreams.size())//if it's not refuge
							costTimes[i][currentImage.index] = times[i];
						hm_MovingTime.put(getImageKey(currentImage, srcs.get(i).getPositionPair()), times[i]);
					}
					/************ found best refuge for current target **********/
					calculateRefugeInformation(currentImage.target);
				}
	/********************** LOG(TimeCost of Ats for current target *******************************/
				dclog.log("\t\t" + currentImage.target + " COSTS-> ");
				for (int z = 0; z < ambulances.size(); z++) {
					dclog.log(costTimes[z][currentImage.index] + " \t");
				}
				dclog.logln("");
	/************ calculate number of ATs that are suitable for rescue current target ****************/
				//Removing Not enough reachable AT to target;
				int count = 0;
				for (int j = 0; j < ambulances.size(); j++) {
					if (costTimes[j][currentImage.index] < AmbulanceConstants.LONG_TIME)
						count++;
				}
	/************* ignore target if there is not suitable enough good At to rescue him ****************/
				int numberOfAtsneedesNow =(currentImage.target.getRescueInfo().getATneedToBeRescued() - currentImage.target.getRescueInfo().getNowWorkingOnMe().size());
				if (count < numberOfAtsneedesNow) {
					ignoredTargets[currentImage.index] = true;
					dclog.logln("\t\tNot enough reachable AT to :" + currentImage.index);
					continue;
				}
	/************************** LOG ( (TimeCost + time_To_be_free ) for each AT ) *********************/
				dclog.log("\t\t COSTS +finishtime    -> ");
				for (int z = 0; z < ambulances.size(); z++) {
					dclog.log((costTimes[z][currentImage.index] + helpingDreams.get(z).time_To_be_free) + " \t");
				}
				dclog.logln("");
	/********************* filter ATs with filter of "who rescue current target sooner?" ***************/
				boolean[] ignoredATs = new boolean[ambulances.size()];
				Arrays.fill(ignoredATs, false);

				dclog.logln("\t\tnewAtNeed" + numberOfAtsneedesNow + " totalATneed:"
				+ currentImage.target.getRescueInfo().getATneedToBeRescued()
				+ " nowWorking:" + currentImage.target.getRescueInfo().getNowWorkingOnMe().size());

				for (int i = 0; i < numberOfAtsneedesNow; i++) {
					int min_at_index = minFreeTimeAmbulanceIndex(helpingDreams, costTimes, currentImage.index, ignoredATs, agent);

					dclog.logln("\t\tmin_at =" + helpingDreams.get(min_at_index).at
							+ " cost+finishtime=" + ((costTimes[min_at_index][currentImage.index]
									+ helpingDreams.get(min_at_index).time_To_be_free)));

					ignoredATs[min_at_index] = true;
					AmbulanceDream ad = helpingDreams.get(min_at_index);
					ad.addImagination(currentImage, costTimes[min_at_index][currentImage.index]);
				}

	/**********************************************************************************************/
			} //end while2
////////////////////////////////////////////////////////////////////////////////////////////
	/**********************************************************************************************/

			/**************** found if there is any penalty for rescuing ************************/
			int penalty = 0;
			dclog.log("\tImagination shots after assigning=");
			long lastPenaltyPriority = 0;
			for (ImaginationShot is : listOfImages) {
				if (ignoredTargets[is.index])
					continue;
				if ((is.goalCondition != CivilianState.DEATH) && is.resultCondition == CivilianState.DEATH) {
					penalty += 1000;
					lastPenaltyPriority = is.target.getRescueInfo().getRescuePriority();
				}
				dclog.log(is + " ");
			}

			dclog.logln("");
			dclog.logln("[Penalty=" + penalty + "]");

			/********************** ignore target with maxATneedNow ************************/
			if (penalty >= 1000) {
				/*** get index of target with maxATneadNow **/
				int index = maxWorkNeedImageIndex(listOfImages, ignoredTargets, lastPenaltyPriority);

				/*** no one need AT now ****/
				if (index == -1) {
					dclog.logln("\tindex=-1");
					break;
				}

				dclog.logln("\tindex=" + index + "  maxWorkRemoving=" + listOfImages.get(index));
				ignoredTargets[index] = true;
				for (AmbulanceDream ad : helpingDreams)
					ad.reset();
				imaginationQueue.clear();
				for (ImaginationShot img : listOfImages) {
					img.reset();
					imaginationQueue.offer(img);
				}
			}
			else
				break;

		}  //end while1
////////////////////////////////////////////////////////////////////////////////////////////
		abstractlog().logln("assigns ={{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{............");
		dclog.logln("assigns ={{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{............");
		assign = new HashMap<AmbulanceTeam, ImaginationShot[]>();
		for (AmbulanceDream ad : helpingDreams) {
			ImaginationShot targets[] = new ImaginationShot[2];
			if (ad.performing.size() > 0)
				targets[0] = ad.performing.get(0);
			if (ad.performing.size() > 1)
				targets[1] = ad.performing.get(1);
			assign.put(ad.at, targets);

			dclog.debug(ad.at +
					"fct:" + ad.at.getWork().getNextFreeTime() +
					"  -->  target1:" + (targets[0] == null ? "null" : targets[0].target) +
					" , target2:" + (targets[1] == null ? null : targets[1].target) + " his current target=" +
					ad.at.getWork().getTarget());
			abstractlog().debug(ad.at +
					"fct:" + ad.at.getWork().getNextFreeTime() +
					"  -->  target1:" + (targets[0] == null ? "null" : targets[0].target) +
					" , target2:" + (targets[1] == null ? null : targets[1].target) + " his current target=" +
					ad.at.getWork().getTarget());
		}
		dclog.logln("......................}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
		abstractlog().logln("......................}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
	}


	private int getMovingLength(AmbulanceTeam at, Area lastPosition, Area target) {
		Area firstpo = at.getPositionArea();

		long moveToLastPo = costTable.getCostFromTo(firstpo, lastPosition);
		if (moveToLastPo >= MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING)
			return MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING;

		long moveToTarget = costTable.getCostFromTo(firstpo, target);
		if (moveToTarget >= MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING)
			return MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING;
		Vector2D v1 = new Vector2D(lastPosition.getX() - firstpo.getX(), lastPosition.getY() - firstpo.getY());
		Vector2D v2 = new Vector2D(target.getX() - firstpo.getX(), target.getY() - firstpo.getY());
		double angle = AliGeometryTools.getAngleInRadian(v1, v2);
		long moveFromLastPosToTargetX = (long) (moveToLastPo - moveToTarget * Math.cos(angle));
		long moveFromLastPosToTargety = (long) (Math.sin(angle));

		double len = Math.hypot(moveFromLastPosToTargetX, moveFromLastPosToTargety);
		if (len > MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING)
			return MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING;
		return (int) len;
	}

	private int[] getMovingTime(ArrayList<Area> srcs, Area dst) {
		int[] times = new int[srcs.size()];
		for (int i = 0; i < srcs.size(); i++) {
			Area src = srcs.get(i);
			times[i] = gu.getFoolMoveTime(costTable.getCostFromTo(src, dst));
		}
		return times;
	}

	private long getImageKey(ImaginationShot is, Pair<? extends Area, Point2D> ad) {
		return (long) ((ad.second().getX() + ad.second().getY()) / (ad.first().getAreaIndex() + 1) + (is.target.getX() + is.target.getY()) / (is.target.getPositionArea().getAreaIndex() + 1));
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public void sendAssignMessages() {
		if (targets.isEmpty())
			return;

		for (Map.Entry<AmbulanceTeam, ImaginationShot[]> mapEntry : assign.entrySet()) {
			ImaginationShot[] img = mapEntry.getValue();

			if (img[0] == null || img[0].target == null/* || me.getKey().getWork().getNextFreeTime()-agent.time()>4 */) {
				dclog.error(" Assigned task was null or its target was null==========>");
				dclog.error("            " + img[0]);
				continue;
			}
			//It's not logical to send message to myself! :D so...
			if (agent instanceof AmbulanceTeamAgent && mapEntry.getKey().getAmbIndex() == self.me().getAmbIndex()) {
				((AmbulanceTeamAgent) agent).setCenterRecommendedTarget(img[0].target);
				continue;
			}
			int mlposIndex = img[0].target.getPositionArea().getAreaIndex();
			int LongLife = 0;
			if (img[0].target.getRescueInfo().longLife())
				LongLife = 1;

			dclog.trace("Sending assign msg-->" + mapEntry.getKey() + "--> target:" + img[0].target + " position:" + img[0].target.getPositionArea() + " longlife?" + img[0].target.getRescueInfo().longLife());
			abstractlog().trace("Sending assign msg-->" + mapEntry.getKey() + "--> target:" + img[0].target + " position:" + img[0].target.getPositionArea() + " longlife?" + img[0].target.getRescueInfo().longLife());
			agent.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_ASSIGN);
			agent.messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, mapEntry.getKey().getAmbIndex());
			agent.messageBlock.addData(MessageXmlConstant.DATA_ID, img[0].target.getID().getValue());
			agent.messageBlock.addData(MessageXmlConstant.DATA_AREA_INDEX, mlposIndex);
			agent.messageBlock.addData(MessageXmlConstant.DATA_LONG_LIFE, LongLife);
			agent.messageBlock.setResendOnNoise(false);
			agent.messages.add(agent.messageBlock);
		}
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	/**
	 * check to see if any ignore message should be sent, and send them.
	 * this method was created to solve a problem in which one or more ATs would spend lots of time on a target
	 * with no result ( the agent would die before the completion of rescue operations ).
	 * </br>Now, if a target needs <code>X</code> ATs but <code>Y</code> ATs are assigned to it ( <code>RescueInfo.getNowWorkingOnMe()</code> ) WHERE <b>X>Y</b> and no other ATs are availabe to be assigned to it, those ATs would ignore and abandon the target.
	 * </br><b>This method also manipulates <code>target</code>'s <code>RescueInfo</code></b>.
	 *
	 * @author sinash
	 * @since Wednesday April 3rd, 2013
	 */

	public void sendIgnoreMessages() {

		if (targets.isEmpty()) {
			return;
		}

		for (Human target : targets) {
			if (target.getRescueInfo().getNowWorkingOnMe().size() > 0
					&& target.getRescueInfo().getATneedToBeRescued() > target.getRescueInfo().getNowWorkingOnMe().size()) {

				boolean isInNewAssigns = false;
				for (Map.Entry<AmbulanceTeam, ImaginationShot[]> mapEntry : assign.entrySet()) {

					ImaginationShot[] image = mapEntry.getValue();

					if (image[0] == null || image[0].target == null) {
						continue;
					}

					if (target.equals(image[0].target)) {
						isInNewAssigns = true;
					}

				}
				if (!isInNewAssigns) {

					target.getRescueInfo().setIgnoredUntil(IgnoreReason.WillDie, 1000);
					dclog.debug("S;INA: target [" + target.getID() + "] ignored cause it's gonna die shortly. sending IgnoreMessages...");
					dclog.trace("Sending ignore msg-->" + target.getRescueInfo().getNowWorkingOnMe().toString() + "--> target:" + target + " position:" + target.getPositionArea() + " needs: " + target.getRescueInfo().getATneedToBeRescued() + " dt:" + target.getRescueInfo().getDeathTime());
					abstractlog().trace("Sending ignore msg-->" + target.getRescueInfo().getNowWorkingOnMe().toString() + "--> target:" + target + " position:" + target.getPositionArea() + " needs: " + target.getRescueInfo().getATneedToBeRescued() + " dt:" + target.getRescueInfo().getDeathTime());
					//tell myself if i'm center (rather than sending a message to myself, which is not logical btw :D )
					//age faghat center dare rush kar mikone (khodam centeram)
					if (!(agent instanceof AmbulanceTeamAgent
							&& target.getRescueInfo().getNowWorkingOnMe().contains(self.getID().getValue())
							&& target.getRescueInfo().getNowWorkingOnMe().size() == 1)) {
						//nowWorkingOnMe().size() nemitoone 0 bashe chon age bashe asan nemiad tu in for! :D fuck fuck fuck
						//send ignore message
						agent.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_IGNORED_TARGET);
						agent.messageBlock.addData(MessageXmlConstant.DATA_ID, target.getID().getValue());
						agent.messageBlock.setResendOnNoise(true);
						agent.messages.add(agent.messageBlock);
					}
				}
			}
		}
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	protected void setMinTimeRefuge(Human hm, int[] times) {
		if (hm.getRescueInfo().getRefugeCalculatedTime() == agent.time() || times.length == 0 || agent.model().refuges().isEmpty())
			return;
		int refTimes[] = new int[agent.model().refuges().size()];
		if (times.length > agent.model().refuges().size()) {
			for (int i = 0; i < agent.model().refuges().size(); i++) {
				refTimes[refTimes.length - 1 - i] = times[times.length - 1 - i];
			}
		} else {
			for (int i = 0; i < refTimes.length; i++)
				refTimes[i] = times[i];
		}

		int min = Integer.MAX_VALUE;
		Refuge rf = null;
		for (int i = 0; i < refTimes.length; i++) {
			if (min > refTimes[i]) {
				min = refTimes[i];
				rf = agent.model().refuges().get(i);
			}
		}
		if (rf != null && min < Integer.MAX_VALUE / 2) {
			hm.getRescueInfo().setBestRefuge(rf);
			hm.getRescueInfo().setTimeToRefuge(min);
		}
		hm.getRescueInfo().setRefugeCalculatedTime(agent.time());
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	protected int maxWorkNeedImageIndex(ArrayList<ImaginationShot> list, boolean[] ignored, long priority) {
		ImaginationShot max = null;
		int max_val = 0;

		for (ImaginationShot is : list) {
			if (ignored[is.index] || is.target.getRescueInfo().getRescuePriority() < priority)
				continue;
			int numberOfAtsneedesNow =(is.target.getRescueInfo().getATneedToBeRescued() - is.target.getRescueInfo().getNowWorkingOnMe().size());
			if ( numberOfAtsneedesNow > max_val) {
				max_val = numberOfAtsneedesNow;
				max = is;
			}
			else if (numberOfAtsneedesNow  == max_val) {

				if (max != null && is.target.getBuriedness() > max.target.getBuriedness())
					max = is;
			}
		}
		dclog.debug("Ignoring " + max + " in maxWorkNeedImage");
		return max.index;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public int timeOfReachingBuriedHuman(Human h, Pair<? extends Area, Point2D> from) {
		if (!h.isPositionDefined() || from == null || hugeMap)
			return AVERAGE_MOVE_TO_TARGET;
		return gu.getFoolMoveTimeFromTo(from.first(), h.getPositionArea());
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public int timeOfTakingCivilianToRefuge(Human h, Refuge refuge) {
		if (!h.isPositionDefined() || hugeMap)
			return AVERAGE_MOVE_TO_TARGET;
		if (agent.model().refuges().isEmpty())
			return 1;
		if (refuge == null)
			return AVERAGE_MOVE_TO_TARGET;
		return gu.getFoolMoveTimeFromTo(h.getPositionArea(), refuge);
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public int timeOfTakingCivilianToRefugeFool(Human h, Refuge r) {
		if (agent.model().refuges().isEmpty())
			return 1;
		if (hugeMap)
			return AVERAGE_MOVE_TO_TARGET;
		return gu.getFoolMoveTimeFromTo(h.getPositionArea(), r);
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	protected int minFreeTimeAmbulanceIndex(ArrayList<AmbulanceDream> ADlist, int[][] cost, int index, boolean[] ignored, SOSAgent<?> ag) {

		int minIndex = 0;
		int minValue = Integer.MAX_VALUE;
		for (int i = 0; i < ADlist.size(); i++) {
			if (ignored[i])
				continue;

			AmbulanceDream ad = ADlist.get(i);
			int performFinishTime = ad.time_To_be_free + cost[i][index];

			if (minValue > performFinishTime) {
				minIndex = i;
				minValue = performFinishTime;
			}

		}

		return minIndex;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	protected void assignPriority() {

		if (targets.isEmpty())
			return;

		//sinash 2013 - determination of ready forces
		dclog.logln("SINA: In ready forces determination : ");

		ArrayList<FireBrigade> readyFireBrigades = new ArrayList<FireBrigade>(agent.model().fireBrigades().size()); //sinash
		ArrayList<AmbulanceTeam> readyAmbulanceTeams = new ArrayList<AmbulanceTeam>(agent.model().ambulanceTeams().size()); //siansh
		ArrayList<PoliceForce> readyPoliceForces = new ArrayList<PoliceForce>(agent.model().policeForces().size()); //sinash

		for (FireBrigade fb : agent.model().fireBrigades()) {
			if (fb.isReadyToAct()) {
				readyFireBrigades.add(fb);
			}
		}
		for (PoliceForce pf : agent.model().policeForces()) {
			if (pf.isReadyToAct()) {
				readyPoliceForces.add(pf);
			}
		}
		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.isReadyToAct()) {
				readyAmbulanceTeams.add(at);
			}
		}
		readyFireBrigades.trimToSize();
		readyAmbulanceTeams.trimToSize();
		readyPoliceForces.trimToSize();
		dclog.logln("SINA: readyFireBrigades: " + readyFireBrigades.size() + " readyAmbulanceTeams: " + readyAmbulanceTeams.size() + " readyPoliceForces: " + readyPoliceForces.size());
		// end determination of ready forces

		for (Human hm : targets) {
			switch (getTargetCurrentState(hm)) {
			case CRITICAL: //TODO sinash : inja begim ke harki buriednessesh 0 bood vali damage dasht ( va fireDamage nabood, yani faghat halati ke tu khiaboon unload shode bashe ya rescue shode bashe vali load nashode bashe) bishtarin priority e momken.
				if (hm instanceof Civilian) {
					int priority = 10000000 / (hm.getRescueInfo().getDeathTime() + 1 - agent.time());
					priority = (priority * (320 - hm.getBuriedness() * 2)) / 300;
					if (hm.isBuriednessDefined() && hm.getBuriedness() == 0) {
						if (hm.isDamageDefined() && hm.getDamage() > 0) {
							priority *= 1.5; //added by sinash for final day IranOpen 2013
						}
					}
					hm.getRescueInfo().setRescuePriority(priority);
					//					if (hm.getDamage() != 0 && hm.getBuriedness() == 0)
					//						hm.getRescueInfo().setRescuePriority(1000000);

					if (hm.getRescueInfo().longLife()) {
						hm.getRescueInfo().setRescuePriority(hm.getRescueInfo().getRescuePriority() / 10);
					}
				} else {
					//Added by Ali for distributing rescuing agent
					int priority = agent.time() < 100 ? 10000000 : 1;
					if (hm instanceof FireBrigade)
						priority += (30 - readyFireBrigades.size()) / 5;
					if (hm instanceof AmbulanceTeam)
						priority += (30 - readyAmbulanceTeams.size()) / 5;
					if (hm instanceof PoliceForce)
						priority += (30 - readyPoliceForces.size()) / 5;

					priority += hm.getAgentIndex() % 3;

					hm.getRescueInfo().setRescuePriority(priority);
				}
				break;
			case DEATH:
				hm.getRescueInfo().setRescuePriority(0);
				break;
			}
		}

	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/**
	 * if there is no way from destination to goal returns Integer.maxValue/2
	 * else finding a route and calculate path taking time
	 * using reachability that default is true
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public int timeOfMoving(Pair<? extends Area, Point2D> from, Pair<? extends Area, Point2D> to) {
		if (hugeMap)
			return AVERAGE_MOVE_TO_TARGET;
		ArrayList<Pair<? extends Area, Point2D>> b = new ArrayList<Pair<? extends Area, Point2D>>();
		b.add(to);
		return movingTime(from, b)[0];
	}

	public int timeOfMovingArea(Area from, Area to) {
		if (hugeMap)
			return gu.getFoolMoveTimeFromTo(from, to);

		//		Path p = agent.move.getPathFromTo(Collections.singleton(from), Collections.singleton(to), StandardMove.class);
		//		if (p.isPathSafe()) {
		//			return gu.getFoolMoveTime(p.getLenght());
		//		}
		return -1;
	}

	public int[] movingTime(Pair<? extends Area, Point2D> from, ArrayList<Pair<? extends Area, Point2D>> targets) {
		int[] times = new int[targets.size()];
		if (hugeMap) {
			for (int i = 0; i < times.length; i++) {
				times[i] = gu.getFoolMoveTimeFromTo(from.first(), targets.get(i).first());
			}
			return times;
		}
		long[] lenghts = agent.move.getMMLenToTargets_notImportantPoint(from, targets);
		for (int i = 0; i < times.length; i++) {
			times[i] = (lenghts[i] >= MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING) ? Integer.MAX_VALUE / 2 : gu.getFoolMoveTime(lenghts[i]);
		}
		return times;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public CivilianState getTargetCurrentState(Human hm) {
		//        if (hm.averageTime >= agent.time() && hm.averageTime <= hm.criticalTime)
		//            return CivilianState.HEALTHY;
		//        if (hm.criticalTime >= agent.time() && hm.criticalTime <= hm.deathTime)
		//            return CivilianState.AVERAGE;
		if (hm.getRescueInfo().getDeathTime() >= agent.time())
			return CivilianState.CRITICAL;
		return CivilianState.DEATH;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public CivilianState getTargetDesireState(Human hm, CivilianState current) {
		switch (current) {
		case HEALTHY:
		case AVERAGE:
		case CRITICAL:
			return CivilianState.CRITICAL;

		case DEATH:
		default:
			return CivilianState.DEATH;
		}
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public int taskPerformingDurationForMe(Human target, boolean isPrimary) {
		int cycle = 0;
		try {
			cycle += Math.ceil(target.getBuriedness() / (float) target.getRescueInfo().getATneedToBeRescued());
			cycle += (target.getRescueInfo().getATneedToBeRescued() - 1);
			if (isPrimary && target instanceof Civilian) {
				cycle++; //for loading
				cycle += target.getRescueInfo().getTimeToRefuge();
			}
			//			cycle += timeOfReachingBuriedHuman(hm, position);
			long moveWeight = self.move.getWeightTo(target.getAreaPosition(), StandardMove.class) * MoveConstants.DIVISION_UNIT_FOR_GET;
			cycle += gu.getFoolMoveTime(moveWeight);
		} catch (Exception ex) {
			cycle += target.getBuriedness() + 2;
			cycle += (2 * AVERAGE_MOVE_TO_TARGET);
			ex.printStackTrace();
		}
		return cycle;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	protected int maxValueIndex(ArrayList<Integer> list) {
		int max = 0;
		for (int i = 0; i < list.size(); i++) {
			max = list.get(max) < list.get(i) ? i : max;
		}
		return max;
	}

	protected int maxValueIndex(int[] list) {
		int max = 0;
		for (int i = 0; i < list.length; i++) {
			max = list[max] < list[i] ? i : max;
		}
		if (list[max] == 0)
			return -1;
		return max;
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	public int getATNeedNow(Human hmn, int limitTime) {
		if (hmn instanceof FireBrigade) {
			limitTime = Math.min(limitTime, Math.max(60, agent.time() + 40));
			limitTime = Math.min(limitTime, 100);
		}

		int num = limitTime - (agent.time() + hmn.getRescueInfo().getTimeToRefuge() + 1 + // 1 for load
				AmbulanceUtils.getCommunicationDelay(hmn) + (AVERAGE_MOVE_TO_TARGET - 1)) + 1;//1for unload (2013)
		if (num == 0)
			return 1;
		//		if(!(hmn instanceof Civilian)){
		//			if(hmn.getBuriedness()>30)
		//		}
		//TODO if hum is agent and have more than 30 buriedness we should assign two ambulance
		if (num > 0)
			return (int) Math.floor(hmn.getBuriedness() / num) + 1;
		return 100;
	}

	public void removeTooBuriedCiviliansInStartofSimulation() {
		if (agent.time() < 15) {
			dclog.info("removeing TooBuriedCiviliansInStartofSimulation(20)");
			ArrayList<Human> remove = new ArrayList<Human>();
			for (Human target : targets) {
				if (target instanceof Civilian && target.getBuriedness() > 30)
					remove.add(target);

			}
			dclog.debug("removed civilians because too buridness in start of sim :" + remove);
			targets.removeAll(remove);
			dclog.debug("current targets:" + targets);
		}
	}

	public void assignPriorityToTargets() {
		//*************************************
		assignPriority();
		Collections.sort(targets, new PriorityComparator());
		//*************************************
		if (!SOSConstant.IS_CHALLENGE_RUNNING) {
			dclog.log("PRIORITY::::::");
			for (Human hum : targets) {
				dclog.log("[" + hum + " ->     RescuePriority:" + hum.getRescueInfo().getRescuePriority() + "\tTarget CurrentState " + getTargetCurrentState(hum) + "] ");
			}
			dclog.logln("");
		}
	}

	public void removeTargetsInCenterAssignListOrWorkInfoForSelfAssigning() {
		dclog.info("remove Targets In Center Assign List OrWorkInfo");
		dclog.debug("Center Assign List:" + self.centerAssignLists);
		targets.removeAll(self.centerAssignLists);
		dclog.debug("current targets:" + targets);

		ArrayList<Human> removeTargetsBecauseOtherATGetIt = new ArrayList<Human>();
		for (AmbulanceTeam at : self.model().ambulanceTeams()) {
			if (at.getWork() != null && at.getWork().getTarget() != null) {
				removeTargetsBecauseOtherATGetIt.add(at.getWork().getTarget());
			}
		}

		dclog.debug("removeTargetsBecauseOtherATGetIt:" + removeTargetsBecauseOtherATGetIt);
		targets.removeAll(removeTargetsBecauseOtherATGetIt);
		dclog.debug("current targets:" + targets);

	}

	//***********************************************************************************************************
	//***********************************************************************************************************

	public SOSLoggerSystem abstractlog() {
		if (agent instanceof AmbulanceTeamAgent)
			return ((AmbulanceTeamAgent) agent).abstractlog;
		return ((CenterAgent) agent).abstractlog;
	}

	public void removeOtherClusterTargetsInLowOrNoCommunication() {
		if (agent.messageSystem.type != Type.LowComunication && agent.messageSystem.type != Type.NoComunication)
			return;
		ArrayList<ClusterData> validClusters = new ArrayList<ClusterData>();

		final ClusterData myCluster = self.model().searchWorldModel.getClusterData();
		validClusters.add(myCluster);

		ArrayList<ClusterData> cds = new ArrayList<ClusterData>(self.model().searchWorldModel.getAllClusters());
		Collections.sort(cds, new Comparator<ClusterData>() {

			@Override
			public int compare(ClusterData o1, ClusterData o2) {
				double o1s = SOSGeometryTools.distance(self.me().getX(), self.me().getY(), o1.getX(), o1.getY());
				double o2s = SOSGeometryTools.distance(self.me().getX(), self.me().getY(), o2.getX(), o2.getY());
				if (o1s > o2s)
					return 1;
				if (o1s < o2s)
					return -1;
				return 0;
			}
		});
		if (!cds.isEmpty())
			validClusters.add(cds.get(0));
		dclog.debug("removeOtherClusterTargetsInLowOrNoCommunication:current targets:" + targets);
		ArrayList<Human> newTargets = new ArrayList<Human>();
		for (Human target : targets) {
			if (target.isPositionDefined())
				for (ClusterData validCluster : validClusters) {
					if (validCluster.getBuildings().contains(target.getAreaPosition()))
						newTargets.add(target);
				}
		}
		targets = newTargets;
		dclog.debug("current targets:" + targets);
	}


	public void removeAgentsTargetAfterMiddleOfSimulation() {
		int middletime = 120;
		if (middletime < agent.time())
			return;
		dclog.debug("removeAgentsTargetAfterMiddleOfSimulation::current targets:" + targets);

		targets.removeAll(agent.model().agents());
		dclog.debug("current targets:" + targets);
	}
}