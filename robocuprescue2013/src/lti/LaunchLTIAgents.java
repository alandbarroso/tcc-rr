package lti;

import java.util.Arrays;
import java.util.Iterator;

import lti.agent.ambulance.LTIAmbulanceTeam;
import lti.agent.fire.LTIFireBrigade;
import lti.agent.police.LTIPoliceForce;
import rescuecore2.components.Component;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.config.Config;
import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;


public final class LaunchLTIAgents {
	
	private static final int MAX_PLATOONS = 50;
	private static final int MAX_CENTRES = 5;
	private static final String VERBOSE_FLAG = "-v";
	private static final String DEBUG_FLAG = "-d";
	
	private static boolean verbose_launch = false;
	private static boolean debug = false;
	
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
			
	        Iterator<String> it = Arrays.asList(args).iterator();
	        while (it.hasNext()) {
	        	String next = it.next();
	            if (VERBOSE_FLAG.equals(next)) {
	                verbose_launch = true;
	            } else if (DEBUG_FLAG.equals(next)) {
	            	verbose_launch = true;
	            	debug = true;
	            }
	        }
	        
	        args = CommandLineOptions.processArgs(args, config);
			int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
					Constants.DEFAULT_KERNEL_PORT_NUMBER);
			String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY,
					Constants.DEFAULT_KERNEL_HOST_NAME);
			launcher = new TCPComponentLauncher(host, port, config);
			
			connectPlatoon("lti.agent.fire.LTIFireBrigade");
			connectPlatoon("lti.agent.police.LTIPoliceForce");
			connectPlatoon("lti.agent.ambulance.LTIAmbulanceTeam");

			connectCentre("sample.SampleCentre");

			if (debug)
				connect("sample.SampleCivilian", 1000);
			
		} catch (Exception e) {
			Logger.error("Error connecting agents", e);
		}

	}

	private static void connectPlatoon(String classname)
			throws InterruptedException, ConnectionException {
		connect(classname, MAX_PLATOONS);
	}

	private static void connectCentre(String classname)
			throws InterruptedException, ConnectionException {
		connect(classname, MAX_CENTRES);
	}

	/**
	 * @param classname
	 * @param max_agents
	 * @throws InterruptedException
	 * @throws ConnectionException
	 */
	private static void connect(String classname, int max_agents)
			throws InterruptedException, ConnectionException {
		String[] cnsplit = classname.split("[.]");
		String agentname = cnsplit[cnsplit.length - 1];
		Component c = null;
		
		log("Started launching " + agentname + "s >>>");
		try {
			for (int i = 1; i < max_agents; i++) {
				log("Launching " + agentname + " " + i + "... ");
				c = (Component) Class.forName(classname).newInstance();
				if (c instanceof LTIAmbulanceTeam)
					((LTIAmbulanceTeam)c).setVerbose(debug);
				if (c instanceof LTIPoliceForce)
					((LTIPoliceForce)c).setVerbose(debug);
				if (c instanceof LTIFireBrigade)
					((LTIFireBrigade)c).setVerbose(debug);
				launcher.connect(c);
				log(agentname + " " + i + " launched.");
			}
		} catch (Exception e) {
			log(e.getMessage());
		}
		log("<<< Finished launching " + agentname + "s.");
		log_newline();
	}
	
	private static void log_newline() {
		log("");
	}
	
	private static void log(String msg) {
		if (verbose_launch)
			System.out.println(msg);
	}
}
