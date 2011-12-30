package com.barbermot.pilot.parser;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Logger;

import com.barbermot.pilot.flight.FlightComputer;
import com.barbermot.pilot.quad.QuadCopter;

public class Parser {
    
    private static final Logger logger = Logger.getLogger("Parser");
    private FlightComputer      computer;
    
    public Parser(FlightComputer computer) {
        this.computer = computer;
    }
    
    public void doCmd(String cmd) throws ConnectionLostException {
        int x = 0;
        float f = 0;
        Scanner scanner = new Scanner(cmd);
        
        if (scanner.hasNext(".")) {
            char c = scanner.next(".").charAt(0);
            switch (c) {
                
                // manual calibration
                case 'a':
                case 'A':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.setZeroThrottle(x);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // k calibrates the throttle
                case 'k':
                case 'K':
                    computer.calibrateControls();
                    break;
                
                // i <int> <int> inverts the respective servo
                case 'i':
                case 'I':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        int on;
                        if (scanner.hasNextInt()) {
                            on = scanner.nextInt();
                            QuadCopter.Direction d = null;
                            switch (x) {
                                case 0:
                                    d = QuadCopter.Direction.LATERAL;
                                    break;
                                case 1:
                                    d = QuadCopter.Direction.LONGITUDINAL;
                                    break;
                                case 2:
                                    d = QuadCopter.Direction.ROTATIONAL;
                                    break;
                                case 3:
                                    d = QuadCopter.Direction.VERTICAL;
                                    break;
                                default:
                                    break;
                            }
                            if (d != null) {
                                computer.getUfo().invert(d, on == 1);
                            }
                        }
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command o <string> sets the control mask of the RC
                case 'o':
                case 'O':
                    if (scanner.hasNext()) {
                        try {
                            int mask = Integer.valueOf(scanner.next(), 16);
                            computer.getRc().setControlMask((char) mask);
                        } catch (NumberFormatException nfe) {}
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command r <int> rotates to a magnetic direction (-180,
                // 180)
                case 'r':
                case 'R':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.rotate(x);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command f <int> goes forward/backward in stabilized hover
                case 'f':
                case 'F':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.forward(x);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command g <int> goes left/right in stabilized hover
                case 'g':
                case 'G':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.sideways(x);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command b resets the zero lateral/longitudal angle to the
                // current angles
                case 'b':
                case 'B':
                    computer.balance();
                    break;
                
                // Command "W <float>" holds the waypoint at altitude <float>
                case 'w':
                case 'W':
                    if (scanner.hasNextFloat()) {
                        f = scanner.nextFloat();
                        computer.waypoint(f);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command "H <float>" hovers the thing at altitude <float>
                case 'h':
                case 'H':
                    if (scanner.hasNextFloat()) {
                        f = scanner.nextFloat();
                        computer.hover(f);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command "T <float>" takeoff and start hovering at <float>
                case 't':
                case 'T':
                    if (scanner.hasNextFloat()) {
                        f = scanner.nextFloat();
                        computer.takeoff(f);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Command "L" lands the thing
                case 'l':
                case 'L':
                    computer.land();
                    break;
                
                // Command "C .... " sets the configuration for a particular pid
                // controller
                case 'C':
                case 'c': {
                    int type;
                    
                    float proportional;
                    float integral;
                    float derivative;
                    float min;
                    float max;
                    
                    try {
                        type = scanner.nextInt();
                        proportional = scanner.nextFloat();
                        integral = scanner.nextFloat();
                        derivative = scanner.nextFloat();
                        min = scanner.nextFloat();
                        max = scanner.nextFloat();
                    } catch (NoSuchElementException e) {
                        fail(cmd);
                        return;
                    }
                    
                    float[] conf = { proportional, integral, derivative, min,
                            max };
                    switch (type) {
                        case 1:
                            computer.setHoverConf(conf);
                            break;
                        case 2:
                            computer.setLandingConf(conf);
                            break;
                        case 3:
                            computer.setOrientationConf(conf);
                            break;
                        case 4:
                            computer.setGpsConf(conf);
                            break;
                        default:
                            fail(cmd);
                            break;
                    }
                }
                    break;
                
                // Disengage auto throttle
                case 'd':
                case 'D':
                    computer.manualControl();
                    break;
                
                // (Re-)Engage auto throttle
                case 'e':
                case 'E':
                    computer.autoControl();
                    break;
                
                // Command "S" turns on/off stabilization
                case 's':
                case 'S':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.stabilize(x == 0 ? false : true);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Set minimum throttle
                case 'm':
                case 'M':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.setMinThrottle(x);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Set maximum throttle
                case 'n':
                case 'N':
                    if (scanner.hasNextInt()) {
                        x = scanner.nextInt();
                        computer.setMaxThrottle(x);
                    } else {
                        fail(cmd);
                    }
                    break;
                
                // Commands "X" stops the thing
                case 'x':
                case 'X':
                    computer.abort();
                    break;
                default:
                    fail(cmd);
                    break;
            }
        }
        
    }
    
    public void fail(String cmd) {
        logger.warning("Failed to execute command:" + cmd);
    }
}
