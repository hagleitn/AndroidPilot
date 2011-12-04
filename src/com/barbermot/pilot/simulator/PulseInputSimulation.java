package com.barbermot.pilot.simulator;

import ioio.lib.api.PulseInput;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.concurrent.TimeoutException;

public class PulseInputSimulation extends DeviceSimulation implements
        PulseInput {
    
    public PulseInputSimulation(int pin, PhysicsEngine engine) {
        super(pin, engine);
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public float getDuration() throws InterruptedException,
            ConnectionLostException {
        return engine.pulseIn(pin);
    }
    
    @Override
    public float getDuration(float timeout) throws InterruptedException,
            ConnectionLostException, TimeoutException {
        return getDuration();
    }
    
    @Override
    public float waitPulseGetDuration() throws InterruptedException,
            ConnectionLostException {
        return getDuration();
    }
    
    @Override
    public float waitPulseGetDuration(float timeout)
            throws InterruptedException, ConnectionLostException,
            TimeoutException {
        return getDuration();
    }
    
    @Override
    public float getFrequency() throws InterruptedException,
            ConnectionLostException {
        return getDuration();
    }
    
    @Override
    public float getFrequency(float timeout) throws InterruptedException,
            ConnectionLostException, TimeoutException {
        return getDuration();
    }
    
}
