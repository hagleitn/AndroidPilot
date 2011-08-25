package com.barbermot.pilot;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;

public class RemoteControl {
    
    public static final char FULL_MANUAL 	= 0xff;
    public static final char ELEVATOR_MASK	= 0x01;
    public static final char AILERON_MASK 	= 0x02;
    public static final char THROTTLE_MASK 	= 0x04;
    public static final char RUDDER_MASK 	= 0x08;
    public static final int  TIMEOUT 		= 20000;
    
    public RemoteControl(IOIO ioio, QuadCopter ufo, int aileronPin, int rudderPin, 
    		int throttlePin, int elevatorPin, int gainPin) {
    	controlMask = FULL_MANUAL;
    	pins = new EnumMap<QuadCopter.Direction,Integer>(QuadCopter.Direction.class);
    	pins.put(QuadCopter.Direction.LATERAL, aileronPin);
    	pins.put(QuadCopter.Direction.ROTATIONAL, rudderPin);
    	pins.put(QuadCopter.Direction.VERTICAL, throttlePin);
    	pins.put(QuadCopter.Direction.LONGITUDINAL, elevatorPin);
    	this.gainPin = gainPin;
    	this.ufo = ufo;
    }
    
    private int pulseIn(int pin, boolean val, int timeout) {
    	return 0;
    }
    
    public void update() throws ConnectionLostException {
    	if (controlMask == FULL_MANUAL || isEngaged()) {
    		controlMask = FULL_MANUAL;
    	}

    	char mask = 0x01;
    	for (QuadCopter.Direction d: QuadCopter.Direction.values()) {
    		if ((controlMask & mask) != 0) {
    			int value = pulseIn(pins.get(d),true,TIMEOUT);
    			ufo.writeRaw(d,value);
    		}
    		mask = (char) (mask << 1);
    	}
    }
    
    public void arm(boolean arm) {
    	armed = arm;
    }
        
    public void setControlMask(char mask) { 
    	controlMask = mask; 
    }
    
    public char getControlMask() { 
    	return controlMask; 
    }
    
    public boolean isEngaged() {
        int value = pulseIn(pins.get(QuadCopter.Direction.VERTICAL),true,TIMEOUT);
        if (armed) {
            int vertical = ufo.readRaw(QuadCopter.Direction.VERTICAL);
            return value > THROTTLE_MIN && (value+THROTTLE_DELTA) > vertical;
        } else {
            if (value < THROTTLE_MIN) {
                arm(true);
            }
            return false;
        }
    }
    
    private static final int THROTTLE_MIN 	= 1300; // min raw value from pulse in to take over
    private static final int THROTTLE_DELTA = 200;  // if manual throttle comes that close to current setting take over
    
    private char controlMask; // all bits set means manual control for the particular servo
    private EnumMap<QuadCopter.Direction,Integer> pins;
    private int gainPin;
    private boolean armed;
    private QuadCopter ufo;
}
