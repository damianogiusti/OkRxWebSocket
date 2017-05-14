package com.damianogiusti.okrxwebsocketapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.damianogiusti.okrxwebsocket.websocket.OkRxWebSocket;
import com.damianogiusti.okrxwebsocket.websocket.OkRxWebSocketActivity;
import com.damianogiusti.okrxwebsocket.websocket.OkRxWebSocketMessage;
import com.damianogiusti.okrxwebsocket.websocket.OkRxWebSocketMessageType;
import com.damianogiusti.okrxwebsocket.websocket.OkRxWebSocketState;
import com.google.gson.JsonObject;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private OkRxWebSocket websocket;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private CompositeSubscription socketEventsSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Timber.plant(new Timber.DebugTree());

        // Create a new websocket
        websocket = new OkRxWebSocket.Builder(new OkHttpClient())
                .url("wss://sandbox.kaazing.net/echo")
                .byteCharsetEncoding(Charset.forName("UTF-8"))
                .enableLogging()
                .loggerProvider(new TimberLoggerProvider())
                .responseParser(new GsonResponseParser())
                .build();

        // observe incoming messages
        Subscription socketMessagesSubscription = websocket.observeSocketMessages()
                .map(OkRxWebSocketMessage::getResponseString)
                .map(string -> "observeSocketMessages -> " + string)
                .subscribe(Timber::i, Timber::e);
        socketEventsSubscription.add(socketMessagesSubscription);

        // observe socket activity
        Subscription socketActivitySubscription = websocket.observeSocketActivity()
                .map(OkRxWebSocketActivity::toString)
                .map(string -> "observeSocketActivity -> " + string)
                .subscribe(Timber::i, Timber::e);
        socketEventsSubscription.add(socketActivitySubscription);

        // open connection to the websocket
        compositeSubscription.add(websocket.open()
                .filter(activity -> activity.getMessageType() == OkRxWebSocketMessageType.OPENED)
                .subscribe(activity -> onSocketConnected(), Timber::e));

        // do something on socket closed
        compositeSubscription.add(websocket.on(OkRxWebSocketState.CLOSED)
                .map(state -> "Socket closed")
                .subscribe(Timber::i, Timber::e));
    }

    private void onSocketConnected() {
        // when connected, send 10 messages (one every second) then close the socket
        Subscription subscription = Observable.timer(1, TimeUnit.SECONDS)
                .repeat(10)
                .map(number -> UUID.randomUUID().toString())
                .flatMap(this::sendMessage)
                .map(json -> "Received: " + json)
                .doOnCompleted(() -> websocket.close().subscribe(() -> {}, Timber::e))
                .subscribe(Timber::d, Timber::e);
        compositeSubscription.add(subscription);
    }

    private Observable<JsonObject> sendMessage(String uuid) {
        return Observable.defer(() -> {
            JsonObject json = new JsonObject();
            json.addProperty("msgId", uuid);
            json.addProperty("msgType", "greets");
            json.addProperty("msg", "Hello, World! I'm an Rx WebSocket.");
            json.addProperty("timestamp", System.currentTimeMillis());

            // get the response to this message, parsed as a JsonObject (it's an echo server)
            return websocket.send(json.toString(), JsonObject.class, message -> message.getResponseString().contains(uuid));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.unsubscribe();
        socketEventsSubscription.unsubscribe();
    }
}
