package com.barbermot.pilot.flight;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.PrintStream;
import java.util.concurrent.ScheduledExecutorService;

import android.util.Log;

import com.barbermot.pilot.pid.AutoControl;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.rc.RemoteControl;

public class FlightComputer implements Runnable {
    
    public final String TAG = "FlightComputer";
    
    // Flight computer states
    private enum State {
        GROUND, HOVER, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL, ENGAGING_AUTO_CONTROL
    };
    
    private FlightConfiguration      config;
    
    // the actual quad copter
    private QuadCopter               ufo;
    
    // RC signals (from RC controller)
    private RemoteControl            rc;
    
    // autopilot for throttle
    private AutoControl              autoThrottle;
    
    // autopilot for elevator
    private AutoControl              autoElevator;
    
    // autopilot for aileron
    private AutoControl              autoAileron;
    
    // autopilot for rudder
    private AutoControl              autoRudder;
    
    // values for the PID controller
    private float[]                  hoverConf;
    private float[]                  landingConf;
    private float[]                  orientationConf;
    
    // Log writer
    private PrintStream              printer;
    
    // min/max for the automatic control of the throttle
    private int                      minThrottle;
    private int                      maxThrottle;
    
    private int                      minTilt;
    private int                      maxTilt;
    
    private State                    state;
    
    private float                    height;
    private float                    zeroHeight;
    
    private float                    longitudinalDisplacement;
    private float                    lateralDisplacement;
    private float                    heading;
    
    private volatile long            time;
    private volatile long            lastTimeOrientationSignal;
    private volatile long            lastTimeHeightSignal;
    
    private int                      currentThrottle;
    private int                      currentElevator;
    private int                      currentAileron;
    private int                      currentRudder;
    
    private ScheduledExecutorService scheduler;
    
    public FlightComputer() {
        config = FlightConfiguration.get();
        
        this.hoverConf = config.getHoverConf();
        this.landingConf = config.getLandingConf();
        this.orientationConf = config.getOrientationConf();
        
        this.minThrottle = config.getMinThrottle();
        this.maxThrottle = config.getMaxThrottle();
        
        this.minTilt = config.getMinTilt();
        this.maxTilt = config.getMaxTilt();
        
        this.state = State.GROUND;
        
        time = System.currentTimeMillis();
        lastTimeHeightSignal = time;
        lastTimeOrientationSignal = time;
    }
    
    public void shutdown() {
        scheduler.shutdownNow();
    }
    
    public synchronized void takeoff(float height) {
        if (state == State.GROUND) {
            state = State.HOVER;
            autoThrottle.setConfiguration(hoverConf);
            autoThrottle.setGoal(height);
            autoThrottle.engage(true);
        }
    }
    
    public synchronized void hover(float height) {
        if (state == State.HOVER || state == State.LANDING
                || state == State.ENGAGING_AUTO_CONTROL) {
            state = State.HOVER;
            autoThrottle.setConfiguration(hoverConf);
            autoThrottle.setGoal(height);
            autoThrottle.engage(true);
        }
    }
    
    public synchronized void ground() throws ConnectionLostException {
        if (State.LANDING == state) {
            state = State.GROUND;
            autoThrottle.engage(false);
            ufo.throttle(QuadCopter.MIN_SPEED);
            currentThrottle = QuadCopter.MIN_SPEED;
        }
    }
    
    public synchronized void land() {
        if (state == State.HOVER || state == State.EMERGENCY_LANDING) {
            state = State.LANDING;
            autoThrottle.setConfiguration(landingConf);
            autoThrottle.setGoal(zeroHeight);
            autoThrottle.engage(true);
        }
    }
    
    public synchronized void emergencyDescent() throws ConnectionLostException {
        if (State.FAILED != state && State.GROUND != state
                && State.EMERGENCY_LANDING != state) {
            autoThrottle.engage(false);
            ufo.throttle(config.getEmergencyDescent());
            currentThrottle = config.getEmergencyDescent();
            state = State.EMERGENCY_LANDING;
        }
    }
    
    public synchronized void manualControl() throws ConnectionLostException {
        if (state != State.MANUAL_CONTROL) {
            autoThrottle.engage(false);
            stabilize(false);
            state = State.MANUAL_CONTROL;
        }
    }
    
    public synchronized void autoControl() throws ConnectionLostException {
        if (state == State.MANUAL_CONTROL) {
            state = State.ENGAGING_AUTO_CONTROL;
            
            // set rc to allow auto control of throttle
            char controlMask = rc.getControlMask();
            controlMask = (char) (controlMask & (~RemoteControl.THROTTLE_MASK));
            rc.setControlMask(controlMask);
            
            // disarm rc, so it doesn't immediately engage again
            rc.arm(false);
            
            // use current throttle setting and height for start values
            currentThrottle = ufo.read(QuadCopter.Direction.VERTICAL);
            hover(height);
        }
    }
    
    public synchronized void abort() throws ConnectionLostException {
        state = State.FAILED;
        autoThrottle.engage(false);
        stabilize(false);
        ufo.throttle(QuadCopter.MIN_SPEED);
        currentThrottle = QuadCopter.MIN_SPEED;
    }
    
    public synchronized void stabilize(boolean engage)
            throws ConnectionLostException {
        char mask = RemoteControl.AILERON_MASK | RemoteControl.ELEVATOR_MASK
                | RemoteControl.RUDDER_MASK;
        char controlMask = rc.getControlMask();
        
        if (engage) {
            controlMask = (char) (controlMask & ~mask);
        } else {
            controlMask = (char) (controlMask | mask);
            currentElevator = QuadCopter.STOP_SPEED;
            currentAileron = QuadCopter.STOP_SPEED;
            currentRudder = QuadCopter.STOP_SPEED;
        }
        rc.setControlMask(controlMask);
        
        autoElevator.setConfiguration(orientationConf);
        autoAileron.setConfiguration(orientationConf);
        autoRudder.setConfiguration(orientationConf);
        autoElevator.setGoal(0);
        autoAileron.setGoal(0);
        autoRudder.setGoal(0);
        autoElevator.engage(engage);
        autoAileron.engage(engage);
        autoRudder.engage(engage);
    }
    
    public synchronized void run() {
        try {
            time = System.currentTimeMillis();
            
            // the following state transitions can origin in any state
            
            // allow for manual inputs first
            if (rc.getControlMask() == RemoteControl.FULL_MANUAL) {
                manualControl();
            }
            
            // no height signal from ultra sound try descending
            if (time - lastTimeHeightSignal > config.getEmergencyDelta()) {
                Log.d(TAG, "Time: " + time + ", last height: "
                        + lastTimeHeightSignal);
                emergencyDescent();
            }
            
            // here are specific transitions
            
            switch (state) {
                case GROUND:
                    // calibration
                    zeroHeight = height;
                    break;
                case HOVER:
                    // nothing
                    break;
                case LANDING:
                    // turn off throttle when close to ground
                    if (height <= zeroHeight + config.getThrottleOffHeight()) {
                        ground();
                    }
                    break;
                case EMERGENCY_LANDING:
                    // if we have another reading land
                    if (time - lastTimeHeightSignal < config
                            .getEmergencyDelta()) {
                        land();
                    }
                    break;
                case MANUAL_CONTROL:
                    // nothing
                    break;
                case FAILED:
                    // nothing
                    break;
                case ENGAGING_AUTO_CONTROL:
                    // nothing
                    break;
                default:
                    // this is bad
                    land();
                    break;
            }
        } catch (ConnectionLostException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void setHoverConfiguration(float[] conf) {
        hoverConf = conf;
    }
    
    public void setLandingConfiguration(float[] conf) {
        landingConf = conf;
    }
    
    public void setStabilizerConfiguration(float[] conf) {
        orientationConf = conf;
    }
    
    public void setMinThrottle(int min) {
        this.minThrottle = min;
    }
    
    public void setMaxThrottle(int max) {
        this.maxThrottle = max;
    }
    
    public float getLongitudinalDisplacement() {
        return longitudinalDisplacement;
    }
    
    public void setLongitudinalDisplacement(float longitudinalDisplacement) {
        this.longitudinalDisplacement = longitudinalDisplacement;
    }
    
    public float getLateralDisplacement() {
        return lateralDisplacement;
    }
    
    public void setLateralDisplacement(float lateralDisplacement) {
        this.lateralDisplacement = lateralDisplacement;
    }
    
    public QuadCopter getUfo() {
        return ufo;
    }
    
    public void setUfo(QuadCopter ufo) {
        this.ufo = ufo;
    }
    
    public AutoControl getAutoThrottle() {
        return autoThrottle;
    }
    
    public void setAutoThrottle(AutoControl autoThrottle) {
        this.autoThrottle = autoThrottle;
    }
    
    public AutoControl getAutoElevator() {
        return autoElevator;
    }
    
    public void setAutoElevator(AutoControl autoElevator) {
        this.autoElevator = autoElevator;
    }
    
    public AutoControl getAutoAileron() {
        return autoAileron;
    }
    
    public void setAutoAileron(AutoControl autoAileron) {
        this.autoAileron = autoAileron;
    }
    
    public AutoControl getAutoRudder() {
        return autoRudder;
    }
    
    public void setAutoRudder(AutoControl autoRudder) {
        this.autoRudder = autoRudder;
    }
    
    public PrintStream getPrinter() {
        return printer;
    }
    
    public void setPrinter(PrintStream printer) {
        this.printer = printer;
    }
    
    public int getMinThrottle() {
        return minThrottle;
    }
    
    public int getMaxThrottle() {
        return maxThrottle;
    }
    
    public void setMinTilt(int minTilt) {
        this.minTilt = minTilt;
    }
    
    public int getMinTilt() {
        return minTilt;
    }
    
    public void setMaxTilt(int maxTilt) {
        this.maxTilt = maxTilt;
    }
    
    public int getMaxTilt() {
        return maxTilt;
    }
    
    public long getLastTimeOrientationSignal() {
        return lastTimeOrientationSignal;
    }
    
    public void setLastTimeOrientationSignal(long lastTimeOrientationSignal) {
        this.lastTimeOrientationSignal = lastTimeOrientationSignal;
    }
    
    public long getLastTimeHeightSignal() {
        return lastTimeHeightSignal;
    }
    
    public void setLastTimeHeightSignal(long lastTimeHeightSignal) {
        this.lastTimeHeightSignal = lastTimeHeightSignal;
    }
    
    public void setExecutor(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    
    public ScheduledExecutorService getExecutor() {
        return scheduler;
    }
    
    public RemoteControl getRc() {
        return rc;
    }
    
    public void setRc(RemoteControl rc) {
        this.rc = rc;
    }
    
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public float getHeight() {
        return height;
    }
    
    public void setHeight(float height) {
        this.height = height;
    }
    
    public float getHeading() {
        return heading;
    }
    
    public void setHeading(float heading) {
        this.heading = heading;
    }
    
    public long getTime() {
        return time;
    }
    
    public void setTime(long time) {
        this.time = time;
    }
    
    public int getCurrentThrottle() {
        return currentThrottle;
    }
    
    public void setCurrentThrottle(int currentThrottle) {
        this.currentThrottle = currentThrottle;
    }
    
    public int getCurrentElevator() {
        return currentElevator;
    }
    
    public void setCurrentElevator(int currentElevator) {
        this.currentElevator = currentElevator;
    }
    
    public int getCurrentAileron() {
        return currentAileron;
    }
    
    public void setCurrentAileron(int currentAileron) {
        this.currentAileron = currentAileron;
    }
    
    public int getCurrentRudder() {
        return currentRudder;
    }
    
    public void setCurrentRudder(int currentRudder) {
        this.currentRudder = currentRudder;
    }
    
}