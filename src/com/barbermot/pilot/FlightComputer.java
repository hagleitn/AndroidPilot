package com.barbermot.pilot;

import java.io.PrintStream;

import android.hardware.SensorManager;
import android.util.Log;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

class FlightComputer {
	
	public final String TAG = "FlightComputer";
	
	// values for the PID controller
	private static final double[] HOVER_CONF   		= { 0.57, 0.0007,  350, -6000,  40000 };
	private static final double[] LANDING_CONF 		= { 0, 0.001, 600,   -10000, 10000 };
	private static final double[] ORIENTATION_CONF	= { 0.5, 0.0007,   200, -6000,   40000 };

	// Flight computer states
	private enum State {GROUND, HOVER, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL, ENGAGING_AUTO_CONTROL};

	// delay between readings of the ultra sound module
	private static final int MIN_TIME_ULTRA_SOUND = 100;

	// delay between readings of the gyro
	private static final int MIN_TIME_ORIENTATION = 150;

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
	private OrientationSignal longitudinalSignal; // displacement on y axis
	private OrientationSignal lateralSignal; // displacement on x axis
	private OrientationSignal compassSignal;

	private AutoControl autoThrottle; // autopilot for throttle
	private AutoControl autoElevator; // autopilot for elevator
	private AutoControl autoAileron; // autopilot for aileron
	private AutoControl autoRudder; // autopilot for rudder

	// values for the PID controller
	private double[] hoverConf;
	private double[] landingConf;
	private double[] orientationConf;
	
	// Log writer
	private PrintStream printer;

	// min/max for the automatic control of the throttle
	private int minThrottle;
	private int maxThrottle;

	private State state;

	private double height;
	private double zeroHeight;

	private double longitudinalDisplacement;
	private double lateralDisplacement;
	private double heading;

	private long time;
	private long lastTimeHeightSignal;
	private long lastTimeOrientationSignal;
	private long lastTimeLog;

	private int currentThrottle;
	private int currentElevator;
	private int currentAileron;
	private int currentRudder;

	public FlightComputer(IOIO ioio, int ultraSoundPin, int aileronPinOut, int rudderPinOut, 
			int throttlePinOut, int elevatorPinOut, int gainPinOut, int aileronPinIn, int rudderPinIn, 
			int throttlePinIn, int elevatorPinIn, int gainPinIn, int txPin, PrintStream printer, SensorManager manager) 
				throws ConnectionLostException {
		
		this.hoverConf = HOVER_CONF;
		this.landingConf = LANDING_CONF;
		this.orientationConf = ORIENTATION_CONF;

		this.autoThrottle = new AutoControl(this.throttleControl);
		this.autoAileron = new AutoControl(this.aileronControl) { protected double computeError(double value) {
			return FlightComputer.minRadianDistance(getGoal(), value);
		} };
		this.autoElevator = new AutoControl(this.elevatorControl) { protected double computeError(double value) {
			return FlightComputer.minRadianDistance(getGoal(), value);		
		} };
		this.autoRudder = new AutoControl(this.rudderControl) { protected double computeError(double value) {
			return FlightComputer.minRadianDistance(getGoal(), value);		
		} };

		this.ultraSoundSignal = new UltrasoundSignal(ioio, ultraSoundPin);
		this.longitudinalSignal = new OrientationSignal(ioio, manager, OrientationSignal.Type.PITCH);
		this.lateralSignal = new OrientationSignal(ioio, manager, OrientationSignal.Type.ROLL);
		this.compassSignal = new OrientationSignal(ioio, manager, OrientationSignal.Type.YAW);
		
		this.ultraSoundSignal.registerListener(this.heightListener);
		this.ultraSoundSignal.registerListener(this.autoThrottle);
		
		this.lateralSignal.registerListener(this.lateralListener);
		this.lateralSignal.registerListener(this.autoAileron);
		
		this.longitudinalSignal.registerListener(this.longitudinalListener);
		this.longitudinalSignal.registerListener(this.autoElevator);
		
		this.compassSignal.registerListener(this.compassListener);
		this.compassSignal.registerListener(this.autoRudder);

		this.ufo = new QuadCopter(ioio, aileronPinOut, rudderPinOut, throttlePinOut, elevatorPinOut, gainPinOut);
		this.rc = new RemoteControl(ioio, ufo, aileronPinIn, rudderPinIn, throttlePinIn, elevatorPinIn, gainPinIn);
		
		this.rc.setControlMask((char) ~RemoteControl.THROTTLE_MASK);

		this.minThrottle = MIN_THROTTLE;
		this.maxThrottle = MAX_THROTTLE;

		this.state = State.GROUND;
		
		this.printer = printer;
		
		time = System.currentTimeMillis();
		lastTimeHeightSignal = time;
		lastTimeOrientationSignal = time;
		lastTimeLog = time;

	}
	
	public static double minRadianDistance(double goal, double value) {
		double left = goal - value;
		double right = goal < value ?  goal - (value - 2*Math.PI) : goal - (value + 2*Math.PI);
		return Math.abs(left) < Math.abs(right) ? left : right;
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
		char mask = RemoteControl.AILERON_MASK | RemoteControl.ELEVATOR_MASK | RemoteControl.RUDDER_MASK;
		char controlMask = rc.getControlMask();

		if (engage) {
			controlMask = (char) (controlMask & ~mask);
		} else {
			controlMask = (char) (controlMask | mask);
			currentElevator = QuadCopter.STOP_SPEED;
			currentAileron = QuadCopter.STOP_SPEED;
			currentRudder = QuadCopter.STOP_SPEED;
		}
		rc.setControlMask(controlMask);

		autoElevator.setConfiguration(orientationConf);
		autoAileron.setConfiguration(orientationConf);
		autoRudder.setConfiguration(orientationConf);
		autoElevator.setGoal(0);
		autoAileron.setGoal(0);
		autoRudder.setGoal(0);
		autoElevator.engage(engage);
		autoAileron.engage(engage);
		autoRudder.engage(engage);
	}

	private void log() {
		printer.printf("st: %s\tms: %d\trc: %h\nh: %f\tdy: %f\tdx: %f\tdz: %f\nt: %d\te: %d\ta: %d\tr: %d\n",
				state,time,(byte)rc.getControlMask(),height,longitudinalDisplacement,lateralDisplacement,
				heading,currentThrottle,currentElevator,currentAileron,currentRudder);
		lastTimeLog = time;
	}

	public void adjust() throws ConnectionLostException {
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

		if (time - lastTimeOrientationSignal > MIN_TIME_ORIENTATION) {
			longitudinalSignal.signal();
			lateralSignal.signal();
			compassSignal.signal();
		}
	}

	public void setHoverConfiguration(double[] conf) {
		hoverConf = conf;
	}

	public void setLandingConfiguration(double[] conf) {
		landingConf = conf;
	}

	public void setStabilizerConfiguration(double[] conf) {
		orientationConf = conf;
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

	// adjusts output from PID controller for rudder setting
	private ControlListener rudderControl = new ControlListener() {
		public void adjust(double x) throws ConnectionLostException {
			currentRudder = (int)limit(x, MIN_TILT, MAX_TILT);
			FlightComputer.this.ufo.rudder(currentRudder);
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
			FlightComputer.this.lateralDisplacement = x;
			FlightComputer.this.lastTimeOrientationSignal = time;
		}
	};


	// Listener to update the longitudinal force on the flight computer
	private SignalListener longitudinalListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.longitudinalDisplacement = x;
			FlightComputer.this.lastTimeOrientationSignal = time;
		}
	};
	
	// Listener to update the compass heading of the flight computer
	private SignalListener compassListener = new SignalListener() {
		public void update(double x, long time) {
			FlightComputer.this.heading = x;
			FlightComputer.this.lastTimeOrientationSignal = time;
		}
	};
}