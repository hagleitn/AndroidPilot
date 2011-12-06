package com.barbermot.pilot.util;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class PhysicsFilter implements Filter {
    
    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getLoggerName().equals("Height")
                || record.getLoggerName().equals("Yaw")
                || record.getLoggerName().equals("Pitch")
                || record.getLoggerName().equals("Roll")) {
            return false;
        }
        return true;
    }
}
