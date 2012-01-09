package com.barbermot.pilot.logger;

import static com.barbermot.pilot.quad.QuadCopter.Direction.LATERAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.LONGITUDINAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.ROTATIONAL;
import static com.barbermot.pilot.quad.QuadCopter.Direction.VERTICAL;

import java.io.IOException;
import java.io.PrintStream;

import com.barbermot.pilot.flight.FlightComputer;
import com.barbermot.pilot.io.Connection;
import com.barbermot.pilot.quad.QuadCopter;

/**
 * Flight Logger is a periodic task that logs information about the status of
 * the flight computer.
 * 
 */
public class FlightLogger implements Runnable {
    
    private Connection     connection;
    private PrintStream    printer;
    private FlightComputer computer;
    private QuadCopter     ufo;
    
    public FlightLogger(Connection connection) throws IOException {
        this.connection = connection;
        this.printer = new PrintStream(connection.getOutputStream());
    }
    
    public void setComputer(FlightComputer computer) {
        this.computer = computer;
    }
    
    public void setQuadCopter(QuadCopter ufo) {
        this.ufo = ufo;
    }
    
    @Override
    public void run() {
        String str = String
                .format("st: %s\tms: %d\trc: %h\th: %f\tdy: %f\tdx: %f\tdz: %f\tgh: %f\tlat: %f\tlon: %f\tt: %d\te: %d\ta: %d\tr: %d",
                        computer.getState().getType(), // current state
                        computer.getTime(), // time in millis
                        computer.getRc().getControlMask(), // rc override
                        computer.getHeight(), // height measured by ultrasound
                        computer.getLongitudinalDisplacement(), // forward angle
                        computer.getLateralDisplacement(), // sideways angle
                        computer.getHeading(), // magnetic heading in radians
                        computer.getGpsHeight(), // height in meters (gps)
                        computer.getLatitude(), // lat measured by gps
                        computer.getLongitude(), // lon measured by gps
                        ufo.read(VERTICAL), // throttle
                        ufo.read(LONGITUDINAL), // elevator
                        ufo.read(LATERAL), // aileron
                        ufo.read(ROTATIONAL)); // rudder
        
        printer.println(str);
        if (printer.checkError()) {
            try {
                connection.reconnect();
                this.printer = new PrintStream(connection.getOutputStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
