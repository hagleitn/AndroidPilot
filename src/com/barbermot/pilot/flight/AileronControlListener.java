package com.barbermot.pilot.flight;

import ioio.lib.api.exception.ConnectionLostException;

public class AileronControlListener extends FlightControlListener {

	@Override
	public void adjust(float x) throws ConnectionLostException {
		int currentAileron = (int) limit(x, computer.getMinTilt(),
				computer.getMaxTilt());
		computer.setCurrentAileron(currentAileron);
		computer.getUfo().aileron(currentAileron);
	}

}
