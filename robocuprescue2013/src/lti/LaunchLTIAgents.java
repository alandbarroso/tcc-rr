package lti;


import java.io.IOException;

import lti.agent.ambulance.LTIAmbulanceTeam;
import lti.agent.fire.LTIFireBrigade;
import lti.agent.police.LTIPoliceForce;
import sample.SampleCentre;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

public final class LaunchLTIAgents {

	/**
	 * Launch LTI Agent Rescue
	 * 
	 * @param args
	 *            firebrigade firestation policeforce policeoffice ambulanceteam
	 *            ambulancecentre
	 */
	public static void main(String[] args) {
		Logger.setLogContext("lti");
		
		//TODO: Remove comment
		
		try {
			Registry.SYSTEM_REGISTRY
					.registerEntityFactory(StandardEntityFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY
					.registerMessageFactory(StandardMessageFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY
					.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
			Config config = new Config();
			args = CommandLineOptions.processArgs(args, config);

			if (args.length >= 8) {
				int fb = Integer.parseInt(args[0]);
				int fs = Integer.parseInt(args[1]);
				int pf = Integer.parseInt(args[2]);
				int po = Integer.parseInt(args[3]);
				int at = Integer.parseInt(args[4]);
				int ac = Integer.parseInt(args[5]);
				int cv = Integer.parseInt(args[6]);

				int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
						Constants.DEFAULT_KERNEL_PORT_NUMBER);
				// String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY,
				// Constants.DEFAULT_KERNEL_HOST_NAME);

				String host = args[7];

				ComponentLauncher launcher = new TCPComponentLauncher(host,
						port, config);
				connect(launcher, fb, fs, pf, po, at, ac, cv);
			}
		} catch (IOException e) {
			Logger.error("Error connecting agents", e);
		} catch (ConfigException e) {
			Logger.error("Configuration error", e);
		} catch (ConnectionException e) {
			Logger.error("Error connecting agents", e);
		} catch (InterruptedException e) {
			Logger.error("Error connecting agents", e);
		}

	}

	/**
	 * 
	 * @param launcher
	 * @param fb
	 *            fire brigade
	 * @param fs
	 *            fire station
	 * @param pf
	 *            police force
	 * @param po
	 *            police office
	 * @param at
	 *            ambulance team
	 * @param ac
	 *            ambulance centre
	 * @param config
	 * @throws InterruptedException
	 * @throws ConnectionException
	 */
	private static void connect(ComponentLauncher launcher, int fb, int fs,
			int pf, int po, int at, int ac, int cv)
			throws InterruptedException, ConnectionException {
		try {
			int cnt = 1;
			while (fb-- != 0) {
				System.out.print("Launching Fire Brigade " + cnt + "... ");
				launcher.connect(new LTIFireBrigade());
				System.out.println("success.");
				cnt++;
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Fire Brigades.");

		try {
			if ((fs > 0) || (fs == -1)) {
				System.out.print("Launching fire station 1... ");
				launcher.connect(new SampleCentre());
				System.out.println("success.");
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Fire Stations.");

		Logger.info("Start launching Police Forces");
		try {
			int cnt = 1;
			while (pf-- != 0) {
				System.out.print("Launching Police Force " + cnt + "... ");
				launcher.connect(new LTIPoliceForce());
				System.out.println("success.");
				cnt++;
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Police Forces.");

		try {
			if ((po > 0) || (po == -1)) {
				System.out.print("Launching Police Office 1... ");
				launcher.connect(new SampleCentre());
				System.out.println("success.");
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Police Offices.");

		try {
			int cnt = 1;
			while (at-- != 0) {
				System.out.print("Launching Ambulance Team " + cnt + "... ");
				launcher.connect(new LTIAmbulanceTeam());
				System.out.println("success.");
				cnt++;
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Ambulance Teams.");

		try {
			if ((ac > 0) || (ac == -1)) {
				System.out.print("Launching Ambulance Centre 1... ");
				launcher.connect(new SampleCentre());
				System.out.println("success.");
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Ambulance Centres.");

		try {
			int cnt = 1;
			while (cv-- != 0) {
				System.out.print("Launching Civilian " + cnt + "... ");
				launcher.connect(new LTIAmbulanceTeam());
				System.out.println("success.");
				cnt++;
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		System.out.println("Finished launching Civilians.");
	}
}
