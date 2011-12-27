package com.barbermot.pilot.flight;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.PrintStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import com.barbermot.pilot.flight.state.FlightState;
import com.barbermot.pilot.flight.state.StateEvent;
import com.barbermot.pilot.pid.AutoControl;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.rc.RemoteControl;

public class FlightComputer implements Runnable {
    
    private static final Logger logger = Logger.getLogger("FlightComputer");
    
    private FlightState<?>      state;
    
    private FlightConfiguration config;
    
    // the actual QuadCopter
    private QuadCopter          ufo;
    
    // RC signals (from RC controller)
    private RemoteControl       rc;
    
    // autopilot for throttle
    private AutoControl         autoThrottle;
    
    // autopilot for elevator
    private AutoControl         autoElevator;
    
    // autopilot for aileron
    private AutoControl         autoAileron;
    
    // autopilot for rudder
    private AutoControl         autoRudder;
    
    // values for the PID controller
    private float[]             hoverConf;
    private float[]             landingConf;
    private float[]             orientationConf;
    private float[]             gpsConf;
    
    // Log writer
    private PrintStream         printer;
    
    // min/max for the automatic control of the throttle
    private int                 minThrottle;
    private int                 maxThrottle;
    
    private int                 minTilt;
    private int                 maxTilt;
    
    private float               minTiltAngle;
    private float               maxTiltAngle;
    
    private float               minSpeed;
    private float               maxSpeed;
    
    private float               height;
    private float               zeroHeight;
    
    private Waypoint            currentLocation;
    private Waypoint            zeroLocation;
    
    private float               goalHeight;
    
    private float               longitudinalDisplacement;
    private float               zeroLongitudinalDisplacement;
    private float               lateralDisplacement;
    private float               zeroLateralDisplacement;
    
    public float getZeroLongitudinalDisplacement() {
        return zeroLongitudinalDisplacement;
    }
    
    public void setZeroLongitudinalDisplacement(
            float zeroLongitudinalDisplacement) {
        this.zeroLongitudinalDisplacement = zeroLongitudinalDisplacement;
    }
    
    public float getZeroLateralDisplacement() {
        return zeroLateralDisplacement;
    }
    
    public void setZeroLateralDisplacement(float zeroLateralDisplacement) {
        this.zeroLateralDisplacement = zeroLateralDisplacement;
    }
    
    private float                    heading;
    
    private volatile long            time;
    private volatile long            lastTimeOrientationSignal;
    private volatile long            lastTimeHeightSignal;
    private volatile long            lastTimeGpsHeight;
    
    private int                      currentThrottle;
    private int                      currentElevator;
    private int                      currentAileron;
    private int                      currentRudder;
    
    private ScheduledExecutorService scheduler;
    
    public FlightComputer() {
        config = FlightConfiguration.get();
        
        currentLocation = new Waypoint(0, 0, 0);
        zeroLocation = new Waypoint(0, 0, 0);
        
        this.hoverConf = config.getHoverConf();
        this.landingConf = config.getLandingConf();
        this.orientationConf = config.getOrientationConf();
        this.gpsConf = config.getGpsConf();
        
        this.minThrottle = config.getMinThrottle();
        this.maxThrottle = config.getMaxThrottle();
        
        this.minTilt = config.getMinTilt();
        this.maxTilt = config.getMaxTilt();
        
        this.minSpeed = config.getMinSpeed();
        this.maxSpeed = config.getMaxSpeed();
        
        this.minTiltAngle = config.getMinTiltAngle();
        this.maxTiltAngle = config.getMaxTiltAngle();
        
        time = System.currentTimeMillis();
        lastTimeHeightSignal = time;
        lastTimeOrientationSignal = time;
    }
    
    public void shutdown() {
        scheduler.shutdownNow();
    }
    
    public synchronized void takeoff(float height)
            throws ConnectionLostException {
        state.transition(new StateEvent<Float>(FlightState.Type.HOVER, height));
    }
    
    public synchronized void hover(float height) throws ConnectionLostException {
        state.transition(new StateEvent<Float>(FlightState.Type.HOVER, height));
    }
    
    public void waypoint(float height) throws ConnectionLostException {
        Waypoint wp;
        try {
            wp = (Waypoint) currentLocation.clone();
            wp.altitude = height;
            state.transition(new StateEvent<Waypoint>(
                    FlightState.Type.WAYPOINT_HOLD, wp));
        } catch (CloneNotSupportedException e) {
            logger.warning("Waypoint clone not supported.");
        }
    }
    
    public synchronized void ground() throws ConnectionLostException {
        state.transition(new StateEvent<Void>(FlightState.Type.GROUND, null));
    }
    
    public synchronized void land() throws ConnectionLostException {
        state.transition(new StateEvent<Float>(FlightState.Type.LANDING, null));
    }
    
    public synchronized void emergencyDescent() throws ConnectionLostException {
        state.transition(new StateEvent<Float>(
                FlightState.Type.EMERGENCY_LANDING, height));
    }
    
    public synchronized void manualControl() throws ConnectionLostException {
        state.transition(new StateEvent<Float>(FlightState.Type.MANUAL_CONTROL,
                null));
    }
    
    public synchronized void autoControl() throws ConnectionLostException {
        state.transition(new StateEvent<Void>(FlightState.Type.LANDING, null));
    }
    
    public synchronized void abort() throws ConnectionLostException {
        state.transition(new StateEvent<Float>(FlightState.Type.FAILED, null));
    }
    
    public synchronized void stabilize(boolean b)
            throws ConnectionLostException {
        if (b) {
            state.transition(new StateEvent<Float>(
                    FlightState.Type.STABILIZED_HOVER, goalHeight));
        } else {
            state.transition(new StateEvent<Float>(FlightState.Type.HOVER,
                    goalHeight));
        }
    }
    
    public synchronized void forward(int speed) {
        if (state.getType() == FlightState.Type.STABILIZED_HOVER) {
            float angle = map(speed, minSpeed, maxSpeed, minTiltAngle,
                    maxTiltAngle);
            logger.info("Setting pitch angle to: " + angle);
            autoElevator.setGoal(angle + getZeroLongitudinalDisplacement());
        }
    }
    
    public synchronized void sideways(int speed) {
        if (state.getType() == FlightState.Type.STABILIZED_HOVER) {
            float angle = map(speed, minSpeed, maxSpeed, minTiltAngle,
                    maxTiltAngle);
            logger.info("Setting roll angle to: " + angle);
            autoAileron.setGoal(angle + getZeroLateralDisplacement());
        }
    }
    
    public synchronized void rotate(int angle) {
        if (state.getType() == FlightState.Type.STABILIZED_HOVER) {
            float radian = map(angle, -180, 180, (float) -Math.PI,
                    (float) Math.PI);
            logger.info("Setting yaw angle to: " + radian);
            autoRudder.setGoal(radian);
        }
    }
    
    private float map(float value, float minIn, float maxIn, float minOut,
            float maxOut) {
        return ((value - minIn) / (maxIn - minIn)) * (maxOut - minOut) + minOut;
    }
    
    public void balance() {
        setZeroLateralDisplacement(getLateralDisplacement());
        setZeroLongitudinalDisplacement(getLongitudinalDisplacement());
    }
    
    public void calibrate() {
        setZeroHeight(getHeight());
        setZeroGpsHeight(getGpsHeight());
        setZeroLatitude(getLatitude());
        setZeroLongitude(getLongitude());
        balance();
    }
    
    public synchronized void run() {
        try {
            time = System.currentTimeMillis();
            
            // the following state transitions can origin in any state
            
            // allow for manual inputs first
            if (rc.getControlMask() == RemoteControl.FULL_MANUAL
                    && state.getType() != FlightState.Type.MANUAL_CONTROL) {
                logger.info("Manual control is engaged");
                manualControl();
            }
            
            state.update();
        } catch (ConnectionLostException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean hasHeightSignal() {
        return (time - lastTimeHeightSignal) < config.getEmergencyDelta();
    }
    
    public boolean hasGpsSignal() {
        return (time - lastTimeGpsHeight) < config.getEmergencyDeltaGps();
    }
    
    public float[] getHoverConf() {
        return hoverConf;
    }
    
    public void setHoverConf(float[] hoverConf) {
        this.hoverConf = hoverConf;
    }
    
    public float[] getLandingConf() {
        return landingConf;
    }
    
    public void setLandingConf(float[] landingConf) {
        this.landingConf = landingConf;
    }
    
    public float[] getOrientationConf() {
        return orientationConf;
    }
    
    public void setOrientationConf(float[] orientationConf) {
        this.orientationConf = orientationConf;
    }
    
    public float[] getGpsConf() {
        return gpsConf;
    }
    
    public void setGpsConf(float[] gpsConf) {
        this.gpsConf = gpsConf;
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
    
    public FlightState<?> getState() {
        return state;
    }
    
    public void setState(FlightState<?> state) {
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
    
    public void setGpsHeight(float gpsHeight) {
        this.currentLocation.altitude = gpsHeight;
    }
    
    public float getGpsHeight() {
        return currentLocation.altitude;
    }
    
    public long getLastTimeGpsHeight() {
        return lastTimeGpsHeight;
    }
    
    public void setLastTimeGpsHeight(long lastTimeGpsHeight) {
        this.lastTimeGpsHeight = lastTimeGpsHeight;
    }
    
    public float getZeroHeight() {
        return zeroHeight;
    }
    
    public void setZeroHeight(float zeroHeight) {
        this.zeroHeight = zeroHeight;
    }
    
    public float getZeroGpsHeight() {
        return zeroLocation.altitude;
    }
    
    public void setZeroGpsHeight(float zeroGpsHeight) {
        this.zeroLocation.altitude = zeroGpsHeight;
    }
    
    public void setGoalHeight(float height) {
        this.goalHeight = height;
    }
    
    public float getLatitude() {
        return currentLocation.latitude;
    }
    
    public void setLatitude(float lat) {
        currentLocation.latitude = lat;
    }
    
    public float getLongitude() {
        return currentLocation.longitude;
    }
    
    public void setLongitude(float lon) {
        currentLocation.longitude = lon;
    }
    
    public void setZeroLatitude(float latitude) {
        zeroLocation.latitude = latitude;
    }
    
    public void setZeroLongitude(float longitude) {
        zeroLocation.longitude = longitude;
    }
    
    public float getZeroLatitude() {
        return zeroLocation.latitude;
    }
    
    public float getZeroLongitude() {
        return zeroLocation.longitude;
    }
}