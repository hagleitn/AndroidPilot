package com.barbermot.pilot.simulator;

import java.io.IOException;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.util.PassThruFormatter;

public class PhysicsEngine {
    
    private Logger       heightLogger;
    
    private double       msl;
    private int          lastThrottle;
    private long         lastMillisHeight;
    private long         baseMillis;
    private double       acceleration;
    private double       speed;
    
    private Angle        yaw;
    private Angle        roll;
    private Angle        pitch;
    
    private final double copterHeight         = 0.1;
    private final double rotationalMultiplier = (2 * Math.PI) / 35.0;
    private final double g                    = 10;
    private final double multiplier           = g / 93.0;
    
    Random               rand;
    FlightConfiguration  config;
    int                  throttlePin;
    int                  rudderPin;
    int                  aileronPin;
    int                  elevatorPin;
    int                  ultrasoundPin;
    private double       deviation            = 0.01;
    private double       gpsDeviation         = 1;
    
    class Angle {
        
        long   lastControlInput = 0;
        long   lastUpdateTime   = 0;
        double value;
        Logger angleLogger;
        
        Angle(Logger angleLogger, double value) {
            this.angleLogger = angleLogger;
            this.value = value;
        }
        
        void updateDirection() {
            long time = System.currentTimeMillis();
            double tDelta = (time - lastUpdateTime) / 1000.0;
            if (tDelta == 0) {
                return;
            }
            
            // no changes on the ground;
            if (msl > copterHeight) {
                // f = thr*const-g
                double rotationalSpeed = rotationalMultiplier
                        * ((double) lastControlInput);
                
                value += tDelta * rotationalSpeed;
                while (value >= Math.PI) {
                    value -= 2 * Math.PI;
                }
                
                while (value < -Math.PI) {
                    value += 2 * Math.PI;
                }
                
                String msg = String.format("%d\t%f\t%f\t%f\t%d\n", time
                        - baseMillis, tDelta, rotationalSpeed, value,
                        lastControlInput);
                angleLogger.info(msg);
            }
            lastUpdateTime = time;
        }
    }
    
    public PhysicsEngine() {
        heightLogger = Logger.getLogger("Height");
        Logger yawLogger = Logger.getLogger("Yaw");
        Logger rollLogger = Logger.getLogger("Roll");
        Logger pitchLogger = Logger.getLogger("Pitch");
        
        try {
            FileHandler handler = new FileHandler("/tmp/height%u.log");
            handler.setFormatter(new PassThruFormatter());
            handler.setFilter(null);
            heightLogger.addHandler(handler);
            
            handler = new FileHandler("/tmp/yaw%u.log");
            handler.setFormatter(new PassThruFormatter());
            handler.setFilter(null);
            yawLogger.addHandler(handler);
            
            handler = new FileHandler("/tmp/roll%u.log");
            handler.setFormatter(new PassThruFormatter());
            handler.setFilter(null);
            rollLogger.addHandler(handler);
            
            handler = new FileHandler("/tmp/pitch%u.log");
            handler.setFormatter(new PassThruFormatter());
            handler.setFilter(null);
            pitchLogger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        baseMillis = lastMillisHeight = System.currentTimeMillis();
        msl = copterHeight;
        
        yaw = new Angle(yawLogger, -Math.PI);
        roll = new Angle(rollLogger, 0.2);
        pitch = new Angle(pitchLogger, 0.2);
        
        rand = new Random();
        config = FlightConfiguration.get();
        throttlePin = config.getPinMap().get(
                FlightConfiguration.PinType.THROTTLE_OUT);
        rudderPin = config.getPinMap().get(
                FlightConfiguration.PinType.RUDDER_OUT);
        aileronPin = config.getPinMap().get(
                FlightConfiguration.PinType.AILERON_OUT);
        elevatorPin = config.getPinMap().get(
                FlightConfiguration.PinType.ELEVATOR_OUT);
        ultrasoundPin = config.getPinMap().get(
                FlightConfiguration.PinType.ULTRA_SOUND);
    }
    
    private void updateHeight() {
        long time = System.currentTimeMillis();
        double tDelta = (time - lastMillisHeight) / 1000.0;
        
        // f = thr*const-g
        acceleration = (multiplier * (lastThrottle + 75) - g);
        if (msl <= copterHeight && acceleration < 0) {
            acceleration = 0;
            speed = 0;
        }
        
        speed += tDelta * acceleration;
        
        msl += tDelta * speed;
        if (msl < copterHeight) {
            msl = copterHeight;
            speed = 0;
        }
        
        lastMillisHeight = time;
        String msg = String.format("%d\t%f\t%f\t%f\t%f\t%d\n", lastMillisHeight
                - baseMillis, tDelta, acceleration, speed, msl, lastThrottle);
        heightLogger.info(msg);
    }
    
    private double normalDistribution() {
        double r = Math.sqrt(-2 * Math.log(rand.nextDouble()))
                * Math.cos(2 * Math.PI * rand.nextDouble());
        return r;
    }
    
    private int mapReverse(float value, int minIn, int maxIn, int minOut,
            int maxOut) {
        return (int) (minIn + (maxIn - minIn) * (value - minOut)
                / (maxOut - minOut));
    }
    
    public void digitalWrite(int pin, boolean val) {}
    
    public synchronized void pulseOut(int pin, float pulseWidthUs) {
        
        if (pin == throttlePin) {
            updateHeight();
            lastThrottle = mapReverse(pulseWidthUs, QuadCopter.MIN_SPEED,
                    QuadCopter.MAX_SPEED, QuadCopter.MIN_SERVO,
                    QuadCopter.MAX_SERVO);
        } else if (pin == rudderPin) {
            yaw.updateDirection();
            yaw.lastControlInput = mapReverse(pulseWidthUs,
                    QuadCopter.MIN_SPEED, QuadCopter.MAX_SPEED,
                    QuadCopter.MIN_SERVO, QuadCopter.MAX_SERVO);
        } else if (pin == aileronPin) {
            roll.updateDirection();
            roll.lastControlInput = mapReverse(pulseWidthUs,
                    QuadCopter.MIN_SPEED, QuadCopter.MAX_SPEED,
                    QuadCopter.MIN_SERVO, QuadCopter.MAX_SERVO);
        } else if (pin == elevatorPin) {
            pitch.updateDirection();
            pitch.lastControlInput = mapReverse(pulseWidthUs,
                    QuadCopter.MIN_SPEED, QuadCopter.MAX_SPEED,
                    QuadCopter.MIN_SERVO, QuadCopter.MAX_SERVO);
        }
    }
    
    public synchronized int pulseIn(int pin) {
        int value = 0;
        
        if (pin == ultrasoundPin) {
            updateHeight();
            double r = normalDistribution();
            value = (int) (msl * 29 * 2 * 100 + r * deviation);
        }
        
        return value;
    }
    
    public void init() {}
    
    public float getLatitude() {
        return 0;
    }
    
    public float getLongitude() {
        return 0;
    }
    
    public float getGpsAlitude() {
        updateHeight();
        float deviation = (float) (gpsDeviation * normalDistribution());
        return (float) Math.round(msl + deviation);
    }
    
    public long getTime() {
        return System.currentTimeMillis();
    }
    
    public synchronized float getYawAngle() {
        yaw.updateDirection();
        return (float) yaw.value;
    }
    
    public float getRollAngle() {
        roll.updateDirection();
        return (float) roll.value;
    }
    
    public float getPitchAngle() {
        pitch.updateDirection();
        return (float) pitch.value;
    }
    
}
