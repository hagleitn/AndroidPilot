package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

public interface SignalListener {
    
    void update(float value, long time) throws ConnectionLostException;
}
