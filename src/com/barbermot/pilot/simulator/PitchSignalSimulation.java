package com.barbermot.pilot.simulator;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.Signal;

public class PitchSignalSimulation extends Signal implements Runnable {
    
    PhysicsEngine engine;
    
    public PitchSignalSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run() {
        try {
            super.notifyListeners(engine.getPitchAngle(), engine.getTime());
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }
}
