package com.huobi.api.client.domain.resp;

import okhttp3.WebSocket;

import java.io.Closeable;

/**
 * created by jacky. 2018/7/24 7:44 PM
 */
public interface ApiCallback<T> {

    /**
     * Called whenever a response comes back
     *
     * @param webSocket
     * @param response
     */
    void onResponse(WebSocket webSocket, T response);

    /**
     * Called whenever a error occur
     *
     * @param throwable
     */
    default void onFailure(Throwable throwable) {
    }

    /**
     * call when expired
     *
     * @param webSocket
     */
    default void onExpired(WebSocket webSocket, int code, String reason) {
    }

    /**
     * call when connect
     *
     * @param closeable
     */
    default void onConnect(WebSocket ws, Closeable closeable) {
    }


    /**
     * call when server ping
     * @param ws
     */
    default  void onPing(WebSocket ws,Closeable closeable){
    }
}
