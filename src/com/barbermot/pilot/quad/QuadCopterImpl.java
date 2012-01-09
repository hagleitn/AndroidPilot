package com.barbermot.pilot.quad;

import static com.barbermot.pilot.quad.QuadCopter.Direction.LATERAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.LONGITUDINAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.ROTATIONAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.VERTICAL;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;

/**
 * Interface to the QuadCopter's servos. Allows to directly set values for all
 * control dimensions.
 * 
 */
public class QuadCopterImpl extends QuadCopter {
    
    public static final int             MIN_SERVO = 1050; // measured min
                                                          // activation
    public static final int             MAX_SERVO = 1950; // measured max
                                                          // response
                                                          
    EnumMap<Direction, Servo>           servos;
    Servo                               gain;
    
    private EnumMap<Direction, Integer> pins;
    
    /*
     * Color map for GU-344 gyroscope pins (available with GAUI 330X)
     * aileronPin; // White rudderPin; // Yellow throttlePin; // Orange
     * elevatorPin; // Red gainPin; // Green (Gain/Gear)
     */
    public QuadCopterImpl(IOIO ioio, int aileronPin, int rudderPin,
            int throttlePin, int elevatorPin, int gainPin)
            throws ConnectionLostException {
        pins = new EnumMap<Direction, Integer>(Direction.class);
        pins.put(LONGITUDINAL, elevatorPin); // Red
        pins.put(LATERAL, aileronPin); // White
        pins.put(VERTICAL, throttlePin); // Orange
        pins.put(ROTATIONAL, rudderPin); // Yellow
        
        servos = new EnumMap<Direction, Servo>(Direction.class);
        
        for (Direction d : Direction.values()) {
            servos.put(d, new Servo(ioio, pins.get(d), MIN_SPEED, MAX_SPEED,
                    MIN_SERVO, MAX_SERVO));
        }
        
        gain = new Servo(ioio, gainPin, MIN_SPEED, MAX_SPEED, MIN_SERVO,
                MAX_SERVO);
        
    }
    
    @Override
    public void adjustGain(int value) throws ConnectionLostException {
        gain.write(value);
    }
    
    @Override
    public int readRaw(Direction d) {
        return servos.get(d).readRaw();
    }
    
    @Override
    public void writeRaw(Direction d, int ms) throws ConnectionLostException {
        servos.get(d).writeRaw(ms);
    }
    
    @Override
    public int read(Direction d) {
        return servos.get(d).read();
    }
    
    @Override
    public boolean isInverted(Direction d) {
        return servos.get(d).isInverted();
    }
    
    @Override
    public void invert(Direction d, boolean on) {
        servos.get(d).invert(on);
    }
    
    @Override
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
    
    @Override
    public int convert(Direction d, int speed) {
        return servos.get(d).convert(speed);
    }
}
