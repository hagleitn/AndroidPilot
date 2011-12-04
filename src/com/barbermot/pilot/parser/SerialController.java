package com.barbermot.pilot.parser;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.barbermot.pilot.flight.FlightComputer;

/**
 * SerialController is a tasks that waits for user input and relays commands to
 * the FlightComputer.
 * 
 */
public class SerialController implements Runnable {
    
    private static final Logger logger = Logger.getLogger("SerialController");
    private long                startSleep;
    private long                sleepTime;
    private Parser              parser;
    private PrintStream         printer;
    private InputStream         in;
    char                        delim;
    
    public SerialController(FlightComputer computer, char delim,
            InputStream in, PrintStream printer) throws ConnectionLostException {
        this.in = in;
        this.parser = new Parser(computer);
        this.printer = printer;
        this.delim = delim;
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                executeCommand();
            }
        } catch (ConnectionLostException e) {
            logger.log(Level.WARNING, "Connection Lost", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO problem", e);
            throw new RuntimeException(e);
        }
    }
    
    public void executeCommand() throws IOException, ConnectionLostException {
        if (startSleep != 0) {
            if (System.currentTimeMillis() - startSleep < sleepTime) {
                return;
            } else {
                sleepTime = 0;
                startSleep = 0;
            }
        }
        
        StringBuffer sb = new StringBuffer();
        char c;
        while ((c = (char) in.read()) != ';') {
            sb.append(c);
        }
        
        String cmd = sb.toString().trim();
        if (cmd.length() > 0) {
            printer.println(cmd);
            logger.info(cmd);
            if (cmd.charAt(0) == 'z' || cmd.charAt(0) == 'Z') {
                int x = 0;
                String num = cmd.substring(1);
                num.trim();
                try {
                    x = Integer.parseInt(num);
                    sleepTime = x;
                    startSleep = System.currentTimeMillis();
                } catch (NumberFormatException e) {
                    parser.fail(cmd);
                }
            } else {
                parser.doCmd(cmd);
            }
        }
    }
}
