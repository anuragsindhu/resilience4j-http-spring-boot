package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.property.RestClientDefaultSettings;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class RestClientDefaultSettingsTest {

    @Nested
    class RetryDefaults {

        @ParameterizedTest
        @EnumSource(
                value = HttpStatus.class,
                names = {"TOO_MANY_REQUESTS", "BAD_GATEWAY", "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT"})
        void shouldContainRetryableHttpStatus(HttpStatus status) {
            var retryProps = RestClientDefaultSettings.defaultRetryWrapper();
            Assertions.assertThat(retryProps.getRetryStatus()).contains(status);
        }

        static Stream<Class<? extends Throwable>> retryableExceptions() {
            return Stream.of(
                    ConnectException.class,
                    HttpClientErrorException.class,
                    HttpServerErrorException.class,
                    TimeoutException.class,
                    SocketTimeoutException.class);
        }

        @ParameterizedTest
        @MethodSource("retryableExceptions")
        void shouldRetryOnRetryExceptionTypes(Class<? extends Throwable> exceptionType) throws Exception {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(4)
                    .waitDuration(Duration.ofMillis(100))
                    .retryExceptions(exceptionType)
                    .build();

            Retry retry = RetryRegistry.of(config).retry("test-retry");

            CheckedSupplier<String> supplier = () -> {
                throw createException(exceptionType);
            };

            CheckedSupplier<String> decorated = Retry.decorateCheckedSupplier(retry, supplier);

            Assertions.assertThatThrownBy(decorated::get).isInstanceOf(exceptionType);
            Assertions.assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                    .isGreaterThan(0);
        }

        private Throwable createException(Class<? extends Throwable> type) throws Exception {
            if (type == HttpClientErrorException.class) {
                return new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
            } else if (type == HttpServerErrorException.class) {
                return new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad gateway");
            } else {
                return type.getDeclaredConstructor(String.class).newInstance("fail");
            }
        }
    }

    @Nested
    class RateLimiterDefaults {

        @Test
        void shouldBlockAfterExceedingLimitForPeriod() {
            var props = RestClientDefaultSettings.defaultRateLimiterProperties();
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(props.getLimitForPeriod())
                    .limitRefreshPeriod(props.getLimitRefreshPeriod())
                    .timeoutDuration(props.getTimeoutDuration())
                    .writableStackTraceEnabled(props.getWritableStackTraceEnabled())
                    .build();

            RateLimiter rateLimiter = RateLimiterRegistry.of(config).rateLimiter("test-rl");

            for (int i = 0; i < config.getLimitForPeriod(); i++) {
                Assertions.assertThat(rateLimiter.acquirePermission()).isTrue();
            }

            Assertions.assertThatThrownBy(() -> RateLimiter.decorateCheckedSupplier(rateLimiter, () -> "hit limit")
                            .get())
                    .isInstanceOf(RequestNotPermitted.class);
        }
    }

    @Nested
    class CircuitBreakerDefaults {

        @Test
        void shouldIgnoreConfiguredExceptions() throws Exception {
            var props = RestClientDefaultSettings.defaultCircuitBreakerProperties();
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .automaticTransitionFromOpenToHalfOpenEnabled(
                            props.getAutomaticTransitionFromOpenToHalfOpenEnabled())
                    .failureRateThreshold(props.getFailureRateThreshold())
                    .ignoreExceptions(props.getIgnoreExceptions())
                    .maxWaitDurationInHalfOpenState(props.getMaxWaitDurationInHalfOpenState())
                    .minimumNumberOfCalls(props.getMinimumNumberOfCalls())
                    .permittedNumberOfCallsInHalfOpenState(props.getPermittedNumberOfCallsInHalfOpenState())
                    .recordExceptions(props.getRecordExceptions())
                    .slidingWindowSize(props.getSlidingWindowSize())
                    .slidingWindowType(props.getSlidingWindowType())
                    .slowCallDurationThreshold(props.getSlowCallDurationThreshold())
                    .slowCallRateThreshold(props.getSlowCallRateThreshold())
                    .waitDurationInOpenState(props.getWaitDurationInOpenState())
                    .build();

            CircuitBreaker cb = CircuitBreakerRegistry.of(config).circuitBreaker("test-cb-ignore");

            CheckedSupplier<String> supplier = () -> {
                throw new IllegalArgumentException("should be ignored");
            };

            CheckedSupplier<String> decorated = CircuitBreaker.decorateCheckedSupplier(cb, supplier);

            Assertions.assertThatThrownBy(decorated::get).isInstanceOf(IllegalArgumentException.class);
            Assertions.assertThat(cb.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        }

        @Test
        @SneakyThrows
        void shouldOpenCircuitAfterThresholdBreached() {
            var props = RestClientDefaultSettings.defaultCircuitBreakerProperties();
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(props.getFailureRateThreshold())
                    .minimumNumberOfCalls(props.getMinimumNumberOfCalls())
                    .slidingWindowSize(props.getSlidingWindowSize())
                    .waitDurationInOpenState(props.getWaitDurationInOpenState())
                    .build();

            CircuitBreaker cb = CircuitBreakerRegistry.of(config).circuitBreaker("test-cb-open");

            for (int i = 0; i < props.getMinimumNumberOfCalls(); i++) {
                try {
                    CircuitBreaker.decorateCheckedSupplier(cb, () -> {
                                throw new RuntimeException("fail");
                            })
                            .get();
                } catch (Exception ignored) {
                }
            }

            cb.transitionToOpenState();

            Assertions.assertThatThrownBy(() -> CircuitBreaker.decorateCheckedSupplier(cb, () -> "fallback")
                            .get())
                    .isInstanceOf(CallNotPermittedException.class);
        }
    }
}
