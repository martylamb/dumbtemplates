package com.martiansoftware.dumbtemplates;

/**
 * DumbLogger?  Or the <b>dumbest</b> logger?
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public interface DumbLogger {    
    default void log(Exception e) { e.printStackTrace(); }
    default void log(String msg) { System.err.println(msg); }
}
