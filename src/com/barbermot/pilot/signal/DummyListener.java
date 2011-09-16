package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

public class DummyListener implements SignalListener {
    
    @Override
    public void update(float value, long time) throws ConnectionLostException {}
    
}
