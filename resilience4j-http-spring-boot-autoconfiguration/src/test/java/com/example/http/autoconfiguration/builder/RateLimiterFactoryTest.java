package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RateLimiterFactoryTest {

    @Test
    void shouldReturnNullWhenPropertiesAreNull() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiter result = RateLimiterFactory.create("null-test", registry, null);

        Assertions.assertThat(result).isNull();
    }

    @Test
    void shouldCreateRateLimiterWithDefaultRegistrySettings() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setLimitForPeriod(10);
        props.setLimitRefreshPeriod(Duration.ofSeconds(1));
        props.setTimeoutDuration(Duration.ofMillis(500));

        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiter limiter = RateLimiterFactory.create("basic-test", registry, props);

        Assertions.assertThat(limiter).isNotNull();
        Assertions.assertThat(limiter.getName()).isEqualTo("basic-test");

        RateLimiterConfig config = limiter.getRateLimiterConfig();
        Assertions.assertThat(config.getLimitForPeriod()).isEqualTo(10);
        Assertions.assertThat(config.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));
        Assertions.assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void shouldHandlePartialConfigurationGracefully() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setLimitForPeriod(7); // other fields left null

        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiter limiter = RateLimiterFactory.create("partial-test", registry, props);

        RateLimiterConfig config = limiter.getRateLimiterConfig();

        Assertions.assertThat(config.getLimitForPeriod()).isEqualTo(7);
        Assertions.assertThat(config.getLimitRefreshPeriod()).isNotNull(); // uses default
        Assertions.assertThat(config.getTimeoutDuration()).isNotNull(); // uses default
    }

    @Test
    void shouldUseCustomTimeoutAndRefreshPeriod() {
        RateLimiterProperties.InstanceProperties props = new RateLimiterProperties.InstanceProperties();
        props.setTimeoutDuration(Duration.ofSeconds(2));
        props.setLimitRefreshPeriod(Duration.ofMillis(750));

        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiter limiter = RateLimiterFactory.create("custom-refresh", registry, props);

        RateLimiterConfig config = limiter.getRateLimiterConfig();

        Assertions.assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(2));
        Assertions.assertThat(config.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(750));
    }
}
