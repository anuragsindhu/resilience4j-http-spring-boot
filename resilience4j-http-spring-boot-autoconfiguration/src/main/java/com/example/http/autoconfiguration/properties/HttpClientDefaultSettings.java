package com.example.http.autoconfiguration.properties;

import java.time.Duration;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;

public final class HttpClientDefaultSettings {

    private HttpClientDefaultSettings() {}

    public static HttpClientProperties defaultHttpClient() {
        return HttpClientProperties.builder()
                .enabled(true)
                .pool(defaultPool())
                .requestFactory(defaultRequestFactory())
                .ssl(defaultSsl())
                .build();
    }

    public static HttpClientProperties.Pool defaultPool() {
        return HttpClientProperties.Pool.builder()
                .concurrencyPolicy("LAX")
                .maxConnectionsPerRoute(20)
                .maxTotalConnections(200)
                .connection(defaultConnection())
                .socket(defaultSocket())
                .build();
    }

    public static HttpClientProperties.Pool.Connection defaultConnection() {
        return HttpClientProperties.Pool.Connection.builder()
                .idleEvictionTimeout(Duration.ofMinutes(1))
                .timeToLive(Duration.ofMinutes(5))
                .validateAfterInactivity(Duration.ofSeconds(30))
                .build();
    }

    public static HttpClientProperties.Pool.Socket defaultSocket() {
        return HttpClientProperties.Pool.Socket.builder()
                .soLinger(Duration.ofSeconds(-1))
                .rcvBuffSize(32 * 1024)
                .sndBuffSize(32 * 1024)
                .soTimeout(Duration.ofSeconds(10))
                .tcpNoDelay(true)
                .build();
    }

    public static HttpClientProperties.RequestFactory defaultRequestFactory() {
        return HttpClientProperties.RequestFactory.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .connectionRequestTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static HttpClientProperties.Ssl defaultSsl() {
        return HttpClientProperties.Ssl.builder()
                .enabled(false)
                .trustAll(false)
                .hostnameVerificationPolicy(HostnameVerificationPolicy.BUILTIN)
                .build();
    }
}
