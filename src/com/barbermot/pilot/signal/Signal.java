package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Signal {
    
    protected Queue<SignalListener> listeners;
    
    public Signal() {
        super();
        listeners = new ConcurrentLinkedQueue<SignalListener>();
    }
    
    public void registerListener(SignalListener listener) {
        listeners.add(listener);
    }
    
    public void notifyListeners(float value, long time)
            throws ConnectionLostException {
        for (SignalListener l : listeners) {
            l.update(value, time);
        }
    }
    
    public void abort() {}
    
}