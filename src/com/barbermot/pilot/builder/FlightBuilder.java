package com.barbermot.pilot.builder;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.hardware.SensorManager;
import android.location.LocationManager;

import com.barbermot.pilot.flight.AileronControlListener;
import com.barbermot.pilot.flight.ElevatorControlListener;
import com.barbermot.pilot.flight.FlightComputer;
import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.flight.FlightControlListener;
import com.barbermot.pilot.flight.RudderControlListener;
import com.barbermot.pilot.flight.ThrottleControlListener;
import com.barbermot.pilot.flight.state.EmergencyLandingState;
import com.barbermot.pilot.flight.state.FailedState;
import com.barbermot.pilot.flight.state.FlightState;
import com.barbermot.pilot.flight.state.GroundState;
import com.barbermot.pilot.flight.state.HoverState;
import com.barbermot.pilot.flight.state.LandingState;
import com.barbermot.pilot.flight.state.ManualControlState;
import com.barbermot.pilot.flight.state.StabilizedHoverState;
import com.barbermot.pilot.flight.state.WaypointHoldState;
import com.barbermot.pilot.flight.state.WaypointTrackState;
import com.barbermot.pilot.logger.FlightLogger;
import com.barbermot.pilot.parser.SerialController;
import com.barbermot.pilot.pid.AutoControl;
import com.barbermot.pilot.pid.RadianAutoControl;
import com.barbermot.pilot.quad.QuadCopter;
import com.barbermot.pilot.rc.RemoteControl;
import com.barbermot.pilot.signal.Signal;
import com.barbermot.pilot.signal.SignalListener;
import com.barbermot.pilot.signal.SignalManager;

/**
 * FlightBuilder wires up the system. It builds the flight computer instance and
 * hooks it up with the respective signals and auto controls. FlightBuilder also
 * starts the processing.
 * 
 * It is a one time instance. It can only be used to reliably create one
 * instance of the system and when a disconnect from ioio occurs the system has
 * to be shutdown and a new instance is needed to resume operation after the
 * connection comes back.
 */
public class FlightBuilder {
    
    private FlightComputer                            computer;
    private PrintStream                               printer;
    private SensorManager                             sensorManager;
    private LocationManager                           locationManager;
    private ScheduledExecutorService                  scheduler;
    private QuadCopter                                ufo;
    private FlightConfiguration                       config;
    private Map<FlightConfiguration.PinType, Integer> map;
    private IOIO                                      ioio;
    private Uart                                      uart;
    private AutoControl                               autoThrottle;
    private AutoControl                               autoAileron;
    private AutoControl                               autoElevator;
    private AutoControl                               autoRudder;
    private EnumMap<FlightState.Type, FlightState<?>> stateMap;
    private List<Future<?>>                           futures;
    
    /**
     * getComputer builds the FlightComputer. It hooks up the controls to the
     * respective signals and starts processing.
     * 
     * @param ioio
     *            A valid connection to the IOIO board.
     * @param manager
     *            An Android SensorManager to access the phone sensors
     * @return A one time instance of the FlightComputer
     * @throws BuildException
     */
    public FlightComputer getComputer(IOIO ioio, SensorManager sensorManager,
            LocationManager locationManager) throws BuildException {
        try {
            futures = new LinkedList<Future<?>>();
            this.ioio = ioio;
            this.sensorManager = sensorManager;
            this.locationManager = locationManager;
            this.computer = new FlightComputer();
            this.config = FlightConfiguration.get();
            this.map = config.getPinMap();
            this.stateMap = new EnumMap<FlightState.Type, FlightState<?>>(
                    FlightState.Type.class);
            
            buildScheduler();
            buildUart();
            buildPrinter();
            buildQuadCopter();
            buildRemoteControl();
            buildControls();
            buildFlightStates();
            buildTransitions();
            buildSignalArray();
            buildLogger();
            buildSerialController();
            
            futures.add(scheduler.scheduleWithFixedDelay(computer, 0,
                    config.getMinTimeFlightComputer(), TimeUnit.MILLISECONDS));
        } catch (ConnectionLostException e) {
            e.printStackTrace();
            throw new BuildException();
        }
        return computer;
    }
    
    /**
     * getFutures returns handles to the periodic and continuous tasks started
     * by the FlightBuilder. These futures can be used to orderly shutdown the
     * system.
     * 
     * @return A list of futures for all the tasks started by the builder.
     */
    public List<Future<?>> getFutures() {
        return futures;
    }
    
    private void buildScheduler() {
        scheduler = new ScheduledThreadPoolExecutor(config.getNumberThreads());
        computer.setExecutor(scheduler);
    }
    
    private void buildUart() throws ConnectionLostException {
        uart = ioio.openUart(
                config.getPinMap().get(FlightConfiguration.PinType.RX), config
                        .getPinMap().get(FlightConfiguration.PinType.TX), 9600,
                Uart.Parity.NONE, Uart.StopBits.ONE);
    }
    
    private void buildLogger() {
        FlightLogger logger = new FlightLogger(printer);
        logger.setComputer(computer);
        futures.add(scheduler.scheduleWithFixedDelay(logger, 0,
                config.getMinTimeStatusMessage(), TimeUnit.MILLISECONDS));
    }
    
    private void buildSerialController() throws ConnectionLostException {
        InputStream in = uart.getInputStream();
        SerialController controller = new SerialController(computer, ';', in,
                printer);
        futures.add(scheduler.submit(controller));
    }
    
    private void buildControls() {
        
        FlightControlListener listener;
        
        listener = new ThrottleControlListener();
        listener.setComputer(computer);
        autoThrottle = new AutoControl(listener, "ThrottleControl", false);
        
        listener = new AileronControlListener();
        listener.setComputer(computer);
        autoAileron = new RadianAutoControl(listener, "AileronControl", false);
        
        listener = new RudderControlListener();
        listener.setComputer(computer);
        autoRudder = new RadianAutoControl(listener, "RudderControl", false);
        
        listener = new ElevatorControlListener();
        listener.setComputer(computer);
        autoElevator = new RadianAutoControl(listener, "ElevatorControl", false);
    }
    
    private void buildQuadCopter() throws ConnectionLostException {
        ufo = new QuadCopter(ioio,
                map.get(FlightConfiguration.PinType.AILERON_OUT),
                map.get(FlightConfiguration.PinType.RUDDER_OUT),
                map.get(FlightConfiguration.PinType.THROTTLE_OUT),
                map.get(FlightConfiguration.PinType.ELEVATOR_OUT),
                map.get(FlightConfiguration.PinType.GAIN_OUT));
        computer.setUfo(ufo);
    }
    
    private void buildRemoteControl() throws ConnectionLostException {
        RemoteControl rc = new RemoteControl(ioio, ufo,
                map.get(FlightConfiguration.PinType.AILERON_IN),
                map.get(FlightConfiguration.PinType.RUDDER_IN),
                map.get(FlightConfiguration.PinType.THROTTLE_IN),
                map.get(FlightConfiguration.PinType.ELEVATOR_IN),
                map.get(FlightConfiguration.PinType.THROTTLE_MONITOR),
                map.get(FlightConfiguration.PinType.GAIN_IN));
        
        rc.setControlMask((char) ~RemoteControl.THROTTLE_MASK);
        computer.setRc(rc);
        
        futures.add(scheduler.scheduleWithFixedDelay(rc, 0,
                config.getMinTimeRcEngagement(), TimeUnit.MILLISECONDS));
    }
    
    private void buildPrinter() throws ConnectionLostException {
        printer = new PrintStream(uart.getOutputStream());
    }
    
    private void buildFlightStates() throws ConnectionLostException {
        FlightState.Type type = FlightState.Type.EMERGENCY_LANDING;
        FlightState<?> state = new EmergencyLandingState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        
        type = FlightState.Type.FAILED;
        state = new FailedState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        
        type = FlightState.Type.GROUND;
        state = new GroundState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        computer.setState(state);
        
        type = FlightState.Type.HOVER;
        state = new HoverState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        ((HoverState) state).setAutoThrottle(autoThrottle);
        
        type = FlightState.Type.LANDING;
        state = new LandingState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        ((LandingState) state).setAutoThrottle(autoThrottle);
        
        type = FlightState.Type.MANUAL_CONTROL;
        state = new ManualControlState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        
        type = FlightState.Type.STABILIZED_HOVER;
        state = new StabilizedHoverState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        ((StabilizedHoverState) state).setAutoThrottle(autoThrottle);
        ((StabilizedHoverState) state).setAutoElevator(autoElevator);
        ((StabilizedHoverState) state).setAutoAileron(autoAileron);
        ((StabilizedHoverState) state).setAutoRudder(autoRudder);
        
        type = FlightState.Type.WAYPOINT_HOLD;
        state = new WaypointHoldState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
        
        type = FlightState.Type.WAYPOINT_TRACK;
        state = new WaypointTrackState();
        state.setType(type);
        state.setComputer(computer);
        stateMap.put(type, state);
    }
    
    private void buildTransitions() throws ConnectionLostException {
        FlightState<?> abort = stateMap.get(FlightState.Type.FAILED);
        for (FlightState<?> state : stateMap.values()) {
            if (state != abort) {
                state.addTransition(abort);
            }
        }
        
        FlightState<?> manual = stateMap.get(FlightState.Type.MANUAL_CONTROL);
        for (FlightState<?> state : stateMap.values()) {
            if (state != manual) {
                state.addTransition(manual);
            }
        }
        
        // Ground
        stateMap.get(FlightState.Type.GROUND).addTransition(
                stateMap.get(FlightState.Type.HOVER));
        
        // Hover
        stateMap.get(FlightState.Type.HOVER).addTransition(
                stateMap.get(FlightState.Type.HOVER));
        stateMap.get(FlightState.Type.HOVER).addTransition(
                stateMap.get(FlightState.Type.STABILIZED_HOVER));
        stateMap.get(FlightState.Type.HOVER).addTransition(
                stateMap.get(FlightState.Type.EMERGENCY_LANDING));
        stateMap.get(FlightState.Type.HOVER).addTransition(
                stateMap.get(FlightState.Type.LANDING));
        
        // Emergency Landing
        stateMap.get(FlightState.Type.EMERGENCY_LANDING).addTransition(
                stateMap.get(FlightState.Type.LANDING));
        
        // Landing
        stateMap.get(FlightState.Type.LANDING).addTransition(
                stateMap.get(FlightState.Type.HOVER));
        stateMap.get(FlightState.Type.LANDING).addTransition(
                stateMap.get(FlightState.Type.EMERGENCY_LANDING));
        stateMap.get(FlightState.Type.LANDING).addTransition(
                stateMap.get(FlightState.Type.GROUND));
        
        // Stabilized Hover
        stateMap.get(FlightState.Type.STABILIZED_HOVER).addTransition(
                stateMap.get(FlightState.Type.HOVER));
        stateMap.get(FlightState.Type.STABILIZED_HOVER).addTransition(
                stateMap.get(FlightState.Type.STABILIZED_HOVER));
        stateMap.get(FlightState.Type.STABILIZED_HOVER).addTransition(
                stateMap.get(FlightState.Type.LANDING));
        stateMap.get(FlightState.Type.STABILIZED_HOVER).addTransition(
                stateMap.get(FlightState.Type.EMERGENCY_LANDING));
        
        // Manual Control
        stateMap.get(FlightState.Type.MANUAL_CONTROL).addTransition(
                stateMap.get(FlightState.Type.HOVER));
    }
    
    private void buildSignalArray() throws ConnectionLostException {
        SignalManager signalManager = SignalManager.getManager(ioio,
                sensorManager, locationManager, scheduler);
        Signal signal = signalManager.getUltraSoundSignal(
                config.getMinTimeUltraSound(),
                map.get(FlightConfiguration.PinType.ULTRA_SOUND));
        
        signal.registerListener(new SignalListener() {
            
            public void update(float x, long time) {
                computer.setHeight(x);
                computer.setLastTimeHeightSignal(time);
            }
        });
        signal.registerListener(autoThrottle);
        
        signal = signalManager.getRollSignal(config.getMinTimeOrientation());
        signal.registerListener(new SignalListener() {
            
            public void update(float x, long time) {
                computer.setLateralDisplacement(x);
                computer.setLastTimeOrientationSignal(time);
            }
        });
        signal.registerListener(autoAileron);
        
        signal = signalManager.getPitchSignal(config.getMinTimeOrientation());
        signal.registerListener(new SignalListener() {
            
            public void update(float x, long time) {
                computer.setLongitudinalDisplacement(x);
                computer.setLastTimeOrientationSignal(time);
            }
        });
        signal.registerListener(autoElevator);
        
        signal = signalManager.getYawSignal(config.getMinTimeOrientation());
        signal.registerListener(new SignalListener() {
            
            public void update(float x, long time) {
                computer.setHeading(x);
                computer.setLastTimeOrientationSignal(time);
            }
        });
        signal.registerListener(autoRudder);
        
        signal = signalManager.getGpsAltitudeSignal(config.getMinTimeGps());
        signal.registerListener(new SignalListener() {
            
            public void update(float x, long time) {
                computer.setGpsHeight(x);
                computer.setLastTimeGpsHeight(time);
            }
        });
        futures.addAll(signalManager.getFutures());
    }
}
