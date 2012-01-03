package com.barbermot.pilot;


import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Priority;

import de.mindpipe.android.logging.log4j.LogCatAppender;

import android.os.Environment;
import android.util.Log;

public class Log4jInitializer {
  static {
    //String propertyFileName = System.getProperty("org.apache.log4j.config.file", "log4j.properties");
    //final LogConfigurator logConfigurator = new LogConfigurator();
    //logConfigurator.setFileName(Environment.getExternalStorageDirectory() + "/myapp.log");
    
    PatternLayout terse   = new PatternLayout("%r %m%n");
    PatternLayout verbose = new PatternLayout("%r %-5p %c{2} - %m%n");
    
    ConsoleAppender console = new ConsoleAppender(verbose);
    console.setImmediateFlush(false);
    console.setThreshold(Priority.DEBUG);
    
    LogCatAppender logcat = new LogCatAppender(terse);
    logcat.setThreshold(Priority.DEBUG);
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.INFO);
    rootLogger.addAppender(logcat);
    
    DailyRollingFileAppender data;
    try {
      data = new DailyRollingFileAppender(terse, "/sdcard/data","'_'yyyy-MM-dd-HH-mm'.dat'");
      data.setBufferedIO(true);
      data.setImmediateFlush(false);

      String[] logNames = { "AutoControl", "GpsSignal", "ThrottleControl", "ThrottleGpsControl", "AileronControl", "AileronGpsControl",
          "RudderControl", "ElevatorControl", "ElevatorGpsControl", "Signal" };

      for(String name : logNames) {
        Logger logger = Logger.getLogger(name);
        logger.addAppender(data);
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    DailyRollingFileAppender state;
    try {
      state = new DailyRollingFileAppender(terse, "/sdcard/state", "'_'yyyy-MM-dd-HH-mm'.dat'");
      state.setBufferedIO(true);
      state.setImmediateFlush(false);
      Logger logger = Logger.getLogger("FlightState");
      logger.addAppender(state);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}