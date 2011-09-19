package com.barbermot.pilot.logger;

import java.io.PrintStream;

import com.barbermot.pilot.flight.FlightComputer;

/**
 * Flight Logger is a periodic task that logs information about the status of
 * the flight computer.
 * 
 */
public class FlightLogger implements Runnable {
    
    @SuppressWarnings("unused")
    private static final String TAG = "FlightLogger";
    
    private PrintStream         printer;
    private FlightComputer      computer;
    
    public FlightLogger(PrintStream printer) {
        this.printer = printer;
    }
    
    public void setComputer(FlightComputer computer) {
        this.computer = computer;
    }
    
    @Override
    public void run() {
        String str = String
                .format("st: %s\tms: %d\trc: %h\nh: %f\tdy: %f\tdx: %f\tdz: %f\ngh: %f\nt: %d\te: %d\ta: %d\tr: %d",
                        computer.getState().getType(), computer.getTime(),
                        computer.getRc().getControlMask(),
                        computer.getHeight(),
                        computer.getLongitudinalDisplacement(),
                        computer.getLateralDisplacement(),
                        computer.getHeading(), computer.getGpsHeight(),
                        computer.getCurrentThrottle(),
                        computer.getCurrentElevator(),
                        computer.getCurrentAileron(),
                        computer.getCurrentRudder());
        // Log.d(TAG, str);
        printer.println(str);
    }
}
