package com.barbermot.pilot.parser;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import com.barbermot.pilot.flight.FlightComputer;
import com.barbermot.pilot.io.Connection;

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
    private Connection          connection;
    private PrintStream         printer;
    private InputStream         in;
    char                        delim;
    
    public SerialController(FlightComputer computer, char delim,
            Connection connection) throws ConnectionLostException, IOException {
        this.connection = connection;
        this.parser = new Parser(computer);
        this.delim = delim;
        this.in = connection.getInputStream();
        this.printer = new PrintStream(connection.getOutputStream());
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                executeCommand();
            } catch (ConnectionLostException e) {
                logger.warn("Connection Lost", e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                logger.fatal("IO problem, reconnecting", e);
                try {
                    reconnect();
                } catch (Exception io) {
                    throw new RuntimeException(io);
                }
            }
        }
    }
    
    private void reconnect() throws IOException, ConnectionLostException,
            InterruptedException {
        connection.reconnect();
        in = connection.getInputStream();
        printer = new PrintStream(connection.getOutputStream());
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
        int ic;
        while (true) {
            ic = in.read();
            char c = (char) ic;
            
            if (ic == -1) {
                throw new IOException("EOS");
            } else if (c == delim) {
                break;
            } else {
                sb.append(c);
            }
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
