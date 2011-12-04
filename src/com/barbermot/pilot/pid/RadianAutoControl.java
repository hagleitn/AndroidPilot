package com.barbermot.pilot.pid;

import java.util.logging.Logger;

public class RadianAutoControl extends AutoControl {
    
    public RadianAutoControl(ControlListener control) {
        super(control);
    }
    
    public RadianAutoControl(ControlListener control, Logger logger) {
        super(control, logger);
    }
    
    @Override
    public float computeError(float value) {
        float left = goal - value;
        float right = (float) (goal < value ? goal - (value - 2 * Math.PI)
                : goal - (value + 2 * Math.PI));
        return Math.abs(left) < Math.abs(right) ? left : right;
    }
}
