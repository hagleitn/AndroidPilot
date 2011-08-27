package com.barbermot.pilot;

import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;

import java.io.IOException;
import java.io.PrintStream;

import android.hardware.SensorManager;
import android.util.Log;


public class FlightThread extends Thread {
	
	public final String TAG = "FlightThread";
	
	// Pulse IN
	public final int ultraSoundPin 	= 4; 
	public final int aileronPinIn 	= 28;
	public final int rudderPinIn 	= 6; 
	public final int throttlePinIn 	= 7;
	public final int elevatorPinIn 	= 27;
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
	
	protected IOIO ioio;
	private boolean abort = false;
	private boolean connected = true;
	
	private FlightComputer computer;
	private PrintStream printer;
	private SerialController controller;
	private SensorManager manager;
	
	public FlightThread(SensorManager manager) {
		this.manager = manager;
	}

	@Override
	public final void run() {
		while (true) {
			try {
				synchronized (this) {
					if (abort) {
						break;
					}
					ioio = IOIOFactory.create();
				}
				ioio.waitForConnect();
				connected = true;
				setup();
				while (!abort) {
					loop();
				}
				ioio.disconnect();
			} catch (ConnectionLostException e) {
				if (abort) {
					break;
				}
			} catch (IncompatibilityException e) {
				Log.e("AbstractIOIOActivity",
						"Incompatible IOIO firmware", e);
				// nothing to do - just wait until physical disconnection
				try {
					ioio.waitForDisconnect();
				} catch (InterruptedException e1) {
					ioio.disconnect();
				}
			} catch (Exception e) {
				Log.e("AbstractIOIOActivity",
						"Unexpected exception caught", e);
				ioio.disconnect();
				break;
			} finally {
				try {
					if (ioio != null) {
						ioio.waitForDisconnect();
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	public synchronized final void abort() {
		abort = true;
		if (ioio != null) {
			ioio.disconnect();
		}
		if (connected) {
			interrupt();
		}
	}

	private void setup() throws ConnectionLostException {
		printer = new PrintStream(ioio.openUart(IOIO.INVALID_PIN, txPin, 9600, 
				Uart.Parity.NONE, Uart.StopBits.ONE).getOutputStream());
		computer = new FlightComputer(ioio, 
				ultraSoundPin, aileronPinOut, rudderPinOut, 
				throttlePinOut, elevatorPinOut, gainPinOut, 
				aileronPinIn, rudderPinIn,throttlePinIn, 
				elevatorPinIn, gainPinIn, txPin, printer, 
				manager);
		controller = new SerialController(ioio, computer, ';', rxPin, printer);
		Log.d(TAG, "Setup complete.");
	}

	private void loop() throws ConnectionLostException {
		try {
			controller.executeCommand();
		} catch(IOException e) {
			Log.e(TAG, "IOException");
		}
		computer.adjust();
	}
}
