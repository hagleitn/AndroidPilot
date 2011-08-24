package com.barbermot.pilot;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;

public class Servo {
	
	public Servo(IOIO ioio, int pin, int minIn, int maxIn, int minOut, int maxOut) throws ConnectionLostException {
		this.pin = pin;
		this.minIn = minIn;
		this.maxIn = maxIn;
		this.minOut = minOut;
		this.maxOut = maxOut;
		pwm = ioio.openPwmOutput(pin, 50);
	}
	
	public int read() {
		return mapReverse(readRaw());
	}
	
	public void write(int value) throws ConnectionLostException {
		writeRaw(map(value));
	}
	
	public int readRaw() {
		return pulseWidth;
	}
	
	public void writeRaw(int pulseWidth) throws ConnectionLostException {
		this.pulseWidth = pulseWidth;
		pwm.setPulseWidth(pulseWidth);
	}
	
	private int map(int value) {
		return minOut + (maxOut-minOut)*(value-minIn)/(maxIn-minIn);
	}
	
	private int mapReverse(int value) {
		return minIn + (maxIn-minIn)*(value-minOut)/(maxOut-minOut);
	}
	
	int pin;
	int pulseWidth;
	int minIn;
	int maxIn;
	int minOut;
	int maxOut;
	PwmOutput pwm;
}
