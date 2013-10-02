package rescuecore2.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import rescuecore2.config.Config;
import rescuecore2.connection.Connection;
import rescuecore2.connection.ConnectionException;
import rescuecore2.connection.StreamConnection;

public class OfflineComponentLauncher extends ComponentLauncher {
	private String fileName;

	public OfflineComponentLauncher(String fileName, Config config) {
		super(config);
		this.fileName = fileName;
	}

	protected Connection makeConnection(String s) throws ConnectionException {
		try {
			return new StreamConnection(new FileInputStream(new File(fileName)),
					new FileOutputStream(new File(fileName + ".out")));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
