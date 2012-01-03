package com.barbermot.pilot.signal;

import static com.barbermot.pilot.signal.GpsSignal.Type.HEIGHT;
import static com.barbermot.pilot.signal.GpsSignal.Type.LAT;
import static com.barbermot.pilot.signal.GpsSignal.Type.LOCATION;
import static com.barbermot.pilot.signal.GpsSignal.Type.LON;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;

import org.apache.log4j.Logger;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

class GpsSignal implements LocationListener, Runnable {
    
    private static final Logger logger = Logger.getLogger("GpsSignal");
    
    private int                 minTime;
    private Looper              looper;
    
    public enum Type {
        HEIGHT, LAT, LON, LOCATION
    };
    
    EnumMap<Type, SignalListener> listenerMap;
    LocationManager               manager;
    
    public GpsSignal(LocationManager manager, SignalListener height,
            SignalListener lat, SignalListener lon, SignalListener location,
            int minTime) {
        
        this.manager = manager;
        this.minTime = minTime;
        
        listenerMap = new EnumMap<Type, SignalListener>(Type.class);
        listenerMap.put(HEIGHT, height);
        listenerMap.put(LAT, lat);
        listenerMap.put(LON, lon);
        listenerMap.put(LOCATION, location);
    }
    
    @Override
    public void run() {
        looper = Looper.myLooper();
        Looper.prepare();
        
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
                0, GpsSignal.this);
        
        Looper.loop();
    }
    
    public void abort() {
        if (looper != null) {
            looper.quit();
            manager.removeUpdates(this);
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        long time = location.getTime();
        /*
         * if (last != null) { logger.info("Bearing: " +
         * location.bearingTo(last) + ", distance: " +
         * location.distanceTo(last)); logger.info("Altitude: " +
         * location.getAltitude() + ", lon: " + location.getLatitude() +
         * ", lat: " + location.getLongitude()); logger.info("Accuracy: " +
         * location.getAccuracy()); }
         * 
         * last = location;
         */

        try {
            listenerMap.get(HEIGHT)
                    .update((float) location.getAltitude(), time);
            listenerMap.get(LAT).update((float) location.getLatitude(), time);
            listenerMap.get(LON).update((float) location.getLongitude(), time);
        } catch (ConnectionLostException e) {
            logger.info("Connection lost: ", e);
        }
        
    }
    
    @Override
    public void onProviderDisabled(String provider) {}
    
    @Override
    public void onProviderEnabled(String provider) {}
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
