package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.concurrent.TimeoutException;

class PulseSignal extends IoioSignal {
    
    public final String  TAG     = "Signal";
    public final float   TIMEOUT = 0.2f;
    
    protected int        pin;
    protected PulseInput pulse;
    
    public PulseSignal(IOIO ioio, int pin) throws ConnectionLostException {
        super(ioio);
        this.pin = pin;
    }
    
    protected long measure() throws ConnectionLostException,
            MeasurementException {
        pulse = ioio.openPulseInput(pin, PulseMode.POSITIVE);
        long measurement = 0;
        try {
            while (true) {
                try {
                    measurement = (long) (pulse.getDuration(TIMEOUT) * 1000000);
                    break;
                } catch (InterruptedException e) {
                    /* retry */
                } catch (TimeoutException e) {
                    // Log.i(TAG, "Read on " + pin + " timed out.");
                    throw new MeasurementException(e);
                }
            }
        } finally {
            if (pulse != null) {
                pulse.close();
            }
        }
        return measurement;
    }
}
