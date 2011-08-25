package com.barbermot.pilot;

import java.io.PrintStream;

import android.util.Log;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

class FlightComputer {
	
	public final String TAG = "FlightComputer";

	public FlightComputer(IOIO ioio, int ultraSoundPin, int aileronPinOut, int rudderPinOut, 
			int throttlePinOut, int elevatorPinOut, int gainPinOut, int aileronPinIn, int rudderPinIn, 
			int throttlePinIn, int elevatorPinIn, int gainPinIn, int txPin, PrintStream printer) 
				throws ConnectionLostException {
		
		this.hoverConf = HOVER_CONF;
		this.landingConf = LANDING_CONF;
		this.gyroConf = GYRO_CONF;

		this.autoThrottle = new AutoControl(this.throttleControl);
		this.autoAileron = new AutoControl(this.aileronControl);
		this.autoElevator = new AutoControl(this.elevatorControl);

		this.ultraSoundSignal = new UltrasoundSignal(ioio, ultraSoundPin);
		this.longitudinalSignal = new GyroSignal(ioio, 0);
		this.lateralSignal = new GyroSignal(ioio, 0);
		
		this.ultraSoundSignal.registerListener(this.heightListener);
		this.ultraSoundSignal.registerListener(this.autoThrottle);
		
		this.lateralSignal.registerListener(this.lateralListener);
		this.lateralSignal.registerListener(this.autoAileron);
		
		this.longitudinalSignal.registerListener(this.longitudinalListener);
		this.longitudinalSignal.registerListener(this.autoElevator);

		this.ufo = new QuadCopter(ioio, aileronPinOut, rudderPinOut, throttlePinOut, elevatorPinOut, gainPinOut);
		this.rc = new RemoteControl(ioio, ufo, aileronPinIn, rudderPinIn, throttlePinIn, elevatorPinIn, gainPinIn);

		this.minThrottle = MIN_THROTTLE;
		this.maxThrottle = MAX_THROTTLE;

		this.state = State.GROUND;
		
		this.printer = printer;
		
		time = System.currentTimeMillis();
		lastTimeHeightSignal = time;
		lastTimeAccelSignal = time;
		lastTimeLog = time;

	}

	public void takeoff(double height) {
		if (state == State.GROUND) {
			state = State.HOVER;
			autoThrottle.setConfiguration(hoverConf);
			autoThrottle.setGoal(height);
			autoThrottle.engage(true);
		}
	}

	public void hover(double height) {
		if (state == State.HOVER || state == State.LANDING || state == State.ENGAGING_AUTO_CONTROL) {
			state = State.HOVER;
			autoThrottle.setConfiguration(hoverConf);
			autoThrottle.setGoal(height);
			autoThrottle.engage(true);
		}
	}

	public void ground() throws ConnectionLostException {
		if (State.LANDING == state) {
			state = State.GROUND;
			autoThrottle.engage(false);
			ufo.throttle(QuadCopter.MIN_SPEED);
			currentThrottle = QuadCopter.MIN_SPEED;
		}	    	
	}

	public void land() {
		if (state == State.HOVER || state == State.EMERGENCY_LANDING) {
			state = State.LANDING;
			autoThrottle.setConfiguration(landingConf);
			autoThrottle.setGoal(zeroHeight);
			autoThrottle.engage(true);
		}
	}

	public void emergencyDescent() throws ConnectionLostException {
		if (State.FAILED != state && State.GROUND != state && State.EMERGENCY_LANDING != state) {
			autoThrottle.engage(false);
			ufo.throttle(EMERGENCY_DESCENT);
			currentThrottle = EMERGENCY_DESCENT;
			state = State.EMERGENCY_LANDING;
		}
	}

	public void manualControl() {
		if (state != State.MANUAL_CONTROL) {
			autoThrottle.engage(false);
			stabilize(false);
			state = State.MANUAL_CONTROL;
		}
	}

	public void autoControl() {
		if (state == State.MANUAL_CONTROL) {
			state = State.ENGAGING_AUTO_CONTROL;

			// set rc to allow auto control of throttle
			char controlMask = rc.getControlMask();
			controlMask = (char) (controlMask & (~RemoteControl.THROTTLE_MASK));
			rc.setControlMask(controlMask);

			// disarm rc, so it doesn't immediately engage again
			rc.arm(false);

			// use current throttle setting and height for start values
			currentThrottle = ufo.read(QuadCopter.Direction.VERTICAL);
			hover(height);
		}
	}

	public void abort() throws ConnectionLostException {
		state = State.FAILED;
		autoThrottle.engage(false);
		stabilize(false);
		ufo.throttle(QuadCopter.MIN_SPEED);
		currentThrottle = QuadCopter.MIN_SPEED;
	}

	public void stabilize(boolean engage) {
		char mask = RemoteControl.AILERON_MASK | RemoteControl.ELEVATOR_MASK;
		char controlMask = rc.getControlMask();

		if (engage) {
			controlMask = (char) (controlMask & ~mask);
		} else {
			controlMask = (char) (controlMask | mask);
			currentElevator = QuadCopter.STOP_SPEED;
			currentAileron = QuadCopter.STOP_SPEED;
		}
		rc.setControlMask(controlMask);

		autoElevator.setConfiguration(gyroConf);
		autoAileron.setConfiguration(gyroConf);
		autoElevator.setGoal(zeroLongitudinalForce);
		autoAileron.setGoal(zeroLateralForce);
		autoElevator.engage(engage);
		autoAileron.engage(engage);
	}

	private void log() {
		printer.print("st: ");
		printer.print(state);
		printer.print(", ms: ");
		printer.print(time);
		printer.print(", rc: ");
		printer.println((byte)rc.getControlMask());
		printer.print("h: ");
		printer.print(height/*-zeroHeight*/);
		printer.print(", y'': ");
		printer.print(longitudinalForce-zeroLongitudinalForce);
		printer.print(", x'': ");
		printer.println(lateralForce-zeroLateralForce);
		printer.print("t: ");
		printer.print(currentThrottle);
		printer.print(", e: ");
		printer.print(currentElevator);
		printer.print(", a: ");
		printer.println(currentAileron);
		printer.println();
		printer.flush();
		lastTimeLog = time;
	}

	public void adjust() throws ConnectionLostException {
		time = System.currentTimeMillis();
		//Log.d(TAG, "Adjust: "+time);

		// the following state transitions can origin in any state

		// allow for manual inputs first
		//Log.d(TAG, "Updating rc..");
		rc.update();
		if (rc.getControlMask() == RemoteControl.FULL_MANUAL) {
			manualControl();
		}

		// no height signal from ultra sound try descending
		if (time - lastTimeHeightSignal > EMERGENCY_DELTA) {
			//Log.d(TAG, "time: "+time);
			//Log.d(TAG, "height time: "+lastTimeHeightSignal);
			emergencyDescent();
		}

		// here are specific transitions

		switch (state) {
		case GROUND:
			// calibration
			zeroHeight = height;
			zeroLongitudinalForce = longitudinalForce;
			zeroLateralForce = lateralForce;
			break;
		case HOVER:
			// nothing
			break;
		case LANDING:
			// turn off throttle when close to ground
			if (height <= zeroHeight + THROTTLE_OFF_HEIGHT) {
				ground();
			}
			break;
		case EMERGENCY_LANDING:
			// if we have another reading land
			if (time - lastTimeHeightSignal < EMERGENCY_DELTA) {
				land();
			}
			break;
		case MANUAL_CONTROL:
			// nothing
			break;
		case FAILED:
			// nothing
			break;
		case ENGAGING_AUTO_CONTROL:
			// nothing
			break;
		default:
			// this is bad
			land();
			break;
		}

		// sensors and log

		if (time - lastTimeHeightSignal > MIN_TIME_ULTRA_SOUND) {
			//Log.d(TAG, "Requesting ultrasound signal...");
			ultraSoundSignal.signal();
		}

		if (time - lastTimeLog > MIN_TIME_STATUS_MESSAGE) {
			//Log.d(TAG, "Logging...");
			log();
		}

		if (time - lastTimeAccelSignal > MIN_TIME_GYRO) {
			//Log.d(TAG, "Requesting gyro signal...");
			longitudinalSignal.signal();
			lateralSignal.signal();
		}
	}

	public void setHoverConfiguration(double[] conf) {
		hoverConf = conf;
	}

	public void setLandingConfiguration(double[] conf) {
		landingConf = conf;
	}

	public void setStabilizerConfiguration(double[] conf) {
		gyroConf = conf;
	}

	public void setMinThrottle(int min) {
		this.minThrottle = min;
	}

	public void setMaxThrottle(int max) {
		this.maxThrottle = max;
	}

	// limit value to range
	private static double limit(double value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	// adjusts output from PID controller for throttle setting	    
	private ControlListener throttleControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentThrottle = (int)limit(x, FlightComputer.this.minThrottle, FlightComputer.this.maxThrottle);
			FlightComputer.this.ufo.throttle(currentThrottle);
		}
	};

	// adjusts output from PID controller for elevator setting
	private ControlListener elevatorControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentElevator = (int)limit(x, MIN_TILT, MAX_TILT);
			FlightComputer.this.ufo.elevator(currentElevator);
		}
	};

	// adjusts output from PID controller for aileron setting
	private ControlListener aileronControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentAileron = (int)limit(x, MIN_TILT, MAX_TILT);
			FlightComputer.this.ufo.aileron(currentAileron);
		}
	};


	// Listener to update the height of the flight computer
	private SignalListener heightListener = new SignalListener() {
		public void update(double x, long time) {
			//Log.d(TAG, "Height: "+x);
			FlightComputer.this.height = x;
			FlightComputer.this.lastTimeHeightSignal = time;
		}
	};

	// Listener to update the lateral force on the flight computer	    
	private SignalListener lateralListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.lateralForce = x;
			FlightComputer.this.lastTimeAccelSignal = time;
		}
	};


	// Listener to update the longitudinal force on the flight computer
	private SignalListener longitudinalListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.longitudinalForce = x;
			FlightComputer.this.lastTimeAccelSignal = time;
		}
	};
	
	// values for the PID controller
	private static final double[] HOVER_CONF   	= { 0.57, 0.0007,  350, -6000,  40000 };
	private static final double[] LANDING_CONF 	= { 0, 0.001, 600,   -10000, 10000 };
	private static final double[] GYRO_CONF		= { 0.5, 0.005,   200, -1000,   1000 };

	// Flight computer states
	private enum State {GROUND, HOVER, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL, ENGAGING_AUTO_CONTROL};

	// delay between readings of the ultra sound module
	private static final int MIN_TIME_ULTRA_SOUND = 100;

	// delay between readings of the gyro
	private static final int MIN_TIME_GYRO = 50;

	// delay between status messages
	private static final int MIN_TIME_STATUS_MESSAGE = 5000;

	// initial min/max throttle setting
	private static final int MIN_THROTTLE = QuadCopter.MIN_SPEED+(QuadCopter.MAX_SPEED-QuadCopter.MIN_SPEED)/3;
	private static final int MAX_THROTTLE = QuadCopter.MAX_SPEED-(QuadCopter.MAX_SPEED-QuadCopter.MIN_SPEED)/8;

	// min/max for the automatic control of the aileron and elevator
	private static final int MIN_TILT = QuadCopter.MIN_SPEED/2;
	private static final int MAX_TILT = QuadCopter.MAX_SPEED/2;

	// landings will cut the power once this height is reached
	private static final int THROTTLE_OFF_HEIGHT = 10;

	// throttle setting for when we don't know the height anymore
	private static final int EMERGENCY_DESCENT = QuadCopter.STOP_SPEED-(QuadCopter.MAX_SPEED-QuadCopter.MIN_SPEED)/20;
	private static final int EMERGENCY_DELTA = 1000;

	private QuadCopter ufo; // quad copter
	private RemoteControl rc; // RC signal (from RC controller)

	private UltrasoundSignal ultraSoundSignal; // distance pointing down
	private GyroSignal longitudinalSignal; // accel on y axis
	private GyroSignal lateralSignal; // accel on x axis

	private AutoControl autoThrottle; // autopilot for throttle
	private AutoControl autoElevator; // autopilot for elevator
	private AutoControl autoAileron; // autopilot for aileron

	// values for the PID controller
	private double[] hoverConf;
	private double[] landingConf;
	private double[] gyroConf;
	
	// Log writer
	private PrintStream printer;

	// min/max for the automatic control of the throttle
	private int minThrottle;
	private int maxThrottle;

	private State state;

	private double height;
	private double zeroHeight;

	private double longitudinalForce;
	private double zeroLongitudinalForce;

	private double lateralForce;
	private double zeroLateralForce;

	private long time;
	private long lastTimeHeightSignal;
	private long lastTimeAccelSignal;
	private long lastTimeLog;

	private int currentThrottle;
	private int currentElevator;
	private int currentAileron;
}