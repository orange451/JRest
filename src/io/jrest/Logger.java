package io.jrest;

public class Logger {
    
    private LogType logLevel = LogType.TRACE;
    
    public void log(LogType type, Object...objects) {
        if ( type.level < logLevel.level || logLevel == LogType.NONE )
            return;
        
        StringBuilder output = new StringBuilder();
        for(Object o : objects)
            output.append(o);
        
        if ( type == LogType.ERROR )
            System.err.println(output);
        else
            System.out.println(output);
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
    
    public void setMinLogType(LogType type) {
        this.logLevel = type;
    }
    
    public LogType getMinLogType() {
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