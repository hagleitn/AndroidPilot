package com.barbermot.pilot.flight.state;

public class StateEvent<T> {
    
    FlightState.Type type;
    T                arg;
    
    public StateEvent(FlightState.Type type, T arg) {
        this.type = type;
        this.arg = arg;
    }
}
