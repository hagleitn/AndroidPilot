package com.barbermot.pilot.simulator;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;

public class PwmOutputSimulation extends DeviceSimulation implements PwmOutput {
    
    public PwmOutputSimulation(int pin, PhysicsEngine engine) {
        super(pin, engine);
    }
    
    @Override
    public void close() {}
    
    @Override
    public void setDutyCycle(float dutyCycle) throws ConnectionLostException {}
    
    @Override
    public void setPulseWidth(int pulseWidthUs) throws ConnectionLostException {
        setPulseWidth((float) pulseWidthUs);
    }
    
    @Override
    public void setPulseWidth(float pulseWidthUs)
            throws ConnectionLostException {
        engine.pulseOut(pin, pulseWidthUs);
    }
    
}
