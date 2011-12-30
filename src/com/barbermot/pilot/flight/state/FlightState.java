package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;
import java.util.logging.Logger;

import com.barbermot.pilot.flight.FlightComputer;

public abstract class FlightState<T> {
    
    // Flight computer states
    public enum Type {
        GROUND, HOVER, STABILIZED_HOVER, WAYPOINT_HOLD, WAYPOINT_TRACK, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL, CALIBRATION
    };
    
    protected static final Logger           logger = Logger.getLogger("FlightState");
    private Type                            type;
    protected EnumMap<Type, FlightState<?>> map;
    protected FlightComputer                computer;
    
    public FlightState() {
        map = new EnumMap<Type, FlightState<?>>(Type.class);
    }
    
    @SuppressWarnings("unchecked")
    public <D> void transition(Type nextType, D arg)
            throws ConnectionLostException {
        logger.info("Transition: " + type + " -> " + nextType);
        if (map.containsKey(nextType)) {
            FlightState<D> nextState = (FlightState<D>) map.get(nextType);
            if (!nextState.guard(arg)) {
                logger.warning("Condition for entry not met.");
            } else {
                exit();
                computer.setState(nextState);
                nextState.enter(arg);
            }
        } else {
            logger.warning("Illegal state transition requested.");
        }
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public FlightComputer getComputer() {
        return computer;
    }
    
    public void setComputer(FlightComputer computer) {
        this.computer = computer;
    }
    
    public Type getType() {
        return type;
    }
    
    public void addTransition(FlightState<?> s) {
        map.put(s.getType(), s);
    }
    
    public abstract boolean guard(T arg) throws ConnectionLostException;
    
    public abstract void enter(T arg) throws ConnectionLostException;
    
    public abstract void exit() throws ConnectionLostException;
    
    public abstract void update() throws ConnectionLostException;
    
}
