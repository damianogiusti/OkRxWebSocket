package com.damianogiusti.okrxwebsocket.websocket;

import java.nio.charset.Charset;

import okio.ByteString;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public class OkRxWebSocketMessage {

    static OkRxWebSocketMessage createForNewMessage(Charset charset, String message) {
        OkRxWebSocketMessage websocketMessage = new OkRxWebSocketMessage();
        websocketMessage.setResponseString(message);
        websocketMessage.setResponseByte(message.getBytes(charset));
        websocketMessage.setMessageType(OkRxWebSocketMessageType.NEW_MESSAGE);
        return websocketMessage;
    }

    static OkRxWebSocketMessage createForNewMessage(Charset charset, ByteString message) {
        OkRxWebSocketMessage websocketMessage = new OkRxWebSocketMessage();
        websocketMessage.setResponseString(message.string(charset));
        websocketMessage.setResponseByte(message.toByteArray());
        websocketMessage.setMessageType(OkRxWebSocketMessageType.NEW_MESSAGE);
        return websocketMessage;
    }

    protected OkRxWebSocketMessageType messageType;
    protected String responseString;
    protected byte[] responseByte;
    protected int code;

    protected OkRxWebSocketMessage() {}

    public OkRxWebSocketMessageType getMessageType() {
        return messageType;
    }

    void setMessageType(OkRxWebSocketMessageType messageType) {
        this.messageType = messageType;
    }

    public String getResponseString() {
        return responseString;
    }

    void setResponseString(String responseString) {
        this.responseString = responseString;
    }

    public byte[] getResponseByte() {
        return responseByte;
    }

    void setResponseByte(byte[] responseByte) {
        this.responseByte = responseByte;
    }

    public int getCode() {
        return code;
    }

    void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return String.format("[%s] - %s", getMessageType(), getResponseString());
    }
}
