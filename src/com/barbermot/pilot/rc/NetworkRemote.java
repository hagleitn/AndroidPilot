package com.barbermot.pilot.rc;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.concurrent.TimeoutException;

import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.quad.QuadCopter.Direction;

public class NetworkRemote extends RemoteControl {
    
    SwitchedQuadCopter manualControlUfo;
    SwitchedQuadCopter computerControlUfo;
    
    public NetworkRemote(QuadCopter ufo, SwitchedQuadCopter manual,
            SwitchedQuadCopter computer) {
        super(ufo);
        manualControlUfo = manual;
        computerControlUfo = computer;
    }
    
    @Override
    public void setControlMask(char mask) throws ConnectionLostException {
        super.setControlMask(mask);
        
        mask = 0x1;
        
        for (Direction d : Direction.values()) {
            if ((controlMask & mask) != 0) {
                manualControlUfo.enable(d);
                computerControlUfo.disable(d);
            } else {
                manualControlUfo.disable(d);
                computerControlUfo.enable(d);
            }
            mask = (char) (mask << 1);
        }
    }
    
    @Override
    protected int readManualThrottle() throws ConnectionLostException,
            TimeoutException {
        return manualControlUfo.readRaw(Direction.VERTICAL);
    }
    
}
