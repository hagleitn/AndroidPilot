package com.barbermot.pilot.simulator;

public class DeviceSimulation {
    
    protected int           pin;
    protected PhysicsEngine engine;
    
    public DeviceSimulation(int pin, PhysicsEngine engine) {
        this.pin = pin;
        this.engine = engine;
    }
}
