package com.barbermot.pilot.flight;

import ioio.lib.api.exception.ConnectionLostException;

public class RudderControlListener extends FlightControlListener {
    
    @Override
    public void adjust(float x) throws ConnectionLostException {
        int currentRudder = (int) limit(x, computer.getMinTilt(),
                computer.getMaxTilt());
        computer.setCurrentRudder(currentRudder);
        computer.getUfo().rudder(currentRudder);
    }
    
}
