package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryProperties;

public final class RetryFactory {

    private RetryFactory() {
        // static utility
    }

    public static Retry create(String name, RetryRegistry registry, RetryProperties.InstanceProperties props) {
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
        IntervalFunction intervalFn = null;

        if (props.getWaitDuration() != null) {
            if (props.getExponentialBackoffMultiplier() != null
                    && props.getRandomizedWaitFactor() != null
                    && props.getExponentialMaxWaitDuration() != null) {
                intervalFn = IntervalFunction.ofExponentialRandomBackoff(
                        props.getWaitDuration(),
                        props.getExponentialBackoffMultiplier(),
                        props.getRandomizedWaitFactor(),
                        props.getExponentialMaxWaitDuration());
            } else if (props.getExponentialBackoffMultiplier() != null
                    && props.getRandomizedWaitFactor() != null
                    && props.getExponentialMaxWaitDuration() == null) {
                intervalFn = IntervalFunction.ofExponentialRandomBackoff(
                        props.getWaitDuration(),
                        props.getExponentialBackoffMultiplier(),
                        props.getRandomizedWaitFactor());
            } else if (props.getExponentialBackoffMultiplier() != null && props.getRandomizedWaitFactor() != null) {
                intervalFn = IntervalFunction.ofExponentialRandomBackoff(
                        props.getWaitDuration(),
                        props.getExponentialBackoffMultiplier(),
                        props.getRandomizedWaitFactor());
            } else if (props.getRandomizedWaitFactor() != null) {
                intervalFn =
                        IntervalFunction.ofExponentialBackoff(props.getWaitDuration(), props.getRandomizedWaitFactor());
            } else if (props.getWaitDuration().toMillis() > 0) {
                intervalFn = IntervalFunction.of(props.getWaitDuration());
            }
        }

        return intervalFn;
    }
}
