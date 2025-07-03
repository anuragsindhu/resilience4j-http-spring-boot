package com.example.http.autoconfiguration.properties;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Set;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.springframework.http.HttpStatus;

public final class RestClientDefaultSettings {

    private RestClientDefaultSettings() {
        // Utility class
    }

    public static RestClientProperties.Resilience defaultResilience() {
        return RestClientProperties.Resilience.builder()
                .circuitBreakerEnabled(false)
                .retryEnabled(false)
                .rateLimiterEnabled(false)
                .circuitBreaker(defaultCircuitBreakerProperties())
                .retry(defaultRetryWrapper())
                .rateLimiter(defaultRateLimiterProperties())
                .build();
    }

    public static CircuitBreakerProperties.InstanceProperties defaultCircuitBreakerProperties() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setFailureRateThreshold(50f);
        props.setWaitDurationInOpenState(Duration.ofSeconds(30));
        props.setSlidingWindowSize(100);
        props.setMinimumNumberOfCalls(10);
        return props;
    }

    public static RestClientProperties.RetryWrapper defaultRetryWrapper() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(5)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        Duration.ofMillis(500), // Initial delay
                        2.0, // Multiplier
                        0.4, // Jitter
                        Duration.ofSeconds(8) // Max cap
                        ))
                .retryOnResult(response -> false)
                .retryOnException(ex -> ex instanceof ConnectException
                        || ex instanceof ConnectionRequestTimeoutException
                        || ex instanceof SocketTimeoutException)
                .build();

        return RestClientProperties.RetryWrapper.builder()
                .config(config)
                .retryStatus(Set.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        HttpStatus.BAD_GATEWAY,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        HttpStatus.GATEWAY_TIMEOUT))
                .build();
    }

    public static RateLimiterProperties.InstanceProperties defaultRateLimiterProperties() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setLimitRefreshPeriod(Duration.ofSeconds(1));
        props.setLimitForPeriod(50);
        props.setTimeoutDuration(Duration.ofMillis(100));
        return props;
    }
}
