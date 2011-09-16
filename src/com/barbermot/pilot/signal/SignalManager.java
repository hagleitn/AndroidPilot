package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.hardware.SensorManager;
import android.location.LocationManager;

public class SignalManager {
    
    private static SignalManager     manager;
    private ScheduledExecutorService scheduler;
    private EnumMap<Type, Signal>    signalMap;
    private SensorManager            sensorManager;
    private LocationManager          locationManager;
    private OrientationSignal        orientation;
    private GpsSignal                gps;
    private IOIO                     ioio;
    private List<Future<?>>          futures;
    
    private enum Type {
        ORIENTATION_YAW, ORIENTATION_PITCH, ORIENTATION_ROLL, ULTRASOUND_HEIGHT, GPS_HEIGHT
    };
    
    private SignalManager(IOIO ioio, SensorManager sensorManager,
            LocationManager locationManager, ScheduledExecutorService scheduler) {
        this.sensorManager = sensorManager;
        this.locationManager = locationManager;
        this.scheduler = scheduler;
        this.ioio = ioio;
        this.futures = new LinkedList<Future<?>>();
        signalMap = new EnumMap<Type, Signal>(Type.class);
    }
    
    public void shutdown() {
        orientation.abort();
        gps.abort();
        for (Type t : Type.values()) {
            Signal s = signalMap.get(t);
            if (null != s) {
                s.abort();
            }
        }
    }
    
    public List<Future<?>> getFutures() {
        return futures;
    }
    
    public static SignalManager getManager(IOIO ioio,
            SensorManager sensorManager, LocationManager locationManager,
            ScheduledExecutorService scheduler) {
        return SignalManager.manager == null ? SignalManager.manager = new SignalManager(
                ioio, sensorManager, locationManager, scheduler)
                : SignalManager.manager;
    }
    
    public Signal getYawSignal(int interval) throws ConnectionLostException {
        if (!signalMap.containsKey(Type.ORIENTATION_YAW)) {
            createOrientationSignals(interval);
        }
        return signalMap.get(Type.ORIENTATION_YAW);
    }
    
    public Signal getPitchSignal(int interval) {
        if (!signalMap.containsKey(Type.ORIENTATION_PITCH)) {
            createOrientationSignals(interval);
        }
        return signalMap.get(Type.ORIENTATION_PITCH);
    }
    
    public Signal getRollSignal(int interval) {
        if (!signalMap.containsKey(Type.ORIENTATION_ROLL)) {
            createOrientationSignals(interval);
        }
        return signalMap.get(Type.ORIENTATION_ROLL);
    }
    
    public Signal getUltraSoundSignal(int interval, int pin)
            throws ConnectionLostException {
        if (!signalMap.containsKey(Type.ULTRASOUND_HEIGHT)) {
            IoioSignal signal = new UltrasoundSignal(ioio, pin);
            futures.add(scheduler.scheduleWithFixedDelay(signal, 0, interval,
                    TimeUnit.MILLISECONDS));
            signalMap.put(Type.ULTRASOUND_HEIGHT, signal);
        }
        return signalMap.get(Type.ULTRASOUND_HEIGHT);
    }
    
    public Signal getGpsAltitudeSignal(int interval) {
        if (!signalMap.containsKey(Type.GPS_HEIGHT)) {
            createGpsSignals(interval);
        }
        return signalMap.get(Type.GPS_HEIGHT);
    }
    
    private void createGpsSignals(int interval) {
        SensorAdapter height = new SensorAdapter();
        gps = new GpsSignal(locationManager, height, new DummyListener(),
                new DummyListener(), new DummyListener(), interval);
        futures.add(scheduler.submit(gps));
        signalMap.put(Type.GPS_HEIGHT, height);
    }
    
    private void createOrientationSignals(int interval) {
        SensorAdapter yaw = new SensorAdapter();
        SensorAdapter pitch = new SensorAdapter();
        SensorAdapter roll = new SensorAdapter();
        orientation = new OrientationSignal(sensorManager, yaw, roll, pitch);
        signalMap.put(Type.ORIENTATION_YAW, yaw);
        signalMap.put(Type.ORIENTATION_ROLL, roll);
        signalMap.put(Type.ORIENTATION_PITCH, pitch);
        
    }
}
