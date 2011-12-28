package com.barbermot.pilot.quad;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;
import java.util.Map;

/**
 * Interface to the QuadCopter's servos. Allows to directly set values for all
 * control dimensions.
 * 
 */
public class QuadCopter {
    
    public enum Direction {
        LONGITUDINAL, LATERAL, VERTICAL, ROTATIONAL
    };
    
    public static final int             MIN_SPEED  = -100;
    public static final int             STOP_SPEED = 0;
    public static final int             MAX_SPEED  = 100;
    
    public static final int             MIN_SERVO  = 1050; // measured min
                                                           // activation
    public static final int             MAX_SERVO  = 1950; // measured max
                                                           // response
                                                           
    private EnumMap<Direction, Servo>   servos;
    private Servo                       gain;
    
    private EnumMap<Direction, Integer> pins;
    
    @SuppressWarnings("unused")
    private int                         gainPin;
    
    /*
     * Color map for GU-344 gyroscope pins (available with GAUI 330X)
     * aileronPin; // White rudderPin; // Yellow throttlePin; // Orange
     * elevatorPin; // Red gainPin; // Green (Gain/Gear)
     */
    public QuadCopter(IOIO ioio, int aileronPin, int rudderPin,
            int throttlePin, int elevatorPin, int gainPin)
            throws ConnectionLostException {
        pins = new EnumMap<Direction, Integer>(Direction.class);
        pins.put(Direction.LONGITUDINAL, elevatorPin); // Red
        pins.put(Direction.LATERAL, aileronPin); // White
        pins.put(Direction.VERTICAL, throttlePin); // Orange
        pins.put(Direction.ROTATIONAL, rudderPin); // Yellow
        
        servos = new EnumMap<Direction, Servo>(Direction.class);
        
        for (Direction d : Direction.values()) {
            servos.put(d, new Servo(ioio, pins.get(d), MIN_SPEED, MAX_SPEED,
                    MIN_SERVO, MAX_SERVO));
        }
        
        this.gainPin = gainPin;
        gain = new Servo(ioio, gainPin, MIN_SPEED, MAX_SPEED, MIN_SERVO,
                MAX_SERVO);
        
    }
    
    public boolean isInverted(Direction d) {
        return servos.get(d).isInverted();
    }
    
    public void invert(Direction d, boolean on) {
        servos.get(d).invert(on);
    }
    
    public void move(int x, int y, int z, int r) throws ConnectionLostException {
        move(Direction.LONGITUDINAL, x);
        move(Direction.LATERAL, y);
        move(Direction.VERTICAL, z);
        move(Direction.ROTATIONAL, r);
    }
    
    public void move(Map<Direction, Integer> m) throws ConnectionLostException {
        for (Direction d : m.keySet()) {
            move(d, m.get(d));
        }
    }
    
    public void move(Direction d, int speed) throws ConnectionLostException {
        if (speed > MAX_SPEED) {
            speed = MAX_SPEED;
        } else if (speed < MIN_SPEED) {
            speed = MIN_SPEED;
        }
        
        Servo s = servos.get(d);
        
        if (speed != s.read()) {
            s.write(speed);
        }
    }
    
    public void stop(Direction d) throws ConnectionLostException {
        move(d, STOP_SPEED);
    }
    
    public void stop() throws ConnectionLostException {
        for (Direction d : Direction.values()) {
            stop(d);
        }
    }
    
    public int read(Direction d) {
        return servos.get(d).read();
    }
    
    public void throttle(int speed) throws ConnectionLostException {
        move(Direction.VERTICAL, speed);
    }
    
    public void elevator(int speed) throws ConnectionLostException {
        move(Direction.LONGITUDINAL, speed);
    }
    
    public void aileron(int speed) throws ConnectionLostException {
        move(Direction.LATERAL, speed);
    }
    
    public void rudder(int speed) throws ConnectionLostException {
        move(Direction.ROTATIONAL, speed);
    }
    
    public void adjustGain(int value) throws ConnectionLostException {
        gain.write(value);
    }
    
    public int readRaw(Direction d) {
        return servos.get(d).readRaw();
    }
    
    public void writeRaw(Direction d, int ms) throws ConnectionLostException {
        servos.get(d).writeRaw(ms);
    }
}
