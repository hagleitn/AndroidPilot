package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.rc.RemoteControl;

public class ManualControlState extends FlightState<Void> {
    
    @Override
    public boolean guard(Void arg) throws ConnectionLostException {
        return true;
    }
    
    @Override
    public void enter(Void arg) throws ConnectionLostException {
        logger.info("Entering manual control state");
        
        // set rc to allow auto control of throttle
        computer.getRc().setControlMask(RemoteControl.FULL_MANUAL);
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        // set rc to allow auto control of throttle
        char controlMask = computer.getRc().getControlMask();
        controlMask = (char) (controlMask & (~RemoteControl.THROTTLE_MASK));
        computer.getRc().setControlMask(controlMask);
        
        // disarm rc, so it doesn't immediately engage again
        computer.getRc().arm(false);
    }
    
    @Override
    public void update() throws ConnectionLostException {}
    
}
