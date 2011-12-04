package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.flight.FlightConfiguration;

public class EmergencyLandingState extends FlightState<Void> {
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering emergency landing state");
        
        computer.getUfo().throttle(
                FlightConfiguration.get().getEmergencyDescent());
        computer.setCurrentThrottle(FlightConfiguration.get()
                .getEmergencyDescent());
    }
    
    @Override
    public void exit() throws ConnectionLostException {}
    
    @Override
    public void update() throws ConnectionLostException {
        if (computer.hasHeightSignal()) {
            transition(new StateEvent<Void>(Type.LANDING, null));
        }
        
    }
    
}
