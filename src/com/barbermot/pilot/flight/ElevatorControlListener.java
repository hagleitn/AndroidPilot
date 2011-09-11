package com.barbermot.pilot.flight;

import ioio.lib.api.exception.ConnectionLostException;

public class ElevatorControlListener extends FlightControlListener {

	@Override
	public void adjust(float x) throws ConnectionLostException {
		int currentElevator = (int) limit(x, computer.getMinTilt(),
				computer.getMaxTilt());
		computer.setCurrentElevator(currentElevator);
		computer.getUfo().elevator(currentElevator);
	}

}
