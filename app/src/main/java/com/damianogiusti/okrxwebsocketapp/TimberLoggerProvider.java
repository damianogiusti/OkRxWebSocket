package com.damianogiusti.okrxwebsocketapp;

import com.damianogiusti.okrxwebsocket.log.OkRxWebSocketLoggerProvider;

import timber.log.Timber;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public class TimberLoggerProvider implements OkRxWebSocketLoggerProvider {

    @Override
    public void debug(String tag, String message) {
        Timber.tag(tag).d(message);
    }

    @Override
    public void info(String tag, String message) {
        Timber.tag(tag).i(message);
    }

    @Override
    public void warn(String tag, String message) {
        Timber.tag(tag).w(message);
    }

    @Override
    public void error(String tag, String message, Throwable throwable) {
        Timber.tag(tag).e(throwable, message);
    }
}
