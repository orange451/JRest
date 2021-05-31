package io.jrest;

import java.io.PrintStream;

public class Logger {
    
    private LogType logLevel = LogType.TRACE;
    
    public void log(LogType type, Object...objects) {
        if ( type.level < logLevel.level || logLevel == LogType.NONE )
            return;
        
        PrintStream outputStream = (type == LogType.ERROR) ? System.err : System.out;
        
        for(Object o : objects) {        	
        	if ( o instanceof Exception ) {
        		((Exception)o).printStackTrace(outputStream);
        	} else {
        		outputStream.print(o);
        	}
        }
        
		outputStream.println();
    }
    
    public void trace(Object...objects) {
        this.log(LogType.TRACE, objects);
    }
    
    public void debug(Object...objects) {
        this.log(LogType.DEBUG, objects);
    }
    
    public void warn(Object...objects) {
        this.log(LogType.WARN, objects);
    }
    
    public void error(Object...objects) {
        this.log(LogType.ERROR, objects);
    }
    
    public void setLogType(LogType type) {
        this.logLevel = type;
    }
    
    public LogType getLogType() {
        return this.logLevel;
    }

    public static enum LogType {
        TRACE(1),
        DEBUG(2),
        WARN(3),
        ERROR(4),
        NONE(Integer.MAX_VALUE);
        
        private int level;
        
        LogType(int level) {
            this.level = level;
        }
    }
}