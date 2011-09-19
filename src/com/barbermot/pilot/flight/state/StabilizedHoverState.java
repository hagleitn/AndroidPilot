package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.pid.AutoControl;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.rc.RemoteControl;

public class StabilizedHoverState extends HoverState {
    
    private AutoControl autoElevator;
    private AutoControl autoRudder;
    private AutoControl autoAileron;
    
    public void stabilize(boolean engage) throws ConnectionLostException {
        char mask = RemoteControl.AILERON_MASK | RemoteControl.ELEVATOR_MASK
                | RemoteControl.RUDDER_MASK;
        char controlMask = computer.getRc().getControlMask();
        
        if (engage) {
            controlMask = (char) (controlMask & ~mask);
        } else {
            controlMask = (char) (controlMask | mask);
            computer.setCurrentElevator(QuadCopter.STOP_SPEED);
            computer.setCurrentAileron(QuadCopter.STOP_SPEED);
            computer.setCurrentRudder(QuadCopter.STOP_SPEED);
        }
        computer.getRc().setControlMask(controlMask);
        
        autoElevator.setConfiguration(computer.getOrientationConf());
        autoAileron.setConfiguration(computer.getOrientationConf());
        autoRudder.setConfiguration(computer.getOrientationConf());
        
        autoElevator.setGoal(0);
        autoAileron.setGoal(0);
        autoRudder.setGoal(0);
        
        autoElevator.engage(engage);
        autoAileron.engage(engage);
        autoRudder.engage(engage);
        
        computer.setAutoElevator(engage ? autoElevator : null);
        computer.setAutoAileron(engage ? autoAileron : null);
        computer.setAutoRudder(engage ? autoRudder : null);
    }
    
    @Override
    public void enter(Float height) throws ConnectionLostException {
        super.enter(height);
        stabilize(true);
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        super.exit();
        stabilize(false);
    }
    
    public AutoControl getAutoElevator() {
        return autoElevator;
    }
    
    public void setAutoElevator(AutoControl autoElevator) {
        this.autoElevator = autoElevator;
    }
    
    public AutoControl getAutoRudder() {
        return autoRudder;
    }
    
    public void setAutoRudder(AutoControl autoRudder) {
        this.autoRudder = autoRudder;
    }
    
    public AutoControl getAutoAileron() {
        return autoAileron;
    }
    
    public void setAutoAileron(AutoControl autoAileron) {
        this.autoAileron = autoAileron;
    }
}
