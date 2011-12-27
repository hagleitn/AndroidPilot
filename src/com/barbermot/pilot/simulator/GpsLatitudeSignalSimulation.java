package com.barbermot.pilot.simulator;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.Signal;

public class GpsLatitudeSignalSimulation extends Signal implements Runnable {
    
    PhysicsEngine engine;
    
    public GpsLatitudeSignalSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run() {
        try {
            super.notifyListeners(engine.getLatitude(), engine.getTime());
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }
    
}
