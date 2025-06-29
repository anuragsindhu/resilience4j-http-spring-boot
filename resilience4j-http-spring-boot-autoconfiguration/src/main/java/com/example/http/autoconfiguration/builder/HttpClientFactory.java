package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.ResilientRestClientProperties;
import java.time.Duration;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public final class HttpClientFactory {

    private static final Duration DEFAULT_CONNECT = Duration.ofSeconds(1);
    private static final Duration DEFAULT_READ = Duration.ofSeconds(1);

    private HttpClientFactory() {
        // static utility
    }

    public static HttpComponentsClientHttpRequestFactory createFactory(ResilientRestClientProperties.Client cfg) {
        // fallbacks
        Duration ct = cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : DEFAULT_CONNECT;
        Duration rt = cfg.getReadTimeout() != null ? cfg.getReadTimeout() : DEFAULT_READ;

        // 1) Connection‐level config (TCP connect timeout)
        ConnectionConfig connConfig =
                ConnectionConfig.custom().setConnectTimeout(Timeout.of(ct)).build();

        // 2) Request‐level config (response/read timeout)
        RequestConfig reqConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(Duration.ofSeconds(5)))
                .setResponseTimeout(Timeout.of(rt))
                .build();

        // 3) Build a pooling connection manager with our ConnectionConfig
        PoolingHttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connConfig)
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(100)
                .build();

        // 4) Bake both configs into the CloseableHttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(reqConfig)
                .disableAutomaticRetries()
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
