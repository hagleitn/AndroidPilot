package com.barbermot.pilot;

import java.io.PrintStream;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

class FlightComputer {

	// values for the PID controller
	static final double[] HOVER_CONF   	= { 0.57, 0.0007,  350, -6000,  40000 };
	static final double[] LANDING_CONF 	= { 0, 0.001, 600,   -10000, 10000 };
	static final double[] GYRO_CONF		= { 0.5, 0.005,   200, -1000,   1000 };

	// Flight computer states
	enum State {GROUND, HOVER, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL, ENGAGING_AUTO_CONTROL};

	// delay between readings of the ultra sound module
	static final int MIN_TIME_ULTRA_SOUND = 100;

	// delay between readings of the gyro
	static final int MIN_TIME_GYRO = 50;

	// delay between status messages
	static final int MIN_TIME_STATUS_MESSAGE = 5000;

	// initial min/max throttle setting
	static final int MIN_THROTTLE = QuadCopter.MIN_SPEED+(QuadCopter.MAX_SPEED-QuadCopter.MIN_SPEED)/3;
	static final int MAX_THROTTLE = QuadCopter.MAX_SPEED-(QuadCopter.MAX_SPEED-QuadCopter.MIN_SPEED)/8;

	// min/max for the automatic control of the aileron and elevator
	static final int MIN_TILT = QuadCopter.MIN_SPEED/2;
	static final int MAX_TILT = QuadCopter.MAX_SPEED/2;

	// landings will cut the power once this height is reached
	static final int THROTTLE_OFF_HEIGHT = 10;

	// throttle setting for when we don't know the height anymore
	static final int EMERGENCY_DESCENT = QuadCopter.STOP_SPEED-(QuadCopter.MAX_SPEED-QuadCopter.MIN_SPEED)/20;
	static final int EMERGENCY_DELTA = 1000;

	FlightComputer(IOIO ioio, int ultraSoundPin, int aileronPin, int rudderPin, 
			int throttlePin, int elevatorPin, int gainPin, int rxPin, int txPin) throws ConnectionLostException {
		this.hoverConf = HOVER_CONF;
		this.landingConf = LANDING_CONF;
		this.gyroConf = GYRO_CONF;

		this.autoThrottle = new AutoControl(this.throttleControl);
		this.autoAileron = new AutoControl(this.aileronControl);
		this.autoElevator = new AutoControl(this.elevatorControl);

		this.ultraSoundSignal = new UltrasoundSignal(ioio, ultraSoundPin);
		this.longitudinalSignal = new GyroSignal(ioio, 0);
		this.lateralSignal = new GyroSignal(ioio, 0);

		this.ufo = new QuadCopter(ioio, aileronPin, rudderPin, throttlePin, elevatorPin, gainPin);
		this.rc = new RemoteControl();

		this.minThrottle = MIN_THROTTLE;
		this.maxThrottle = MAX_THROTTLE;

		this.state = State.GROUND;
		
		this.printer = new PrintStream(ioio.openUart(rxPin, txPin, 9600, Uart.Parity.NONE, Uart.StopBits.ONE).getOutputStream());
	}

	void takeoff(double height) {
		if (state == State.GROUND) {
			state = State.HOVER;
			autoThrottle.setConfiguration(hoverConf);
			autoThrottle.setGoal(height);
			autoThrottle.engage(true);
		}
	}

	void hover(double height) {
		if (state == State.HOVER || state == State.LANDING || state == State.ENGAGING_AUTO_CONTROL) {
			state = State.HOVER;
			autoThrottle.setConfiguration(hoverConf);
			autoThrottle.setGoal(height);
			autoThrottle.engage(true);
		}
	}

	void ground() throws ConnectionLostException {
		if (State.LANDING == state) {
			state = State.GROUND;
			autoThrottle.engage(false);
			ufo.throttle(QuadCopter.MIN_SPEED);
			currentThrottle = QuadCopter.MIN_SPEED;
		}	    	
	}

	void land() {
		if (state == State.HOVER || state == State.EMERGENCY_LANDING) {
			state = State.LANDING;
			autoThrottle.setConfiguration(landingConf);
			autoThrottle.setGoal(zeroHeight);
			autoThrottle.engage(true);
		}
	}

	void emergencyDescent() throws ConnectionLostException {
		if (State.FAILED != state && State.GROUND != state && State.EMERGENCY_LANDING != state) {
			autoThrottle.engage(false);
			ufo.throttle(EMERGENCY_DESCENT);
			currentThrottle = EMERGENCY_DESCENT;
			state = State.EMERGENCY_LANDING;
		}
	}

	void manualControl() {
		if (state != State.MANUAL_CONTROL) {
			autoThrottle.engage(false);
			stabilize(false);
			state = State.MANUAL_CONTROL;
		}
	}

	void autoControl() {
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

	void abort() throws ConnectionLostException {
		state = State.FAILED;
		autoThrottle.engage(false);
		stabilize(false);
		ufo.throttle(QuadCopter.MIN_SPEED);
		currentThrottle = QuadCopter.MIN_SPEED;
	}

	void stabilize(boolean engage) {
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

	void log() {
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
		lastTimeLog = time;
	}

	void adjust() throws ConnectionLostException {
		time = System.currentTimeMillis();

		// the following state transitions can origin in any state

		// allow for manual inputs first
		rc.update();
		if (rc.getControlMask() == RemoteControl.FULL_MANUAL) {
			manualControl();
		}

		// no height signal from ultra sound try descending
		if (time - lastTimeHeightSignal > EMERGENCY_DELTA) {
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
			ultraSoundSignal.signal();
		}

		if (time - lastTimeLog > MIN_TIME_STATUS_MESSAGE) {
			log();
		}

		if (time - lastTimeAccelSignal > MIN_TIME_GYRO) {
			longitudinalSignal.signal();
			lateralSignal.signal();
		}
	}

	void setHoverConfiguration(double[] conf) {
		hoverConf = conf;
	}

	void setLandingConfiguration(double[] conf) {
		landingConf = conf;
	}

	void setStabilizerConfiguration(double[] conf) {
		gyroConf = conf;
	}

	void setMinThrottle(int min) {
		this.minThrottle = min;
	}

	void setMaxThrottle(int max) {
		this.maxThrottle = max;
	}

	// limit value to range
	static double limit(double value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	QuadCopter ufo; // quad copter
	RemoteControl rc; // RC signal (from RC controller)

	UltrasoundSignal ultraSoundSignal; // distance pointing down
	GyroSignal longitudinalSignal; // accel on y axis
	GyroSignal lateralSignal; // accel on x axis

	// adjusts output from PID controller for throttle setting	    
	ControlListener throttleControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentThrottle = (int)limit(x, FlightComputer.this.minThrottle, FlightComputer.this.maxThrottle);
			FlightComputer.this.ufo.throttle(currentThrottle);
		}
	};

	// adjusts output from PID controller for elevator setting
	ControlListener elevatorControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentElevator = (int)limit(x, MIN_TILT, MAX_TILT);
			FlightComputer.this.ufo.elevator(currentElevator);
		}
	};

	// adjusts output from PID controller for aileron setting
	ControlListener aileronControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentAileron = (int)limit(x, MIN_TILT, MAX_TILT);
			FlightComputer.this.ufo.aileron(currentAileron);
		}
	};


	// Listener to update the height of the flight computer
	SignalListener heightListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.height = x;
			FlightComputer.this.lastTimeHeightSignal = time;
		}
	};

	// Listener to update the lateral force on the flight computer	    
	SignalListener lateralListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.lateralForce = x;
			FlightComputer.this.lastTimeAccelSignal = time;
		}
	};


	// Listener to update the longitudinal force on the flight computer
	SignalListener longitudinalListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.longitudinalForce = x;
			FlightComputer.this.lastTimeAccelSignal = time;
		}
	};


	AutoControl autoThrottle; // autopilot for throttle
	AutoControl autoElevator; // autopilot for elevator
	AutoControl autoAileron; // autopilot for aileron

	// values for the PID controller
	double[] hoverConf;
	double[] landingConf;
	double[] gyroConf;
	
	// Log writer
	PrintStream printer;

	// min/max for the automatic control of the throttle
	int minThrottle;
	int maxThrottle;

	State state;

	double height;
	double zeroHeight;

	double longitudinalForce;
	double zeroLongitudinalForce;

	double lateralForce;
	double zeroLateralForce;

	long time;
	long lastTimeHeightSignal;
	long lastTimeAccelSignal;
	long lastTimeLog;

	int currentThrottle;
	int currentElevator;
	int currentAileron;
}