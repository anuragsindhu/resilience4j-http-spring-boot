package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterFactoryTest {

    private final RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();

    @Test
    void shouldReturnNullWhenPropsIsNull() {
        RateLimiter result = RateLimiterFactory.create("test-limiter", RateLimiterRegistry.ofDefaults(), null);
        assertThat(result).isNull();
    }

    @Test
    void createWithDefaultsUsesRegistryDefaults() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        RateLimiter limiter = RateLimiterFactory.create("defaultLimiter", registry, props);

        RateLimiterConfig cfg = limiter.getRateLimiterConfig();
        RateLimiterConfig defaultCfg = registry.getDefaultConfig();

        assertThat(cfg.getLimitForPeriod()).isEqualTo(defaultCfg.getLimitForPeriod());
        assertThat(cfg.getLimitRefreshPeriod()).isEqualTo(defaultCfg.getLimitRefreshPeriod());
        assertThat(cfg.getTimeoutDuration()).isEqualTo(defaultCfg.getTimeoutDuration());
    }

    @Test
    void createWithCustomValuesShouldBuildConfiguredRateLimiter() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setLimitForPeriod(42);
        props.setLimitRefreshPeriod(Duration.ofMillis(750));
        props.setTimeoutDuration(Duration.ofMillis(200));

        RateLimiter limiter = RateLimiterFactory.create("customLimiter", registry, props);
        RateLimiterConfig cfg = limiter.getRateLimiterConfig();

        assertThat(cfg.getLimitForPeriod()).isEqualTo(42);
        assertThat(cfg.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(750));
        assertThat(cfg.getTimeoutDuration()).isEqualTo(Duration.ofMillis(200));
    }

    @Test
    void createReusesInstanceForSameName() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setLimitForPeriod(10);

        RateLimiter one = RateLimiterFactory.create("reusedLimiter", registry, props);
        RateLimiter two = RateLimiterFactory.create("reusedLimiter", registry, props);

        assertThat(one).isSameAs(two);
    }
}
