package com.example.http.autoconfiguration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.junit.jupiter.api.Test;

class HttpClientDefaultSettingsTest {

    @Test
    void defaultHttpClientMatchesBuilderDefaults() {
        HttpClientProperties fromSettings = HttpClientDefaultSettings.defaultHttpClient();
        HttpClientProperties fromBuilder = HttpClientProperties.builder().build();
        assertThat(fromSettings).isEqualTo(fromBuilder);
        assertThat(fromSettings).isNotSameAs(fromBuilder);
    }

    @Test
    void defaultPoolHasExpectedValues() {
        var pool = HttpClientDefaultSettings.defaultPool();
        assertThat(pool.getConcurrencyPolicy()).isEqualTo("LAX");
        assertThat(pool.getMaxConnectionsPerRoute()).isEqualTo(20);
        assertThat(pool.getMaxTotalConnections()).isEqualTo(200);

        // nested connection
        var conn = pool.getConnection();
        assertThat(conn.getIdleEvictionTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(conn.getTimeToLive()).isEqualTo(Duration.ofMinutes(5));
        assertThat(conn.getValidateAfterInactivity()).isEqualTo(Duration.ofSeconds(30));

        // nested socket
        var sock = pool.getSocket();
        assertThat(sock.getLingerTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(sock.getReceiveBufferSize()).isEqualTo(8192);
        assertThat(sock.getSocketTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(sock.isTcpNoDelay()).isTrue();
    }

    @Test
    void defaultRequestFactoryValues() {
        var rf = HttpClientDefaultSettings.defaultRequestFactory();
        assertThat(rf.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(rf.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(rf.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void defaultSslValues() {
        var ssl = HttpClientDefaultSettings.defaultSsl();
        assertThat(ssl.isEnabled()).isFalse();
        assertThat(ssl.isTrustAll()).isFalse();
        assertThat(ssl.getHostnameVerificationPolicy()).isEqualTo(HostnameVerificationPolicy.CLIENT);
    }
}
