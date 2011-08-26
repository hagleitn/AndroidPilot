package com.barbermot.pilot;

import java.io.IOException;
import java.io.PrintStream;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Printer;

public class FlightActivity extends AbstractIOIOActivity {
	
	public final String TAG = "FlightActivity";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		FlightComputer computer;
		PrintStream printer;
		SerialController controller;
		
		// Pulse IN
		public final int ultraSoundPin 	= 4; 
		public final int aileronPinIn 	= 16;
		public final int rudderPinIn 	= 6; 
		public final int throttlePinIn 	= 7;
		public final int elevatorPinIn 	= 8;
		public final int gainPinIn 		= 22;
		
		// PWM
		public final int aileronPinOut 	= 10;
		public final int rudderPinOut 	= 11; 
		public final int throttlePinOut = 12;
		public final int elevatorPinOut = 13;
		public final int gainPinOut 	= 14;
		
		//UART
		public final int rxPin 			= 9;
		public final int txPin 			= 3;

		@Override
		protected void setup() throws ConnectionLostException {
			SensorManager manager = (SensorManager)FlightActivity.this.getSystemService(Context.SENSOR_SERVICE);
			printer = new PrintStream(ioio_.openUart(IOIO.INVALID_PIN, txPin, 9600, 
					Uart.Parity.NONE, Uart.StopBits.ONE).getOutputStream());
			computer = new FlightComputer(ioio_, 
					ultraSoundPin, aileronPinOut, rudderPinOut, 
					throttlePinOut, elevatorPinOut, gainPinOut, 
					aileronPinIn, rudderPinIn,throttlePinIn, 
					elevatorPinIn, gainPinIn, txPin, printer, 
					manager);
			controller = new SerialController(ioio_, computer, ';', rxPin, printer);
			Log.d(TAG, "Setup complete.");
		}

		@Override
		protected void loop() throws ConnectionLostException {
			try {
				controller.executeCommand();
			} catch(IOException e) {
				Log.d(TAG, "IOException");
			}
			computer.adjust();
		}
	}

	@Override
	protected ioio.lib.util.AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
}
