package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

public class EmergencyLandingState extends FlightState<Void> {
    
    @Override
    public boolean guard(Void arg) throws ConnectionLostException {
        return true;
    }
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering emergency landing state");
        
        int throttle = computer.getEmergencyDescentThrottle();
        
        computer.getUfo().throttle(throttle);
        computer.setCurrentThrottle(throttle);
    }
    
    @Override
    public void exit() throws ConnectionLostException {}
    
    @Override
    public void update() throws ConnectionLostException {
        if (computer.hasHeightSignal()) {
            transition(Type.LANDING, null);
        }
        
    }
    
}
