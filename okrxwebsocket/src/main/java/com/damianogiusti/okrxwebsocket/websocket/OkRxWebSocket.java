package com.damianogiusti.okrxwebsocket.websocket;

import android.text.TextUtils;

import com.damianogiusti.okrxwebsocket.exceptions.InvalidURLException;
import com.damianogiusti.okrxwebsocket.exceptions.ParserNotImplementedException;
import com.damianogiusti.okrxwebsocket.log.OkRxWebSocketLogger;
import com.damianogiusti.okrxwebsocket.log.OkRxWebSocketLoggerProvider;
import com.damianogiusti.okrxwebsocket.parser.OkRxWebSocketParser;

import java.net.URI;
import java.nio.charset.Charset;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import rx.Completable;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by Damiano Giusti on 14/05/17.
 */
public final class OkRxWebSocket {

    private static final String TAG = "OkRxWebSocket";

    private WebSocket webSocket;
    private OkHttpClient okHttpClient;
    private String url;
    private Charset charset = Charset.forName("UTF-8");
    private OkRxWebSocketLogger logger = new OkRxWebSocketLogger(false); // disabled by default
    private OkRxWebSocketParser parser;

    private Scheduler socketScheduler = Schedulers.io();
    private PublishSubject<OkRxWebSocketActivity> socketActivitySubject = PublishSubject.create();
    private PublishSubject<OkRxWebSocketMessage> socketMessagesSubject = PublishSubject.create();
    private PublishSubject<OkRxWebSocketState> socketStateChangesSubject = PublishSubject.create();
    private OkRxWebSocketState socketState = OkRxWebSocketState.CLOSED;

    public static class Builder {

        private static final String TAG = "OkRxWebSocket.Builder";

        private OkRxWebSocket instance;

        public Builder(OkHttpClient okHttpClient) {
            instance = new OkRxWebSocket(okHttpClient);
        }

        public Builder url(String url) {
            instance.url = url;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            instance.socketScheduler = scheduler;
            return this;
        }

        public Builder byteCharsetEncoding(Charset charset) {
            instance.charset = charset;
            return this;
        }

        public Builder enableLogging() {
            instance.logger.setEnabled(true);
            return this;
        }

        public Builder loggerProvider(OkRxWebSocketLoggerProvider loggerProvider) {
            instance.logger.setLoggerProvider(loggerProvider);
            return this;
        }

        public Builder responseParser(OkRxWebSocketParser responseParser) {
            instance.parser = responseParser;
            return this;
        }

        public OkRxWebSocket build() {
            if (instance.url == null || TextUtils.isEmpty(instance.url.trim())) {
                instance.logger.warn(TAG, "Creating a OkRxWebSocket without url");
            }
            return instance;
        }
    }

    private OkRxWebSocket(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (webSocket == null) {
            this.url = url;
        } else {
            throw new IllegalStateException("Unable to change OkRxWebSocket URL when opened");
        }
    }

    public OkRxWebSocketState getSocketState() {
        return socketState;
    }

    private void setSocketState(OkRxWebSocketState socketState) {
        this.socketState = socketState;
        socketStateChangesSubject.onNext(socketState);
    }

    public Observable<OkRxWebSocketMessage> observeSocketMessages() {
        return socketMessagesSubject;
    }

    public Observable<OkRxWebSocketActivity> observeSocketActivity() {
        return socketActivitySubject;
    }

    public Observable<OkRxWebSocketState> observeSocketStateChanges() {
        return socketStateChangesSubject;
    }

    public Observable<OkRxWebSocketState> on(final OkRxWebSocketState socketState) {
        return socketStateChangesSubject.filter(new Func1<OkRxWebSocketState, Boolean>() {
            @Override
            public Boolean call(OkRxWebSocketState okRxWebSocketState) {
                return okRxWebSocketState == socketState;
            }
        });
    }

    /**
     * Opens a WebSocket to the configured URL.<br/>
     * Returns an {@link Observable} which emits events for monitoring the socket activity, in particular:
     * <ul>
     * <li>Socket opened</li>
     * <li>Socket closing</li>
     * <li>Socket closed</li>
     * </ul>
     *
     * @return {@link Observable}<{@link OkRxWebSocketActivity}> for monitoring socket activity.<br/>
     * The Observable works by default in the io {@link Scheduler}
     */
    public Observable<OkRxWebSocketActivity> open() {
        return Observable.create(new Action1<Emitter<OkRxWebSocketActivity>>() {
            @Override
            public void call(final Emitter<OkRxWebSocketActivity> emitter) {
                if (url == null) {
                    throw new IllegalStateException("Attempting to connect to a null URL");
                }

                try {
                    URI.create(url); // validate URL
                } catch (Exception e) {
                    throw new InvalidURLException(e.getMessage());
                }

                setSocketState(OkRxWebSocketState.OPENING);

                final Request request = new Request.Builder().url(url).build();

                webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        super.onOpen(webSocket, response);
                        emitter.onNext(OkRxWebSocketActivity.createForOpenedWebsocket(response));
                        setSocketState(OkRxWebSocketState.OPENED);
                        socketActivitySubject.onNext(OkRxWebSocketActivity.createForOpenedWebsocket(response));
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        super.onMessage(webSocket, text);
                        socketMessagesSubject.onNext(OkRxWebSocketMessage.createForNewMessage(charset, text));
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, ByteString bytes) {
                        super.onMessage(webSocket, bytes);
                        socketMessagesSubject.onNext(OkRxWebSocketMessage.createForNewMessage(charset, bytes));
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        super.onClosing(webSocket, code, reason);
                        socketActivitySubject.onNext(OkRxWebSocketActivity.createForClosingWebsocket(code, reason));
                        setSocketState(OkRxWebSocketState.CLOSING);
                    }

                    @Override
                    public void onClosed(WebSocket ws, int code, String reason) {
                        super.onClosed(ws, code, reason);
                        emitter.onCompleted();
                        socketActivitySubject.onNext(OkRxWebSocketActivity.createForClosedWebsocket(code, reason));
                        setSocketState(OkRxWebSocketState.CLOSED);
                        socketActivitySubject.onCompleted();
                        socketMessagesSubject.onCompleted();
                        webSocket = null;
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        super.onFailure(webSocket, t, response);
                        emitter.onError(t);
                        setSocketState(OkRxWebSocketState.ERROR);
                        socketActivitySubject.onError(t);
                    }
                });

                emitter.setCancellation(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        closeWebsocket();
                    }
                });
            }
        }, Emitter.BackpressureMode.NONE).compose(this.<OkRxWebSocketActivity>observableSchedulers());
    }

    /**
     * Closes the previously opened WebSocket
     *
     * @return {@link Completable} for operation completion
     */
    public Completable close() {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                if (webSocket != null) {
                    closeWebsocket();
                }
            }
        }).compose(completableSchedulers());
    }

    private void closeWebsocket() {
        webSocket.close(1000, "Socket closed by client");
    }

    /**
     * Writes the given message into the WebSocket.
     *
     * @param message message to write
     * @return {@link Completable} for operation completion
     */
    public Completable send(final String message) {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                logger.debug(TAG, "Sending message: " + message);
                webSocket.send(message);
            }
        }).compose(completableSchedulers());
    }

    /**
     * Writes the given byte array message into the WebSocket.
     *
     * @param message message to write
     * @return {@link Completable} for operation completion
     */
    public Completable send(final byte[] message) {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                ByteString byteString = ByteString.of(message);
                logger.debug(TAG, "Sending message: " + byteString.base64());
                webSocket.send(byteString);
            }
        }).compose(completableSchedulers());
    }

    /**
     * Writes the given message into the WebSocket, and returns an {@link Observable} which emits the
     * response filtered with the given response matcher function
     *
     * @param message         message to write
     * @param responseMatcher function applied to determine the response
     * @return {@link Observable} for response delivery
     */
    public Observable<OkRxWebSocketMessage> send(final String message, final Func1<OkRxWebSocketMessage, Boolean> responseMatcher) {
        return Observable.defer(new Func0<Observable<OkRxWebSocketMessage>>() {
            @Override
            public Observable<OkRxWebSocketMessage> call() {
                if (webSocket == null) {
                    return Observable.error(new UnsupportedOperationException("OkRxWebSocket is closed"));
                }
                logger.debug(TAG, "Sending message: " + message);
                webSocket.send(message);
                return socketMessagesSubject.filter(responseMatcher).filter(new Func1<OkRxWebSocketMessage, Boolean>() {
                    @Override
                    public Boolean call(OkRxWebSocketMessage okRxWebSocketMessage) {
                        return okRxWebSocketMessage.getMessageType() == OkRxWebSocketMessageType.NEW_MESSAGE;
                    }
                }).take(1);
            }
        }).compose(this.<OkRxWebSocketMessage>observableSchedulers());
    }

    /**
     * Writes the given byte array message into the WebSocket, and returns an {@link Observable} which emits the
     * response filtered with the given response matcher function
     *
     * @param message         message to write
     * @param responseMatcher function applied to determine the response
     * @return {@link Observable} for response delivery
     */
    public Observable<OkRxWebSocketMessage> send(final byte[] message, final Func1<OkRxWebSocketMessage, Boolean> responseMatcher) {
        return Observable.defer(new Func0<Observable<OkRxWebSocketMessage>>() {
            @Override
            public Observable<OkRxWebSocketMessage> call() {
                if (webSocket == null) {
                    return Observable.error(new UnsupportedOperationException("OkRxWebSocket is closed"));
                }
                ByteString byteString = ByteString.of(message);
                logger.debug(TAG, "Sending message: " + byteString.base64());
                webSocket.send(byteString);
                return socketMessagesSubject.filter(responseMatcher).filter(new Func1<OkRxWebSocketMessage, Boolean>() {
                    @Override
                    public Boolean call(OkRxWebSocketMessage okRxWebSocketMessage) {
                        return okRxWebSocketMessage.getMessageType() == OkRxWebSocketMessageType.NEW_MESSAGE;
                    }
                }).take(1);
            }
        }).compose(this.<OkRxWebSocketMessage>observableSchedulers());
    }

    /**
     * Writes the given byte array message into the WebSocket, and returns an {@link Observable} which emits the
     * response filtered with the given response matcher function
     *
     * @param message         message to write
     * @param responseType    type of the response in which be parsed
     * @param responseMatcher function applied to determine the response
     * @return {@link Observable} for response delivery
     */
    public <T> Observable<T> send(final String message, final Class<T> responseType, final Func1<OkRxWebSocketMessage, Boolean> responseMatcher) {
        return Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                if (parser == null) {
                    return Observable.error(new ParserNotImplementedException());
                }
                return send(message, responseMatcher).map(new Func1<OkRxWebSocketMessage, T>() {
                    @Override
                    public T call(OkRxWebSocketMessage okRxWebSocketMessage) {
                        return parser.parseResponse(responseType, okRxWebSocketMessage.getResponseString());
                    }
                });
            }
        }).compose(this.<T>observableSchedulers());
    }

    /**
     * Writes the given byte array message into the WebSocket, and returns an {@link Observable} which emits the
     * response filtered with the given response matcher function
     *
     * @param message         message to write
     * @param responseType    type of the response in which be parsed
     * @param responseMatcher function applied to determine the response
     * @return {@link Observable} for response delivery
     */
    public <T> Observable<T> send(final byte[] message, final Class<T> responseType, final Func1<OkRxWebSocketMessage, Boolean> responseMatcher) {
        return Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                if (parser == null) {
                    return Observable.error(new ParserNotImplementedException());
                }
                return send(message, responseMatcher).map(new Func1<OkRxWebSocketMessage, T>() {
                    @Override
                    public T call(OkRxWebSocketMessage okRxWebSocketMessage) {
                        return parser.parseResponse(responseType, okRxWebSocketMessage.getResponseByte());
                    }
                });
            }
        }).compose(this.<T>observableSchedulers());
    }

    private <T> Observable.Transformer<T, T> observableSchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> observable) {
                return observable.subscribeOn(socketScheduler).unsubscribeOn(socketScheduler);
            }
        };
    }

    private Completable.Transformer completableSchedulers() {
        return new Completable.Transformer() {
            @Override
            public Completable call(Completable completable) {
                return completable.subscribeOn(socketScheduler).unsubscribeOn(socketScheduler);
            }
        };
    }
}
