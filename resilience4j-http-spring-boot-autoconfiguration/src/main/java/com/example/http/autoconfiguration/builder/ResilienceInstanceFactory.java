package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.properties.RestClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryProperties;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResilienceInstanceFactory {

    public CircuitBreaker getCircuitBreaker(
            String clientName, CircuitBreakerRegistry registry, RestClientProperties.Resilience resilience) {

        if (registry.find(clientName).isPresent()) {
            return registry.circuitBreaker(clientName);
        }

        return CircuitBreakerFactory.create(clientName, registry, resilience.getCircuitBreaker());
    }

    public Retry getRetry(String clientName, RetryRegistry registry, RestClientProperties.Resilience resilience) {

        if (registry.find(clientName).isPresent()) {
            return registry.retry(clientName);
        }

        RetryProperties.InstanceProperties props = resilience.getRetry();
        return RetryFactory.create(clientName, registry, props);
    }

    public RateLimiter getRateLimiter(
            String clientName, RateLimiterRegistry registry, RestClientProperties.Resilience resilience) {

        if (registry.find(clientName).isPresent()) {
            return registry.rateLimiter(clientName);
        }

        return RateLimiterFactory.create(clientName, registry, resilience.getRateLimiter());
    }
}
