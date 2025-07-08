package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.property.RestClientDefaultSettings;
import com.example.http.autoconfiguration.property.RestClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ResilienceInstanceFactoryTest {

    private static final String CLIENT = "test-client";

    @Test
    void shouldReturnExistingCircuitBreakerFromRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker existing = registry.circuitBreaker(CLIENT);

        RestClientProperties.Resilience resilience =
                RestClientProperties.Resilience.builder().build();

        CircuitBreaker result = ResilienceInstanceFactory.getCircuitBreaker(CLIENT, registry, resilience);

        Assertions.assertThat(result).isSameAs(existing);
    }

    @Test
    void shouldCreateCircuitBreakerIfMissing() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .circuitBreaker(RestClientDefaultSettings.defaultCircuitBreakerProperties())
                .build();

        CircuitBreaker cb = ResilienceInstanceFactory.getCircuitBreaker(CLIENT, registry, resilience);

        Assertions.assertThat(cb).isNotNull();
        Assertions.assertThat(registry.find(CLIENT)).isPresent();
    }

    @Test
    void shouldReturnExistingRetryFromRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        Retry existing = registry.retry(CLIENT);

        RestClientProperties.Resilience resilience =
                RestClientProperties.Resilience.builder().build();

        Retry result = ResilienceInstanceFactory.getRetry(CLIENT, registry, resilience);

        Assertions.assertThat(result).isSameAs(existing);
    }

    @Test
    void shouldCreateRetryIfMissing() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .retry(RestClientDefaultSettings.defaultRetryWrapper())
                .build();

        Retry retry = ResilienceInstanceFactory.getRetry(CLIENT, registry, resilience);

        Assertions.assertThat(retry).isNotNull();
        Assertions.assertThat(registry.find(CLIENT)).isPresent();
    }

    @Test
    void shouldReturnExistingRateLimiterFromRegistry() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiter existing = registry.rateLimiter(CLIENT);

        RestClientProperties.Resilience resilience =
                RestClientProperties.Resilience.builder().build();

        RateLimiter result = ResilienceInstanceFactory.getRateLimiter(CLIENT, registry, resilience);

        Assertions.assertThat(result).isSameAs(existing);
    }

    @Test
    void shouldCreateRateLimiterIfMissing() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .rateLimiter(RestClientDefaultSettings.defaultRateLimiterProperties())
                .build();

        RateLimiter rl = ResilienceInstanceFactory.getRateLimiter(CLIENT, registry, resilience);

        Assertions.assertThat(rl).isNotNull();
        Assertions.assertThat(registry.find(CLIENT)).isPresent();
    }
}
