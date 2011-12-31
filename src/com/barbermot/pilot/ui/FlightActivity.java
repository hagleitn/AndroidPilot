package com.barbermot.pilot.ui;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

import com.barbermot.pilot.R;

public class FlightActivity extends Activity {
    
    private static final String TAG = "FlightActivity";
    Camera                      camera;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final ToggleButton togglebutton = (ToggleButton) findViewById(R.id.togglebutton);
        final CheckBox checkbox = (CheckBox) findViewById(R.id.checkBox1);
        final RadioGroup group = (RadioGroup) findViewById(R.id.radioGroup1);
        final EditText url = (EditText) findViewById(R.id.editText1);
        final EditText port = (EditText) findViewById(R.id.editText2);
        
        // url.setText(getLocalIpAddress());
        // url.setEnabled(false);
        
        togglebutton.setChecked(isFlightServiceRunning() ? true : false);
        togglebutton.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                if (togglebutton.isChecked()) {
                    Log.i(TAG, "Starting service...");
                    
                    checkbox.setEnabled(false);
                    group.setEnabled(false);
                    url.setEnabled(false);
                    port.setEnabled(false);
                    
                    Intent start = new Intent(FlightActivity.this,
                            FlightService.class);
                    start.putExtra(
                            getString(R.string.flight_service_simulation_flag),
                            checkbox.isChecked());
                    
                    RadioButton r = (RadioButton) findViewById(group
                            .getCheckedRadioButtonId());
                    start.putExtra(
                            getString(R.string.flight_service_serial_flag),
                            r.getText());
                    
                    start.putExtra(
                            getString(R.string.flight_service_serial_url_flag),
                            url.getText().toString());
                    
                    int portNum = 3333;
                    try {
                        portNum = Integer.parseInt(port.getText().toString());
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Couldn't parse port");
                    }
                    
                    start.putExtra(
                            getString(R.string.flight_service_serial_port_flag),
                            portNum);
                    FlightActivity.this.startService(start);
                } else {
                    Log.i(TAG, "Stopping service...");
                    
                    checkbox.setEnabled(true);
                    group.setEnabled(true);
                    url.setEnabled(true);
                    port.setEnabled(true);
                    
                    FlightActivity.this.stopService(new Intent(
                            FlightActivity.this, FlightService.class));
                }
            }
        });
    }
    
    private boolean isFlightServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if ("com.barbermot.pilot.ui.FlightService".equals(service.service
                    .getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }
}
