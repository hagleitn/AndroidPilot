package com.barbermot.pilot.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.util.Log;

public class AndroidHandler extends Handler {
    
    @Override
    public void close() {
        // no op
    }
    
    @Override
    public void flush() {
        // no op
    }
    
    @Override
    public void publish(LogRecord arg0) {
        String tag = arg0.getLoggerName();
        String msg = arg0.getMessage();
        Level level = arg0.getLevel();
        
        if (level == Level.INFO) {
            Log.i(tag, msg);
        } else if (level == Level.SEVERE) {
            Log.e(tag, msg);
        } else if (level == Level.WARNING) {
            Log.w(tag, msg);
        } else {
            Log.d(tag, msg);
        }
    }
}
