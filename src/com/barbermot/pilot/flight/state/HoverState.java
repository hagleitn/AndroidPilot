package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

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
    public void update() {}
    
}
