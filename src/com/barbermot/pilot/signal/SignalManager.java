package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public interface SignalManager {
    
    public abstract void shutdown();
    
    public abstract List<Future<?>> getFutures();
    
    public abstract Signal getYawSignal(int interval)
            throws ConnectionLostException;
    
    public abstract Signal getPitchSignal(int interval);
    
    public abstract Signal getRollSignal(int interval);
    
    public abstract Signal getUltraSoundSignal(int interval, int pin)
            throws ConnectionLostException;
    
    public abstract Signal getGpsAltitudeSignal(int interval);
    
    public abstract Signal getGpsLongitudeSignal(int interval);
    
    public abstract Signal getGpsLatitudeSignal(int interval);
    
    void setScheduler(ScheduledExecutorService scheduler);
    
}