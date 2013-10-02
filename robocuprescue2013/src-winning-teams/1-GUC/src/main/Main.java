package main;

import rescuecore2.Constants;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;
import Centres.AmbulanceCenter;
import Centres.FireCenter;
import Centres.PoliceCenter;
import agents.AmbulanceAgent;
import agents.FirstFireBrigadeAgent;
import agents.FirstPoliceAgent;

/// Launching Agents
public class Main {

	public static void main(String[] args) {

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

			int fb = Integer.parseInt(args[0]);
			int pf = Integer.parseInt(args[2]);
			int at = Integer.parseInt(args[4]);
			int fc = Integer.parseInt(args[1]);
			int pc = Integer.parseInt(args[3]);
			int ac = Integer.parseInt(args[5]);

			String host = args[6];
			int preCompute = Integer.parseInt(args[7]);

			// CHECKSTYLE:ON:ModifiedControlVariable
			ComponentLauncher launcher = new TCPComponentLauncher(host, port,
					config);

			try {

				while (fb != 0) {
					launcher.connect(new FirstFireBrigadeAgent(preCompute));
					fb--;
				}
			} catch (ComponentConnectionException e) {
				//
				System.out.println("Done Connecting Fire Brigades.");
			}

			try {
				while (pf != 0) {
					launcher.connect(new FirstPoliceAgent(preCompute));
					pf--;
				}
			} catch (ComponentConnectionException e) {
				System.out.println("Done Connecting Police Forces.");
			}

			try {
				while (at != 0) {
					launcher.connect(new AmbulanceAgent(preCompute));
					at--;
				}
			} catch (ComponentConnectionException e) {
				System.out.println("Done Connecting Ambulance Teams.");
			}

			try {
				while (ac != 0) {
					launcher.connect(new AmbulanceCenter());
					ac--;
				}
			} catch (ComponentConnectionException e) {
				System.out.println("Done Connecting Centers.");
			}
			try {
				while (fc != 0) {
					launcher.connect(new FireCenter());
					fc--;
				}
			} catch (ComponentConnectionException e) {
				System.out.println("Done Connecting Centers.");
			}
			try {
				while (pc != 0) {
					launcher.connect(new PoliceCenter());
					pc--;
				}
			} catch (ComponentConnectionException e) {
				System.out.println("Done Connecting Centers.");
			}
		} catch (ConnectionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
