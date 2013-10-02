package rescuecore2.components;

import rescuecore2.config.Config;
import rescuecore2.connection.Connection;
import rescuecore2.connection.TCPConnection;
import rescuecore2.connection.ConnectionException;

import java.io.File;
import java.io.FileOutputStream;

/**
   A class that knows how to connect components to the kernel via TCP.
 */
public class TCPComponentLauncher extends ComponentLauncher {
    private String host;
    private int port;
    private boolean writeOffline;

    /**
       Construct a new TCPComponentLauncher.
       @param host The host name.
       @param port The host port.
       @param config The system configuration.
    */
    public TCPComponentLauncher(String host, int port, Config config) {
    	this(host, port, config, false);
    }

    public TCPComponentLauncher(String host, int port, Config config, boolean writeOffline) {
        super(config);
        this.host = host;
        this.port = port;
        this.writeOffline = writeOffline;
    }

    @Override
    protected Connection makeConnection(String fileName) throws ConnectionException {
        try {
        	if (writeOffline)
        		return new TCPConnection(host, port, new FileOutputStream(new File(fileName)));
        	else
        		return new TCPConnection(host, port);
        }
        catch (Exception e) {
            throw new ConnectionException(e);
        }
    }
}
