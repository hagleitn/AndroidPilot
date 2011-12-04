package com.barbermot.pilot.simulator;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.Signal;

public class GpsAltitudeSignalSimulation extends Signal implements Runnable {
    
    PhysicsEngine engine;
    
    public GpsAltitudeSignalSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run() {
        try {
            super.notifyListeners(engine.getGpsAlitude(), engine.getTime());
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }
    
}
