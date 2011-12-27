package com.barbermot.pilot.flight;

public class Waypoint implements Cloneable {
    
    public float altitude;
    public float latitude;
    public float longitude;
    
    public Waypoint(float alt, float lat, float lon) {
        altitude = alt;
        latitude = lat;
        longitude = lon;
    }
    
    @Override
    public Waypoint clone() throws CloneNotSupportedException {
        return (Waypoint) super.clone();
    }
}
