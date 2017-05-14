package com.damianogiusti.okrxwebsocket.parser;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public interface OkRxWebSocketParser {

    <T> T parseResponse(Class<T> type, String response);

    <T> T parseResponse(Class<T> type, byte[] response);
}
