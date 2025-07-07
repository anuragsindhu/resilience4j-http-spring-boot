package com.example.http.autoconfiguration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import org.junit.jupiter.api.Test;

class RestClientDefaultSettingsTest {

    @Test
    void defaultResilienceMatchesDefaultsInBuilder() {
        RestClientProperties.Resilience fromSettings = RestClientDefaultSettings.defaultResilience();
        RestClientProperties.Resilience fromBuilder =
                RestClientProperties.Resilience.builder().build();

        assertThat(fromSettings.isCircuitBreakerEnabled()).isEqualTo(fromBuilder.isCircuitBreakerEnabled());
        assertThat(fromSettings.isRetryEnabled()).isEqualTo(fromBuilder.isRetryEnabled());
        assertThat(fromSettings.isRateLimiterEnabled()).isEqualTo(fromBuilder.isRateLimiterEnabled());

        CircuitBreakerProperties.InstanceProperties cbSettings = fromSettings.getCircuitBreaker();
        CircuitBreakerProperties.InstanceProperties cbBuilder = fromBuilder.getCircuitBreaker();
        assertThat(cbSettings.getFailureRateThreshold()).isEqualTo(cbBuilder.getFailureRateThreshold());

        RestClientProperties.RetryWrapper retrySettings = fromSettings.getRetry();
        RestClientProperties.RetryWrapper retryBuilder = fromBuilder.getRetry();

        assertThat(retrySettings.getMaxAttempts()).isEqualTo(retryBuilder.getMaxAttempts());

        assertThat(retrySettings.getRetryStatus()).containsExactlyInAnyOrderElementsOf(retryBuilder.getRetryStatus());

        RateLimiterProperties.InstanceProperties rlSettings = fromSettings.getRateLimiter();
        RateLimiterProperties.InstanceProperties rlBuilder = fromBuilder.getRateLimiter();
        assertThat(rlSettings.getLimitForPeriod()).isEqualTo(rlBuilder.getLimitForPeriod());
    }

    @Test
    void defaultCircuitBreakerProperties() {
        var cb = RestClientDefaultSettings.defaultCircuitBreakerProperties();
        CircuitBreakerProperties.InstanceProperties defCB =
                RestClientProperties.Resilience.builder().build().getCircuitBreaker();
        assertThat(cb.getSlidingWindowSize()).isEqualTo(defCB.getSlidingWindowSize());
        assertThat(cb.getFailureRateThreshold()).isEqualTo(defCB.getFailureRateThreshold());
    }

    @Test
    void defaultRateLimiterProperties() {
        var rl = RestClientDefaultSettings.defaultRateLimiterProperties();
        RateLimiterProperties.InstanceProperties defRl =
                RestClientProperties.Resilience.builder().build().getRateLimiter();

        assertThat(rl.getLimitForPeriod()).isEqualTo(defRl.getLimitForPeriod());
        assertThat(rl.getLimitRefreshPeriod()).isEqualTo(defRl.getLimitRefreshPeriod());
    }

    @Test
    void defaultRetryWrapperProperties() {
        var retry = RestClientDefaultSettings.defaultRetryWrapper();
        RestClientProperties.RetryWrapper defRetry =
                RestClientProperties.Resilience.builder().build().getRetry();

        assertThat(retry.getMaxAttempts()).isEqualTo(defRetry.getMaxAttempts());
        assertThat(retry.getRetryStatus()).containsExactlyInAnyOrderElementsOf(defRetry.getRetryStatus());
    }
}
