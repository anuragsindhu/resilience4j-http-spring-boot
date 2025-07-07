package com.example.http.autoconfiguration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import javax.net.ssl.HostnameVerifier;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.junit.jupiter.api.Test;

class HttpClientPropertiesTest {

    @Test
    void defaultConfigMatchesDefaultSettings() {
        HttpClientProperties fromBuilder = HttpClientProperties.builder().build();
        HttpClientProperties fromDefaults = HttpClientProperties.defaultConfig();
        HttpClientProperties explicitDefaults = HttpClientDefaultSettings.defaultHttpClient();

        assertThat(fromBuilder).isEqualTo(fromDefaults);
        assertThat(fromDefaults).isEqualTo(explicitDefaults);
    }

    @Test
    void defaultSettingsPoolMatchesPoolDefaults() {
        HttpClientProperties.Pool poolFromSettings = HttpClientDefaultSettings.defaultPool();
        HttpClientProperties.Pool poolFromBuilder =
                HttpClientProperties.Pool.builder().build();

        assertThat(poolFromSettings).isEqualTo(poolFromBuilder);
        assertThat(poolFromSettings.getConcurrencyPolicy()).isEqualTo("LAX");
        assertThat(poolFromSettings.getMaxConnectionsPerRoute()).isEqualTo(20);
        assertThat(poolFromSettings.getMaxTotalConnections()).isEqualTo(200);
    }

    @Test
    void defaultSettingsConnectionMatchesConnectionDefaults() {
        HttpClientProperties.Pool.Connection connFromSettings = HttpClientDefaultSettings.defaultConnection();
        HttpClientProperties.Pool.Connection connFromBuilder =
                HttpClientProperties.Pool.Connection.builder().build();

        assertThat(connFromSettings).isEqualTo(connFromBuilder);
        assertThat(connFromSettings.getIdleEvictionTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(connFromSettings.getTimeToLive()).isEqualTo(Duration.ofMinutes(5));
        assertThat(connFromSettings.getValidateAfterInactivity()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void defaultSettingsSocketMatchesSocketDefaults() {
        HttpClientProperties.Pool.Socket sockFromSettings = HttpClientDefaultSettings.defaultSocket();
        HttpClientProperties.Pool.Socket sockFromBuilder =
                HttpClientProperties.Pool.Socket.builder().build();

        assertThat(sockFromSettings).isEqualTo(sockFromBuilder);
        assertThat(sockFromSettings.getSoLinger()).isEqualTo(Duration.ofSeconds(-1));
        assertThat(sockFromSettings.getRcvBuffSize()).isEqualTo(32 * 1024);
        assertThat(sockFromSettings.getSndBuffSize()).isEqualTo(32 * 1024);
        assertThat(sockFromSettings.getSoTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(sockFromSettings.isTcpNoDelay()).isTrue();
    }

    @Test
    void defaultSettingsRequestFactoryMatchesDefaults() {
        HttpClientProperties.RequestFactory rfFromSettings = HttpClientDefaultSettings.defaultRequestFactory();
        HttpClientProperties.RequestFactory rfFromBuilder =
                HttpClientProperties.RequestFactory.builder().build();

        assertThat(rfFromSettings).isEqualTo(rfFromBuilder);
        assertThat(rfFromSettings.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(rfFromSettings.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(rfFromSettings.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void defaultSettingsSslMatchesDefaults() {
        HttpClientProperties.Ssl sslFromSettings = HttpClientDefaultSettings.defaultSsl();
        HttpClientProperties.Ssl sslFromBuilder =
                HttpClientProperties.Ssl.builder().build();

        assertThat(sslFromSettings).isEqualTo(sslFromBuilder);
        assertThat(sslFromSettings.isEnabled()).isFalse();
        assertThat(sslFromSettings.isTrustAll()).isFalse();
        assertThat(sslFromSettings.getTruststore()).isNull();
        assertThat(sslFromSettings.getKeystore()).isNull();
        assertThat(sslFromSettings.getHostnameVerifierBeanName()).isNull();
        assertThat(sslFromSettings.getHostnameVerifier()).isNull();
        assertThat(sslFromSettings.getHostnameVerificationPolicy()).isEqualTo(HostnameVerificationPolicy.BUILTIN);
    }

    @Test
    void builderOverridesEverything() {
        HttpClientProperties.Pool.Connection customConn = HttpClientProperties.Pool.Connection.builder()
                .idleEvictionTimeout(Duration.ZERO)
                .timeToLive(Duration.ZERO)
                .validateAfterInactivity(Duration.ZERO)
                .build();

        HttpClientProperties.Pool.Socket customSock = HttpClientProperties.Pool.Socket.builder()
                .soLinger(Duration.ZERO)
                .rcvBuffSize(1)
                .sndBuffSize(2)
                .soTimeout(Duration.ZERO)
                .tcpNoDelay(false)
                .build();

        HttpClientProperties.Pool customPool = HttpClientProperties.Pool.builder()
                .concurrencyPolicy("STRICT")
                .maxConnectionsPerRoute(1)
                .maxTotalConnections(2)
                .connection(customConn)
                .socket(customSock)
                .build();

        HttpClientProperties.RequestFactory customRf = HttpClientProperties.RequestFactory.builder()
                .connectTimeout(Duration.ofMillis(11))
                .connectionRequestTimeout(Duration.ofMillis(22))
                .readTimeout(Duration.ofMillis(33))
                .build();

        HttpClientProperties.Store trust = HttpClientProperties.Store.builder()
                .location("/trust.jks")
                .password("secret")
                .type("JKS")
                .provider("SUN")
                .build();

        HttpClientProperties.Store key = HttpClientProperties.Store.builder()
                .location("/key.p12")
                .password("secret")
                .type("PKCS12")
                .provider("BC")
                .build();

        HostnameVerifier verifier = (h, s) -> true;
        HttpClientProperties.Ssl customSsl = HttpClientProperties.Ssl.builder()
                .enabled(true)
                .trustAll(true)
                .truststore(trust)
                .keystore(key)
                .hostnameVerifierBeanName("beanVerifier")
                .hostnameVerifier(verifier)
                .hostnameVerificationPolicy(HostnameVerificationPolicy.BOTH)
                .build();

        HttpClientProperties props = HttpClientProperties.builder()
                .enabled(false)
                .pool(customPool)
                .requestFactory(customRf)
                .ssl(customSsl)
                .build();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getPool()).isSameAs(customPool);
        assertThat(props.getRequestFactory()).isSameAs(customRf);
        assertThat(props.getSsl()).isSameAs(customSsl);
        assertThat(props.getSsl().getTruststore()).isSameAs(trust);
        assertThat(props.getSsl().getKeystore()).isSameAs(key);
        assertThat(props.getSsl().getHostnameVerificationPolicy()).isEqualTo(HostnameVerificationPolicy.BOTH);
    }
}
