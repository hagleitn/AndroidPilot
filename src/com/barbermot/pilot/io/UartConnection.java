package com.barbermot.pilot.io;

import static com.barbermot.pilot.flight.FlightConfiguration.PinType.RX;
import static com.barbermot.pilot.flight.FlightConfiguration.PinType.TX;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.barbermot.pilot.flight.FlightConfiguration;

public class UartConnection extends Connection {
    
    Logger logger = Logger.getLogger("UartConnection");
    Uart   uart;
    IOIO   ioio;
    
    public UartConnection(IOIO ioio) {
        this.ioio = ioio;
    }
    
    @Override
    protected void reEstablishConnection() throws ConnectionLostException {
        logger.info("Setting up UART");
        
        if (uart != null) {
            uart.close();
        }
        
        uart = ioio.openUart(FlightConfiguration.get().getPinMap().get(RX),
                FlightConfiguration.get().getPinMap().get(TX), 9600,
                Uart.Parity.NONE, Uart.StopBits.ONE);
    }
    
    @Override
    public OutputStream getOutputStream() {
        return uart.getOutputStream();
    }
    
    @Override
    public InputStream getInputStream() {
        return uart.getInputStream();
    }
}
