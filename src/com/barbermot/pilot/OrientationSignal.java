package com.barbermot.pilot;

import java.util.Arrays;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class OrientationSignal extends Signal {
	
	public static final String TAG = "OrientationSignal";
	
	public enum Type {YAW,ROLL,PITCH};

	public OrientationSignal(IOIO ioio, SensorManager manager, Type type) throws ConnectionLostException {
		super(ioio, IOIO.INVALID_PIN);
		this.type = type;
		if (OrientationSignal.adapter == null) {
			OrientationSignal.adapter = new SensorAdapter(manager);
		}
	}
	
	@Override
	public long measure() {
		return 0;
	}
	
	@Override
	public Double convert(long x) {
		double reading;
		
		switch(type) {
		case YAW:
			reading = adapter.yaw;
			break;
		case ROLL:
			reading = adapter.roll;
			break;
		case PITCH:
			reading = adapter.pitch;
			break;
		default:
			reading = 0;
		}
		return reading;
	}
	
	private class SensorAdapter implements SensorEventListener {
		
		public SensorAdapter(SensorManager manager) {
			Sensor accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			Sensor magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
			manager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_UI);
			
			this.accel = new float[3];
			this.magnetic = new float[3];
			this.orientation = new float[3];
			this.R = new float[16];
			this.I = new float[16];
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
				//Log.d(TAG, "Accel: "+Arrays.toString(data));
			} else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
				data = magnetic;
				//Log.d(TAG, "Magnetic: "+Arrays.toString(data));
			} else {
				// we should not be here.
				return;
			}
			for (int i=0 ; i<3 ; i++) {
				data[i] = event.values[i];
			}

			SensorManager.getRotationMatrix(R, I, accel, magnetic);
			SensorManager.getOrientation(R, orientation);

			final float rad2deg = (float)(180.0f/Math.PI);
			yaw = orientation[0]*rad2deg;
			pitch = orientation[1]*rad2deg;
			roll = orientation[2]*rad2deg;
			
			//Log.d(TAG, "Orientation: "+yaw+", "+pitch+", "+roll);
		}

		private float yaw;
		private float roll;
		private float pitch;
		float[] orientation;
		private float[] accel;
		private float[] magnetic;
		private float[] R;
		private float[] I;
	}
	
	private static SensorAdapter adapter;
	private Type type;
}
