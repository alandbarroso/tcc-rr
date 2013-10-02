package Think;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import Communication.Comm.MESSAGE_ID;
import Communication.RadioChannel;
import Communication.VoiceMessage;
import clustering.Cluster;
import clustering.FireCluster;

public class Think {
	private static final int RANDOM_WALK_LENGTH = 50;

	public static ArrayList<EntityID> appendEntityIDsCopy(
			ArrayList<EntityID> first, ArrayList<EntityID> second) {
		ArrayList<EntityID> append = Think.copyEntityIDsList(first);
		for (EntityID s : second) {
			append.add(s);
		}

		return append;
	}

	public static ArrayList<Building> appendBuildingsCopy(
			ArrayList<Building> first, ArrayList<Building> second) {
		ArrayList<Building> append = Think.copyBuildingsList(first);
		for (Building s : second) {
			append.add(s);
		}

		return append;
	}

	public static ArrayList<Pair<EntityID, TempInt>> appendPairsCopy(
			ArrayList<Pair<EntityID, TempInt>> first,
			ArrayList<Pair<EntityID, TempInt>> second, StandardWorldModel model) {
		ArrayList<Pair<EntityID, TempInt>> append = Think.copyPairsList(model,
				first);
		for (Pair<EntityID, TempInt> s : second) {
			append.add(new Pair<EntityID, TempInt>(s.first(), s.second()));
		}

		return append;
	}

	public static boolean isBlocked(EntityID lastPosition, Human me,
			double lastPositionX, double lastPositionY) {

		return (lastPosition != null
				&& lastPosition.getValue() == me.getPosition().getValue() && Math
				.hypot(Math.abs(me.getX() - lastPositionX),
						Math.abs(me.getY() - lastPositionY)) < 8000);
		// 8000 hypothetical number
	}

	public static boolean isBlocked(EntityID lastPosition, Human me,
			double lastPositionX, double lastPositionY, double threshold) {

		return (lastPosition != null
				&& lastPosition.getValue() == me.getPosition().getValue() && Math
				.hypot(Math.abs(me.getX() - lastPositionX),
						Math.abs(me.getY() - lastPositionY)) < threshold);
	}

	public static int getIndexIfExists(ArrayList<StandardEntity> list,
			EntityID id) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getID().getValue() == id.getValue()) {
				return i;
			}
		}
		return id.getValue();
	}

	// removes current exists in the list, by checking ID value
	// The value is checked as it's common that two EntityIDs have the same
	// value but they're not the same Object
	// When u get an object from the model, u only get a copy from the object,
	// so if u get the same object multiple
	// times, u only get multiple copies of the same object, but they are
	// definitely not the same reference
	public static boolean removeIfExists(EntityID current, List<EntityID> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getValue() == current.getValue()) {
				list.remove(i);
				return true;
			}
		}
		return false;
	}

	public static void removeIfExistsPair(EntityID current,
			ArrayList<Pair<EntityID, TempInt>> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).first().getValue() == current.getValue()) {
				list.remove(i);
				return;
			}

		}
	}

	public static void removeIfExists(AKSpeak message, ArrayList<AKSpeak> list) {
		String content = new String(message.getContent());
		for (int i = 0; i < list.size(); i++) {
			String content2 = new String(list.get(i).getContent());
			if (content.equals(content2)) {
				list.remove(i);
				return;
			}
		}
	}

	public static void removeIfExists(StandardEntity current,
			ArrayList<StandardEntity> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getID().getValue() == current.getID().getValue()) {
				list.remove(i);
				return;
			}

		}
	}

	public static void removeIfExists(Building current, ArrayList<Building> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getID().getValue() == current.getID().getValue()) {
				list.remove(i);
				return;
			}

		}
	}

	public static void removeIfExists(Human current, List<Human> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getID().getValue() == current.getID().getValue()) {
				list.remove(i);
				return;
			}
		}
	}

	public static boolean removeIfExistsBuildingInformation(EntityID current,
			ArrayList<BuildingInformation> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).building.getID().getValue() == current.getValue()) {
				list.remove(i);
				return true;
			}
		}
		return false;
	}

	public static boolean addIfNotExists(Human current, List<Human> list) {
		if (!exists(current, list)) {
			list.add(current);
			return true;
		}
		return false;
	}

	public static boolean addIfNotExists(EntityID current, List<EntityID> list) {
		if (!exists(current, list)) {
			list.add(current);
			return true;
		}
		return false;
	}

	public static boolean addIfNotExists(Building current, List<Building> list) {
		if (!exists(current, list)) {
			list.add(current);
			return true;
		}
		return false;
	}

	public static boolean addIfNotExists(BuildingInformation current,
			List<BuildingInformation> list) {
		if (!exists(current, list)) {
			list.add(current);
			return true;
		}
		return false;
	}

	public static boolean addIfNotExists(AKSpeak message,
			ArrayList<AKSpeak> list) {
		String content = new String(message.getContent());
		for (int i = 0; i < list.size(); i++) {
			String content2 = new String(list.get(i).getContent());
			if (list.get(i).getChannel() == message.getChannel()
					&& content.equals(content2)) {
				return false;
			}
		}
		list.add(message);
		return true;
	}

	public static void addIfNotExists(VoiceMessage message,
			ArrayList<VoiceMessage> list) {
		String content = Think.getMessage(new String(message.getData()));
		int index1 = Think.getIDIndex(content);
		for (VoiceMessage msg : list) {
			String content2 = Think.getMessage(new String(msg.getData()));
			int index2 = Think.getIDIndex(content2);
			if (isFireMessage(message.getMessageType())) {
				if (isFireMessage(msg.getMessageType()) && index1 == index2) {
					if (msg.getMessageType().equals(
							MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID.name())) {
						return;
					} else {
						if (message.getTimeToLive() > msg.getTimeToLive()) {
							msg.setTTL(message.getTimeToLive());
							msg.setMessageType(message.getMessageType());
							msg.setData(message.getData());
							msg.setPriority(message.getPriority());
							return;
						} else {
							return;
						}
					}
				} else {
					continue;
				}
			} else {
				if (content.equals(content2)) {
					if (message.getTimeToLive() > msg.getTimeToLive()) {
						msg.setTTL(message.getTimeToLive());
						msg.setData(message.getData());
						msg.setPriority(message.getPriority());
						return;
					} else {
						return;
					}
				}
			}
		}
		if (message.getTimeToLive() > 0) {
			list.add(message);
			return;
		}
	}

	public static boolean isFireMessage(String message) {
		if (message.equals(MESSAGE_ID.BUILDING_ON_FIRE_MESSAGE_ID.name())
				|| message.equals(MESSAGE_ID.EXTINGUISHED_FIRE_MESSAGE_ID
						.name())
				|| message.equals(MESSAGE_ID.WARM_BUILDING.name())
				|| message.equals(MESSAGE_ID.COLLAPSED_BUILDING_MESSAGE_ID
						.name())) {
			return true;
		} else {
			return false;
		}
	}

	public static byte[] extractMessage(String s) {
		int i;
		for (i = s.length() - 1; i >= 0; i--) {
			if (s.charAt(i) == ',') {
				break;
			}
		}
		return (s.substring(0, i)).getBytes();
	}

	public static String getMessage(String message) {
		String[] split = message.split(",");
		return split[0] + "," + split[1];
	}

	public static int getIDIndex(String message) {
		String[] split = message.split(",");
		return Integer.parseInt(split[1]);
	}

	// checks if this road has a neighbouring building which has only this road
	// as a neigbour
	public static boolean roadOnlyEntranceToBuilding(Road r,
			StandardWorldModel model) {
		for (EntityID b : r.getNeighbours()) {
			Area a = (Area) model.getEntity(b);
			if (a instanceof Building && a.getNeighbours().size() == 1)
				return true;
		}
		return false;
	}

	// checks if target exists in the list
	public static boolean exists(BuildingInformation target,
			List<BuildingInformation> list) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).building.getID().getValue() == target.building
					.getID().getValue())
				return true;
		return false;
	}

	public static boolean exists(Road target, List<Road> list) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).getID().getValue() == target.getID().getValue())
				return true;
		return false;
	}

	public static boolean exists(Blockade blockade, List<Blockade> list) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).getID().getValue() == blockade.getID().getValue())
				return true;
		return false;
	}

	// checks if target exists in the list
	public static boolean exists(EntityID target, List<EntityID> list) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).getValue() == target.getValue())
				return true;
		return false;
	}

	public static boolean existsPair(EntityID target,
			List<Pair<EntityID, TempInt>> list) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).first().getValue() == target.getValue())
				return true;
		return false;
	}

	public static boolean exists(StandardEntity target,
			List<StandardEntity> list) {
		for (StandardEntity en : list)
			if (target.getID().getValue() == en.getID().getValue())
				return true;
		return false;
	}

	public static boolean exists(Building target, List<Building> list) {
		for (StandardEntity en : list)
			if (target.getID().getValue() == en.getID().getValue())
				return true;
		return false;
	}

	public static boolean exists(Human target, List<Human> list) {
		for (StandardEntity en : list)
			if (target.getID().getValue() == en.getID().getValue())
				return true;
		return false;
	}

	public static boolean existsPairNode(EntityID target,
			ArrayList<Pair<EntityID, ArrayList<EntityID>>> list) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).first().getValue() == target.getValue())
				return true;
		return false;
	}

	public static boolean existsBuildingInformation(EntityID target,
			List<BuildingInformation> list) {
		for (BuildingInformation en : list)
			if (target.getValue() == en.building.getID().getValue())
				return true;
		return false;
	}

	public static boolean existsHumanInLocation(EntityID location,
			List<Human> list) {
		for (Human human : list)
			if (location.getValue() == human.getPosition().getValue())
				return true;
		return false;
	}

	// remove a list of targets from another list
	public static void removeEntityIDs(ArrayList<EntityID> list,
			ArrayList<EntityID> targetsList) {
		for (EntityID target : targetsList)
			Think.removeIfExists(target, list);
	}

	public static void removeBuildingsFromEntityIDs(ArrayList<EntityID> list,
			ArrayList<Building> listToBeRemoved) {
		for (Building target : listToBeRemoved)
			Think.removeIfExists(target.getID(), list);
	}

	public static void removeBuildings(ArrayList<Building> list,
			ArrayList<Building> targetsList) {
		for (Building target : targetsList)
			Think.removeIfExists(target, list);
	}

	public static void removeHumansInLocation(EntityID location,
			ArrayList<Human> humans) {
		for (int i = 0; i < humans.size(); i++) {
			if (humans.get(i).getPosition().getValue() == location.getValue()) {
				humans.remove(i--);
			}
		}
	}

	// returns a random path
	public static List<EntityID> randomWalk(EntityID position,
			Map<EntityID, Set<EntityID>> neighbours, Random random) {

		List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
		Set<EntityID> seen = new HashSet<EntityID>();
		EntityID current = position;
		for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
			result.add(current);
			seen.add(current);
			List<EntityID> possible = new ArrayList<EntityID>(
					neighbours.get(current));
			Collections.shuffle(possible, random);
			boolean found = false;
			for (EntityID next : possible) {
				if (seen.contains(next)) {
					continue;
				}
				current = next;
				found = true;
				break;
			}
			if (!found) {
				// We reached a dead-end.
				break;
			}
		}
		// System.out.println(result.toString());
		return result;

	}

	// returns a copy of the list
	public static ArrayList<EntityID> copyList(ArrayList<EntityID> list) {
		ArrayList<EntityID> copyList = new ArrayList<EntityID>();
		for (EntityID en : list)
			copyList.add(en);

		return copyList;
	}

	public static ArrayList<EntityID> copyEntityIDsList(ArrayList<EntityID> list) {
		ArrayList<EntityID> copyList = new ArrayList<EntityID>();
		for (EntityID en : list)
			copyList.add(en);

		return copyList;
	}

	public static ArrayList<Building> copyBuildingsList(ArrayList<Building> list) {
		ArrayList<Building> copyList = new ArrayList<Building>();
		for (Building en : list)
			copyList.add(en);

		return copyList;
	}

	public static Cluster copyCluster(Cluster cluster) {
		Cluster copyCluster = new Cluster(null, cluster.centroid,
				cluster.center, cluster.perc);
		for (EntityID en : cluster.cluster)
			copyCluster.cluster.add(en);

		return copyCluster;
	}

	public static ArrayList<Pair<EntityID, TempInt>> copyPairsList(
			StandardWorldModel model, ArrayList<Pair<EntityID, TempInt>> list) {
		ArrayList<Pair<EntityID, TempInt>> copyList = new ArrayList<Pair<EntityID, TempInt>>();
		for (Pair<EntityID, TempInt> en : list)
			copyList.add(new Pair<EntityID, TempInt>(en.first(), en.second()));

		return copyList;
	}

	public static String convertBinaryToHex(String bin) {
		/*
		 * long num = Long.parseLong(bin); long rem; while (num > 0) { rem = num
		 * % 10; num = num / 10; if (rem != 0 && rem != 1) {
		 * System.out.println("This is not a binary number.");
		 * System.out.println("Please try once again."); System.exit(0); } }
		 */
		long i = Long.parseLong(bin, 2);
		String hexString = Long.toHexString(i);
		return hexString;

	}

	public static String convertBinaryToDecimal(String bin) {
		long num = Long.parseLong(bin);
		long rem;
		while (num > 0) {
			rem = num % 10;
			num = num / 10;
			if (rem != 0 && rem != 1) {
				System.out.println("This is not a binary number.");
				System.out.println("Please try once again.");
				System.exit(0);
			}
		}
		int i = Integer.parseInt(bin, 2);
		return i + "";

	}

	public static String convertHexToBinary(String hex) {
		int i = Integer.parseInt(hex, 16);
		String by = Integer.toBinaryString(i);
		return by;

	}

	public static String convertHexToDecimal(String hex) {
		int i = Integer.parseInt(hex, 16);
		return i + "";

	}

	public static String convertDecimalToBinary(String decimal) {

		return Integer.toBinaryString(Integer.parseInt(decimal));

	}

	public static String convertDecimalToHex(String decimal) {
		return decimal;
		// return Integer.toHexString(Integer.parseInt(decimal));

	}

	public static ArrayList<EntityID> clearedRoads(String binary,
			ArrayList<EntityID> cluster) {

		ArrayList<EntityID> clearedRoads = new ArrayList<EntityID>();
		for (int i = 0; i < binary.length(); i++)
			if (binary.charAt(i) == '1')
				clearedRoads.add(cluster.get(i));

		return clearedRoads;
	}

	public static String binaryClearedRoads(StandardWorldModel model,
			ArrayList<EntityID> remaining, ArrayList<EntityID> cluster) {
		ArrayList<EntityID> copy = Think.copyEntityIDsList(remaining);
		String binaryClearedRoads = "";
		for (EntityID current : cluster) {
			if (copy.size() > 0 && current.getValue() != copy.get(0).getValue()) {

				binaryClearedRoads += "1";
				continue;
			}
			binaryClearedRoads += "0";
		}
		return binaryClearedRoads;
	}

	// returns all buildings in site
	public static ArrayList<Building> getChangedBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> changedBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities())
			if (model.getEntity(ob) instanceof Building)
				changedBuildings.add((Building) model.getEntity(ob));
		return changedBuildings;
	}

	// Takes the world model and the changeset as input and
	// returns a list of changed humans
	public static ArrayList<Human> getChangedHumans(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Human> changedHumans = new ArrayList<Human>();

		for (EntityID entityID : arg1.getChangedEntities())
			if (model.getEntity(entityID) instanceof Human)
				changedHumans.add((Human) model.getEntity(entityID));

		return changedHumans;
	}

	// Takes the world model and the changeset as input and
	// returns a list of changed humans
	public static ArrayList<Human> getChangedCivilians(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Human> changedHumans = new ArrayList<Human>();

		for (EntityID entityID : arg1.getChangedEntities())
			if (model.getEntity(entityID) instanceof Civilian)
				changedHumans.add((Human) model.getEntity(entityID));

		return changedHumans;
	}

	public static ArrayList<EntityID> getChangedCiviliansEntityID(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<EntityID> changedHumans = new ArrayList<EntityID>();

		for (EntityID entityID : arg1.getChangedEntities())
			if (model.getEntity(entityID) instanceof Civilian)
				changedHumans.add(entityID);

		return changedHumans;
	}

	// returns all collapsed buildings in site, all collapsed buildings have
	// fieryness 8
	public static ArrayList<Building> getCollapsedBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> collapsedBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);
			if (b instanceof Building && ((Building) b).getFieryness() == 8)
				collapsedBuildings.add((Building) b);
		}
		return collapsedBuildings;
	}

	public static ArrayList<Road> getClearedRoadsNew(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Road> clearRoads = new ArrayList<Road>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity r = model.getEntity(ob);
			if (r instanceof Road && ((Road) r).isBlockadesDefined()) {
				if (isClearedRoad((Road) r, model))
					clearRoads.add((Road) r);
			}
		}
		return clearRoads;
	}

	public static boolean isClearedRoad(Road road, StandardWorldModel model) {
		if (road.getBlockades().size() == 0)
			return true;

		boolean isBlocked = false;
		for (EntityID b : road.getBlockades()) {
			StandardEntity entity = model.getEntity(b);
			if (entity != null
					&& Think.isBlockingRoad(model, (Blockade) entity)) {
				isBlocked = true;
				break;
			}
		}
		return (!isBlocked);
	}

	// buildings on fire
	public static ArrayList<Building> getBurningBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> burningBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);
			if (b instanceof Building && ((Building) b).isOnFire())
				burningBuildings.add((Building) b);
		}
		return burningBuildings;
	}

	// buildings with temperatures more than zero
	public static ArrayList<Building> getBuildingsWarm(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> burningBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);
			if (b instanceof Building && ((Building) b).getTemperature() > 0)
				burningBuildings.add((Building) b);
		}
		return burningBuildings;
	}

	// buildings with temperatures more than zero but not onFire
	public static ArrayList<Building> getBuildingsTempNotZeroNotOnFire(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> burningBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);
			if (b instanceof Building && ((Building) b).getTemperature() > 0
					&& !((Building) b).isOnFire())
				burningBuildings.add((Building) b);
		}
		return burningBuildings;
	}

	// buildings not on fire
	public static ArrayList<Building> getNotBurningBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> burningBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);

			if (b instanceof Building && !((Building) b).isOnFire())
				burningBuildings.add((Building) b);
		}
		return burningBuildings;
	}

	/**
	 * Author: Noha Khater
	 * 
	 * returns a list of the civilians that need help either buried or injured
	 * 
	 */
	public static ArrayList<EntityID> getSeenInjuredOrBuriedCiviliansIDs(
			ChangeSet changeset, StandardWorldModel model) {
		ArrayList<EntityID> injuredOrBuriedCivilians = new ArrayList<EntityID>();
		for (EntityID eID : changeset.getChangedEntities()) {
			StandardEntity c = model.getEntity(eID);
			if (c instanceof Civilian) {
				Civilian civilian = (Civilian) c;
				if ((civilian.isBuriednessDefined() && civilian.getBuriedness() > 1)
						|| (civilian.isDamageDefined() && civilian.getDamage() > 1)) {
					injuredOrBuriedCivilians.add(eID);
				}
			}
		}
		return injuredOrBuriedCivilians;
	}

	public static ArrayList<Building> getNotCollapsedBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> collapsedBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);

			if (b instanceof Building && !(((Building) b).getFieryness() == 8))
				collapsedBuildings.add((Building) b);
		}
		return collapsedBuildings;
	}

	public static ArrayList<Building> getNotCollapsedNotBurningBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> collapsedBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);

			if (b instanceof Building && !(((Building) b).getFieryness() == 8)
					&& !((Building) b).isOnFire())
				collapsedBuildings.add((Building) b);
		}
		return collapsedBuildings;
	}

	public static ArrayList<Building> getCollapsedOrBurningBuildings(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<Building> collapsedBuildings = new ArrayList<Building>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);

			if (b instanceof Building
					&& (((Building) b).getFieryness() == 8 || ((Building) b)
							.isOnFire()))
				collapsedBuildings.add((Building) b);
		}
		return collapsedBuildings;
	}

	public static ArrayList<EntityID> getCollapsedOrBurningEntityIDs(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<EntityID> collapsedorBurningBuildings = new ArrayList<EntityID>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity b = model.getEntity(ob);
			if (b instanceof Building
					&& (((Building) b).getFieryness() == 8 || ((Building) b)
							.isOnFire()))
				collapsedorBurningBuildings.add(ob);
		}
		return collapsedorBurningBuildings;
	}

	// damaged Human in site
	public static ArrayList<Human> getDamagedHuman(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Human> changedHuman = new ArrayList<Human>();
		for (EntityID ob : arg1.getChangedEntities()) {
			if (model.getEntity(ob) instanceof Human) {
				Human h = (Human) model.getEntity(ob);
				if (h.isHPDefined() && h.isBuriednessDefined()
						&& h.isDamageDefined() && h.isPositionDefined()
						&& h.getHP() > 0 && (h.getDamage() > 0)) {
					changedHuman.add(h);
				}
			}
		}
		return changedHuman;
	}

	// buried Human in site
	public static ArrayList<Human> getBuriedHumans(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Human> buriedHumans = new ArrayList<Human>();

		for (EntityID entityID : arg1.getChangedEntities()) {
			if (model.getEntity(entityID) instanceof Human) {
				Human human = (Human) model.getEntity(entityID);

				if (human.isHPDefined() && human.isBuriednessDefined()
						&& human.isDamageDefined() && human.isPositionDefined()
						&& human.getHP() > 0 && (human.getBuriedness() > 0))
					buriedHumans.add(human);

			}
		}

		return buriedHumans;
	}

	// returns a list of injured humans
	public static ArrayList<Human> getInjuredHumans(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Human> injuredHumans = new ArrayList<Human>();

		for (EntityID entityID : arg1.getChangedEntities()) {
			if (model.getEntity(entityID) instanceof Human) {
				Human human = (Human) model.getEntity(entityID);

				if (human.isHPDefined() && human.isBuriednessDefined()
						&& human.isDamageDefined() && human.isPositionDefined()
						&& human.getHP() > 0 && human.getDamage() > 0)
					injuredHumans.add(human);
			}
		}

		return injuredHumans;
	}

	// prints ArrayList
	public static void printArrayListStandardEntity(
			Collection<StandardEntity> collection) {
		System.out.println("ArrayStandardEntity:");
		if (collection != null)
			for (StandardEntity entry : collection) {
				System.out.print(entry.getID() + " " + entry.getURN() + ", ");
			}
		System.out.println();
	}

	// convert ArrayList to String
	public static String ArrayListAreatoString(List<Area> array) {
		String res = "ArrayAreas:";
		if (array != null)
			for (Area entry : array) {
				res += entry.getID() + ", ";
			}
		res += "\n";
		return res;
	}

	public static String ArrayListRoadtoString(List<Road> array) {
		String res = "ArrayRoads:";
		if (array != null)
			for (Road entry : array) {
				res += entry.getID() + ", ";
			}
		res += "\n";
		return res;
	}

	// convert ArrayList of EntityIDs to String
	public static String ArrayListBuildingtoString(List<Building> array) {
		String res = "ArrayBuildings:";
		if (array != null)
			for (Building entry : array) {
				res += entry.getID() + ", ";
			}
		res += "\n";
		return res;
	}

	// convert ArrayList of EntityIDs to String

	public static String ArrayListEntityIDtoString(List<EntityID> array) {
		String res = "ArrayEntityIDs:";
		if (array != null)
			for (EntityID entry : array) {
				res += entry + ", ";
			}
		res += "\n";
		return res;
	}

	// convert ArrayList of humans to String
	public static String ArrayListHumanstoString(List<Human> array) {
		String res = "ArrayHumans:";
		if (array != null)
			for (Human entry : array) {
				res += entry.getID().getValue() + ", ";
			}
		res += "\n";
		return res;
	}

	// prints ArrayList
	public static void printArrayListEntityID(List<EntityID> array) {
		System.out.println("ArrayEntityIDs:");
		if (array != null)
			for (EntityID entry : array) {
				System.out.print(entry + ", ");
			}
		System.out.println();
	}

	public static void printArrayListHumans(List<Human> array) {
		System.out.println("ArrayHUmanIDs:");
		if (array != null)
			for (Human entry : array) {
				System.out.print(entry.getID() + ", ");
			}
		System.out.println();
	}

	public static void printArrayListBuilding(ArrayList<Building> array) {
		System.out.println("ArrayBuildings:");
		if (array != null)

			for (Building entry : array) {
				System.out.print(entry + ", ");
			}
		System.out.println();
	}

	public static void printArrayListPair(
			ArrayList<Pair<EntityID, TempInt>> array) {
		if (!array.isEmpty()) {
			System.out.print("ArrayPair:");
			for (Pair<EntityID, TempInt> entry : array) {
				System.out.print("(" + entry.first() + ", "
						+ entry.second().timeStamp + ") ,");
			}
			System.out.println();
		}
	}

	public static ArrayList<EntityID> getClusterBuildingsEntrance(
			StandardWorldModel model, Cluster cluster) {
		ArrayList<EntityID> buildingsEntrance = new ArrayList<EntityID>();
		for (EntityID roadID : cluster.cluster) {
			Road road = (Road) model.getEntity(roadID);
			if (isBuildingEntrance(model, road)) {
				buildingsEntrance.add(roadID);
			}
		}
		return buildingsEntrance;
	}

	public static boolean isBuildingEntrance(StandardWorldModel model, Road road) {
		for (EntityID e : road.getNeighbours()) {
			if (model.getEntity(e) instanceof Building) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNeighbourToBuildingEntrance(
			StandardWorldModel model, Road road) {
		for (EntityID e : road.getNeighbours()) {
			if (model.getEntity(e) instanceof Road
					&& isBuildingEntrance(model, (Road) model.getEntity(e))) {
				return true;
			}
		}
		return false;
	}

	public static List<EntityID> getBuildingEntrance(StandardWorldModel model,
			Building b) {
		List<EntityID> entrances = new ArrayList<EntityID>();
		for (EntityID a : b.getNeighbours()) {
			if (model.getEntity(a) instanceof Road)
				entrances.add(a);
		}
		return entrances;
	}

	public static int getEuclidianDistance(double x1, double y1, double x2,
			double y2) {
		return (int) Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
	}

	public static int getEuclidianDistance(Area a, Area b) {
		return (int) Math.sqrt(Math.pow((double) (a.getX() - b.getX()), 2.0)
				+ Math.pow((double) (a.getY() - b.getY()), 2.0));
	}

	public static int getEuclidianDistance(Area a, Human b) {
		return (int) Math.sqrt(Math.pow((double) (a.getX() - b.getX()), 2.0)
				+ Math.pow((double) (a.getY() - b.getY()), 2.0));
	}

	public static int getEuclidianDistance(Human a, Human b) {
		return (int) Math.sqrt(Math.pow((double) (a.getX() - b.getX()), 2.0)
				+ Math.pow((double) (a.getY() - b.getY()), 2.0));
	}

	public static int getEuclidianDistance(Human a, Area b) {
		return (int) Math.sqrt(Math.pow((double) (a.getX() - b.getX()), 2.0)
				+ Math.pow((double) (a.getY() - b.getY()), 2.0));
	}

	public static Area getStandardEntityPosition(StandardEntity a,
			StandardWorldModel model) {
		if (a instanceof Area)
			return (Area) a;
		else
			return (Area) model.getEntity(((Human) a).getPosition());
	}

	// returns a new list with the entityIDs of all buildings
	public static ArrayList<EntityID> BuildingsToEntityIDs(
			ArrayList<Building> buildings) {
		ArrayList<EntityID> IDs = new ArrayList<EntityID>();
		for (Building building : buildings)
			IDs.add(building.getID());

		return IDs;
	}

	// returns a new list with the buildings of all entityIDs
	public static ArrayList<Building> EntityIDsToBuildings(
			ArrayList<EntityID> IDs, StandardWorldModel model) {
		ArrayList<Building> buildings = new ArrayList<Building>();
		for (EntityID ID : IDs)
			buildings.add((Building) model.getEntity(ID));

		return buildings;
	}

	public static ArrayList<EntityID> PairToEntityIDs(
			ArrayList<Pair<EntityID, TempInt>> IDs, StandardWorldModel model) {
		ArrayList<EntityID> buildings = new ArrayList<EntityID>();
		for (Pair<EntityID, TempInt> pair : IDs)
			buildings.add((EntityID) pair.first());
		return buildings;
	}

	public static int indexOfAgentID(ArrayList<EntityID> agentsIDs, EntityID id) {
		for (int i = 0; i < agentsIDs.size(); i++) {
			if (agentsIDs.get(i).getValue() == id.getValue())
				return i;
		}
		return -1;
	}

	public static ArrayList<EntityID> AmbulanceAgentsInSameBuilding(
			StandardWorldModel model, ChangeSet arg1, EntityID me, EntityID pos) {
		ArrayList<EntityID> ambulanceAgents = new ArrayList<EntityID>();

		for (Human human : getChangedHumans(model, arg1)) {
			if (human.getStandardURN().equals(StandardEntityURN.AMBULANCE_TEAM)) {
				if (!(human.getID().getValue() == me.getValue())) {
					if (((rescuecore2.standard.entities.AmbulanceTeam) model
							.getEntity(human.getID())).getPosition().getValue() == pos
							.getValue()) {
						if (((rescuecore2.standard.entities.AmbulanceTeam) model
								.getEntity(human.getID())).getBuriedness() == 0) {
							ambulanceAgents.add(human.getID());
						}
					}
				}
			}
		}

		return ambulanceAgents;
	}

	public static ArrayList<EntityID> FireBrigadesInSite(
			StandardWorldModel model, ChangeSet arg1) {
		ArrayList<EntityID> fireBrigades = new ArrayList<EntityID>();

		for (Human human : getChangedHumans(model, arg1))
			if (human.getStandardURN().equals(StandardEntityURN.FIRE_BRIGADE))
				fireBrigades.add(human.getID());

		return fireBrigades;
	}

	// if there's any EntityID in the list higher than me, should be used to
	// help in deciding which one should load the civilian
	public static boolean isRescuer(ArrayList<EntityID> ambInB, EntityID me) {
		boolean ret = true;
		for (EntityID x : ambInB) {
			if (x.getValue() > me.getValue()) {
				ret = false;
			}
		}
		return ret;
	}

	public static void removeFromArrayList(ArrayList<EntityID> list, EntityID id) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getValue() == id.getValue()) {
				list.remove(i);
				return;
			}
		}
	}

	public static double findDistance(Blockade b, int x, int y) {
		return Math.sqrt(Math.pow(b.getX() - x, 2) + Math.pow(b.getY() - y, 2));
	}

	public static int findDistanceTo(Blockade b, int x, int y) {
		List<Line2D> lines = GeometryTools2D.pointsToLines(
				GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D origin = new Point2D(x, y);
		for (Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next,
					origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			if (d < best) {
				best = d;
			}

		}
		return (int) best;
	}

	public static Building getLowestTemperatureBuilding(Collection<Building> all) {
		Building sameTarget = null;
		for (Building next : all)
			if (sameTarget == null
					|| sameTarget.getTemperature() > next.getTemperature())
				sameTarget = next;
		return sameTarget;
	}

	public static Building getHighestTemperatureBuilding(
			Collection<Building> all) {
		Building sameTarget = null;
		for (Building next : all)
			if (sameTarget == null
					|| sameTarget.getTemperature() < next.getTemperature())
				sameTarget = next;
		return sameTarget;
	}

	public static Building getLowestFierynessBuilding(Collection<Building> all) {
		Building sameTarget = null;
		for (Building next : all) {
			int fiS = 9, fiN = 9;
			if (sameTarget != null && sameTarget.isFierynessDefined())
				fiS = sameTarget.getFieryness();
			if (next.isFierynessDefined())
				fiN = next.getFieryness();

			if (fiS > fiN)
				sameTarget = next;
		}
		return sameTarget;
	}

	public static Building getHighestFierynessBuilding(Collection<Building> all) {
		Building sameTarget = null;
		for (Building next : all) {
			int fiS = 9, fiN = 9;
			if (sameTarget != null && sameTarget.isFierynessDefined())
				fiS = sameTarget.getFieryness();
			if (next.isFierynessDefined())
				fiN = next.getFieryness();

			if (fiS < fiN)
				sameTarget = next;

		}
		return sameTarget;
	}

	// returns a new sorted list according to fieryness
	public static ArrayList<Building> SortLowestFierynessBuilding(
			Collection<Building> all) {
		Building sameTarget = null;
		ArrayList<Building> sorted = new ArrayList<Building>();
		int length = all.size();
		for (int i = 0; i < length && all.size() > 0; i++) {
			for (Building next : all) {
				int fiS = 9, fiN = 9;
				if (sameTarget == null)
					sameTarget = next;
				if (sameTarget != null && sameTarget.isFierynessDefined())
					fiS = sameTarget.getFieryness();
				if (next.isFierynessDefined())
					fiN = next.getFieryness();

				if (fiS > fiN)
					sameTarget = next;
			}
			if (sameTarget == null)
				System.out.println("target is null");

			if (sameTarget != null) {
				System.out.println("target is not null");
				sorted.add(sameTarget);
				all.remove(sameTarget);
			}
		}
		return sorted;
	}

	public static ArrayList<Building> buildingsInRangeBuildings(
			ArrayList<Building> buildings, StandardWorldModel model, Human me,
			int maxDistance) {
		ArrayList<Building> inRange = new ArrayList<Building>();
		for (Building b : buildings)
			if (model.getDistance(b, me) <= maxDistance)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<EntityID> buildingsInRangeEntityIDs(
			ArrayList<EntityID> buildings, StandardWorldModel model, Human me,
			int maxDistance) {
		ArrayList<EntityID> inRange = new ArrayList<EntityID>();
		for (EntityID b : buildings)
			if (model.getDistance(b, me.getID()) <= maxDistance)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<Building> buildingsOutofRangeBuildings(
			ArrayList<Building> buildings, StandardWorldModel model, Human me,
			int maxDistance) {
		ArrayList<Building> inRange = new ArrayList<Building>();
		for (Building b : buildings)
			if (model.getDistance(b, me) > maxDistance)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<EntityID> buildingsInRangeEntityIDs(
			ArrayList<EntityID> buildings, StandardWorldModel model,
			EntityID me, int maxDistance) {
		ArrayList<EntityID> inRange = new ArrayList<EntityID>();
		for (EntityID b : buildings)
			if (model.getDistance(b, me) <= maxDistance)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<Building> buildingsInSpecificRangeBuildings(
			ArrayList<Building> buildings, double range, Building center,
			StandardWorldModel model) {
		ArrayList<Building> inRange = new ArrayList<Building>();
		for (Building b : buildings)
			if (model.getDistance(b, center) <= range)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<EntityID> buildingsInSpecificRangeEntityIDs(
			ArrayList<EntityID> buildings, double range, EntityID center,
			StandardWorldModel model) {
		ArrayList<EntityID> inRange = new ArrayList<EntityID>();
		for (EntityID b : buildings)
			if (model.getDistance(b, center) <= range)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<EntityID> buildingsInSpecificRangeCluster(
			Cluster buildings, double range, EntityID center,
			StandardWorldModel model) {
		ArrayList<EntityID> inRange = new ArrayList<EntityID>();
		for (EntityID b : buildings.cluster)
			if (model.getDistance(b, center) <= range)
				inRange.add(b);

		return inRange;
	}

	public static ArrayList<Building> buildingsNotInRangeBuildings(
			ArrayList<Building> buildings, StandardWorldModel model,
			EntityID me, int maxDistance) {
		ArrayList<Building> inRange = new ArrayList<Building>();
		for (Building b : inRange)
			if (model.getDistance(b.getID(), me) > maxDistance)
				buildings.add(b);

		return inRange;
	}

	public static ArrayList<EntityID> buildingsNotInRangeEntityIDs(
			ArrayList<EntityID> buildings, StandardWorldModel model,
			EntityID me, int maxDistance) {
		ArrayList<EntityID> inRange = new ArrayList<EntityID>();
		for (EntityID b : inRange)
			if (model.getDistance(b, me) > maxDistance)
				buildings.add(b);

		return inRange;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static int getOrderAmongFireBrigadesInsite(ChangeSet arg1,
			StandardWorldModel model, EntityID me) {
		int order = 0;
		for (EntityID agent : Think.FireBrigadesInSite(model, arg1)) {
			if (agent.getValue() < me.getValue())
				order++;
		}
		return order;
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// updater map

	public static void addNodeToRemovedNodes(EntityID ne,
			ArrayList<EntityID> removedNodes) {
		removedNodes.add(ne);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void decrementPair(ArrayList<Pair<EntityID, TempInt>> list) {

		for (int i = 0; i < list.size(); i++) {

			((TempInt) list.get(i).second()).timeStamp--;
			if (((TempInt) list.get(i).second()).timeStamp < 1) {
				System.out.println("removed " + (EntityID) list.get(i).first());
				list.remove(i);
				i--;

			}
		}
	}

	/**
	 * combines 2 4 bit numbers into one single string of 8 bits
	 */
	public static String compressNumbers(int n1, int n2) {
		return "" + (n1 | (n2 << 4));
	}

	/**
	 * splits a string into 2 different 4-bit numbers (reversal of
	 * compressNumbers)
	 */
	public static int[] decodeNumbers(String s) {
		int n = Integer.parseInt(s);
		int[] list = new int[2];
		list[0] = n & 00001111;
		list[1] = (n & 11110000) >> 4;
		return list;
	}

	public static ArrayList<Road> TotallyBlockedRoads(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Road> blockedRoads = new ArrayList<Road>();
		ArrayList<Blockade> roadBlockades = new ArrayList<Blockade>();
		for (EntityID ob : arg1.getChangedEntities()) {
			StandardEntity r = model.getEntity(ob);
			if (r instanceof Road && (((Road) r).getBlockades()) != null
					&& (((Road) r).getBlockades()).size() > 0) {
				roadBlockades = getBlockades(model, arg1);
				if (isBlocked(((Road) r), roadBlockades))
					blockedRoads.add(((Road) r));
			}
		}
		return blockedRoads;
	}

	public static boolean isBlocked(Road road, ArrayList<Blockade> blockades) {
		List<rescuecore2.standard.entities.Edge> roadEdges = road.getEdges();
		// road doesn't have any blockades so it's definately not blocked
		if (blockades.size() == 0) {
			return false;
		}
		// road has 1 blockade, check if all the road verticies are there
		if (blockades.size() == 1) {
			for (Point2D roadVertex : getRoadVertices(road)) {
				boolean flag = false;
				for (Point2D blockadeVertex : getBlockadeVertices(blockades
						.get(0))) {
					if (roadVertex.getX() == blockadeVertex.getX()
							&& roadVertex.getY() == blockadeVertex.getY()) {
						// road vertex found
						flag = true;
						break;
					}
				}
				// road vertex not found
				if (!flag)
					return false;
			}
			// all road vertices were found so the road is blocked
			return true;
		}
		for (rescuecore2.standard.entities.Edge roadEdge : roadEdges) {
			if (roadEdge.isPassable()) {
				boolean flag = false;
				for (Blockade blockade : blockades) {
					for (rescuecore2.standard.entities.Edge blockadeEdge : getBlockadeEdges(blockade)) {
						if ((roadEdge.getStartX() == blockadeEdge.getStartX()
								&& roadEdge.getEndX() == blockadeEdge.getEndX()
								&& roadEdge.getStartY() == blockadeEdge
										.getStartY() && roadEdge.getEndY() == blockadeEdge
								.getEndY())
								|| (roadEdge.getStartX() == blockadeEdge
										.getEndX()
										&& roadEdge.getEndX() == blockadeEdge
												.getStartX()
										&& roadEdge.getStartY() == blockadeEdge
												.getEndY() && roadEdge
										.getEndY() == blockadeEdge.getStartY())) {
							flag = true;
							break;
						}
					}
					if (flag)
						break;
				}
				if (!flag)
					return false;
			}
		}
		return true;
	}

	public static ArrayList<rescuecore2.standard.entities.Edge> getBlockadeEdges(
			Blockade blockade) {
		ArrayList<rescuecore2.standard.entities.Edge> apexesList = new ArrayList<rescuecore2.standard.entities.Edge>();
		for (int i = 0; i < blockade.getApexes().length - 3; i = i + 2) {
			Point2D start = new Point2D(blockade.getApexes()[i],
					blockade.getApexes()[i + 1]);
			Point2D end = new Point2D(blockade.getApexes()[i + 2],
					blockade.getApexes()[i + 3]);
			rescuecore2.standard.entities.Edge e = new rescuecore2.standard.entities.Edge(
					start, end);
			apexesList.add(e);
		}
		Point2D start = new Point2D(
				blockade.getApexes()[blockade.getApexes().length - 2],
				blockade.getApexes()[blockade.getApexes().length - 1]);
		Point2D end = new Point2D(blockade.getApexes()[0],
				blockade.getApexes()[1]);
		apexesList.add(new rescuecore2.standard.entities.Edge(start, end));
		return apexesList;
	}

	public static ArrayList<Point2D> getBlockadeVertices(Blockade blockade) {
		ArrayList<Point2D> apexesList = new ArrayList<Point2D>();
		for (int i = 0; i < blockade.getApexes().length - 1; i = i + 2) {
			Point2D p = new Point2D(blockade.getApexes()[i],
					blockade.getApexes()[i + 1]);
			apexesList.add(p);
		}
		return apexesList;
	}

	public static ArrayList<Point2D> getRoadVertices(Road road) {
		ArrayList<Point2D> apexesList = new ArrayList<Point2D>();
		for (int i = 0; i < road.getApexList().length - 1; i = i + 2) {
			Point2D p = new Point2D(road.getApexList()[i],
					road.getApexList()[i + 1]);
			apexesList.add(p);
		}
		return apexesList;
	}

	public static ArrayList<Blockade> getBlockades(StandardWorldModel model,
			ChangeSet arg1) {
		ArrayList<Blockade> blockades = new ArrayList<Blockade>();
		StandardEntity entity;
		for (EntityID ob : arg1.getChangedEntities()) {
			entity = model.getEntity(ob);
			if (entity instanceof Blockade)
				blockades.add((Blockade) entity);
		}
		return blockades;
	}

	public static List<rescuecore2.standard.entities.Edge> getImpassableEdges(
			Road road) {
		List<rescuecore2.standard.entities.Edge> impassable = new ArrayList<rescuecore2.standard.entities.Edge>();
		for (rescuecore2.standard.entities.Edge e : road.getEdges())
			if (!e.isPassable())
				impassable.add(e);
		return impassable;
	}

	public static boolean isBlockingRoad(StandardWorldModel model,
			Blockade blockade) {
		Road road = (Road) model.getEntity(blockade.getPosition());
		int[] blockadeVertices = blockade.getApexes();
		for (Edge roadEdge : road.getEdges()) {
			if (roadEdge.isPassable()) {
				for (int i = 0; i < blockadeVertices.length; i += 2) {
					if (!openRoadEdge(blockade.getID(), model, road, roadEdge,
							blockadeVertices)) {
						return true;
					}
				}
			}
		}
		List<EntityID> otherBlockades = road.getBlockades();
		if (otherBlockades != null && otherBlockades.size() > 1) {
			for (EntityID otherBlockadeID : otherBlockades) {
				if (otherBlockadeID.getValue() != blockade.getID().getValue()) {
					StandardEntity entity = model.getEntity(otherBlockadeID);
					if (entity == null)
						continue;
					int[] otherBlockadeVertices = ((Blockade) entity)
							.getApexes();
					for (int j = 0; j < otherBlockadeVertices.length; j += 2) {
						for (int i = 0; i < blockadeVertices.length; i += 2) {
							Line2D blockadeLine = new Line2D(new Point2D(
									blockadeVertices[i],
									blockadeVertices[i + 1]), new Point2D(
									blockadeVertices[(i + 2)
											% blockadeVertices.length],
									blockadeVertices[(i + 3)
											% blockadeVertices.length]));
							Point2D closestPt = GeometryTools2D
									.getClosestPointOnSegment(
											blockadeLine,
											new Point2D(
													otherBlockadeVertices[j],
													otherBlockadeVertices[j + 1]));
							double distance = GeometryTools2D.getDistance(
									closestPt, new Point2D(
											otherBlockadeVertices[j],
											otherBlockadeVertices[j + 1]));
							if (distance <= 2000) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean openRoadEdge(EntityID blockadeID,
			StandardWorldModel model, Road road, Edge roadEdge,
			int[] blockadeVertices) {
		double minDistanceStart = Double.POSITIVE_INFINITY;
		double minDistanceEnd = Double.POSITIVE_INFINITY;
		Line2D roadEdgeLine = roadEdge.getLine();
		ArrayList<Point2D> projectedPoints = new ArrayList<Point2D>();

		for (int i = 0; i < blockadeVertices.length; i += 2) {
			Point2D point = GeometryTools2D.getClosestPointOnSegment(
					roadEdgeLine, new Point2D(blockadeVertices[i],
							blockadeVertices[i + 1]));
			if (!projectedPoints.contains(point)) {
				projectedPoints.add(point);
			}
		}
		if (projectedPoints.size() < 2)
			return true;
		for (Point2D projectedPoint : projectedPoints) {
			double dStart = GeometryTools2D.getDistance(projectedPoint,
					roadEdge.getStart());
			if (dStart < minDistanceStart) {
				minDistanceStart = dStart;
			}
			double dEnd = GeometryTools2D.getDistance(projectedPoint,
					roadEdge.getEnd());
			if (dEnd < minDistanceEnd) {
				minDistanceEnd = dEnd;
			}
		}
		if (minDistanceEnd > 2000 || minDistanceStart > 2000) {
			Edge neighbourEdge = getNeighbourEdge(model, road, roadEdge);
			Line2D neighbourEdgeLine = neighbourEdge.getLine();
			projectedPoints = new ArrayList<Point2D>();
			for (int i = 0; i < blockadeVertices.length; i += 2) {
				Point2D point = GeometryTools2D.getClosestPointOnSegment(
						neighbourEdgeLine, new Point2D(blockadeVertices[i],
								blockadeVertices[i + 1]));
				if (!projectedPoints.contains(point)) {
					projectedPoints.add(point);
				}
			}
			if (projectedPoints.size() < 2)
				return true;

			minDistanceStart = Double.POSITIVE_INFINITY;
			minDistanceEnd = Double.POSITIVE_INFINITY;
			for (Point2D projectedPoint : projectedPoints) {
				double dStart = GeometryTools2D.getDistance(projectedPoint,
						neighbourEdge.getStart());
				if (dStart < minDistanceStart) {
					minDistanceStart = dStart;
				}

				double dEnd = GeometryTools2D.getDistance(projectedPoint,
						neighbourEdge.getEnd());
				if (dEnd < minDistanceEnd) {
					minDistanceEnd = dEnd;
				}
			}
			if (minDistanceEnd > 2000 || minDistanceStart > 2000) {
				return true;
			}
		}
		return false;
	}

	public static Edge getNeighbourEdge(StandardWorldModel model, Road road,
			Edge roadEdge) {
		Area neighbour = (Area) model.getEntity(roadEdge.getNeighbour());
		for (Edge neighbourEdge : neighbour.getEdges()) {
			if (neighbourEdge.isPassable()
					&& neighbourEdge.getNeighbour().getValue() == road.getID()
							.getValue())
				return neighbourEdge;
		}
		return null;
	}

	public static boolean stuckInBlockade(StandardWorldModel model,
			ChangeSet arg1, int positionX, int positionY) {
		boolean flag = false;
		for (EntityID ob : arg1.getChangedEntities())
			if (model.getEntity(ob) instanceof Blockade) {
				ArrayList<Integer> X = new ArrayList<Integer>();
				ArrayList<Integer> Y = new ArrayList<Integer>();
				for (int i = 0; i < ((Blockade) (model.getEntity(ob)))
						.getApexes().length - 1; i = i + 2) {
					X.add(((Blockade) (model.getEntity(ob))).getApexes()[i]);
					Y.add(((Blockade) (model.getEntity(ob))).getApexes()[i + 1]);
				}
				flag = in_or_out_of_polygon(X, Y, positionX, positionY);
				if (flag)
					return true;
			}
		return flag;
	}

	public static boolean isHumanStuckInBlockade(StandardWorldModel model,
			ChangeSet changeSet, Blockade blockade) {
		ArrayList<Integer> X = new ArrayList<Integer>();
		ArrayList<Integer> Y = new ArrayList<Integer>();
		for (int i = 0; i < blockade.getApexes().length - 1; i = i + 2) {
			X.add(blockade.getApexes()[i]);
			Y.add(blockade.getApexes()[i + 1]);
		}
		for (Human human : getHumansInRange(model, changeSet,
				blockade.getPosition()))
			if (in_or_out_of_polygon(X, Y, human.getX(), human.getY())) {
				return true;
			}
		return false;
	}

	public static ArrayList<Human> getHumansInRange(StandardWorldModel model,
			ChangeSet arg1, EntityID road) {
		ArrayList<Human> changedHumans = new ArrayList<Human>();

		for (EntityID entityID : arg1.getChangedEntities()) {
			StandardEntity entity = model.getEntity(entityID);
			if (entity instanceof Human
					&& ((Human) entity).getPosition().equals(road))
				changedHumans.add((Human) entity);
		}
		return changedHumans;
	}

	/***
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return given 2 points returns the equation of their line
	 */
	public static Double[] lineEqu(double x1, double y1, double x2, double y2) {
		Double[] equ = new Double[2];
		if (x2 == x1) {
			equ[0] = 0.0;
			equ[1] = 0.0;
		} else {
			double m = (y2 - y1) / (x2 - x1);
			double c = y1 - (m * x1);
			equ[0] = m;
			equ[1] = c;
		}
		return equ;
	}

	public static double thresholdDistance(int numAgents, Area origin,
			ArrayList<Cluster> clusterList) {
		ArrayList<Double> radii = new ArrayList<Double>();
		for (Cluster c : clusterList) {
			double x = c.centroid[0] - origin.getX();
			double y = c.centroid[1] - origin.getY();
			double radius = x * x + y * y;
			radii.add(radius);
		}
		Collections.sort(radii);

		if (radii.size() < numAgents) {
			return Math.sqrt(radii.get(radii.size() - 1));
		}
		return Math.sqrt(radii.get(numAgents - 1));
	}

	public static ArrayList<Cluster> closestClusters(int nClusters,
			Area origin, ArrayList<Cluster> clusterList) {
		ArrayList<Cluster> ret = new ArrayList<Cluster>();
		TreeMap<Double, Cluster> radii = new TreeMap<Double, Cluster>();
		for (Cluster c : clusterList) {
			double x = c.centroid[0] - origin.getX();
			double y = c.centroid[1] - origin.getY();
			double radius = x * x + y * y;
			radii.put(radius, c);
		}
		if (radii.size() < nClusters) {
			for (int i = 0; i < radii.size(); i++) {
				double key = radii.firstEntry().getKey();
				Cluster c = radii.firstEntry().getValue();
				radii.remove(key);
				ret.add(c);
			}
		} else {
			for (int i = 0; i < nClusters; i++) {
				double key = radii.firstEntry().getKey();
				Cluster c = radii.firstEntry().getValue();
				radii.remove(key);
				ret.add(c);
			}
		}
		return ret;
	}

	public static double edgeLength(rescuecore2.standard.entities.Edge edge) {
		double x = edge.getEndX() - edge.getStartX();
		double y = edge.getEndY() - edge.getStartY();
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
	}

	public static boolean in_or_out_of_polygon(ArrayList<Integer> X,
			ArrayList<Integer> Y, int x, int y) {
		int i, j;
		boolean c = false;
		for (i = 0, j = X.size() - 1; i < X.size(); j = i++) {
			if ((((Y.get(i) <= y) && (y < Y.get(j))) || ((Y.get(j) <= y) && (y < Y
					.get(i))))
					&& (x < (X.get(j) - X.get(i)) * (y - Y.get(i))
							/ (Y.get(j) - Y.get(i)) + X.get(i)))
				c = !c;
		}
		return c;
	}

	public static <T> Collection<T> filter(Collection<T> target,
			Predicate<T> predicate) {
		Collection<T> result = new ArrayList<T>();
		for (T element : target) {
			if (predicate.apply(element)) {
				result.add(element);
			}
		}
		return result;
	}

	public static String compareClusterLists(ArrayList<EntityID> original,
			ArrayList<EntityID> copy) {
		String result = "";
		for (EntityID id : original) {
			boolean flag = true;
			int value = id.getValue();
			for (EntityID id2 : copy) {
				if (value == id2.getValue()) {
					result += "0";
					flag = false;
					break;
				}
			}
			if (flag) {
				result += "1";
			}
		}
		int num = Integer.parseInt(result, 2);
		String hex = Integer.toHexString(num);
		return hex;
	}
}
