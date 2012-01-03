package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

public class Signal {
    
    protected Queue<SignalListener> listeners;
    
    private static final Logger logger = Logger.getLogger("Signal");
    public Signal() {
        super();
        listeners = new ConcurrentLinkedQueue<SignalListener>();
    }
    
    public void registerListener(SignalListener listener) {
        listeners.add(listener);
    }
    
    public void notifyListeners(float value, long time)
            throws ConnectionLostException {
        logger.info(String.format("%d\t%f", time, value));
        for (SignalListener l : listeners) {
            l.update(value, time);
        }
    }
    
    public void abort() {}
    
}
