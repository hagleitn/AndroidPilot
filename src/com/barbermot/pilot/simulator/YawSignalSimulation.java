package com.barbermot.pilot.simulator;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.Signal;

public class YawSignalSimulation extends Signal implements Runnable {
    
    PhysicsEngine engine;
    
    public YawSignalSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run() {
        try {
            super.notifyListeners(engine.getYawAngle(), engine.getTime());
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }
}
