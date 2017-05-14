package com.damianogiusti.okrxwebsocket.log;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
interface Logger {

    void debug(String tag, String message);

    void info(String tag, String message);

    void warn(String tag, String message);

    void error(String tag, String message, Throwable throwable);
}
