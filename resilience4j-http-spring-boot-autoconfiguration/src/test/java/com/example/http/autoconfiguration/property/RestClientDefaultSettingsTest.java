package com.example.http.autoconfiguration.property;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestClientDefaultSettingsTest {

    @Test
    void shouldProvideDefaultCircuitBreakerSettings() {
        CircuitBreakerProperties.InstanceProperties props = RestClientDefaultSettings.defaultCircuitBreakerProperties();

        assertThat(props.getAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(props.getFailureRateThreshold()).isEqualTo(50F);
        assertThat(props.getIgnoreExceptions()).contains(IllegalArgumentException.class);
        assertThat(props.getMaxWaitDurationInHalfOpenState()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(props.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        assertThat(props.getRecordExceptions()).isEmpty();
        assertThat(props.getSlidingWindowSize()).isEqualTo(10);
        assertThat(props.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(props.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
        assertThat(props.getSlowCallRateThreshold()).isEqualTo(100F);
        assertThat(props.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void shouldProvideDefaultRateLimiterSettings() {
        RateLimiterProperties.InstanceProperties props = RestClientDefaultSettings.defaultRateLimiterProperties();

        assertThat(props.getLimitForPeriod()).isEqualTo(10);
        assertThat(props.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));
        assertThat(props.getTimeoutDuration()).isEqualTo(Duration.ofMillis(500));
        assertThat(props.getWritableStackTraceEnabled()).isFalse();
    }

    @Test
    void shouldProvideDefaultRetrySettings() {
        RestClientProperties.RetryWrapper retry = RestClientDefaultSettings.defaultRetryWrapper();

        assertThat(retry.getExponentialBackoffMultiplier()).isEqualTo(2.0);
        assertThat(retry.getExponentialMaxWaitDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(retry.getFailAfterMaxAttempts()).isTrue();
        assertThat(retry.getIgnoreExceptions()).contains(IllegalArgumentException.class);
        assertThat(retry.getMaxAttempts()).isEqualTo(4);
        assertThat(retry.getRandomizedWaitFactor()).isEqualTo(0.5);
        assertThat(retry.getRetryExceptions())
                .containsExactlyInAnyOrder(
                        java.net.ConnectException.class,
                        org.springframework.web.client.HttpClientErrorException.class,
                        org.springframework.web.client.HttpServerErrorException.class,
                        java.util.concurrent.TimeoutException.class,
                        java.net.SocketTimeoutException.class);
        assertThat(retry.getRetryStatus())
                .containsExactlyInAnyOrder(
                        HttpStatus.BAD_GATEWAY,
                        HttpStatus.GATEWAY_TIMEOUT,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        HttpStatus.TOO_MANY_REQUESTS);
        assertThat(retry.getWaitDuration()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void shouldProvideDefaultResilienceContainer() {
        RestClientProperties.Resilience resilience = RestClientDefaultSettings.defaultResilience();

        assertThat(resilience.isCircuitBreakerEnabled()).isFalse();
        assertThat(resilience.isRateLimiterEnabled()).isFalse();
        assertThat(resilience.isRetryEnabled()).isFalse();
        assertThat(resilience.getCircuitBreaker()).isNotNull();
        assertThat(resilience.getRateLimiter()).isNotNull();
        assertThat(resilience.getRetry()).isNotNull();
    }

    @Test
    void shouldProvideDefaultRequestFactorySettings() {
        RestClientProperties.RequestFactory requestFactory = RestClientDefaultSettings.defaultRequestFactory();

        assertThat(requestFactory.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(requestFactory.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(requestFactory.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
}
