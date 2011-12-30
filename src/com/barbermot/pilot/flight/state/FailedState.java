package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.quad.QuadCopter;

public class FailedState extends FlightState<Void> {
    
    @Override
    public boolean guard(Void arg) throws ConnectionLostException {
        return true;
    }
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering failed state");
        
        computer.getUfo().throttle(QuadCopter.MIN_SPEED);
        computer.setCurrentThrottle(QuadCopter.MIN_SPEED);
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        // No exit
    }
    
    @Override
    public void update() throws ConnectionLostException {}
    
}
