package com.barbermot.pilot;

import ioio.lib.api.exception.ConnectionLostException;

public interface SignalListener {
	void update(double value, long time) throws ConnectionLostException;
}
