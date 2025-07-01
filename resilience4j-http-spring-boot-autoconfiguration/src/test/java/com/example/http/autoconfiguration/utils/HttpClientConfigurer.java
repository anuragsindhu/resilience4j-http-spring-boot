package com.example.http.autoconfiguration.utils;

import java.time.Duration;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class HttpClientConfigurer {

    public static HttpComponentsClientHttpRequestFactory configureHttpClient5() {
        // Sensible, production-oriented timeouts
        Timeout connectTimeout = Timeout.ofSeconds(5);
        Timeout socketTimeout = Timeout.ofSeconds(10);
        Timeout lingerTimeout = Timeout.ofSeconds(2);
        TimeValue connectionTimeToLive = TimeValue.ofMinutes(5);
        TimeValue idleConnectionEviction = TimeValue.ofMinutes(1);

        PoolingHttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setSocketTimeout(socketTimeout)
                        .setConnectTimeout(connectTimeout)
                        .setTimeToLive(connectionTimeToLive)
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30)) // detects stale connections
                        .build())
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(socketTimeout)
                        .setRcvBufSize(8 * 1024) // 8 KB, typical receive buffer
                        .setSndBufSize(8 * 1024) // 8 KB, typical send buffer
                        .setSoLinger(lingerTimeout)
                        .setTcpNoDelay(true) // disables Nagle's algorithm — lower latency
                        .build())
                .setMaxConnPerRoute(20) // Apache default; adjust based on use case
                .setMaxConnTotal(200) // Higher concurrency support
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .evictIdleConnections(idleConnectionEviction)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout(Duration.ofSeconds(2)); // max wait for a connection from pool
        factory.setConnectTimeout(Duration.ofSeconds(5)); // time to establish TCP handshake
        factory.setReadTimeout(Duration.ofSeconds(10)); // max wait for data after request

        return factory;
    }
}
