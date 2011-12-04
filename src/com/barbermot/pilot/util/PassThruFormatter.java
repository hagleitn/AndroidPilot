package com.barbermot.pilot.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class PassThruFormatter extends Formatter {
    
    @Override
    public String format(LogRecord r) {
        return r.getMessage();
    }
    
}
