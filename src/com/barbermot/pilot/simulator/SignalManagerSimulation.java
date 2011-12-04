package com.barbermot.pilot.simulator;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.barbermot.pilot.signal.Signal;
import com.barbermot.pilot.signal.SignalManager;
import com.barbermot.pilot.signal.UltrasoundSignal;

public class SignalManagerSimulation implements SignalManager {
    
    private PhysicsEngine            engine;
    List<Future<?>>                  futures;
    private ScheduledExecutorService scheduler;
    private IOIO                     ioio;
    
    public SignalManagerSimulation(PhysicsEngine engine, IOIO ioio) {
        this.engine = engine;
        futures = new LinkedList<Future<?>>();
        this.ioio = ioio;
    }
    
    @Override
    public void shutdown() {}
    
    @Override
    public List<Future<?>> getFutures() {
        return futures;
    }
    
    @Override
    public Signal getYawSignal(int interval) throws ConnectionLostException {
        YawSignalSimulation yaw = new YawSignalSimulation(engine);
        futures.add(scheduler.scheduleWithFixedDelay(yaw, 0, 100,
                TimeUnit.MILLISECONDS));
        return yaw;
    }
    
    @Override
    public Signal getPitchSignal(int interval) {
        PitchSignalSimulation pitch = new PitchSignalSimulation(engine);
        futures.add(scheduler.scheduleWithFixedDelay(pitch, 0, 100,
                TimeUnit.MILLISECONDS));
        return pitch;
    }
    
    @Override
    public Signal getRollSignal(int interval) {
        RollSignalSimulation roll = new RollSignalSimulation(engine);
        futures.add(scheduler.scheduleWithFixedDelay(roll, 0, 100,
                TimeUnit.MILLISECONDS));
        return roll;
    }
    
    @Override
    public Signal getUltraSoundSignal(int interval, int pin)
            throws ConnectionLostException {
        UltrasoundSignal signal = new UltrasoundSignal(ioio, pin);
        futures.add(scheduler.scheduleWithFixedDelay(signal, 0, interval,
                TimeUnit.MILLISECONDS));
        return signal;
    }
    
    @Override
    public Signal getGpsAltitudeSignal(int interval) {
        GpsAltitudeSignalSimulation gps = new GpsAltitudeSignalSimulation(
                engine);
        futures.add(scheduler.scheduleWithFixedDelay(gps, 0, 250,
                TimeUnit.MILLISECONDS));
        return gps;
    }
    
    @Override
    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
}
