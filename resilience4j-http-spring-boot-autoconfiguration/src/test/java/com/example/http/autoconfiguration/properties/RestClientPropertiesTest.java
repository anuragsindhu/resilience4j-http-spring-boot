package com.example.http.autoconfiguration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestClientPropertiesTest {

    @Test
    void defaultBuilderHasExpectedDefaults() {
        RestClientProperties props = RestClientProperties.builder().build();

        // top-level defaults
        assertThat(props.getBaseUrl()).isNull();
        assertThat(props.getClientName()).isEqualTo("default");

        // httpClient default config
        HttpClientProperties expectedHttp = HttpClientProperties.defaultConfig();
        assertThat(props.getHttpClient()).isEqualTo(expectedHttp);

        // observationTags empty then mutable
        Map<String, String> tags = props.getObservationTags();
        assertThat(tags).isEmpty();
        tags.put("key", "value");
        assertThat(props.getObservationTags()).containsEntry("key", "value");

        // resilience defaults from RestClientDefaultSettings
        RestClientProperties.Resilience resilience = props.getResilience();
        RestClientProperties.Resilience expectedRes = RestClientDefaultSettings.defaultResilience();

        // flags
        assertThat(resilience.isCircuitBreakerEnabled()).isEqualTo(expectedRes.isCircuitBreakerEnabled());
        assertThat(resilience.isRetryEnabled()).isEqualTo(expectedRes.isRetryEnabled());
        assertThat(resilience.isRateLimiterEnabled()).isEqualTo(expectedRes.isRateLimiterEnabled());

        // circuit breaker properties (compare a known default field)
        var cbProps = resilience.getCircuitBreaker();
        var expectedCb = expectedRes.getCircuitBreaker();
        assertThat(cbProps.getFailureRateThreshold()).isEqualTo(expectedCb.getFailureRateThreshold());

        // retry wrapper defaults
        RestClientProperties.RetryWrapper retry = resilience.getRetry();
        RestClientProperties.RetryWrapper expectedRetry = expectedRes.getRetry();

        // maxAttempts default
        assertThat(retry.getConfig().getMaxAttempts())
                .isEqualTo(expectedRetry.getConfig().getMaxAttempts());

        // default retryStatus entries
        assertThat(retry.getRetryStatus()).containsExactlyInAnyOrderElementsOf(expectedRetry.getRetryStatus());

        // rate limiter properties (compare a known default field)
        var rlProps = resilience.getRateLimiter();
        var expectedRl = expectedRes.getRateLimiter();
        assertThat(rlProps.getLimitForPeriod()).isEqualTo(expectedRl.getLimitForPeriod());
    }

    @Test
    void builderOverridesAllFields() {
        String url = "https://api.example";
        String name = "customClient";

        HttpClientProperties customHttp =
                HttpClientProperties.builder().enabled(false).build();

        Map<String, String> customTags = Map.of("env", "test");

        RestClientProperties.Resilience customRes = RestClientProperties.Resilience.builder()
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimiterEnabled(true)
                .build();

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl(url)
                .clientName(name)
                .httpClient(customHttp)
                .observationTags(new java.util.HashMap<>(customTags))
                .resilience(customRes)
                .build();

        assertThat(props.getBaseUrl()).isEqualTo(url);
        assertThat(props.getClientName()).isEqualTo(name);
        assertThat(props.getHttpClient()).isSameAs(customHttp);
        assertThat(props.getObservationTags()).containsExactlyEntriesOf(customTags);
        assertThat(props.getResilience()).isSameAs(customRes);
    }

    @Test
    void nullHttpClientAndResilienceAreAllowed() {
        RestClientProperties props =
                RestClientProperties.builder().httpClient(null).resilience(null).build();

        assertThat(props.getHttpClient()).isNull();
        assertThat(props.getResilience()).isNull();
    }

    @Test
    void retryWrapperDefaultAndOverrideBehavior() {
        // Default RetryWrapper
        RestClientProperties.RetryWrapper defaultWrapper =
                RestClientProperties.RetryWrapper.builder().build();
        RetryConfig defaultConfig = defaultWrapper.getConfig();

        // default maxAttempts is 3
        assertThat(defaultConfig.getMaxAttempts()).isEqualTo(3);

        // default retryStatus empty
        assertThat(defaultWrapper.getRetryStatus()).isEmpty();

        // override both
        RetryConfig customConfig = RetryConfig.custom()
                .waitDuration(Duration.ofSeconds(1))
                .maxAttempts(5)
                .build();
        Set<HttpStatus> statuses = Set.of(HttpStatus.INTERNAL_SERVER_ERROR);

        RestClientProperties.RetryWrapper customWrapper = RestClientProperties.RetryWrapper.builder()
                .config(customConfig)
                .retryStatus(statuses)
                .build();
        RetryConfig overrideConfig = customWrapper.getConfig();

        assertThat(overrideConfig.getMaxAttempts()).isEqualTo(5);
        assertThat(customWrapper.getRetryStatus()).containsExactly(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
