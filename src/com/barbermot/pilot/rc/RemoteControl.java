package com.barbermot.pilot.rc;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.barbermot.pilot.quad.QuadCopter;

public abstract class RemoteControl implements Runnable {
    
    protected static final Logger logger         = Logger.getLogger("RemoteControl");
    public static final char      FULL_MANUAL    = 0xff;
    public static final char      ELEVATOR_MASK  = 0x01;
    public static final char      AILERON_MASK   = 0x02;
    public static final char      THROTTLE_MASK  = 0x04;
    public static final char      RUDDER_MASK    = 0x08;
    protected static final int    THROTTLE_MIN   = 1300;
    protected static final int    THROTTLE_DELTA = 200;
    protected char                controlMask;
    protected boolean             armed;
    protected QuadCopter          ufo;
    
    public RemoteControl(QuadCopter ufo) {
        super();
        this.ufo = ufo;
    }
    
    protected abstract int readManualThrottle() throws ConnectionLostException,
            TimeoutException;
    
    @Override
    public void run() {
        try {
            if (controlMask != FULL_MANUAL && isEngaged()) {
                setControlMask(FULL_MANUAL);
            }
        } catch (TimeoutException e) {
            logger.info("isEngaged timed out.");
        } catch (ConnectionLostException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void arm(boolean arm) {
        armed = arm;
    }
    
    /**
     * setControlMask changes which servos are controlled manually versus by the
     * software.
     * 
     * @param mask
     *            Bitmask specifying which controls are manually controlled
     * @throws ConnectionLostException
     */
    public synchronized void setControlMask(char mask)
            throws ConnectionLostException {
        String maskString = String.format("%h", mask);
        logger.info("Control mask: " + maskString);
        
        controlMask = mask;
    }
    
    public char getControlMask() {
        return controlMask;
    }
    
    /**
     * Tests whether the throttle is above the threshold that indicates manual
     * override.
     * 
     * @return true if manual override, false otherwise.
     * @throws ConnectionLostException
     * @throws TimeoutException
     */
    public boolean isEngaged() throws ConnectionLostException, TimeoutException {
        int value = readManualThrottle();
        if (armed) {
            int vertical = ufo.readRaw(QuadCopter.Direction.VERTICAL);
            return value > THROTTLE_MIN && (value + THROTTLE_DELTA) > vertical;
        } else {
            if (value < THROTTLE_MIN) {
                arm(true);
            }
            return false;
        }
    }
    
}