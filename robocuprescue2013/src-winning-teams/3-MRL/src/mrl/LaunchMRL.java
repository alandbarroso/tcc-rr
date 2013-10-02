package mrl;

import mrl.ambulance.MrlAmbulanceCentre;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.common.MRLConstants;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireStation;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceOffice;
import mrl.viewer.MrlViewer;
import rescuecore2.Constants;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.log.Logger;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;

/**
 * Launcher for sample agents. This will launch as many instances of each of the sample agents as possible, all using one connction.
 */
public final class LaunchMRL {
    private static final String FIRE_BRIGADE_FLAG = "-fb";
    private static final String POLICE_FORCE_FLAG = "-pf";
    private static final String AMBULANCE_TEAM_FLAG = "-at";
    private static final String FIRE_STATION_FLAG = "-fs";
    private static final String POLICE_OFFICE_FLAG = "-po";
    private static final String AMBULANCE_CENTRE_FLAG = "-ac";
    private static final String PRECOMPUTE_FLAG = "-precompute";
    private static final String CIVILIAN_FLAG = "-cv";

    public static boolean SHOULD_PRECOMPUTE=false;

    private LaunchMRL() {
    }

    /**
     * Launch 'em!
     *
     * @param args The following arguments are understood: -p <port>, -h <hostname>, -fb <fire brigades>, -pf <police forces>, -at <ambulance teams>
     */
    public static void main(String[] args) {
        Logger.setLogContext("mrl");
        try {
            Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
            Config config = new Config();
            args = CommandLineOptions.processArgs(args, config);
            config.setValue("random.seed", "1");


            int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
            String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);
            int fb = -1;
            int pf = -1;
            int at = -1;
            int fs = -1;
            int po = -1;
            int ac = -1;            // CHECKSTYLE:OFF:ModifiedControlVariable
            boolean pc = false;
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals(FIRE_BRIGADE_FLAG)) {
                    fb = Integer.parseInt(args[++i]);
                } else if (args[i].equals(FIRE_STATION_FLAG)) {
                    fs = Integer.parseInt(args[++i]);
                } else if (args[i].equals(POLICE_FORCE_FLAG)) {
                    pf = Integer.parseInt(args[++i]);
                } else if (args[i].equals(POLICE_OFFICE_FLAG)) {
                    po = Integer.parseInt(args[++i]);
                } else if (args[i].equals(AMBULANCE_TEAM_FLAG)) {
                    at = Integer.parseInt(args[++i]);
                } else if (args[i].equals(AMBULANCE_CENTRE_FLAG)) {
                    ac = Integer.parseInt(args[++i]);
                } else if (args[i].equals(PRECOMPUTE_FLAG)) {
                    pc = true;
                    SHOULD_PRECOMPUTE=pc;
                } else {
                    Logger.warn("Unrecognised option: " + args[i]);
                }
            }
            // CHECKSTYLE:ON:ModifiedControlVariable
            ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
            connect(launcher, fb, fs, pf, po, at, ac, config, pc);
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

    private static void connect(ComponentLauncher launcher, int fb, int fs, int pf, int po, int at, int ac, Config config, boolean precompute) throws InterruptedException, ConnectionException {
        try {
            if (precompute) {
                File data = new File("precompute");
                if (!data.exists() || !data.isDirectory()) {
                    data.mkdir();
                } else {
                    for (File f : data.listFiles()) {
                        if (!f.isDirectory()) {
                            f.delete();
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        int i = 0;
        try {
            while (fb-- != 0) {
                Logger.info("Connecting fire brigade " + (i++) + "...");
//                System.out.print("Connecting fire brigade " + (i) + ": ");
                launcher.connect(new MrlFireBrigade());
                Logger.info("success");
            }
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        try {
            while (fs-- != 0) {
                Logger.info("Connecting fire station " + (i++) + "...");
//                System.out.print("Connecting fire station " + (i) + ": ");
                launcher.connect(new MrlFireStation());
                Logger.info("success");
            }
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        try {
            while (pf-- != 0) {
                Logger.info("Connecting police force " + (i++) + "...");
//                System.out.print("Connecting police force " + (i) + ": ");
                launcher.connect(new MrlPoliceForce());
                Logger.info("success");
            }
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        try {
            while (po-- != 0) {
                Logger.info("Connecting police office " + (i++) + "...");
//                System.out.print("Connecting police office " + (i) + ": ");
                launcher.connect(new MrlPoliceOffice());
                Logger.info("success");
            }
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }

        try {
            while (at-- != 0) {
                Logger.info("Connecting ambulance team " + (i++) + "...");
//                System.out.print("Connecting ambulance team " + (i) + ": ");
                launcher.connect(new MrlAmbulanceTeam());
                Logger.info("success");
            }
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        try {
            while (ac-- != 0) {
                Logger.info("Connecting ambulance center " + (i++) + "...");
//                System.out.print("Connecting ambulance center " + (i) + ": ");
                launcher.connect(new MrlAmbulanceCentre());
                Logger.info("success");
            }
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            try {
                Logger.info("Connecting viewer ...");
                launcher.connect(new MrlViewer());
                Logger.info("success");
            } catch (ComponentConnectionException e) {
                Logger.info("failed: " + e.getMessage());
            }
        }

        System.out.println("-------======:::::: ALL AGENTS CONNECTED ::::::======-------");

        try {
            FileWriter fileWriter = new FileWriter("messageDebug.txt", true);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.close();
        } catch (Exception e) {

        }
    }
}