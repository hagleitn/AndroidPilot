package com.barbermot.pilot.flight;

import com.barbermot.pilot.pid.ControlListener;

public abstract class FlightControlListener implements ControlListener {
    
    protected FlightComputer computer;
    
    public void setComputer(FlightComputer computer) {
        this.computer = computer;
    }
    
    protected float limit(float value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
    
}
