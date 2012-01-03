package com.barbermot.pilot.flight;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import com.barbermot.pilot.Log4jInitializer;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

import android.hardware.SensorManager;
import android.location.LocationManager;

import com.barbermot.pilot.builder.BuildException;
import com.barbermot.pilot.builder.FlightBuilder;
import com.barbermot.pilot.signal.SignalManager;
import com.barbermot.pilot.simulator.IOIOSimulation;
import com.barbermot.pilot.simulator.PhysicsEngine;

public class FlightThread extends Thread {
    
    private Logger          logger;
    
    protected IOIO          ioio;
    private DigitalOutput   led;
    private boolean         abort     = false;
    private boolean         connected = true;
    
    private SensorManager   sensorManager;
    private LocationManager locationManager;
    
    private FlightComputer  computer;
    private List<Future<?>> handles;
    
    private SignalManager   signalManager;
    
    static {
      try {
        Class.forName("com.barbermot.pilot.Log4jInitializer");
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
    public FlightThread(SensorManager sensorManager,
            LocationManager locationManager) {
        this.sensorManager = sensorManager;
        this.locationManager = locationManager;
        
        logger = Logger.getLogger("FlightThread");
    }
    
    @Override
    public final void run() {
        while (true) {
            try {
                synchronized (this) {
                    if (abort) {
                        break;
                    }
                    if (FlightConfiguration.get().isSimulation()) {
                        ioio = new IOIOSimulation(new PhysicsEngine());
                        logger.info("Simulation!");
                    } else {
                        ioio = IOIOFactory.create();
                    }
                }
                ioio.waitForConnect();
                connected = true;
                logger.info("ioio is connected.");
                setup();
                while (!abort) {
                    loop();
                }
                ioio.disconnect();
                logger.info("ioio is disconnected.");
            } catch (ConnectionLostException e) {
                if (abort) {
                    break;
                }
            } catch (IncompatibilityException e) {
                logger.fatal("Incompatible IOIO firmware", e);
                // nothing to do - just wait until physical disconnection
                try {
                    ioio.waitForDisconnect();
                } catch (InterruptedException e1) {
                    ioio.disconnect();
                }
            } catch (Exception e) {
                logger.fatal("Unexpected exception caught", e);
                ioio.disconnect();
                break;
            } finally {
                try {
                    if (ioio != null) {
                        ioio.waitForDisconnect();
                    }
                } catch (InterruptedException e) {}
            }
        }
    }
    
    public synchronized final void abort() {
        logger.info("Abort requested.");
        
        try {
            if (led != null) {
                led.write(true);
            }
        } catch (ConnectionLostException e) {
            // don't care. Just trying to turn the led off
        }
        
        abort = true;
        
        if (computer != null) {
            logger.info("shutting down flight computer");
            computer.shutdown();
        }
        
        if (signalManager != null) {
            logger.info("shutting down signal array");
            signalManager.shutdown();
        }
        
        if (ioio != null) {
            logger.info("disconnecting ioio");
            ioio.disconnect();
        }
        
        if (connected) {
            interrupt();
        }
        
        try {
            if (ioio != null) {
                ioio.waitForDisconnect();
            }
            if (computer != null && computer.getExecutor() != null) {
                if (!computer.getExecutor().awaitTermination(60,
                        TimeUnit.SECONDS)) {
                    logger.warn("Timeout while shutting down.");
                }
            }
        } catch (InterruptedException e) {
            logger.info("InterruptedException caught");
        }
        logger.info("abort complete.");
    }
    
    private void setup() throws ConnectionLostException, InterruptedException {
        try {
            FlightBuilder builder = new FlightBuilder();
            computer = builder
                    .getComputer(ioio, sensorManager, locationManager);
            handles = builder.getFutures();
            signalManager = builder.getSignalManager();
            led = ioio.openDigitalOutput(0);
        } catch (BuildException e) {
            logger.fatal("Build Exception", e.getCause());
            throw new ConnectionLostException(e);
        }
        logger.info("Setup complete.");
    }
    
    private void loop() throws ConnectionLostException {
        logger.info("entering flight loop");
        led.write(false);
        
        for (Future<?> f : handles) {
            try {
                f.get();
            } catch (InterruptedException e) {
                logger.info("InterruptedException caught.", e);
                break;
            } catch (ExecutionException e) {
                Throwable ex = e;
                while (ex != null) {
                    logger.fatal("Exception caught.", ex);
                    ex = ex.getCause();
                }
            }
        }
        logger.info("exiting flight loop");
    }
}
