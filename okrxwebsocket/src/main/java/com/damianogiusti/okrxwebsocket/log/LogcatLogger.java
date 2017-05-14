package com.damianogiusti.okrxwebsocket.log;

import android.util.Log;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
final class LogcatLogger implements OkRxWebSocketLoggerProvider {

    @Override
    public void debug(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void info(String tag, String message) {
        Log.i(tag, message);
    }

    @Override
    public void warn(String tag, String message) {
        Log.w(tag, message);
    }

    @Override
    public void error(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
}
