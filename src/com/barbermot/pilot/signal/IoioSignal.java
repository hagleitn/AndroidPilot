package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.logging.Logger;

public abstract class IoioSignal extends Signal implements Runnable {
    
    protected IOIO                ioio;
    protected final static Logger logger = Logger.getLogger("Signal");
    private long                  time;
    private float                 measurement;
    
    @SuppressWarnings("serial")
    protected class MeasurementException extends Exception {
        
        public MeasurementException(Exception e) {
            super(e);
        }
        
        public MeasurementException() {
            super();
        }
    };
    
    public IoioSignal(IOIO ioio) {
        this.ioio = ioio;
    }
    
    protected void setupMeasurement() throws ConnectionLostException,
            MeasurementException {}
    
    @Override
    public void run() {
        try {
            float measurement = read();
            
            notifyListeners(measurement, time);
        } catch (MeasurementException e) {
            logger.info("Failed to take measurement");
        } catch (ConnectionLostException e) {
            logger.info("Connection lost");
            throw new RuntimeException(e);
        }
    }
    
    protected abstract long measure() throws ConnectionLostException,
            MeasurementException;
    
    protected float convert(long x) throws MeasurementException {
        return (float) x;
    }
    
    private float read() throws ConnectionLostException, MeasurementException {
        long duration;
        
        setupMeasurement();
        
        duration = measure();
        
        measurement = convert(duration);
        time = System.currentTimeMillis();
        
        return measurement;
    }
    
}
