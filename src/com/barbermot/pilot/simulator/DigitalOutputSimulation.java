package com.barbermot.pilot.simulator;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;

public class DigitalOutputSimulation extends DeviceSimulation implements
        DigitalOutput {
    
    public DigitalOutputSimulation(int pin, PhysicsEngine engine) {
        super(pin, engine);
    }
    
    @Override
    public void close() {}
    
    @Override
    public void write(boolean val) throws ConnectionLostException {
        engine.digitalWrite(pin, val);
    }
}
