package com.barbermot.pilot.pid;

import java.util.logging.Logger;

public class GpsAutoControl extends AutoControl {
    
    public GpsAutoControl(ControlListener control) {
        super(control);
    }
    
    public GpsAutoControl(ControlListener control, Logger logger) {
        super(control, logger);
    }
    
    @Override
    public float computeError(float value) {
        // approximate to meters in our lat/lon
        float error = (goal - value) * 1000;
        return error;
    }
}
