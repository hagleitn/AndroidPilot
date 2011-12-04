package com.barbermot.pilot.simulator;

import ioio.lib.api.PingPin;
import ioio.lib.api.exception.ConnectionLostException;

public class PingPinSimulation extends DeviceSimulation implements PingPin {
    
    public PingPinSimulation(int pin, PhysicsEngine engine) {
        super(pin, engine);
    }
    
    @Override
    public void close() {}
    
    @Override
    public int read() throws InterruptedException, ConnectionLostException {
        return engine.pulseIn(pin);
    }
    
}
