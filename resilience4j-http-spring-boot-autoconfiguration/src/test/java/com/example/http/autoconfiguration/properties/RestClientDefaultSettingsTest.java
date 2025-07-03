package com.example.http.autoconfiguration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import org.junit.jupiter.api.Test;

class RestClientDefaultSettingsTest {

    @Test
    void defaultResilienceMatchesDefaultsInBuilder() {
        // from the helper
        RestClientProperties.Resilience fromSettings = RestClientDefaultSettings.defaultResilience();
        // from the builder, which uses the same defaults
        RestClientProperties.Resilience fromBuilder =
                RestClientProperties.Resilience.builder().build();

        // flags
        assertThat(fromSettings.isCircuitBreakerEnabled()).isEqualTo(fromBuilder.isCircuitBreakerEnabled());
        assertThat(fromSettings.isRetryEnabled()).isEqualTo(fromBuilder.isRetryEnabled());
        assertThat(fromSettings.isRateLimiterEnabled()).isEqualTo(fromBuilder.isRateLimiterEnabled());

        // circuit breaker properties (compare a known default field)
        CircuitBreakerProperties.InstanceProperties cbSettings = fromSettings.getCircuitBreaker();
        CircuitBreakerProperties.InstanceProperties cbBuilder = fromBuilder.getCircuitBreaker();
        assertThat(cbSettings.getFailureRateThreshold()).isEqualTo(cbBuilder.getFailureRateThreshold());

        // retry wrapper defaults: maxAttempts and retryStatus
        RestClientProperties.RetryWrapper retrySettings = fromSettings.getRetry();
        RestClientProperties.RetryWrapper retryBuilder = fromBuilder.getRetry();

        assertThat(retrySettings.getConfig().getMaxAttempts())
                .isEqualTo(retryBuilder.getConfig().getMaxAttempts());

        assertThat(retrySettings.getRetryStatus()).containsExactlyInAnyOrderElementsOf(retryBuilder.getRetryStatus());

        // rate limiter properties (compare a known default field)
        RateLimiterProperties.InstanceProperties rlSettings = fromSettings.getRateLimiter();
        RateLimiterProperties.InstanceProperties rlBuilder = fromBuilder.getRateLimiter();
        assertThat(rlSettings.getLimitForPeriod()).isEqualTo(rlBuilder.getLimitForPeriod());
    }

    @Test
    void defaultCircuitBreakerProperties() {
        var cb = RestClientDefaultSettings.defaultCircuitBreakerProperties();
        // Compare a couple of key defaults against a freshly built instance
        CircuitBreakerProperties.InstanceProperties defCB =
                RestClientProperties.Resilience.builder().build().getCircuitBreaker();
        assertThat(cb.getSlidingWindowSize()).isEqualTo(defCB.getSlidingWindowSize());
        assertThat(cb.getFailureRateThreshold()).isEqualTo(defCB.getFailureRateThreshold());
    }

    @Test
    void defaultRetryWrapperProperties() {
        var retry = RestClientDefaultSettings.defaultRetryWrapper();
        RestClientProperties.RetryWrapper defRetry =
                RestClientProperties.Resilience.builder().build().getRetry();

        assertThat(retry.getConfig().getMaxAttempts())
                .isEqualTo(defRetry.getConfig().getMaxAttempts());

        assertThat(retry.getRetryStatus()).containsExactlyInAnyOrderElementsOf(defRetry.getRetryStatus());
    }

    @Test
    void defaultRateLimiterProperties() {
        var rl = RestClientDefaultSettings.defaultRateLimiterProperties();
        RateLimiterProperties.InstanceProperties defRl =
                RestClientProperties.Resilience.builder().build().getRateLimiter();

        assertThat(rl.getLimitForPeriod()).isEqualTo(defRl.getLimitForPeriod());
        assertThat(rl.getLimitRefreshPeriod()).isEqualTo(defRl.getLimitRefreshPeriod());
    }
}
