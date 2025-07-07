package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class RateLimiterFactory {

    public RateLimiter create(
            String name, RateLimiterRegistry registry, RateLimiterProperties.InstanceProperties props) {
        if (props == null) {
            return null;
        }

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
