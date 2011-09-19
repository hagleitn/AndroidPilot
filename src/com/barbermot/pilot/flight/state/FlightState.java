package com.barbermot.pilot.flight.state;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.EnumMap;

import android.util.Log;

import com.barbermot.pilot.flight.FlightComputer;

public abstract class FlightState<T> {
    
    public static final String TAG = "FlightState";
    
    // Flight computer states
    public enum Type {
        GROUND, HOVER, STABILIZED_HOVER, WAYPOINT_HOLD, WAYPOINT_TRACK, LANDING, FAILED, EMERGENCY_LANDING, MANUAL_CONTROL
    };
    
    private Type                            type;
    
    protected EnumMap<Type, FlightState<?>> map;
    
    protected FlightComputer                computer;
    
    public FlightState() {
        map = new EnumMap<Type, FlightState<?>>(Type.class);
    }
    
    @SuppressWarnings("unchecked")
    public <D> void transition(StateEvent<D> e) throws ConnectionLostException {
        Log.d(TAG, "transition: " + type + " -> " + e.type);
        if (map.containsKey(e.type)) {
            exit();
            FlightState<D> state = (FlightState<D>) map.get(e.type);
            Log.d(TAG, "Setting state...");
            computer.setState(state);
            Log.d(TAG, "done setting");
            Log.d(TAG, "enter: " + e.arg);
            state.enter(e.arg);
        } else {
            Log.d(TAG, "No such transition.");
        }
        Log.d(TAG, "transition done.");
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
    
    public abstract void enter(T arg) throws ConnectionLostException;
    
    public abstract void exit() throws ConnectionLostException;
    
    public abstract void update() throws ConnectionLostException;
    
}
