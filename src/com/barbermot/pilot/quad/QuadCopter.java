package com.barbermot.pilot.quad;

import static com.barbermot.pilot.quad.QuadCopter.Direction.LATERAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.LONGITUDINAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.ROTATIONAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.VERTICAL;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.Map;

public abstract class QuadCopter {
    
    public enum Direction {
        LONGITUDINAL, LATERAL, VERTICAL, ROTATIONAL
    }
    
    public static final int MIN_SPEED  = -100;
    public static final int STOP_SPEED = 0;
    public static final int MAX_SPEED  = 100;
    
    public QuadCopter() {
        super();
    }
    
    public abstract void move(Direction d, int speed)
            throws ConnectionLostException;
    
    public abstract boolean isInverted(Direction d);
    
    public abstract void invert(Direction d, boolean on);
    
    public abstract int read(Direction d);
    
    public abstract void adjustGain(int value) throws ConnectionLostException;
    
    public abstract int readRaw(Direction d);
    
    public abstract void writeRaw(Direction d, int ms)
            throws ConnectionLostException;
    
    public void move(int x, int y, int z, int r) throws ConnectionLostException {
        move(LONGITUDINAL, x);
        move(LATERAL, y);
        move(VERTICAL, z);
        move(ROTATIONAL, r);
    }
    
    public void move(Map<Direction, Integer> m) throws ConnectionLostException {
        for (Direction d : m.keySet()) {
            move(d, m.get(d));
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
    
    public void throttle(int speed) throws ConnectionLostException {
        move(VERTICAL, speed);
    }
    
    public void elevator(int speed) throws ConnectionLostException {
        move(LONGITUDINAL, speed);
    }
    
    public void aileron(int speed) throws ConnectionLostException {
        move(LATERAL, speed);
    }
    
    public void rudder(int speed) throws ConnectionLostException {
        move(ROTATIONAL, speed);
    }
    
    public abstract int convert(Direction d, int speed);
}