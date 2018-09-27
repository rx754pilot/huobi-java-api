package com.huobi.api.client.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huobi.api.client.HuobiApiWebSocketClient;
import com.huobi.api.client.constant.HuobiConsts;
import com.huobi.api.client.domain.enums.MergeLevel;
import com.huobi.api.client.domain.enums.Resolution;
import com.huobi.api.client.domain.event.*;
import com.huobi.api.client.domain.resp.ApiCallback;
import com.huobi.api.client.security.WsAuthentication;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.io.Closeable;

/**
 * created by jacky. 2018/7/24 4:00 PM
 */
@Slf4j
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
        return createNewWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<KlineEventResp>(callback, KlineEventResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                onKlineTick(symbol, period, callback);
            }
        });
    }

    @Override
    public Closeable requestKline(String symbol, Resolution period, long from, long to, ApiCallback<KlineEventResp> callback) {
        KlineEvent event = new KlineEvent();
        event.setSymbol(symbol);
        event.setPeriod(period);
        event.setFrom(from);
        event.setTo(to);
        return createNewWebSocket(event.toRequest(), new HuobiApiWebSocketListener<KlineEventResp>(callback, KlineEventResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                requestKline(symbol, period, from, to, callback);
            }
        });
    }

    @Override
    public Closeable onDepthTick(String symbol, MergeLevel level, ApiCallback<DepthEventResp> callback) {
        DepthEvent event = new DepthEvent();
        event.setSymbol(symbol);
        event.setLevel(level);
        return createNewWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<DepthEventResp>(callback, DepthEventResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                onDepthTick(symbol, level, callback);
            }
        });
    }

    @Override
    public Closeable requestDepth(String symbol, MergeLevel level, long from, long to, ApiCallback<DepthEventResp> callback) {
        DepthEvent event = new DepthEvent();
        event.setSymbol(symbol);
        event.setLevel(level);
        return createNewWebSocket(event.toRequest(), new HuobiApiWebSocketListener<DepthEventResp>(callback, DepthEventResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                requestDepth(symbol, level, from, to, callback);
            }
        });
    }


    @Override
    public Closeable onTradeDetailTick(String symbol, ApiCallback<TradeDetailResp> callback) {
        TradeDetailEvent event = new TradeDetailEvent();
        event.setSymbol(symbol);
        return createNewWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<TradeDetailResp>(callback, TradeDetailResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                onTradeDetailTick(symbol, callback);

            }
        });
    }


    @Override
    public Closeable onMarketDetailTick(String symbol, ApiCallback<MarketDetailResp> callback) {
        MarketDetailEvent event = new MarketDetailEvent();
        event.setSymbol(symbol);
        return createNewWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<MarketDetailResp>(callback, MarketDetailResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                onMarketDetailTick(symbol, callback);
            }
        });
    }

    @Override
    public Closeable onOrderTick(String symbol, ApiCallback<OrderEventResp> callback) {
        OrderEvent event = new OrderEvent(symbol);
        event.setClientId("111");
        //oauth
        return newAuthWebSocket(new WsAuthentication(event.getClientId()).toAuth(), new HuobiApiWebSocketListener<OrderEventResp>((webSocket, response) -> {
            if ("auth".equalsIgnoreCase(response.getOp())) {
                if (response.getErrCode().equals("0")) {
                    //oauth success,sub topic.
                    webSocket.send(event.toSubscribe());
                } else {
                    //oauth failed.show msg.
                    log.info("error " + response.getErrCode() + ":" + response.getErrMsg());
                }
            } else if ("notify".equalsIgnoreCase(response.getOp())) {
                callback.onResponse(webSocket, response);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                try {
                    log.error(mapper.writeValueAsString(response));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }, OrderEventResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                onOrderTick(symbol, callback);
            }
        });
    }

    @Override
    public Closeable onAccountTick(ApiCallback<AccountEventResp> callback) {
        AccountEvent event = new AccountEvent();
        event.setClientId("40sG903yz80oDFWr");
        return newAuthWebSocket(event.toSubscribe(), new HuobiApiWebSocketListener<AccountEventResp>(callback, AccountEventResp.class) {
            @Override
            public void onExpired(WebSocket webSocket, int code, String reason) {
                super.onExpired(webSocket, code, reason);
                onAccountTick(callback);
            }
        });
    }

    private Closeable createNewWebSocket(String topic, HuobiApiWebSocketListener<?> listener) {
        String streamingUrl = HuobiConsts.WS_API_URL;
        return newWebSocket(streamingUrl, topic, listener);
    }

    private Closeable newAuthWebSocket(String topic, HuobiApiWebSocketListener<?> listener) {
        String streamingUrl = HuobiConsts.WS_API_URL + "/v1";
        return newWebSocket(streamingUrl, topic, listener);
    }

    private Closeable newWebSocket(String url, String topic, HuobiApiWebSocketListener<?> listener) {
        Request request = new Request.Builder().url(url).build();
        final WebSocket webSocket = client.newWebSocket(request, listener);
        webSocket.send(topic);
        return () -> {
            final int code = 1000;
            listener.onClosing(webSocket, code, null);
            webSocket.close(code, null);
            listener.onClosed(webSocket, code, null);
        };
    }
}
