package rescuecore2;

import static rescuecore2.misc.java.JavaTools.instantiate;

import agent.Agent;

import rescuecore2.components.AbstractComponent;
import rescuecore2.components.Component;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.OfflineComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.connection.ConnectionException;
import rescuecore2.config.Config;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;
import rescuecore2.log.Logger;

public final class LaunchComponents {
	private static boolean offline = false;
	private static boolean precompute = false;
	private static boolean writeOffline = new utilities.config.Config(
			"./Configs").get("Agent", "AgentLog", "OffLogToFile")
			.equals("True");

	// AmbulanceTeam, AmbulanceCentre, FireBrigade, FireStation, PoliceForce,
	// PoliceOffice
	public static final String[] agentsFlags = { "-at", "-ac", "-fb", "-fs",
			"-pf", "-po" };
	public static final String[] agentsNames = { "agent.AmbulanceTeamAgent",
			"agent.AmbulanceCentreAgent", "agent.FireBrigadeAgent",
			"agent.FireStationAgent", "agent.PoliceForceAgent",
			"agent.PoliceOfficeAgent" };
	public static int[] agentsCount = { Integer.MAX_VALUE, Integer.MAX_VALUE,
			Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
			Integer.MAX_VALUE };

	public static void main(String[] args) {
		Registry.SYSTEM_REGISTRY
				.registerEntityFactory(StandardEntityFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY
				.registerMessageFactory(StandardMessageFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY
				.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
		Config config = new Config();
		String fileName = "";
		try {
			args = CommandLineOptions.processArgs(args, config);
			for (int i = 0; i < args.length; i++) {
				if(args[i].equals("--precompute")){
					precompute = true;
				}
				if (args[i].equals("--offline")) {
					offline = true;
					fileName = args[++i];
				} else if (args[i].equals("--write-offline"))
					writeOffline = true;
				else {
					for (int j = 0; j < agentsFlags.length; j++)
						if (agentsFlags[j].equals(args[i])) {
							String count = args[++i];
							agentsCount[j] = count.equals("-1") ? Integer.MAX_VALUE
									: Integer.parseInt(count);
							break;
						}
				}
			}

			if (offline)
				for (int i = 0; i < agentsCount.length; i++)
					if (fileName.contains(agentsNames[i].replaceAll("agent.",
							"")))
						agentsCount[i] = 1;
					else
						agentsCount[i] = 0;

			int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
					Constants.DEFAULT_KERNEL_PORT_NUMBER);
			String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY,
					Constants.DEFAULT_KERNEL_HOST_NAME);

			ComponentLauncher launcher = null;
			if (offline)
				launcher = new OfflineComponentLauncher(fileName, config);
			else
				launcher = new TCPComponentLauncher(host, port, config,
						writeOffline);

			for (int i = 0; i < agentsNames.length; i++)
				connect(launcher, agentsNames[i], agentsCount[i]);
		} catch (Exception e) {
			Logger.error("Error connecting components", e);
		}
		System.gc();
	}

	@SuppressWarnings({ "unchecked" })
	private static void connect(ComponentLauncher launcher, String className,
			int count) throws InterruptedException, ConnectionException {
		System.out.println("Launching "
				+ (count == Integer.MAX_VALUE ? "many" : count)
				+ " instances of component '" + className + "'...");
		for (int i = 0; i < count; ++i) {
			Component c = instantiate(className, Component.class);
			if (c == null)
				break;
			if (c instanceof AbstractComponent)
				((AbstractComponent<?>) c).offlineMode = offline;
			System.out.println("Launching instance " + (i + 1) + "...");
			try {
				c.initialise();
				String fileName = c.getName() + "-" + i;
				fileName = fileName.replaceAll("agent.", "");
				if (c instanceof Agent) {
					Agent agent = (Agent) c;
					utilities.config.Config config = new utilities.config.Config(
							"./Configs/");
					fileName = config.get("Agent", "AgentLog", "OffLogAddress")
							+ fileName + ".off";
					config.set("Agent", "AgentLog", "OffLogToFile",
							writeOffline ? "True" : "False");
					config.set("Agent", "AgentLog", "OffLogFileName", fileName);
					config.set("Agent", "AgentLog","preCompute", precompute ? "True" : "False");
					agent.psdConfig = config;
				}

				launcher.connect(c, fileName);
				System.out.println("success");
			} catch (Exception e) {
				System.out.println("failed: " + e.getMessage());
				break;
			}
		}
	}
}
