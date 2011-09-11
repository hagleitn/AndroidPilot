package com.barbermot.pilot.signal;

import java.util.LinkedList;
import java.util.List;

public class Signal {

	protected List<SignalListener> listeners;

	public Signal() {
		super();
		listeners = new LinkedList<SignalListener>();
	}

	public void registerListener(SignalListener listener) {
		listeners.add(listener);
	}

	public void abort() {
	}

}