package com.barbermot.pilot.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

import com.barbermot.pilot.R;

public class FlightActivity extends Activity {
    
    public final String TAG = "FlightActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final ToggleButton togglebutton = (ToggleButton) findViewById(R.id.togglebutton);
        togglebutton.setChecked(isFlightServiceRunning() ? true : false);
        togglebutton.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                if (togglebutton.isChecked()) {
                    FlightActivity.this.startService(new Intent(
                            FlightActivity.this, FlightService.class));
                } else {
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
}
