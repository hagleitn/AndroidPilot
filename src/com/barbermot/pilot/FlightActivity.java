package com.barbermot.pilot;

import ioio.lib.api.IOIO;
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
		FlightComputer computer;
		public final int ultraSoundPin = 0; 
		public final int aileronPin = 1;
		public final int rudderPin = 2; 
		public final int throttlePin = 3;
		public final int elevatorPin = 4;
		public final int gainPin = 5;
		public final int rxPin = 6;
		public final int txPin = 7;

		@Override
		protected void setup() throws ConnectionLostException {
			computer = new FlightComputer(ioio_, ultraSoundPin, aileronPin, rudderPin, 
					throttlePin, elevatorPin, gainPin, rxPin, txPin);
		}

		@Override
		protected void loop() throws ConnectionLostException {
			computer.adjust();
		}
	}

	@Override
	protected ioio.lib.util.AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
}
