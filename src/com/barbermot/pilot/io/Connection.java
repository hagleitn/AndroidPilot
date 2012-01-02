package com.barbermot.pilot.io;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.barbermot.pilot.flight.FlightConfiguration;

public abstract class Connection {
    
    boolean isReconnecting = false;
    
    public final void reconnect() throws IOException, ConnectionLostException,
            InterruptedException {
        synchronized (this) {
            if (!isReconnecting) {
                isReconnecting = true;
            } else {
                wait();
                return;
            }
        }
        int retries = 0;
        while (true) {
            try {
                reEstablishConnection();
            } catch (IOException e) {
                if (retries++ == FlightConfiguration.get()
                        .getConnectionRetries()) {
                    synchronized (this) {
                        isReconnecting = false;
                        notifyAll();
                        throw e;
                    }
                }
                Thread.sleep(FlightConfiguration.get()
                        .getWaitBetweenConnectionRetries());
                continue;
            } catch (ConnectionLostException e) {
                synchronized (this) {
                    isReconnecting = false;
                    notifyAll();
                    throw e;
                }
            }
            break;
        }
        synchronized (this) {
            isReconnecting = false;
            notifyAll();
        }
    }
    
    protected abstract void reEstablishConnection() throws IOException,
            ConnectionLostException;
    
    public abstract OutputStream getOutputStream() throws IOException;
    
    public abstract InputStream getInputStream() throws IOException;
    
}
