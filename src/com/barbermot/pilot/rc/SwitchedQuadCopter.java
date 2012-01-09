package com.barbermot.pilot.rc;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;

import com.barbermot.pilot.quad.QuadCopter;

public class SwitchedQuadCopter extends QuadCopter {
    
    private QuadCopter                  ufo;
    private EnumMap<Direction, Boolean> enabledMap;
    private EnumMap<Direction, Integer> valueMap;
    
    public SwitchedQuadCopter() {
        enabledMap = new EnumMap<Direction, Boolean>(Direction.class);
        valueMap = new EnumMap<Direction, Integer>(Direction.class);
        for (Direction d : Direction.values()) {
            enabledMap.put(d, false);
            valueMap.put(d, QuadCopter.STOP_SPEED);
        }
        valueMap.put(Direction.VERTICAL, QuadCopter.MIN_SPEED);
    }
    
    public void setQuadCopter(QuadCopter ufo) {
        this.ufo = ufo;
    }
    
    public void enable(Direction d) {
        enabledMap.put(d, true);
    }
    
    public void disable(Direction d) {
        enabledMap.put(d, false);
    }
    
    public boolean isEnabled(Direction d) {
        return enabledMap.get(d);
    }
    
    @Override
    public void move(Direction d, int speed) throws ConnectionLostException {
        if (enabledMap.get(d)) {
            ufo.move(d, speed);
        } else {
            valueMap.put(d, speed);
        }
    }
    
    @Override
    public boolean isInverted(Direction d) {
        return ufo.isInverted(d);
    }
    
    @Override
    public void invert(Direction d, boolean on) {
        ufo.invert(d, on);
    }
    
    @Override
    public int read(Direction d) {
        if (enabledMap.get(d)) {
            return ufo.read(d);
        } else {
            return valueMap.get(d);
        }
    }
    
    @Override
    public void adjustGain(int value) throws ConnectionLostException {
        ufo.adjustGain(value);
    }
    
    @Override
    public int readRaw(Direction d) {
        return convert(d, read(d));
    }
    
    @Override
    public void writeRaw(Direction d, int ms) throws ConnectionLostException {
        ufo.writeRaw(d, ms);
    }
    
    @Override
    public int convert(Direction d, int speed) {
        return ufo.convert(d, speed);
    }
}
