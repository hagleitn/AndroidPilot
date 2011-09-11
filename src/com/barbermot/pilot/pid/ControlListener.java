package com.barbermot.pilot.pid;

import ioio.lib.api.exception.ConnectionLostException;

public interface ControlListener {
    
    public void adjust(float value) throws ConnectionLostException;
}
