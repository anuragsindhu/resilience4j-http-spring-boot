package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CircuitBreakerFactoryTest {

    private final CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

    @Test
    void createWithDefaultsUsesRegistryDefaults() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        // all props null → use registry defaults

        CircuitBreaker cb = CircuitBreakerFactory.create("testCb", registry, props);
        CircuitBreakerConfig cfg = cb.getCircuitBreakerConfig();

        CircuitBreakerConfig defaultCfg = registry.getDefaultConfig();
        assertThat(cfg.getFailureRateThreshold()).isEqualTo(defaultCfg.getFailureRateThreshold());
    }

    @Test
    void createWithCustomPropsAppliesAllSettings() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setFailureRateThreshold(42f);
        props.setWaitDurationInOpenState(Duration.ofSeconds(7));
        props.setMinimumNumberOfCalls(10);
        props.setPermittedNumberOfCallsInHalfOpenState(2);
        props.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        props.setSlidingWindowSize(20);
        props.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        props.setSlowCallRateThreshold(75f);
        props.setSlowCallDurationThreshold(Duration.ofMillis(500));
        props.setMaxWaitDurationInHalfOpenState(Duration.ofSeconds(3));
        props.setIgnoreExceptions(new Class[] {IllegalArgumentException.class});
        props.setRecordExceptions(new Class[] {NullPointerException.class});

        CircuitBreaker cb = CircuitBreakerFactory.create("customCb", registry, props);
        CircuitBreakerConfig cfg = cb.getCircuitBreakerConfig();

        assertThat(cfg.getFailureRateThreshold()).isEqualTo(42f);
        assertThat(cfg.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(cfg.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
        assertThat(cfg.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(cfg.getSlidingWindowSize()).isEqualTo(20);
        assertThat(cfg.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(cfg.getSlowCallRateThreshold()).isEqualTo(75f);
        assertThat(cfg.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(500));
        assertThat(cfg.getMaxWaitDurationInHalfOpenState()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void createReusesRegistryWhenNameIsSame() {
        CircuitBreakerProperties.InstanceProperties props = new CircuitBreakerProperties.InstanceProperties();
        props.setFailureRateThreshold(10f);

        CircuitBreaker first = CircuitBreakerFactory.create("dupCb", registry, props);
        CircuitBreaker second = CircuitBreakerFactory.create("dupCb", registry, props);

        // Registry returns the same instance for a given name
        assertThat(first).isSameAs(second);
    }
}
