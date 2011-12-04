package com.barbermot.pilot.simulator;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalInput.Spec;
import ioio.lib.api.DigitalInput.Spec.Mode;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IcspMaster;
import ioio.lib.api.PingPin;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.ClockRate;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.SpiMaster.Config;
import ioio.lib.api.SpiMaster.Rate;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.Uart.Parity;
import ioio.lib.api.Uart.StopBits;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;

public class IOIOSimulation implements IOIO {
    
    private PhysicsEngine engine;
    
    public IOIOSimulation(PhysicsEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void waitForConnect() throws ConnectionLostException,
            IncompatibilityException {
        engine.init();
    }
    
    @Override
    public void disconnect() {}
    
    @Override
    public void waitForDisconnect() throws InterruptedException {}
    
    @Override
    public void softReset() throws ConnectionLostException {}
    
    @Override
    public void hardReset() throws ConnectionLostException {}
    
    @Override
    public String getImplVersion(VersionType v) throws ConnectionLostException {
        return null;
    }
    
    @Override
    public DigitalInput openDigitalInput(Spec spec)
            throws ConnectionLostException {
        return null;
    }
    
    @Override
    public DigitalInput openDigitalInput(int pin)
            throws ConnectionLostException {
        return null;
    }
    
    @Override
    public DigitalInput openDigitalInput(int pin, Mode mode)
            throws ConnectionLostException {
        return null;
    }
    
    @Override
    public DigitalOutput openDigitalOutput(
            ioio.lib.api.DigitalOutput.Spec spec, boolean startValue)
            throws ConnectionLostException {
        return openDigitalOutput(spec.pin, startValue);
    }
    
    @Override
    public DigitalOutput openDigitalOutput(int pin,
            ioio.lib.api.DigitalOutput.Spec.Mode mode, boolean startValue)
            throws ConnectionLostException {
        return openDigitalOutput(pin, startValue);
    }
    
    @Override
    public DigitalOutput openDigitalOutput(int pin, boolean startValue)
            throws ConnectionLostException {
        DigitalOutput out = new DigitalOutputSimulation(pin, engine);
        out.write(startValue);
        return out;
        
    }
    
    @Override
    public DigitalOutput openDigitalOutput(int pin)
            throws ConnectionLostException {
        return openDigitalOutput(pin, false);
    }
    
    @Override
    public AnalogInput openAnalogInput(int pin) throws ConnectionLostException {
        return null;
    }
    
    @Override
    public PwmOutput openPwmOutput(ioio.lib.api.DigitalOutput.Spec spec,
            int freqHz) throws ConnectionLostException {
        return openPwmOutput(spec.pin, freqHz);
    }
    
    @Override
    public PwmOutput openPwmOutput(int pin, int freqHz)
            throws ConnectionLostException {
        return new PwmOutputSimulation(pin, engine);
    }
    
    @Override
    public PulseInput openPulseInput(Spec spec, ClockRate rate, PulseMode mode,
            boolean doublePrecision) throws ConnectionLostException {
        return openPulseInput(spec.pin, mode);
    }
    
    @Override
    public PulseInput openPulseInput(int pin, PulseMode mode)
            throws ConnectionLostException {
        return new PulseInputSimulation(pin, engine);
    }
    
    @Override
    public Uart openUart(Spec rx, ioio.lib.api.DigitalOutput.Spec tx, int baud,
            Parity parity, StopBits stopbits) throws ConnectionLostException {
        return openUart(rx.pin, tx.pin, baud, parity, stopbits);
    }
    
    @Override
    public Uart openUart(int rx, int tx, int baud, Parity parity,
            StopBits stopbits) throws ConnectionLostException {
        return new UartSimulation();
    }
    
    @Override
    public SpiMaster openSpiMaster(Spec miso,
            ioio.lib.api.DigitalOutput.Spec mosi,
            ioio.lib.api.DigitalOutput.Spec clk,
            ioio.lib.api.DigitalOutput.Spec[] slaveSelect, Config config)
            throws ConnectionLostException {
        return null;
    }
    
    @Override
    public SpiMaster openSpiMaster(int miso, int mosi, int clk,
            int[] slaveSelect, Rate rate) throws ConnectionLostException {
        return null;
    }
    
    @Override
    public SpiMaster openSpiMaster(int miso, int mosi, int clk,
            int slaveSelect, Rate rate) throws ConnectionLostException {
        return null;
    }
    
    @Override
    public TwiMaster openTwiMaster(int twiNum,
            ioio.lib.api.TwiMaster.Rate rate, boolean smbus)
            throws ConnectionLostException {
        return null;
    }
    
    @Override
    public IcspMaster openIcspMaster() throws ConnectionLostException {
        return null;
    }
    
    @Override
    public PingPin openPingInput(int pin) throws ConnectionLostException {
        return new PingPinSimulation(pin, engine);
    }
    
}
