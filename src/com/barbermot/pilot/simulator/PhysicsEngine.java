package com.barbermot.pilot.simulator;

import java.io.IOException;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.util.PassThruFormatter;

public class PhysicsEngine {
    
    private Logger      logger;
    private double      msl;
    private int         lastThrottle;
    private long        lastMillis;
    private double      acceleration;
    private double      speed;
    private double      copterHeight = 0.1;
    private double      multiplier   = 10 / 93.0;
    private double      g            = 10;
    Random              rand;
    FlightConfiguration config;
    int                 throttlePin;
    int                 ultrasoundPin;
    private double      deviation    = 0.01;
    
    public PhysicsEngine() {
        logger = Logger.getLogger("PhysicsEngine");
        
        try {
            FileHandler handler = new FileHandler("/tmp/data%u.log");
            handler.setFormatter(new PassThruFormatter());
            handler.setFilter(null);
            logger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        lastMillis = System.currentTimeMillis();
        msl = copterHeight;
        rand = new Random();
        config = FlightConfiguration.get();
        throttlePin = config.getPinMap().get(
                FlightConfiguration.PinType.THROTTLE_OUT);
        ultrasoundPin = config.getPinMap().get(
                FlightConfiguration.PinType.ULTRA_SOUND);
    }
    
    private void updateHeight() {
        long time = System.currentTimeMillis();
        double tDelta = (time - lastMillis) / 1000.0;
        
        // f = thr*const-g
        acceleration = (multiplier * (lastThrottle + 75) - g);
        if (msl <= 0.1 && acceleration < 0) {
            acceleration = 0;
        }
        
        speed += tDelta * acceleration;
        
        msl += tDelta * speed;
        if (msl < copterHeight) {
            msl = copterHeight;
            speed = 0;
        }
        lastMillis = time;
        String msg = String.format("%d\t%f\t%f\t%f\t%f\t%d\n", lastMillis,
                tDelta, acceleration, speed, msl, lastThrottle);
        logger.info(msg);
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
        }
    }
    
    public synchronized int pulseIn(int pin) {
        int value = 0;
        
        if (pin == ultrasoundPin) {
            updateHeight();
            double r = normalDistribution();
            value = (int) (msl * 29 * 2 * 100 / 1.8f + r * deviation);
        }
        
        return value;
    }
    
    public void init() {}
    
    public float getGpsAlitude() {
        return 2;
    }
    
    public long getTime() {
        return System.currentTimeMillis();
    }
    
    public float getYawAngle() {
        return 4;
    }
    
    public float getRollAngle() {
        return 5;
    }
    
    public float getPitchAngle() {
        return 6;
    }
    
}
