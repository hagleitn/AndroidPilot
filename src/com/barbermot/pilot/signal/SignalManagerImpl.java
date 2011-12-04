package com.barbermot.pilot.signal;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import android.hardware.SensorManager;
import android.location.LocationManager;

class SignalManagerImpl implements SignalManager {
    
    private static final Logger        logger = Logger.getLogger("SignalManager");
    protected ScheduledExecutorService scheduler;
    protected EnumMap<Type, Signal>    signalMap;
    private SensorManager              sensorManager;
    private LocationManager            locationManager;
    private OrientationSignal          orientation;
    private GpsSignal                  gps;
    private IOIO                       ioio;
    protected List<Future<?>>          futures;
    
    protected enum Type {
        ORIENTATION_YAW, ORIENTATION_PITCH, ORIENTATION_ROLL, ULTRASOUND_HEIGHT, GPS_HEIGHT
    };
    
    public SignalManagerImpl(IOIO ioio, SensorManager sensorManager,
            LocationManager locationManager) {
        this.sensorManager = sensorManager;
        this.locationManager = locationManager;
        this.ioio = ioio;
        this.futures = new LinkedList<Future<?>>();
        signalMap = new EnumMap<Type, Signal>(Type.class);
    }
    
    @Override
    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#shutdown()
     */
    @Override
    public void shutdown() {
        logger.info("shutting down signals");
        
        if (orientation != null) {
            orientation.abort();
        }
        
        if (gps != null) {
            gps.abort();
        }
        
        for (Type t : Type.values()) {
            Signal s = signalMap.get(t);
            if (null != s) {
                s.abort();
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#getFutures()
     */
    @Override
    public List<Future<?>> getFutures() {
        return futures;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#getYawSignal(int)
     */
    @Override
    public Signal getYawSignal(int interval) throws ConnectionLostException {
        if (!signalMap.containsKey(Type.ORIENTATION_YAW)) {
            createOrientationSignals(interval);
        }
        return signalMap.get(Type.ORIENTATION_YAW);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#getPitchSignal(int)
     */
    @Override
    public Signal getPitchSignal(int interval) {
        if (!signalMap.containsKey(Type.ORIENTATION_PITCH)) {
            createOrientationSignals(interval);
        }
        return signalMap.get(Type.ORIENTATION_PITCH);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#getRollSignal(int)
     */
    @Override
    public Signal getRollSignal(int interval) {
        if (!signalMap.containsKey(Type.ORIENTATION_ROLL)) {
            createOrientationSignals(interval);
        }
        return signalMap.get(Type.ORIENTATION_ROLL);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#getUltraSoundSignal(int,
     * int)
     */
    @Override
    public Signal getUltraSoundSignal(int interval, int pin)
            throws ConnectionLostException {
        if (!signalMap.containsKey(Type.ULTRASOUND_HEIGHT)) {
            logger.info("creating ultrasound signal");
            
            IoioSignal signal = new UltrasoundSignal(ioio, pin);
            futures.add(scheduler.scheduleWithFixedDelay(signal, 0, interval,
                    TimeUnit.MILLISECONDS));
            signalMap.put(Type.ULTRASOUND_HEIGHT, signal);
        }
        return signalMap.get(Type.ULTRASOUND_HEIGHT);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.barbermot.pilot.signal.SignalManager#getGpsAltitudeSignal(int)
     */
    @Override
    public Signal getGpsAltitudeSignal(int interval) {
        if (!signalMap.containsKey(Type.GPS_HEIGHT)) {
            createGpsSignals(interval);
        }
        return signalMap.get(Type.GPS_HEIGHT);
    }
    
    protected void createGpsSignals(int interval) {
        logger.info("creating gps signal");
        
        SensorAdapter height = new SensorAdapter();
        gps = new GpsSignal(locationManager, height, new DummyListener(),
                new DummyListener(), new DummyListener(), interval);
        futures.add(scheduler.submit(gps));
        signalMap.put(Type.GPS_HEIGHT, height);
    }
    
    protected void createOrientationSignals(int interval) {
        logger.info("creating orientation signal");
        
        SensorAdapter yaw = new SensorAdapter();
        SensorAdapter pitch = new SensorAdapter();
        SensorAdapter roll = new SensorAdapter();
        orientation = new OrientationSignal(sensorManager, yaw, roll, pitch);
        signalMap.put(Type.ORIENTATION_YAW, yaw);
        signalMap.put(Type.ORIENTATION_ROLL, roll);
        signalMap.put(Type.ORIENTATION_PITCH, pitch);
    }
}
