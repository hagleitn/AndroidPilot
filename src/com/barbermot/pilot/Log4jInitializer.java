package com.barbermot.pilot;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import de.mindpipe.android.logging.log4j.LogCatAppender;

public class Log4jInitializer {
    
    static {
        
        PatternLayout terse = new PatternLayout("%r %m%n");
        PatternLayout verbose = new PatternLayout("%r %-5p %c{2} - %m%n");
        
        ConsoleAppender console = new ConsoleAppender(verbose);
        console.setImmediateFlush(false);
        console.setThreshold(Level.DEBUG);
        
        LogCatAppender logcat = new LogCatAppender(terse);
        logcat.setThreshold(Level.DEBUG);
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(logcat);
        
        DailyRollingFileAppender data;
        try {
            data = new DailyRollingFileAppender(terse,
                    "/sdcard/barbermot/data.txt", "'_'yyyy-MM-dd-HH'.txt'");
            data.setBufferedIO(true);
            data.setBufferSize(4096);
            data.setImmediateFlush(false);
            data.setThreshold(Level.DEBUG);
            
            String[] logNames = { "AutoControl", "GpsSignal",
            "ThrottleControl", "ThrottleGpsControl", "AileronControl",
            "AileronGpsControl", "RudderControl", "ElevatorControl",
            "ElevatorGpsControl", "Signal" };
            
            for (String name : logNames) {
                Logger logger = Logger.getLogger(name);
                logger.setLevel(Level.DEBUG);
                logger.addAppender(data);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        DailyRollingFileAppender state;
        try {
            state = new DailyRollingFileAppender(terse,
                    "/sdcard/barbermot/state.txt", "'_'yyyy-MM-dd-HH'.txt'");
            state.setBufferedIO(true);
            state.setBufferSize(512);
            state.setImmediateFlush(true);
            state.setThreshold(Level.DEBUG);
            Logger logger = Logger.getLogger("FlightState");
            logger.addAppender(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}