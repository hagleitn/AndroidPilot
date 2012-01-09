package com.barbermot.pilot.rc;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.concurrent.TimeoutException;

import com.barbermot.pilot.quad.QuadCopter;

/**
 * RemoteControl is a periodic tasks that checks whether the user has engaged
 * manual override of the quad copter controls via opening the throttle on the
 * RC controller.
 * 
 * It also allows fine grained control over which control dimensions are
 * manually v. automatically controlled.
 * 
 */
public class ExternalRemote extends RemoteControl implements Runnable {
    
    public static final float TIMEOUT = 0.5f;
    
    @SuppressWarnings("unused")
    private int               gainPin;
    
    IOIO                      ioio;
    DigitalOutput             overridePins[];
    final static int          SIZE    = 4;
    int                       throttleMonitorPin;
    
    public ExternalRemote(IOIO ioio, QuadCopter ufo, int aileronPin,
            int rudderPin, int throttlePin, int elevatorPin,
            int throttleMonitorPin, int gainPin) throws ConnectionLostException {
        super(ufo);
        this.gainPin = gainPin;
        this.ioio = ioio;
        
        this.throttleMonitorPin = throttleMonitorPin;
        
        overridePins = new DigitalOutput[SIZE];
        
        overridePins[0] = ioio.openDigitalOutput(elevatorPin);
        overridePins[1] = ioio.openDigitalOutput(aileronPin);
        overridePins[2] = ioio.openDigitalOutput(throttlePin);
        overridePins[3] = ioio.openDigitalOutput(rudderPin);
        this.setControlMask(FULL_MANUAL);
    }
    
    protected int readManualThrottle() throws ConnectionLostException,
            TimeoutException {
        PulseInput pulse = ioio.openPulseInput(throttleMonitorPin,
                PulseMode.POSITIVE);
        int pw;
        try {
            while (true) {
                try {
                    pw = (int) (pulse.getDuration(TIMEOUT) * 1000000);
                    break;
                } catch (InterruptedException e) {
                    // nothing
                }
            }
        } finally {
            if (pulse != null) {
                pulse.close();
            }
        }
        return pw;
    }
    
    public synchronized void setControlMask(char mask)
            throws ConnectionLostException {
        super.setControlMask(mask);
        
        mask = 0x1;
        
        for (int i = 0; i < SIZE; ++i) {
            overridePins[i].write((controlMask & mask) != 0);
            mask = (char) (mask << 1);
        }
    }
}
