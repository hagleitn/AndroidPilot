package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.quad.QuadCopter;

public class GroundState extends FlightState<Void> {
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering ground state");
        
        computer.getUfo().throttle(QuadCopter.MIN_SPEED);
        computer.setCurrentThrottle(QuadCopter.MIN_SPEED);
    }
    
    @Override
    public void exit() {}
    
    @Override
    public void update() {
        // calibration
        computer.setZeroHeight(computer.getHeight());
        computer.setZeroGpsHeight(computer.getGpsHeight());
    }
}
