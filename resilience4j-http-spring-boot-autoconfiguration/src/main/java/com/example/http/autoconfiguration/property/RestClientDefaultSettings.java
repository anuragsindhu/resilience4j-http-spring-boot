package com.example.http.autoconfiguration.property;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import jakarta.validation.ValidationException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@UtilityClass
public final class RestClientDefaultSettings {

    public RestClientProperties.Resilience defaultResilience() {
        return RestClientProperties.Resilience.builder()
                .circuitBreakerEnabled(false)
                .rateLimiterEnabled(false)
                .retryEnabled(false)
                .circuitBreaker(defaultCircuitBreakerProperties())
                .rateLimiter(defaultRateLimiterProperties())
                .retry(defaultRetryWrapper())
                .build();
    }

    public CircuitBreakerProperties.InstanceProperties defaultCircuitBreakerProperties() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        props.setFailureRateThreshold(50F);
        props.setIgnoreExceptions(new Class[] {IllegalArgumentException.class, ValidationException.class});
        props.setMaxWaitDurationInHalfOpenState(Duration.ofSeconds(5));
        props.setMinimumNumberOfCalls(10);
        props.setPermittedNumberOfCallsInHalfOpenState(3);
        props.setRecordExceptions(new Class[0]);
        props.setSlidingWindowSize(10);
        props.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        props.setSlowCallDurationThreshold(Duration.ofSeconds(2));
        props.setSlowCallRateThreshold(100F);
        props.setWaitDurationInOpenState(Duration.ofSeconds(10));
        return props;
    }

    public RateLimiterProperties.InstanceProperties defaultRateLimiterProperties() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setLimitForPeriod(10);
        props.setLimitRefreshPeriod(Duration.ofSeconds(1));
        props.setTimeoutDuration(Duration.ofMillis(500));
        props.setWritableStackTraceEnabled(false);
        return props;
    }

    public RestClientProperties.RequestFactory defaultRequestFactory() {
        return RestClientProperties.RequestFactory.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .connectionRequestTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    public RestClientProperties.RetryWrapper defaultRetryWrapper() {
        var retry = new RestClientProperties.RetryWrapper();
        retry.setExponentialBackoffMultiplier(2.0);
        retry.setExponentialMaxWaitDuration(Duration.ofSeconds(10));
        retry.setFailAfterMaxAttempts(true);
        retry.setIgnoreExceptions(new Class[] {IllegalArgumentException.class, ValidationException.class});
        retry.setMaxAttempts(4);
        retry.setRetryExceptions(new Class[] {
            ConnectException.class,
            HttpClientErrorException.class,
            HttpServerErrorException.class,
            TimeoutException.class,
            SocketTimeoutException.class
        });
        retry.setRandomizedWaitFactor(0.5);
        retry.setRetryStatus(Set.of(
                HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.GATEWAY_TIMEOUT));
        retry.setWaitDuration(Duration.ofSeconds(1));

        return retry;
    }
}
