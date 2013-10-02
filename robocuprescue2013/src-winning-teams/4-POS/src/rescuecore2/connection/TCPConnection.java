package rescuecore2.connection;

import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;

import rescuecore2.log.Logger;

/**
   TCP implementation of a Connection.
 */
public class TCPConnection extends StreamConnection {
    private Socket socket;

    /**
       Make a connection to the local host on a given port.
       @param port The port to connect to.
       @throws IOException If the host cannot be contacted.
    */
    public TCPConnection(int port) throws IOException {
        this(null, port);
    }

    /**
       Make a connection to a specific host on a given port.
       @param address The address of the host.
       @param port The port to connect to.
       @throws IOException If the host cannot be contacted.
    */
    public TCPConnection(String address, int port) throws IOException {
        this(address, port, null);
    }

    public TCPConnection(String address, int port, OutputStream offlineStream) throws IOException {
        this(new Socket(address, port), offlineStream);
    }

    /**
       Create a TCPConnection from an existing socket.
       @param socket The socket to attach to.
       @throws IOException If there is a problem opening the streams.
    */
    public TCPConnection(Socket socket) throws IOException {
    	this(socket, null);
    }

    public TCPConnection(Socket socket, OutputStream offlineStream) throws IOException {
        super(socket.getInputStream(), socket.getOutputStream(), offlineStream);
        this.socket = socket;
        socket.setSoTimeout(1000);
        setName("TCPConnection: local port " + socket.getLocalPort() + ", endpoint = " + socket.getInetAddress() + ":" + socket.getPort());
    }

    @Override
    protected void shutdownImpl() {
        super.shutdownImpl();
        try {
            socket.close();
        }
        catch (IOException e) {
            Logger.error("Error closing TCP connection", e);
        }
    }
}
