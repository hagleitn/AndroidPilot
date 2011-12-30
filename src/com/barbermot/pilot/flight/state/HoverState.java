package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.pid.AutoControl;

public class HoverState extends FlightState<Float> {
    
    AutoControl autoThrottle;
    
    public AutoControl getAutoThrottle() {
        return autoThrottle;
    }
    
    public void setAutoThrottle(AutoControl autoThrottle) {
        this.autoThrottle = autoThrottle;
    }
    
    @Override
    public boolean guard(Float height) throws ConnectionLostException {
        return computer.isCalibrated()
                && height <= FlightConfiguration.get().getMaxHoverHeight()
                && computer.hasHeightSignal();
    }
    
    @Override
    public void enter(Float height) throws ConnectionLostException {
        logger.info("Entering hover state");
        
        autoThrottle.setConfiguration(computer.getHoverConf());
        autoThrottle.setGoal(height);
        autoThrottle.engage(true);
        computer.setAutoThrottle(autoThrottle);
        computer.setGoalHeight(height);
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        autoThrottle.engage(false);
        computer.setAutoThrottle(null);
        computer.setGoalHeight(0);
    }
    
    @Override
    public void update() throws ConnectionLostException {
        if (!computer.hasHeightSignal()) {
            transition(new StateEvent<Void>(Type.EMERGENCY_LANDING, null));
        }
    }
    
}
