package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryProperties;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class RetryFactory {

    public Retry create(String name, RetryRegistry registry, RetryProperties.InstanceProperties props) {
        if (props == null) {
            return null;
        }

        RetryConfig.Builder builder = RetryConfig.custom();

        if (props.getFailAfterMaxAttempts() != null) {
            builder.failAfterMaxAttempts(props.getFailAfterMaxAttempts());
        }

        if (props.getIgnoreExceptions() != null && props.getIgnoreExceptions().length > 0) {
            builder.ignoreExceptions(props.getIgnoreExceptions());
        }

        IntervalFunction intervalFn = configureIntervalFunction(props);
        if (intervalFn != null) {
            builder.intervalFunction(intervalFn);
        }

        if (props.getMaxAttempts() != null) {
            builder.maxAttempts(props.getMaxAttempts());
        }

        if (props.getRetryExceptions() != null && props.getRetryExceptions().length > 0) {
            builder.retryExceptions(props.getRetryExceptions());
        }

        if (props.getWaitDuration() != null) {
            builder.waitDuration(props.getWaitDuration());
        }

        RetryConfig config = builder.build();
        return registry.retry(name, config);
    }

    private static IntervalFunction configureIntervalFunction(RetryProperties.InstanceProperties props) {
        if (props == null || props.getWaitDuration() == null) {
            return null;
        }

        return IntervalFunction.ofExponentialRandomBackoff(
                props.getWaitDuration(),
                props.getExponentialBackoffMultiplier() != null ? props.getExponentialBackoffMultiplier() : 2.0,
                props.getRandomizedWaitFactor() != null ? props.getRandomizedWaitFactor() : 0.5,
                props.getExponentialMaxWaitDuration() != null
                        ? props.getExponentialMaxWaitDuration()
                        : props.getWaitDuration().multipliedBy(10));
    }
}
