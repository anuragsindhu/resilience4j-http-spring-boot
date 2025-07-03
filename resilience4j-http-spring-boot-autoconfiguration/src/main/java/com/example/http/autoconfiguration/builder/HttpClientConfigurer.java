package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.properties.HttpClientProperties;
import java.io.File;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public final class HttpClientConfigurer {

    private HttpClientConfigurer() {}

    public static HttpComponentsClientHttpRequestFactory configure(HttpClientProperties props) {
        if (!props.isEnabled()) {
            throw new IllegalStateException("HttpClient configuration is disabled.");
        }

        HostnameVerifier verifier = props.getSsl().getHostnameVerifier();
        if (verifier == null) {
            verifier = props.getSsl().isTrustAll()
                    ? (host, session) -> true
                    : HttpsURLConnection.getDefaultHostnameVerifier();
        }

        SSLContext sslContext = null;
        if (props.getSsl().isEnabled()) {
            try {
                if (props.getSsl().isTrustAll()) {
                    sslContext = SSLContexts.custom()
                            .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                            .build();
                } else {
                    sslContext = SSLContexts.custom()
                            .loadTrustMaterial(
                                    getFile(props.getSsl().getTrustStorePath()),
                                    getPassword(props.getSsl().getTrustStorePassword()))
                            .loadKeyMaterial(
                                    getFile(props.getSsl().getKeyStorePath()),
                                    getPassword(props.getSsl().getKeyStorePassword()),
                                    getPassword(props.getSsl().getKeyStorePassword()))
                            .build();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure SSL context", e);
            }
        }

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
                        .setConnectTimeout(Timeout.of(conn.getTimeToLive()))
                        .setSocketTimeout(Timeout.of(sock.getSocketTimeout()))
                        .setTimeToLive(TimeValue.of(conn.getTimeToLive()))
                        .setValidateAfterInactivity(TimeValue.of(conn.getValidateAfterInactivity()))
                        .build())
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.of(sock.getSocketTimeout()))
                        .setSoLinger(TimeValue.of(sock.getLingerTimeout()))
                        .setRcvBufSize(sock.getReceiveBufferSize())
                        .setSndBufSize(sock.getSendBufferSize())
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

    private static File getFile(String path) {
        return path != null ? new File(path) : null;
    }

    private static char[] getPassword(String password) {
        return password != null ? password.toCharArray() : null;
    }
}
