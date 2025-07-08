package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import jakarta.validation.ValidationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CircuitBreakerFactoryTest {

    @Test
    void shouldReturnNullIfPropertiesAreNull() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = CircuitBreakerFactory.create("null-case", registry, null);

        assertThat(breaker).isNull();
    }

    @Test
    void shouldConfigureFailureRateThresholdAndSlidingWindow() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setFailureRateThreshold(50f);
        props.setSlidingWindowSize(10);
        props.setSlidingWindowType(SlidingWindowType.COUNT_BASED);
        props.setMinimumNumberOfCalls(5);

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = CircuitBreakerFactory.create("window-test", registry, props);
        CircuitBreakerConfig config = breaker.getCircuitBreakerConfig();

        assertThat(breaker).isNotNull();
        assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getSlidingWindowType()).isEqualTo(SlidingWindowType.COUNT_BASED);
    }

    @Test
    void shouldConfigureOpenStateWaitUsingIntervalFunction() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setWaitDurationInOpenState(Duration.ofSeconds(10));

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = CircuitBreakerFactory.create("open-wait", registry, props);
        CircuitBreakerConfig config = breaker.getCircuitBreakerConfig();

        long intervalMillis = config.getWaitIntervalFunctionInOpenState().apply(1);

        assertThat(intervalMillis).isEqualTo(10_000L);
    }

    @Test
    void shouldRespectIgnoreAndRecordExceptionsSeparately() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setIgnoreExceptions(new Class[] {IllegalArgumentException.class, ValidationException.class});
        props.setRecordExceptions(new Class[] {RuntimeException.class, NullPointerException.class});

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = CircuitBreakerFactory.create("exception-handling", registry, props);
        CircuitBreakerConfig config = breaker.getCircuitBreakerConfig();

        assertThat(config.getIgnoreExceptionPredicate().test(new IllegalArgumentException()))
                .isTrue();
        assertThat(config.getIgnoreExceptionPredicate().test(new ValidationException()))
                .isTrue();
        assertThat(config.getIgnoreExceptionPredicate().test(new NullPointerException()))
                .isFalse();

        assertThat(config.getRecordExceptionPredicate().test(new RuntimeException()))
                .isTrue();

        assertThat(config.getRecordExceptionPredicate().test(new NullPointerException()))
                .isTrue();

        // Resilience4j uses two predicates to decide whether an exception should be counted toward the failure rate:
        // ignoreExceptions and recordExceptions.
        // If recordExceptions not set, the default behavior is to record all unchecked exceptions, such as
        // RuntimeException, IllegalArgumentException, NullPointerException, etc.
        // By default, recording behavior takes precedence unless the ignore predicate actively returns true first.
        // That means if recordExceptions includes a supertype like RuntimeException,
        // and ignoreExceptions includes a subtype like IllegalArgumentException,
        // we need to explicitly verify that ignoreExceptions wins during evaluation.
        // That's why it is validated as true against our wish!
        assertThat(config.getRecordExceptionPredicate().test(new IllegalArgumentException()))
                .isTrue(); // it's ignored
    }

    @Test
    void shouldConfigureHalfOpenTransitionSettings() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        props.setPermittedNumberOfCallsInHalfOpenState(4);
        props.setMaxWaitDurationInHalfOpenState(Duration.ofSeconds(5));

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = CircuitBreakerFactory.create("half-open-config", registry, props);
        CircuitBreakerConfig config = breaker.getCircuitBreakerConfig();

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(4);
        assertThat(config.getMaxWaitDurationInHalfOpenState()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void shouldRespectSlowCallConfiguration() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setSlowCallRateThreshold(80f);
        props.setSlowCallDurationThreshold(Duration.ofSeconds(2));

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = CircuitBreakerFactory.create("slow-call-cb", registry, props);
        CircuitBreakerConfig config = breaker.getCircuitBreakerConfig();

        assertThat(config.getSlowCallRateThreshold()).isEqualTo(80f);
        assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
    }
}
