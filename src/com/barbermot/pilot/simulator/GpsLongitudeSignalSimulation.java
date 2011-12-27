package com.barbermot.pilot.simulator;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.Signal;

public class GpsLongitudeSignalSimulation extends Signal implements Runnable {
    
    PhysicsEngine engine;
    
    public GpsLongitudeSignalSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run() {
        try {
            super.notifyListeners(engine.getLongitude(), engine.getTime());
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }
}
