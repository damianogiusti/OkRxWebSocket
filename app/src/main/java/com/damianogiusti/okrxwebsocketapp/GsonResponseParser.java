package com.damianogiusti.okrxwebsocketapp;

import com.damianogiusti.okrxwebsocket.parser.OkRxWebSocketParser;
import com.google.gson.Gson;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public class GsonResponseParser implements OkRxWebSocketParser {

    private Gson gson = new Gson();

    @Override
    public <T> T parseResponse(Class<T> type, String response) {
        return gson.fromJson(response, type);
    }

    @Override
    public <T> T parseResponse(Class<T> type, byte[] response) {
        throw new RuntimeException("Not implemented");
    }
}
