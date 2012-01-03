package com.barbermot.pilot.simulator;

import ioio.lib.api.Uart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

public class UartSimulation implements Uart {
    
    private Logger logger = Logger.getLogger("UartSimulation");
    
    private class UartStream extends OutputStream {
        
        StringBuilder builder = new StringBuilder();
        char          newLine = System.getProperty("line.separator").charAt(0);
        
        @Override
        public void write(int oneByte) throws IOException {
            if ((char) oneByte == newLine) {
                logger.info(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(((char) oneByte));
            }
        }
        
    }
    
    @Override
    public void close() {}
    
    @Override
    public InputStream getInputStream() {
        return System.in;
    }
    
    @Override
    public OutputStream getOutputStream() {
        return new UartStream();
    }
    
}
