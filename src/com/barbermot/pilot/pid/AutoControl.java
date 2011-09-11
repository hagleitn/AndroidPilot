package com.barbermot.pilot.pid;

import ioio.lib.api.exception.ConnectionLostException;
import android.util.Log;

import com.barbermot.pilot.signal.SignalListener;

public class AutoControl implements SignalListener {
    
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
    private String          TAG;
    private boolean         log;
    protected float         goal;
    
    public AutoControl(ControlListener control) {
        this(control, "AutoControl", false);
    }
    
    public AutoControl(ControlListener control, String tag, boolean log) {
        this.control = control;
        this.isFirst = true;
        this.log = log;
    }
    
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
                if (log) {
                    Log.d(TAG, "Message from the past: " + timeDelta);
                }
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
            
            if (log) {
                String msg = String.format(
                        "%s (error = %f, dT = %f): %f + %f + %f = %f", TAG,
                        error, timeDelta, pTotal, iTotal, dTotal, gTotal);
                Log.d(TAG, msg);
            }
            
            lastError = error;
            lastTime = time;
            
            control.adjust(gTotal);
        }
    }
    
    protected float computeError(float value) {
        return goal - value;
    }
    
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
    
    public void engage(boolean engaged) {
        if (log) {
            Log.d(TAG, "AutoThrottle " + (engaged ? "engaged" : "disengaged"));
        }
        this.engaged = engaged;
    }
    
    public boolean isEngaged() {
        return engaged;
    }
}
