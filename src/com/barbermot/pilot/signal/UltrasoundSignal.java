package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;
import ioio.lib.api.PingPin;
import ioio.lib.api.exception.ConnectionLostException;

class UltrasoundSignal extends IoioSignal {
    
    public final String     TAG          = "UltrasoundSignal";
    public static final int MAX_RELIABLE = 367;
    public PingPin          ping;
    
    public UltrasoundSignal(IOIO ioio, int pin) throws ConnectionLostException {
        super(ioio);
        ping = ioio.openPingInput(pin);
    }
    
    @Override
    protected float convert(long microseconds) throws MeasurementException {
        // The speed of sound is 340 m/s or 29 microseconds per centimeter.
        // The ping travels out and back, so to find the distance of the
        // object we take half of the distance traveled.
        // Log.d(TAG, "Microseconds "+microseconds);
        float value = microseconds / 29 / 2;
        
        // correction factor for ioio readings
        value = value * 1.8f;
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
