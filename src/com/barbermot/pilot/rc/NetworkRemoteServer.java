package com.barbermot.pilot.rc;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.quad.QuadCopter.Direction;

public class NetworkRemoteServer implements Runnable {
    
    QuadCopter      ufo;
    ServerSocket    server;
    ProtocolHandler handler;
    ExecutorService executor;
    boolean[]       controlMap = { true, true, true, true };                  ;
    Logger          logger     = Logger.getLogger(NetworkRemoteServer.class);
    
    public NetworkRemoteServer() throws IOException {
        server = new ServerSocket(FlightConfiguration.get()
                .getRemoteControlPort());
    }
    
    public void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }
    
    public void setUfo(QuadCopter ufo) {
        this.ufo = ufo;
    }
    
    public void setEnabled(Direction d, boolean enable) {
        controlMap[d.ordinal()] = enable;
    }
    
    private class ProtocolHandler implements Runnable {
        
        Socket                  socket;
        InputStream             in;
        public final static int CHANNELS = 4;
        Logger                  logger   = Logger.getLogger(ProtocolHandler.class);
        
        public ProtocolHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.socket.shutdownOutput();
            this.in = socket.getInputStream();
        }
        
        private void parse(byte[] buffer) throws ConnectionLostException {
            Direction[] d = Direction.values();
            for (int i = 0; i < CHANNELS; ++i) {
                if (controlMap[i]) {
                    ufo.move(d[i], (int) buffer[i]);
                }
            }
            ufo.adjustGain(FlightConfiguration.get().getDefaultGain());
        }
        
        @Override
        public void run() {
            byte[] buffer = new byte[CHANNELS];
            try {
                int count = 0;
                while (!Thread.interrupted()) {
                    count += in.read(buffer, count, CHANNELS - count);
                    if (count == CHANNELS) {
                        count = 0;
                        parse(buffer);
                    }
                }
            } catch (IOException e) {
                logger.info("Lost Connection.", e);
            } catch (ConnectionLostException e) {
                logger.info("Lost IOIO.", e);
            } finally {
                try {
                    if (socket != null) {
                        socket.shutdownInput();
                        socket.close();
                    }
                } catch (IOException e) {
                    // don't care.
                }
            }
        }
    }
    
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                executor.submit(new ProtocolHandler(server.accept()));
            } catch (IOException e) {
                logger.warn("No connection", e);
            }
        }
    }
}
