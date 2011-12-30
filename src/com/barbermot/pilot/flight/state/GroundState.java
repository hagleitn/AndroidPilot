package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.quad.QuadCopter;

public class GroundState extends FlightState<Void> {
    
    @Override
    public boolean guard(Void arg) throws ConnectionLostException {
        return true;
    }
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering ground state");
        
        computer.getUfo().throttle(QuadCopter.MIN_SPEED);
        computer.setCurrentThrottle(QuadCopter.MIN_SPEED);
        
        computer.getUfo().aileron(QuadCopter.STOP_SPEED);
        computer.setCurrentAileron(QuadCopter.STOP_SPEED);
        
        computer.getUfo().rudder(QuadCopter.STOP_SPEED);
        computer.setCurrentRudder(QuadCopter.STOP_SPEED);
        
        computer.getUfo().elevator(QuadCopter.STOP_SPEED);
        computer.setCurrentElevator(QuadCopter.STOP_SPEED);
        
        computer.getUfo().adjustGain(QuadCopter.STOP_SPEED);
    }
    
    @Override
    public void exit() {}
    
    @Override
    public void update() {
        computer.calibrate();
    }
}
