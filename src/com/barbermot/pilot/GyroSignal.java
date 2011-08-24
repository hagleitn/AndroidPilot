package com.barbermot.pilot;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class GyroSignal extends Signal {

	public GyroSignal(IOIO ioio, int pin) throws ConnectionLostException {
		super(ioio, pin);
	}

}
