package com.barbermot.pilot;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.ArrayList;
import java.util.List;

public class Signal {
	
	    public Signal(IOIO ioio, int pin) throws ConnectionLostException {
	    	this.pin = pin;
	    	this.ioio = ioio;
	    	listeners = new ArrayList<SignalListener>(2);
	    }
	    
	    public void registerListener(SignalListener listener) {
	    	listeners.add(listener);
	    }
	    
	    void signal() throws ConnectionLostException {
	    	if (read()) {
	    		for (SignalListener l: listeners) {
	    			l.update(measurement, time);
	    		}
	    	}
	    }
	    
	    protected void setupMeasurement() throws ConnectionLostException {
	    }
	    
	    protected long measure() throws ConnectionLostException {
	    	pulse = ioio.openDigitalInput(pin);
	    	while (true) {
	    		try {
	    			pulse.waitForValue(false);
	    			pulse.waitForValue(true);
	    			long nano = System.nanoTime();
	    			pulse.waitForValue(false);
	    			pulse.close();
	    			return (System.nanoTime() - nano)*1000;
	    		} catch(InterruptedException e) {}
	    	}
	    }
	    
	    protected Double convert(long x) {
	    	return (double) x;
	    }
	    
	    private boolean read() throws ConnectionLostException {
	        long duration;
	        
	        setupMeasurement();

	        duration = measure();
	        
	        measurement = convert(duration);
	        
	        return measurement != null;
	    }
	    	    
	    protected int pin;
	    protected IOIO ioio;
	    private long time;
	    private Double measurement;
	    protected DigitalInput pulse;
	    private List<SignalListener> listeners;	    
}
