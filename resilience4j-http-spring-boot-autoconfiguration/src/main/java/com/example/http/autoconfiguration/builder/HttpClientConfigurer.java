package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.properties.HttpClientProperties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@UtilityClass
public final class HttpClientConfigurer {

    public HttpComponentsClientHttpRequestFactory configure(HttpClientProperties props) {
        if (!props.isEnabled()) {
            throw new IllegalStateException("HttpClient configuration is disabled.");
        }

        HostnameVerifier verifier = props.getSsl().getHostnameVerifier();
        if (verifier == null) {
            verifier = props.getSsl().isTrustAll()
                    ? (host, session) -> true
                    : HttpsURLConnection.getDefaultHostnameVerifier();
        }

        SSLContext sslContext = SslContextBuilder.from(props.getSsl()).build();
        TlsSocketStrategy tlsStrategy = null;
        if (sslContext != null) {
            tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(verifier)
                    .setHostVerificationPolicy(props.getSsl().getHostnameVerificationPolicy())
                    .buildClassic();
        }

        HttpClientProperties.Pool.Connection conn = props.getPool().getConnection();
        HttpClientProperties.Pool.Socket sock = props.getPool().getSocket();

        PoolingHttpClientConnectionManagerBuilder poolBuilder = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(props.getPool().getMaxTotalConnections())
                .setMaxConnPerRoute(props.getPool().getMaxConnectionsPerRoute())
                .setPoolConcurrencyPolicy(
                        PoolConcurrencyPolicy.valueOf(props.getPool().getConcurrencyPolicy()))
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(conn.getConnectTimeout()))
                        .setSocketTimeout(Timeout.of(sock.getSoTimeout()))
                        .setTimeToLive(TimeValue.of(conn.getTimeToLive()))
                        .setValidateAfterInactivity(TimeValue.of(conn.getValidateAfterInactivity()))
                        .build())
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.of(sock.getSoTimeout()))
                        .setSoLinger(
                                sock.getSoLinger().isNegative()
                                        ? TimeValue.NEG_ONE_SECOND
                                        : TimeValue.of(sock.getSoLinger()))
                        .setRcvBufSize(sock.getRcvBuffSize())
                        .setSndBufSize(sock.getSndBuffSize())
                        .setTcpNoDelay(sock.isTcpNoDelay())
                        .build());

        if (tlsStrategy != null) {
            poolBuilder.setTlsSocketStrategy(tlsStrategy);
        }

        HttpClient client = HttpClientBuilder.create()
                .disableAutomaticRetries()
                .setConnectionManager(poolBuilder.build())
                .evictIdleConnections(TimeValue.of(conn.getIdleEvictionTimeout()))
                .build();

        HttpClientProperties.RequestFactory rf = props.getRequestFactory();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectionRequestTimeout(rf.getConnectionRequestTimeout());
        factory.setConnectTimeout(rf.getConnectTimeout());
        factory.setReadTimeout(rf.getReadTimeout());

        return factory;
    }
}
