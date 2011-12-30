package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.pid.AutoControl;

public class LandingState extends FlightState<Void> {
    
    private AutoControl autoThrottle;
    
    @Override
    public boolean guard(Void arg) throws ConnectionLostException {
        return computer.isCalibrated();
    }
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering landing state");
        
        autoThrottle.setConfiguration(computer.getLandingConf());
        autoThrottle.setGoal(computer.getZeroHeight());
        autoThrottle.engage(true);
        computer.setAutoThrottle(autoThrottle);
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        autoThrottle.engage(false);
        computer.setAutoThrottle(null);
    }
    
    public AutoControl getAutoThrottle() {
        return autoThrottle;
    }
    
    public void setAutoThrottle(AutoControl autoThrottle) {
        this.autoThrottle = autoThrottle;
    }
    
    @Override
    public void update() throws ConnectionLostException {
        // no height signal, emergency landing
        if (!computer.hasHeightSignal()) {
            transition(new StateEvent<Void>(Type.EMERGENCY_LANDING, null));
        }
        
        // turn off throttle when close to ground
        if (computer.getHeight() <= computer.getZeroHeight()
                + FlightConfiguration.get().getThrottleOffHeight()) {
            transition(new StateEvent<Void>(Type.GROUND, null));
        }
    }
}
