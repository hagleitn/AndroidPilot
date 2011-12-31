package com.barbermot.pilot.signal;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;

public class CameraSignal extends Signal implements Runnable {
    
    Camera                      camera;
    float[]                     distances;
    
    public static SurfaceHolder holder;
    
    public CameraSignal() {
        camera = Camera.open();
        Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        camera.setParameters(params);
        distances = new float[3];
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }
    
    @Override
    public void run() {
        try {
            camera.getParameters().getFocusDistances(distances);
            this.notifyListeners(
                    distances[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX],
                    System.currentTimeMillis());
        } catch (ConnectionLostException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void abort() {
        camera.stopPreview();
        camera.release();
    }
    
}
