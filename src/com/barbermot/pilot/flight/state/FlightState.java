package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;

import org.apache.log4j.Logger;

import com.barbermot.pilot.flight.FlightComputer;

public abstract class FlightState<T> {
    
    // Flight computer states
    public enum Type {
        GROUND, HOVER, STABILIZED_HOVER, WAYPOINT_HOLD, WAYPOINT_TRACK, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL, CALIBRATION
    };
    
    protected static Logger                 logger = Logger.getLogger("FlightState");
    private Type                            type;
    protected EnumMap<Type, FlightState<?>> map;
    protected FlightComputer                computer;
    
    public FlightState() {
        map = new EnumMap<Type, FlightState<?>>(Type.class);
    }
    
    @SuppressWarnings("unchecked")
    public <D> void transition(Type nextType, D arg)
            throws ConnectionLostException {
        String transitionTag = "from:" + type + "\tto:" + nextType;
        logger.info("transiting: " + transitionTag);
        if (map.containsKey(nextType)) {
            FlightState<D> nextState = (FlightState<D>) map.get(nextType);
            if (!nextState.guard(arg)) {
                logger.warn("entry condition failed for: " + transitionTag);
            } else {
                exit();
                computer.setState(nextState);
                nextState.enter(arg);
                logger.info("transited: " + transitionTag);
            }
        } else {
            logger.warn("Illegal transition: " + transitionTag);
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
        logger.debug("adding transition: " + this.type + " -> " + s.getType());
        map.put(s.getType(), s);
    }
    
    public abstract boolean guard(T arg) throws ConnectionLostException;
    
    public abstract void enter(T arg) throws ConnectionLostException;
    
    public abstract void exit() throws ConnectionLostException;
    
    public abstract void update() throws ConnectionLostException;
    
}
