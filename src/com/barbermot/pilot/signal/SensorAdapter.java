package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

class SensorAdapter extends Signal implements SignalListener {
    
    @Override
    public void update(float value, long time) throws ConnectionLostException {
        notifyListeners(value, time);
    }
}
