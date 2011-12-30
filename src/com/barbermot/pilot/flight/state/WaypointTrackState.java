package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.flight.Waypoint;

public class WaypointTrackState extends FlightState<Waypoint> {
    
    @Override
    public boolean guard(Waypoint arg) throws ConnectionLostException {
        return true;
    }
    
    @Override
    public void enter(Waypoint arg) throws ConnectionLostException {
        logger.info("Entering waypoint track state");
    }
    
    @Override
    public void exit() throws ConnectionLostException {}
    
    @Override
    public void update() throws ConnectionLostException {}
    
}
