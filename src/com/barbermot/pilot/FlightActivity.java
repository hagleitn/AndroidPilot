package com.barbermot.pilot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class FlightActivity extends Activity {
	
	public final String TAG = "FlightActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final ToggleButton togglebutton = (ToggleButton) findViewById(R.id.togglebutton);
        togglebutton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (togglebutton.isChecked()) {
                	FlightActivity.this.startService(new Intent(FlightActivity.this,FlightService.class));
                } else {
                	FlightActivity.this.stopService(new Intent(FlightActivity.this,FlightService.class));
                }
            }
        });
    }    
}
