package com.damianogiusti.okrxwebsocket.websocket;

import okhttp3.Response;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public final class OkRxWebSocketActivity extends OkRxWebSocketMessage {

    static OkRxWebSocketActivity createForOpenedWebsocket(Response response) {
        OkRxWebSocketActivity websocketActivity = new OkRxWebSocketActivity();
        websocketActivity.setResponse(response);
        websocketActivity.setResponseString(response.message());
        websocketActivity.setCode(response.code());
        websocketActivity.setMessageType(OkRxWebSocketMessageType.OPENED);
        return websocketActivity;
    }

    static OkRxWebSocketActivity createForClosingWebsocket(int code, String reason) {
        OkRxWebSocketActivity websocketActivity = new OkRxWebSocketActivity();
        websocketActivity.setResponse(null);
        websocketActivity.setResponseString(reason);
        websocketActivity.setCode(code);
        websocketActivity.setMessageType(OkRxWebSocketMessageType.CLOSING);
        return websocketActivity;
    }

    static OkRxWebSocketActivity createForClosedWebsocket(int code, String reason) {
        OkRxWebSocketActivity websocketActivity = new OkRxWebSocketActivity();
        websocketActivity.setResponse(null);
        websocketActivity.setResponseString(reason);
        websocketActivity.setCode(code);
        websocketActivity.setMessageType(OkRxWebSocketMessageType.CLOSED);
        return websocketActivity;
    }

    private Response response;

    protected OkRxWebSocketActivity() {}

    public Response getResponse() {
        return response;
    }

    void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public String toString() {
        if (response != null) {
            return response.toString();
        }
        return super.toString();
    }
}
