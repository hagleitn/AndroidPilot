package com.barbermot.pilot.simulator;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.Signal;

public class RollSignalSimulation extends Signal implements Runnable {
    
    PhysicsEngine engine;
    
    public RollSignalSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run() {
        try {
            super.notifyListeners(engine.getRollAngle(), engine.getTime());
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }
}
