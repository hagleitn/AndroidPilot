package com.barbermot.pilot;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class OrientationSignal extends Signal {
	
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
			Sensor accelerometer = manager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
			Sensor magnetic = manager.getDefaultSensor(SensorManager.SENSOR_MAGNETIC_FIELD);
			manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
			manager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_GAME);
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
			for (int i=0 ; i<3 ; i++) {
				data[i] = event.values[i];
			}

			SensorManager.getRotationMatrix(R, null, accel, magnetic);
			SensorManager.getOrientation(R, orientation);

			final float rad2deg = (float)(180.0f/Math.PI);
			yaw = orientation[0]*rad2deg;
			pitch = orientation[1]*rad2deg;
			roll = orientation[2]*rad2deg;
		}

		private float yaw;
		private float roll;
		private float pitch;
		float[] orientation;
		private float[] accel;
		private float[] magnetic;
		private float[] R;
	}
	
	private static SensorAdapter adapter;
	private Type type;
}
