package com.barbermot.pilot.simulator;

import ioio.lib.api.IOIO;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;

import com.barbermot.pilot.builder.BuildException;
import com.barbermot.pilot.builder.FlightBuilder;
import com.barbermot.pilot.flight.FlightComputer;
import com.barbermot.pilot.flight.FlightConfiguration;
import com.barbermot.pilot.flight.FlightConfiguration.ConnectionType;
import com.barbermot.pilot.signal.SignalManagerFactory;

public class Simulation {
    
    public static void main(String[] args) {
        String fileName = System.getProperty("com.apache.log4j.logging.config.file", "qc.properties");
        URL url = Loader.getResource(fileName);
        if(url != null) {
            PropertyConfigurator.configure(url);
        } else {
            PropertyConfigurator.configure(fileName);
        }
        FlightConfiguration.get().setConnectionType(ConnectionType.UART);
        PhysicsEngine engine = new PhysicsEngine();
        IOIO ioio = new IOIOSimulation(engine);
        FlightBuilder builder = new FlightBuilder();
        FlightComputer computer = null;
        SignalManagerFactory.setManager(new SignalManagerSimulation(engine,
                ioio));
        
        try {
            computer = builder.getComputer(ioio, null, null);
        } catch (BuildException e) {
            e.getCause().printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        for (Future<?> f : builder.getFutures()) {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                Throwable ex = e;
                while (ex != null) {
                    ex.printStackTrace();
                    ex = ex.getCause();
                }
            } finally {
                computer.shutdown();
            }
        }
        
        try {
            if (!computer.getExecutor().awaitTermination(60, TimeUnit.SECONDS)) {
                System.out.println("Shutdown failed.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
