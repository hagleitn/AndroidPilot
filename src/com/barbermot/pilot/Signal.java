package com.barbermot.pilot;

import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Signal {

	public final String TAG = "Signal";

	protected int pin;
	protected IOIO ioio;
	protected PulseInput pulse;

	private long time;
	private Double measurement;
	private List<SignalListener> listeners;	    

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
		pulse = ioio.openPulseInput(pin, PulseMode.POSITIVE);
		long measurement = 0;
		while (true) {
			try {
				measurement = (long) (pulse.getDuration()*1000000);
				break;
			} catch (InterruptedException e) { /* retry */ }
		}
		pulse.close();
		return measurement;
	}

	protected Double convert(long x) {
		return (double) x;
	}

	private boolean read() throws ConnectionLostException {
		long duration;

		setupMeasurement();

		duration = measure();
		// Log.d(TAG,"Duration: "+duration);

		measurement = convert(duration);
		time = System.currentTimeMillis();

		return measurement != null;
	}	    	    
}
