package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;
import ioio.lib.api.PingPin;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.logging.Logger;

public class UltrasoundSignal extends IoioSignal {
    
    @SuppressWarnings("unused")
    private final static Logger logger       = Logger.getLogger("UltrasoundSignal");
    
    public static final int     MAX_RELIABLE = 367;
    public PingPin              ping;
    
    public UltrasoundSignal(IOIO ioio, int pin) throws ConnectionLostException {
        super(ioio);
        ping = ioio.openPingInput(pin);
    }
    
    @Override
    protected float convert(long microseconds) throws MeasurementException {
        // The speed of sound is 340 m/s or 29 microseconds per centimeter.
        // The ping travels out and back, so to find the distance of the
        // object we take half of the distance traveled.
        float value = microseconds / 29 / 2;
        
        if (value > MAX_RELIABLE) {
            throw new MeasurementException();
        }
        
        // return meters
        return value / 100f;
    }
    
    @Override
    protected long measure() throws ConnectionLostException,
            MeasurementException {
        try {
            return ping.read();
        } catch (InterruptedException e) {
            throw new MeasurementException(e);
        }
    }
}
