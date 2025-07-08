package com.example.http.client.property;

import java.time.Duration;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpClientDefaultSettingsTest {

    @Test
    void shouldCreateDefaultHttpClientProperties() {
        HttpClientProperties props = HttpClientDefaultSettings.defaultHttpClient();

        Assertions.assertThat(props).isNotNull();
        Assertions.assertThat(props.getPool()).isNotNull();
        Assertions.assertThat(props.getRequestFactory()).isNotNull();
        Assertions.assertThat(props.getSsl()).isNotNull();
    }

    @Test
    void shouldCreateDefaultConnectionSettings() {
        HttpClientProperties.Pool.Connection connection = HttpClientDefaultSettings.defaultConnection();

        Assertions.assertThat(connection.getIdleEvictionTimeout()).isEqualTo(Duration.ofMinutes(1));
        Assertions.assertThat(connection.getTimeToLive()).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(connection.getValidateAfterInactivity()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldCreateDefaultPoolSettings() {
        HttpClientProperties.Pool pool = HttpClientDefaultSettings.defaultPool();

        Assertions.assertThat(pool.getConcurrencyPolicy()).isEqualTo("LAX");
        Assertions.assertThat(pool.getConnection()).isNotNull();
        Assertions.assertThat(pool.getMaxConnectionsPerRoute()).isEqualTo(20);
        Assertions.assertThat(pool.getMaxTotalConnections()).isEqualTo(200);
        Assertions.assertThat(pool.getSocket()).isNotNull();
    }

    @Test
    void shouldCreateDefaultRequestFactory() {
        HttpClientProperties.RequestFactory factory = HttpClientDefaultSettings.defaultRequestFactory();

        Assertions.assertThat(factory.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        Assertions.assertThat(factory.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
        Assertions.assertThat(factory.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void shouldCreateDefaultSocketSettings() {
        HttpClientProperties.Pool.Socket socket = HttpClientDefaultSettings.defaultSocket();

        Assertions.assertThat(socket.getRcvBuffSize()).isEqualTo(32 * 1024);
        Assertions.assertThat(socket.getSoLinger()).isEqualTo(Duration.ofSeconds(-1));
        Assertions.assertThat(socket.getSoTimeout()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(socket.getSndBuffSize()).isEqualTo(32 * 1024);
        Assertions.assertThat(socket.isTcpNoDelay()).isTrue();
    }

    @Test
    void shouldCreateDefaultSslSettings() {
        HttpClientProperties.Ssl ssl = HttpClientDefaultSettings.defaultSsl();

        Assertions.assertThat(ssl.getHostnameVerificationPolicy()).isEqualTo(HostnameVerificationPolicy.BUILTIN);
        Assertions.assertThat(ssl.getHostnameVerifier()).isNull();
        Assertions.assertThat(ssl.getHostnameVerifierBeanName()).isNull();
        Assertions.assertThat(ssl.getKeystore()).isNull();
        Assertions.assertThat(ssl.getTruststore()).isNull();
        Assertions.assertThat(ssl.isEnabled()).isFalse();
        Assertions.assertThat(ssl.isTrustAll()).isFalse();
    }
}
