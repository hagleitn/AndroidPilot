package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;
import java.util.logging.Logger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

class OrientationSignal implements SensorEventListener {
    
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger("OrientationSignal");
    
    private float               yaw;
    private float               roll;
    private float               pitch;
    float[]                     orientation;
    private float[]             accel;
    private float[]             magnetic;
    private float[]             R;
    private float[]             I;
    
    public enum Type {
        YAW, ROLL, PITCH
    };
    
    EnumMap<Type, SignalListener> listenerMap;
    SensorManager                 manager;
    
    public OrientationSignal(SensorManager manager, SignalListener yaw,
            SignalListener roll, SignalListener pitch) {
        
        this.manager = manager;
        
        listenerMap = new EnumMap<Type, SignalListener>(Type.class);
        listenerMap.put(Type.YAW, yaw);
        listenerMap.put(Type.PITCH, pitch);
        listenerMap.put(Type.ROLL, roll);
        
        Sensor accelerometer = manager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        manager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_UI);
        
        this.accel = new float[3];
        this.magnetic = new float[3];
        this.orientation = new float[3];
        this.R = new float[16];
        this.I = new float[16];
        
    }
    
    public void abort() {
        manager.unregisterListener(this);
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore...
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        float[] data;
        
        if (type == Sensor.TYPE_ACCELEROMETER) {
            data = accel;
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            data = magnetic;
        } else {
            // we should not be here.
            return;
        }
        for (int i = 0; i < 3; i++) {
            data[i] = event.values[i];
        }
        
        SensorManager.getRotationMatrix(R, I, accel, magnetic);
        SensorManager.getOrientation(R, orientation);
        
        final float rad2deg = (float) (180.0f / Math.PI);
        yaw = orientation[0] * rad2deg;
        pitch = orientation[1] * rad2deg;
        roll = orientation[2] * rad2deg;
        
        try {
            listenerMap.get(Type.YAW).update(yaw, event.timestamp);
            listenerMap.get(Type.PITCH).update(pitch, event.timestamp);
            listenerMap.get(Type.ROLL).update(roll, event.timestamp);
        } catch (ConnectionLostException e) {}
        
    }
}
