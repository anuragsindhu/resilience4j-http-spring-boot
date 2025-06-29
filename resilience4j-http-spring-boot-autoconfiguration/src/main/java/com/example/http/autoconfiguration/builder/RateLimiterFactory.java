package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;

public final class RateLimiterFactory {

    private RateLimiterFactory() {
        // static utility
    }

    public static RateLimiter create(
            String name, RateLimiterRegistry registry, RateLimiterProperties.InstanceProperties props) {
        RateLimiterConfig.Builder builder = RateLimiterConfig.custom();

        if (props.getLimitForPeriod() != null) {
            builder.limitForPeriod(props.getLimitForPeriod());
        }
        if (props.getLimitRefreshPeriod() != null) {
            builder.limitRefreshPeriod(props.getLimitRefreshPeriod());
        }
        if (props.getTimeoutDuration() != null) {
            builder.timeoutDuration(props.getTimeoutDuration());
        }

        RateLimiterConfig config = builder.build();
        return registry.rateLimiter(name, config);
    }
}
