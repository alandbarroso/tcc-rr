package lti;

import lti.agent.ambulance.LTIAmbulanceTeam;
import lti.agent.fire.LTIFireBrigade;
import lti.agent.police.LTIPoliceForce;
import sample.SampleCentre;
import rescuecore2.components.Component;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.config.Config;
import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;


public final class LaunchLTIAgents {

	private static final int MAX_PLATOONS = 50;
	private static final int MAX_CENTRES = 5;
	
	private static ComponentLauncher launcher;
	
	/**
	 * Launch LTI Agent Rescue
	 */
	public static void main(String[] args) {
		Logger.setLogContext("lti");

		try {
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
			launcher = new TCPComponentLauncher(host, port, config);
			
			connectPlatoon("Fire Brigade", new LTIFireBrigade());
			connectPlatoon("Police Force", new LTIPoliceForce());
			connectPlatoon("Ambulance Team", new LTIAmbulanceTeam());
			
			connectCentre("Centre", new SampleCentre());
			
		} catch (ConnectionException e) {
			Logger.error("Error connecting agents", e);
		} catch (InterruptedException e) {
			Logger.error("Error connecting agents", e);
		}

	}

	/**
	 * @param type
	 * @param agent
	 * @throws InterruptedException
	 * @throws ConnectionException
	 */
	private static void connectPlatoon(String type, Component agent)
			throws InterruptedException, ConnectionException {
		connect(type, agent, MAX_PLATOONS);
	}

	/**
	 * @param type
	 * @param agent
	 * @throws InterruptedException
	 * @throws ConnectionException
	 */
	private static void connectCentre(String type, Component agent)
			throws InterruptedException, ConnectionException {
		connect(type, agent, MAX_CENTRES);
	}

	/**
	 * @param type
	 * @param agent
	 * @param max_agents
	 * @throws InterruptedException
	 * @throws ConnectionException
	 */
	private static void connect(String type, Component agent, int max_agents)
			throws InterruptedException, ConnectionException {
		System.out.println("Started launching " + type + "s >>>");
		try {
			for (int i = 1; i < max_agents; i++) {
				System.out.print("Launching " + type + " " + i + "... ");
				launcher.connect(agent);
				System.out.println(type + " " + i + " launched.");
			}
		} catch (ComponentConnectionException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("<<< Finished launching " + type + "s.");
		System.out.println();
	}
}
