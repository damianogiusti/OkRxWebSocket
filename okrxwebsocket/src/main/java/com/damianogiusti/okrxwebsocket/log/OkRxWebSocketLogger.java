package com.damianogiusti.okrxwebsocket.log;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public class OkRxWebSocketLogger implements Logger {

    private boolean isEnabled;
    private OkRxWebSocketLoggerProvider loggerProvider;

    public OkRxWebSocketLogger(boolean isEnabled) {
        this.isEnabled = isEnabled;
        this.loggerProvider = new LogcatLogger(); // default
    }

    public void setLoggerProvider(OkRxWebSocketLoggerProvider loggerProvider) {
        this.loggerProvider = loggerProvider;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public void debug(String tag, String message) {
        if (isEnabled) {
            loggerProvider.debug(tag, message);
        }
    }

    @Override
    public void info(String tag, String message) {
        if (isEnabled) {
            loggerProvider.info(tag, message);
        }
    }

    @Override
    public void warn(String tag, String message) {
        if (isEnabled) {
            loggerProvider.warn(tag, message);
        }
    }

    @Override
    public void error(String tag, String message, Throwable throwable) {
        if (isEnabled) {
            loggerProvider.error(tag, message, throwable);
        }
    }
}
