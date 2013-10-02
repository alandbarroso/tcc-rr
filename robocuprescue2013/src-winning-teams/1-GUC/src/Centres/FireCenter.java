package Centres;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;

import Think.Think;

import Communication.Comm;
import Communication.RadioChannel;
import Communication.VoiceChannel;

import rescuecore2.Constants;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.messages.Command;

import rescuecore2.registry.Registry;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

/**
 * Fire station communication center
 */
public class FireCenter extends StandardAgent<FireStation> {

	static int AgentsConnectedCounter = 0;
	int availableChannels;
	int ignoreAgentCommand;

	int policeChannel;
	int fireChannel;
	int ambulanceChannel;

	ArrayList<VoiceChannel> voiceChannels = new ArrayList<VoiceChannel>();
	ArrayList<RadioChannel> radioChannels = new ArrayList<RadioChannel>();

	ArrayList<Building> burningBuildings = new ArrayList<Building>();
	ArrayList<Building> collapsedBuildings = new ArrayList<Building>();
	ArrayList<Human> buriedHumans = new ArrayList<Human>();

	ArrayList<EntityID> reportedBurningBuildings = new ArrayList<EntityID>();
	ArrayList<EntityID> reportedCollapsedBuildings = new ArrayList<EntityID>();
	ArrayList<EntityID> reportedBuriedHumans = new ArrayList<EntityID>();

	ArrayList<StandardEntity> allEntities = new ArrayList<StandardEntity>();

	public FireCenter() {
		super();
		System.out.println("Fire Center " + AgentsConnectedCounter++
				+ " Created!");
	}

	@Override
	public String toString() {
		return "Fire station communication centre";
	}

	protected void postConnect() {

		Comm.discoverChannels(voiceChannels, radioChannels, config);
		String comm = rescuecore2.standard.kernel.comms.ChannelCommunicationModel.PREFIX;
		availableChannels = config.getIntValue(comm + "max.centre");
		Collection<StandardEntity> tempAll = model.getEntitiesOfType(
				StandardEntityURN.BUILDING, StandardEntityURN.ROAD,
				StandardEntityURN.POLICE_FORCE, StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.AMBULANCE_TEAM);
		for (StandardEntity s : tempAll) {
			allEntities.add(s);
		}
		Comparator<StandardEntity> comp = new Comparator<StandardEntity>() {
			@Override
			public int compare(StandardEntity o1, StandardEntity o2) {
				if (o1.getID().getValue() < o2.getID().getValue()) {
					return -1;
				} else if (o1.getID().getValue() > o2.getID().getValue()) {
					return 1;
				}
				return 0;
			}
		};
		Collections.sort(allEntities, comp);
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		model.merge(changed);
		if (time < ignoreAgentCommand) {
			return;
		}

		if (time == ignoreAgentCommand) {
			if (radioChannels.size() != 0) {
				fireChannel = Comm.decideRadioChannel(radioChannels, 'f');
				ambulanceChannel = Comm.decideRadioChannel(radioChannels, 'a');
				policeChannel = Comm.decideRadioChannel(radioChannels, 'p');
			}

		}

		burningBuildings = Think.getBurningBuildings(model, changed);
		collapsedBuildings = Think.getCollapsedBuildings(model, changed);
		buriedHumans = Think.getBuriedHumans(model, changed);

		for (Building b : burningBuildings) {
			if (!reportedBurningBuildings.contains(b.getID())) {
				/*
				 * sendSpeak(time, fireChannel,
				 * Comm.fireMessageTimeStamped(b.getID(), time, model));
				 */reportedBurningBuildings.add(b.getID());
				System.out.println("Fire Center says: Burning building");
			}
		}

		for (Building b : collapsedBuildings) {
			if (!reportedCollapsedBuildings.contains(b.getID())) {
				/*
				 * sendSpeak(time, fireChannel,
				 * Comm.CollapsedMessage(b.getID()));
				 */reportedCollapsedBuildings.add(b.getID());
				System.out.println("Fire Center says: Collapsed building");
			}
		}

		for (Human h : buriedHumans) {
			if (!reportedBuriedHumans.contains(h.getID())) {
				if (h instanceof Civilian)
					sendSpeak(time, ambulanceChannel,
							Comm.civilianLocationBuried(h.getPosition(),
									allEntities));
				else
					sendSpeak(time, ambulanceChannel,
							Comm.buriedAgent(h.getPosition(), allEntities));

				reportedBuriedHumans.add(h.getID());
				System.out.println("Fire Center says: Buried Human");
			}
		}
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_STATION);
	}

	public static void main(String[] args) {
		Registry.SYSTEM_REGISTRY
				.registerEntityFactory(StandardEntityFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY
				.registerMessageFactory(StandardMessageFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY
				.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
		Config config = new Config();
		int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
				Constants.DEFAULT_KERNEL_PORT_NUMBER);
		String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY,
				Constants.DEFAULT_KERNEL_HOST_NAME);

		ComponentLauncher launcher = new TCPComponentLauncher(host, port,
				config);
		System.out.println("port: " + port + ", host: " + host);
		while (true)
			try {
				launcher.connect(new FireCenter());
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
	}

}