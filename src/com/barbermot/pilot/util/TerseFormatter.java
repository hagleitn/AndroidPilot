package com.barbermot.pilot.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TerseFormatter extends Formatter {
    
    @Override
    public String format(LogRecord r) {
        StringBuilder builder = new StringBuilder();
        builder.append(r.getLoggerName());
        builder.append(":\t");
        builder.append(r.getMillis());
        builder.append("\t");
        builder.append(r.getMessage());
        builder.append(System.getProperty("line.separator"));
        return builder.toString();
    }
    
}
