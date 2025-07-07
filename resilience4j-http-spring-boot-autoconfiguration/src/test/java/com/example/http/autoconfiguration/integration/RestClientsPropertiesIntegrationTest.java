package com.example.http.autoconfiguration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.autoconfiguration.properties.RestClientProperties;
import com.example.http.autoconfiguration.properties.RestClientsProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = RestClientsPropertiesIntegrationTest.TestConfig.class)
@TestPropertySource(
        properties = {
            // Force alpha creation with a no-op default override
            "group.http.clients.alpha.client-name=default",

            // Fully override beta
            "group.http.clients.beta.base-url=https://beta.example.com",
            "group.http.clients.beta.client-name=beta-client",
            "group.http.clients.beta.http-client.pool.concurrency-policy=STRICT",
            "group.http.clients.beta.observation-tags.region=us-west",
            "group.http.clients.beta.resilience.circuit-breaker-enabled=true",
            "group.http.clients.beta.resilience.retry-enabled=true",
            "group.http.clients.beta.resilience.rate-limiter-enabled=true"
        })
class RestClientsPropertiesIntegrationTest {

    @EnableConfigurationProperties(RestClientsProperties.class)
    static class TestConfig {}

    @Autowired
    private RestClientsProperties restClientsProperties;

    @Test
    void alphaUsesAllDefaults() {
        Map<String, RestClientProperties> clients = restClientsProperties.getClients();
        assertThat(clients).containsKeys("alpha", "beta");

        RestClientProperties alpha = clients.get("alpha");
        RestClientProperties defaults = RestClientProperties.defaultConfig();

        assertThat(alpha.getBaseUrl()).isEqualTo(defaults.getBaseUrl());
        assertThat(alpha.getClientName()).isEqualTo(defaults.getClientName());

        assertThat(alpha.getHttpClient()).isEqualTo(defaults.getHttpClient());

        assertThat(alpha.getObservationTags()).isEmpty();

        var alphaRes = alpha.getResilience();
        var defRes = defaults.getResilience();
        assertThat(alphaRes.isCircuitBreakerEnabled()).isEqualTo(defRes.isCircuitBreakerEnabled());
        assertThat(alphaRes.isRetryEnabled()).isEqualTo(defRes.isRetryEnabled());
        assertThat(alphaRes.isRateLimiterEnabled()).isEqualTo(defRes.isRateLimiterEnabled());

        var alphaRetry = alphaRes.getRetry();
        var defRetry = defRes.getRetry();
        assertThat(alphaRetry.getMaxAttempts()).isEqualTo(defRetry.getMaxAttempts());
        assertThat(alphaRetry.getRetryStatus()).containsExactlyInAnyOrderElementsOf(defRetry.getRetryStatus());

        assertThat(alphaRes.getCircuitBreaker().getSlidingWindowSize())
                .isEqualTo(defRes.getCircuitBreaker().getSlidingWindowSize());
        assertThat(alphaRes.getRateLimiter().getLimitForPeriod())
                .isEqualTo(defRes.getRateLimiter().getLimitForPeriod());
    }

    @Test
    void betaReflectsAllOverrides() {
        RestClientProperties beta = restClientsProperties.getClients().get("beta");

        assertThat(beta.getBaseUrl()).isEqualTo("https://beta.example.com");
        assertThat(beta.getClientName()).isEqualTo("beta-client");

        assertThat(beta.getHttpClient().getPool().getConcurrencyPolicy()).isEqualTo("STRICT");

        assertThat(beta.getObservationTags()).hasSize(1).containsEntry("region", "us-west");

        assertThat(beta.getResilience().isCircuitBreakerEnabled()).isTrue();
        assertThat(beta.getResilience().isRetryEnabled()).isTrue();
        assertThat(beta.getResilience().isRateLimiterEnabled()).isTrue();
    }
}
