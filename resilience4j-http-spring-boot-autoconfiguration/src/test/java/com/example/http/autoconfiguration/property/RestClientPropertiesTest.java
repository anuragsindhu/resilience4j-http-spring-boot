package com.example.http.autoconfiguration.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.client.property.HttpClientProperties;
import com.example.http.client.property.HttpClientProperties.Ssl;
import com.example.http.client.property.HttpClientProperties.Store;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestClientPropertiesTest {

    @Test
    void shouldBuildWithDefaultValues() {
        RestClientProperties props = RestClientProperties.builder().build();
        HttpClientProperties client = props.getHttpClient();

        assertThat(props.getBaseUrl()).isNull();
        assertThat(props.getClientName()).isEqualTo("default");
        assertThat(props.getObservationTags()).isEmpty();
        assertThat(props.getRequestFactory().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.getRequestFactory().getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(props.getRequestFactory().getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.getResilience()).isNotNull();
        assertThat(props.isEnabled()).isTrue();

        // Nested HttpClient config
        assertThat(client.getPool().getConcurrencyPolicy()).isEqualTo("LAX");
        assertThat(client.getPool().getMaxConnectionsPerRoute()).isEqualTo(20);
        assertThat(client.getPool().getMaxTotalConnections()).isEqualTo(200);
        assertThat(client.getPool().getConnection().getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(client.getPool().getConnection().getIdleEvictionTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(client.getPool().getConnection().getTimeToLive()).isEqualTo(Duration.ofMinutes(5));
        assertThat(client.getPool().getConnection().getValidateAfterInactivity())
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(client.getPool().getSocket().getSoLinger()).isEqualTo(Duration.ofSeconds(-1));
        assertThat(client.getPool().getSocket().getRcvBuffSize()).isEqualTo(32 * 1024);
        assertThat(client.getPool().getSocket().getSndBuffSize()).isEqualTo(32 * 1024);
        assertThat(client.getPool().getSocket().getSoTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(client.getPool().getSocket().isTcpNoDelay()).isTrue();

        assertThat(client.getRequestFactory().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(client.getRequestFactory().getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(client.getRequestFactory().getReadTimeout()).isEqualTo(Duration.ofSeconds(10));

        assertThat(client.getSsl().isEnabled()).isFalse();
        assertThat(client.getSsl().isTrustAll()).isFalse();
        assertThat(client.getSsl().getHostnameVerificationPolicy().name()).isEqualTo("BUILTIN");
    }

    @Test
    void shouldAcceptCustomSslConfiguration() {
        Store truststore = Store.builder()
                .location("classpath:truststore.p12")
                .password("changeit")
                .type("PKCS12")
                .provider("SunJSSE")
                .build();

        Ssl ssl = Ssl.builder()
                .enabled(true)
                .trustAll(false)
                .truststore(truststore)
                .build();

        HttpClientProperties client = HttpClientProperties.builder().ssl(ssl).build();

        assertThat(client.getSsl().isEnabled()).isTrue();
        assertThat(client.getSsl().getTruststore()).isNotNull();
        assertThat(client.getSsl().getTruststore().getLocation()).isEqualTo("classpath:truststore.p12");
        assertThat(client.getSsl().getTruststore().getPassword()).isEqualTo("changeit");
        assertThat(client.getSsl().getTruststore().getType()).isEqualTo("PKCS12");
        assertThat(client.getSsl().getTruststore().getProvider()).isEqualTo("SunJSSE");
    }

    @Test
    void shouldRespectResilienceFlagsAndRetryStatus() {
        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .circuitBreakerEnabled(true)
                .rateLimiterEnabled(true)
                .retryEnabled(true)
                .build();

        RestClientProperties.RetryWrapper wrapper =
                RestClientProperties.RetryWrapper.builder().build();

        assertThat(resilience.isCircuitBreakerEnabled()).isTrue();
        assertThat(resilience.isRateLimiterEnabled()).isTrue();
        assertThat(resilience.isRetryEnabled()).isTrue();

        assertThat(wrapper.getRetryStatus())
                .containsExactlyInAnyOrder(
                        HttpStatus.TOO_MANY_REQUESTS,
                        HttpStatus.BAD_GATEWAY,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void shouldRespectCustomBaseUrlAndTags() {
        Map<String, String> tags = Map.of("client", "analytics", "env", "stage");

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl("https://data.example.org")
                .observationTags(tags)
                .build();

        assertThat(props.getBaseUrl()).isEqualTo("https://data.example.org");
        assertThat(props.getObservationTags()).containsEntry("client", "analytics");
        assertThat(props.getObservationTags()).containsEntry("env", "stage");
    }

    @Test
    void shouldCreateInstanceUsingDefaultConfigMethod() {
        RestClientProperties props = RestClientProperties.defaultConfig();

        assertThat(props.getClientName()).isEqualTo("default");
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getHttpClient()).isNotNull();
        assertThat(props.getResilience()).isNotNull();
    }
}
