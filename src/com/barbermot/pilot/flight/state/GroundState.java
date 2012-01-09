package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.rc.RemoteControl;

public class GroundState extends FlightState<Void> {
    
    private void writeDefaults() throws ConnectionLostException {
        computer.getUfo().throttle(QuadCopter.MIN_SPEED);
        computer.getUfo().aileron(QuadCopter.STOP_SPEED);
        computer.getUfo().rudder(QuadCopter.STOP_SPEED);
        computer.getUfo().elevator(QuadCopter.STOP_SPEED);
        computer.getUfo()
                .adjustGain(FlightConfiguration.get().getDefaultGain());
    }
    
    @Override
    public boolean guard(Void arg) throws ConnectionLostException {
        return true;
    }
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering ground state");
        computer.getRc().setControlMask((char) ~RemoteControl.FULL_MANUAL);
        writeDefaults();
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        computer.getRc().setControlMask((char) ~RemoteControl.THROTTLE_MASK);
    }
    
    @Override
    public void update() throws ConnectionLostException {
        computer.calibrate();
        writeDefaults();
    }
}
