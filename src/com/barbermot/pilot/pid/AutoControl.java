package com.barbermot.pilot.pid;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.logging.Logger;

import com.barbermot.pilot.signal.SignalListener;

/**
 * AutoControl is a generic PID controller. It connects signals with controls,
 * trying to achieve a specified goal by making adjustments to the control it is
 * given.
 * 
 */
public class AutoControl implements SignalListener {
    
    protected Logger        logger;
    private ControlListener control;
    private float           proportional;
    private float           integral;
    private float           derivative;
    private float           lastError;
    private long            lastTime;
    private float           cummulativeError;
    private float           maxCummulative;
    private float           minCummulative;
    private boolean         engaged;
    private boolean         isFirst;
    protected float         goal;
    
    public AutoControl(ControlListener control) {
        this(control, Logger.getLogger("AutoControl"));
    }
    
    public AutoControl(ControlListener control, Logger logger) {
        this.control = control;
        this.isFirst = true;
        this.logger = logger;
    }
    
    @Override
    public void update(float value, long time) throws ConnectionLostException {
        if (engaged) {
            
            if (isFirst) {
                isFirst = false;
                lastTime = time;
                lastError = computeError(value);
                cummulativeError = 0;
                return;
            }
            
            float pTotal;
            float iTotal;
            float dTotal;
            
            float timeDelta = time - lastTime;
            
            if (timeDelta <= 0) {
                logger.info("Message from the past: " + timeDelta);
                return;
            }
            
            float error = computeError(value);
            float errorDelta = error - lastError;
            
            // simple adjustment proportional to the error
            pTotal = proportional * error;
            
            // reacts to the length of an error
            cummulativeError += error * timeDelta;
            if (cummulativeError > maxCummulative) {
                cummulativeError = maxCummulative;
            } else if (cummulativeError < minCummulative) {
                cummulativeError = minCummulative;
            }
            iTotal = integral * cummulativeError;
            
            // adjustment to react to the closing speed
            dTotal = derivative * (errorDelta / timeDelta);
            
            float gTotal = pTotal + iTotal + dTotal;
            
            lastError = error;
            lastTime = time;
            
            control.adjust(gTotal);
        }
    }
    
    /**
     * Implementation interface to specify the method to compute the error
     * (difference between signal value and goal). Default implementation simply
     * subtracts the value from the goal.
     * 
     * @param value
     *            Current signal value
     * @return Directed (signed) error
     */
    protected float computeError(float value) {
        return goal - value;
    }
    
    /**
     * setConfiguration sets values for proportional, integral and derivative
     * factors for the PID controller. It also requires min and max values for
     * the integral to take.
     * 
     * @param conf
     *            Array of five float values: proportianal, integral,
     *            derivative, min integral, max integral
     */
    public void setConfiguration(float[] conf) {
        proportional = conf[0];
        integral = conf[1];
        derivative = conf[2];
        minCummulative = conf[3];
        maxCummulative = conf[4];
        isFirst = true;
    }
    
    public float getProportional() {
        return proportional;
    }
    
    public void setProportional(float proportional) {
        this.proportional = proportional;
    }
    
    public float getIntegral() {
        return integral;
    }
    
    public void setIntegral(float integral) {
        this.integral = integral;
    }
    
    public float getDerivative() {
        return derivative;
    }
    
    public void setDerivative(float derivative) {
        this.derivative = derivative;
    }
    
    public float getMaxCummulative() {
        return maxCummulative;
    }
    
    public void setMaxCummulative(float max) {
        this.maxCummulative = max;
    }
    
    public float getMinCummulative() {
        return minCummulative;
    }
    
    public void setMinCummulative(float min) {
        this.minCummulative = min;
    }
    
    public float getGoal() {
        return goal;
    }
    
    public void setGoal(float goal) {
        this.goal = goal;
        isFirst = true;
    }
    
    /**
     * engage starts and stops the pid controller. While stopped all signals are
     * ignored and no inputs are sent to the control.
     * 
     * @param engaged
     *            boolean to start/stop the pid controller
     */
    public void engage(boolean engaged) {
        logger.info("AutoThrottle " + (engaged ? "engaged" : "disengaged"));
        this.engaged = engaged;
    }
    
    public boolean isEngaged() {
        return engaged;
    }
}
