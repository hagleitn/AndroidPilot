package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;

import java.util.concurrent.ScheduledExecutorService;

import android.hardware.SensorManager;
import android.location.LocationManager;

public class SignalManagerFactory {
    
    private static SignalManager manager;
    
    public static SignalManager getManager(IOIO ioio,
            SensorManager sensorManager, LocationManager locationManager,
            ScheduledExecutorService scheduler) {
        if (manager == null) {
            manager = new SignalManagerImpl(ioio, sensorManager,
                    locationManager);
        }
        manager.setScheduler(scheduler);
        return manager;
    }
    
    public static void setManager(SignalManager manager) {
        SignalManagerFactory.manager = manager;
    }
    
}
