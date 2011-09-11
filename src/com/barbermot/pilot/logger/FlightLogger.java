package com.barbermot.pilot.logger;

import java.io.PrintStream;

import com.barbermot.pilot.flight.FlightComputer;

public class FlightLogger implements Runnable {

	private static final String TAG = "FlightLogger";
	private PrintStream printer;
	private FlightComputer computer;

	public FlightLogger(PrintStream printer) {
		this.printer = printer;
	}

	public void setComputer(FlightComputer computer) {
		this.computer = computer;
	}

	@Override
	public void run() {
		String str = String
				.format("st: %s\tms: %d\trc: %h\nh: %f\tdy: %f\tdx: %f\tdz: %f\nt: %d\te: %d\ta: %d\tr: %d",
						computer.getState(), computer.getTime(), computer
								.getRc().getControlMask(),
						computer.getHeight(), computer
								.getLongitudinalDisplacement(), computer
								.getLateralDisplacement(), computer
								.getHeading(), computer.getCurrentThrottle(),
						computer.getCurrentElevator(), computer
								.getCurrentAileron(), computer
								.getCurrentRudder());
		// Log.d(TAG, str);
		printer.println(str);
	}

}
