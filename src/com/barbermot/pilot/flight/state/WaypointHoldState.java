package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.flight.Waypoint;
import com.barbermot.pilot.pid.AutoControl;

public class WaypointHoldState extends FlightState<Waypoint> {
    
    private AutoControl autoUltraSoundThrottle;
    private AutoControl autoGpsThrottle;
    private AutoControl autoElevator;
    private AutoControl autoRudder;
    private AutoControl autoAileron;
    
    private boolean     gpsActive;
    
    @Override
    public void enter(Waypoint arg) throws ConnectionLostException {
        logger.info("Entering waypoint hold state");
        
        autoUltraSoundThrottle.setConfiguration(computer.getHoverConf());
        autoUltraSoundThrottle.setGoal(arg.altitude);
        
        autoGpsThrottle.setConfiguration(computer.getGpsConf());
        autoGpsThrottle.setGoal(arg.altitude);
        autoGpsThrottle.engage(true);
        computer.setAutoThrottle(autoGpsThrottle);
        computer.setGoalHeight(arg.altitude);
        
        autoRudder.setConfiguration(computer.getOrientationConf());
        autoRudder.setGoal(0);
        autoRudder.engage(true);
        computer.setAutoRudder(autoRudder);
        
        autoElevator.setConfiguration(computer.getOrientationConf());
        autoElevator.setGoal(computer.getZeroLatitude());
        autoElevator.engage(true);
        computer.setAutoElevator(autoElevator);
        
        autoAileron.setConfiguration(computer.getOrientationConf());
        autoAileron.setGoal(computer.getZeroLongitude());
        autoAileron.engage(true);
        
        gpsActive = true;
    }
    
    @Override
    public void exit() throws ConnectionLostException {
        autoUltraSoundThrottle.engage(false);
        autoUltraSoundThrottle.setGoal(0);
        autoGpsThrottle.engage(false);
        autoGpsThrottle.setGoal(0);
        
        autoRudder.engage(false);
        autoElevator.engage(false);
        autoAileron.engage(false);
        
        computer.setAutoThrottle(null);
        computer.setAutoRudder(null);
        computer.setAutoElevator(null);
        computer.setAutoAileron(null);
    }
    
    @Override
    public void update() throws ConnectionLostException {
        if (computer.hasHeightSignal()) {
            if (gpsActive) {
                gpsActive = false;
                autoUltraSoundThrottle.engage(true);
                autoGpsThrottle.engage(false);
                computer.setAutoThrottle(autoUltraSoundThrottle);
            }
        } else if (computer.hasGpsSignal()) {
            if (!gpsActive) {
                gpsActive = true;
                autoUltraSoundThrottle.engage(false);
                autoGpsThrottle.engage(true);
                computer.setAutoThrottle(autoGpsThrottle);
            }
        } else {
            transition(new StateEvent<Void>(Type.EMERGENCY_LANDING, null));
        }
    }
    
    public AutoControl getAutoUltraSoundThrottle() {
        return autoUltraSoundThrottle;
    }
    
    public void setAutoUltraSoundThrottle(AutoControl autoUltraSoundThrottle) {
        this.autoUltraSoundThrottle = autoUltraSoundThrottle;
    }
    
    public AutoControl getAutoGpsThrottle() {
        return autoGpsThrottle;
    }
    
    public void setAutoGpsThrottle(AutoControl autoGpsThrottle) {
        this.autoGpsThrottle = autoGpsThrottle;
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
