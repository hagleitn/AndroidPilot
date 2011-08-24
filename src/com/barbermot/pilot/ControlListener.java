package com.barbermot.pilot;

import ioio.lib.api.exception.ConnectionLostException;

public interface ControlListener {
	public void adjust(double value) throws ConnectionLostException;
}
