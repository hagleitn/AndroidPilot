package com.barbermot.pilot.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.barbermot.pilot.R;
import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.flight.FlightConfiguration.ConnectionType;
import com.barbermot.pilot.flight.FlightThread;

public class FlightService extends Service {
    
    private FlightThread        flightThread;
    private static final String TAG = "FlightService";
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "starting service");
        Bundle b = intent.getExtras();
        
        FlightConfiguration
                .get()
                .setSimulation(
                        b.getBoolean(getString(R.string.flight_service_simulation_flag)));
        
        ConnectionType connectionType = ConnectionType.UART;
        if (b.getString(getString(R.string.flight_service_serial_flag)).equals(
                getString(R.string.flight_service_tcp))) {
            connectionType = ConnectionType.TCP;
        }
        
        FlightConfiguration.get().setConnectionType(connectionType);
        FlightConfiguration
                .get()
                .setSerialUrl(
                        b.getString(getString(R.string.flight_service_serial_url_flag)));
        FlightConfiguration.get().setSerialPort(
                b.getInt(getString(R.string.flight_service_serial_port_flag)));
        
        FlightConfiguration.get().setRemoteControlPort(
                b.getInt(getString(R.string.flight_service_remote_port_flag)));
        
        flightThread.start();
        return START_STICKY;
    }
    
    @Override
    public void onCreate() {
        Log.i(TAG, "creating service");
        showNotification();
        flightThread = new FlightThread(
                (SensorManager) this.getSystemService(Context.SENSOR_SERVICE),
                (LocationManager) this
                        .getSystemService(Context.LOCATION_SERVICE));
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "detroying service");
        this.stopForeground(true);
        
        flightThread.abort();
        try {
            flightThread.join();
        } catch (InterruptedException e) {}
        flightThread = null;
        
        Toast.makeText(this, R.string.flight_service_stopped,
                Toast.LENGTH_SHORT).show();
    }
    
    private void showNotification() {
        CharSequence text = getText(R.string.flight_service_running);
        
        Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, FlightActivity.class), 0);
        
        notification.setLatestEventInfo(this,
                getText(R.string.flight_service_disable_message), text,
                contentIntent);
        
        startForeground(R.string.flight_service_running, notification);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
}
