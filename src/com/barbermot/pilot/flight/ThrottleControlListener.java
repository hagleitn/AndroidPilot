package com.barbermot.pilot.flight;

import ioio.lib.api.exception.ConnectionLostException;

public class ThrottleControlListener extends FlightControlListener {
    
    @Override
    public void adjust(float x) throws ConnectionLostException {
        int currentThrottle = (int) limit(x + computer.getZeroThrottle(),
                computer.getMinThrottle(), computer.getMaxThrottle());
        computer.setCurrentThrottle(currentThrottle);
        computer.getUfo().throttle(currentThrottle);
    }
    
}
