package com.huobi.api.client.impl;

import com.huobi.api.client.HuobiApiWebSocketClient;
import com.huobi.api.client.constant.HuobiConsts;
import com.huobi.api.client.domain.Candle;
import com.huobi.api.client.domain.enums.MergeLevel;
import com.huobi.api.client.domain.enums.Resolution;
import com.huobi.api.client.domain.event.DepthEvent;
import com.huobi.api.client.domain.event.DepthEventResp;
import com.huobi.api.client.domain.event.KlineEvent;
import com.huobi.api.client.domain.event.KlineEventResp;
import com.huobi.api.client.domain.resp.ApiCallback;
import com.huobi.api.client.domain.resp.RespBody;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.io.Closeable;
import java.io.IOException;

/**
 * created by jacky. 2018/7/24 4:00 PM
 */
public class HuobiApiWebSocketClientImpl implements HuobiApiWebSocketClient {

    private OkHttpClient client;


    public HuobiApiWebSocketClientImpl() {
        Dispatcher d = new Dispatcher();
        d.setMaxRequestsPerHost(100);
        this.client = new OkHttpClient.Builder().dispatcher(d).build();
    }

    @Override
    public Closeable onKlineTick(String symbol, Resolution period, ApiCallback<KlineEventResp> callback) {
        KlineEvent event = new KlineEvent();
        event.setSymbol(symbol);
        event.setPeriod(period);
        return createNewWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<>(callback, KlineEventResp.class));
    }


    public Closeable onDepthTick(String symbol, MergeLevel level, ApiCallback<DepthEventResp> callback) {
        DepthEvent event = new DepthEvent();
        event.setSymbol(symbol);
        event.setLevel(level);
        return createNewWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<>(callback, DepthEventResp.class));
    }


    private Closeable createNewWebSocket(String sub, HuobiApiWebSocketListener<?> listener) {
        String streamingUrl = HuobiConsts.WS_API_BASE_URL_PRO;
        Request request = new Request.Builder().url(streamingUrl).build();
        final WebSocket webSocket = client.newWebSocket(request, listener);
        webSocket.send(sub);
        return () -> {
            final int code = 1000;
            listener.onClosing(webSocket, code, null);
            webSocket.close(code, null);
            listener.onClosed(webSocket, code, null);
        };
    }

}
