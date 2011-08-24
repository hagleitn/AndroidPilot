package com.barbermot.pilot;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import android.os.Bundle;

public class FlightActivity extends AbstractIOIOActivity {

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {

		@Override
		protected void setup() throws ConnectionLostException {
		}

		@Override
		protected void loop() throws ConnectionLostException {
		}
	}

	@Override
	protected ioio.lib.util.AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
}
