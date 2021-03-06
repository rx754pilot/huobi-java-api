package com.huobi.api.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huobi.api.client.constant.HuobiConsts;
import com.huobi.api.client.security.HuobiSigner;
import com.huobi.api.client.security.ZipUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * created by jacky. 2018/9/27 9:19 PM
 */
@Slf4j
@Getter
public class HuobiApiAuthWebSocketClient extends WebSocketClient {

    private String auth = "{" +
            "\"SignatureVersion\":\"%s\"," +
            "\"op\":\"auth\"," +
            "\"AccessKeyId\":\"%s\"," +
            "\"Signature\":\"%s\"," +
            "\"SignatureMethod\":\"%s\"," +
            "\"Timestamp\":\"%s\"," +
            "\"cid\":\"%s\"" +
            "}";

    private String unSub = "{" +
            "\"unsub\": \"%s\"," +
            "\"id\": \"%s\"" +
            "}";

    private String WS_PATH = "/ws/v1";
    private String apiKey;
    private String secretKey;
    @Setter
    private String topic;
    @Setter
    private String clientId;
    @Setter
    private HuobiApiWebSocketListener listener;

    public HuobiApiAuthWebSocketClient(URI serverURI, String apiKey, String secret) {
        super(serverURI);
        this.apiKey = apiKey;
        this.secretKey = secret;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        auth();
    }


    public void auth() {
        HuobiSigner signer = new HuobiSigner(apiKey, secretKey);
        Map<String, String> param = new HashMap<>();
        String now = signer.gmtNow();
        String signature = signer.sign("GET", WS_PATH, param, now);
        send(String.format(auth, HuobiConsts.SIGNATURE_VERSION, apiKey, signature, HuobiConsts.SIGNATURE_METHOD, now, clientId));
    }


    public void sub() {
        send(topic);
    }

    public void unSub() {
        send(String.format(unSub, topic, clientId));
    }


    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            byte[] uncompress = ZipUtil.decompress(bytes.array());
            String text = new String(uncompress);
            onMessage(text);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void onMessage(String text) {
        try {
            if (text.contains("ping")) {
                send(text.replace("ping", "pong"));
                listener.onMessage(null, text);
            } else if (text.contains("pong")) {
                //ignore
            } else {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(text);
                String op = node.get("op").asText();
                if ("auth".equals(op)) {
                    String code = node.get("err-code").asText();
                    if ("0".equals(code)) {
                        //auth success
                        sub();
                    } else {
                        log.error("auth webSocket error.{}", text);
                    }
                } else if ("sub".equals(op)) {
                    //ignore
                } else if ("notify".equals(op)) {
                    listener.onMessage(null, text);
                }else {
                    log.error("unknown op {}",text);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        listener.onClosing(null, i, s);
    }

    @Override
    public void onError(Exception e) {
        listener.onFailure(null, e, null);
    }
}
