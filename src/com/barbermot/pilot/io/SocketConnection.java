package com.barbermot.pilot.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.log4j.Logger;

import com.barbermot.pilot.flight.FlightConfiguration;

public class SocketConnection extends Connection {
    
    Socket socket;
    Logger logger = Logger.getLogger("SocketConnection");
    
    @Override
    protected void reEstablishConnection() throws IOException {
        logger.info("Setting up socket ("
                + FlightConfiguration.get().getSerialUrl() + ", "
                + FlightConfiguration.get().getSerialPort() + ")");
        
        if (socket != null) {
            try {
                socket.shutdownInput(); // workaround to get exception on read()
                socket.close();
            } catch (IOException io) {
                logger.warn("Couldn't close socket.", io);
            } finally {
                socket = null;
            }
        }
        
        // ServerSocket server = new
        // ServerSocket(config.getSerialPort());
        // socket = server.accept();
        socket = new Socket(FlightConfiguration.get().getSerialUrl(),
                FlightConfiguration.get().getSerialPort());
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }
    
}
