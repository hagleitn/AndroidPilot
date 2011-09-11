package com.barbermot.pilot.rc;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.concurrent.TimeoutException;

import com.barbermot.pilot.quad.QuadCopter;

public class RemoteControl implements Runnable {

	public final static String TAG = "RemoteControl";

	public static final char FULL_MANUAL = 0xff;
	public static final char ELEVATOR_MASK = 0x01;
	public static final char AILERON_MASK = 0x02;
	public static final char THROTTLE_MASK = 0x04;
	public static final char RUDDER_MASK = 0x08;
	public static final float TIMEOUT = 0.5f;

	private static final int THROTTLE_MIN = 1300; // min raw value from pulse in
													// to take over
	private static final int THROTTLE_DELTA = 200; // if manual throttle comes
													// that close to current
													// setting take over

	private char controlMask; // all bits set means manual control for the
								// particular servo
	private int gainPin;
	private boolean armed;
	private QuadCopter ufo;
	private IOIO ioio;
	private DigitalOutput overridePins[];
	private final static int SIZE = 4;
	private int throttleMonitorPin;

	public RemoteControl(IOIO ioio, QuadCopter ufo, int aileronPin,
			int rudderPin, int throttlePin, int elevatorPin,
			int throttleMonitorPin, int gainPin) throws ConnectionLostException {
		this.gainPin = gainPin;
		this.ufo = ufo;
		this.ioio = ioio;

		this.throttleMonitorPin = throttleMonitorPin;

		overridePins = new DigitalOutput[SIZE];

		overridePins[0] = ioio.openDigitalOutput(aileronPin);
		overridePins[1] = ioio.openDigitalOutput(rudderPin);
		overridePins[2] = ioio.openDigitalOutput(throttlePin);
		overridePins[3] = ioio.openDigitalOutput(elevatorPin);
		this.setControlMask(FULL_MANUAL);
	}

	private int pulseIn(int pin, boolean val, float timeout)
			throws ConnectionLostException, TimeoutException {
		PulseInput pulse = ioio.openPulseInput(pin, val ? PulseMode.POSITIVE
				: PulseMode.NEGATIVE);
		int pw;
		try {
			while (true) {
				try {
					pw = (int) (pulse.getDuration(TIMEOUT) * 1000000);
					break;
				} catch (InterruptedException e) {
					// nothing
				}
			}
		} finally {
			if (pulse != null) {
				pulse.close();
			}
		}
		return pw;
	}

	@Override
	public void run() {
		try {
			if (controlMask == FULL_MANUAL || isEngaged()) {
				setControlMask(FULL_MANUAL);
			}
		} catch (TimeoutException e) {
			// Log.i(TAG, "isEngaged timed out.");
		} catch (ConnectionLostException e) {
			throw new RuntimeException(e);
		}
	}

	public void arm(boolean arm) {
		armed = arm;
	}

	public synchronized void setControlMask(char mask)
			throws ConnectionLostException {
		controlMask = mask;
		mask = 0x1;

		for (int i = 0; i < SIZE; ++i) {
			overridePins[i].write((controlMask & mask) != 0);
		}
	}

	public char getControlMask() {
		return controlMask;
	}

	public boolean isEngaged() throws ConnectionLostException, TimeoutException {
		int value = pulseIn(throttleMonitorPin, true, TIMEOUT);
		if (armed) {
			int vertical = ufo.readRaw(QuadCopter.Direction.VERTICAL);
			return value > THROTTLE_MIN && (value + THROTTLE_DELTA) > vertical;
		} else {
			if (value < THROTTLE_MIN) {
				arm(true);
			}
			return false;
		}
	}
}
