package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;

public final class CircuitBreakerFactory {

    private CircuitBreakerFactory() {
        // static utility
    }

    public static CircuitBreaker create(
            String name, CircuitBreakerRegistry registry, CircuitBreakerProperties.InstanceProperties props) {
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();

        if (props.getAutomaticTransitionFromOpenToHalfOpenEnabled() != null) {
            builder.automaticTransitionFromOpenToHalfOpenEnabled(
                    props.getAutomaticTransitionFromOpenToHalfOpenEnabled());
        }

        if (props.getFailureRateThreshold() != null) {
            builder.failureRateThreshold(props.getFailureRateThreshold());
        }

        if (props.getIgnoreExceptions() != null && props.getIgnoreExceptions().length > 0) {
            builder.ignoreExceptions(props.getIgnoreExceptions());
        }

        if (props.getMaxWaitDurationInHalfOpenState() != null) {
            builder.maxWaitDurationInHalfOpenState(props.getMaxWaitDurationInHalfOpenState());
        }

        if (props.getMinimumNumberOfCalls() != null) {
            builder.minimumNumberOfCalls(props.getMinimumNumberOfCalls());
        }

        if (props.getPermittedNumberOfCallsInHalfOpenState() != null) {
            builder.permittedNumberOfCallsInHalfOpenState(props.getPermittedNumberOfCallsInHalfOpenState());
        }

        if (props.getRecordExceptions() != null && props.getRecordExceptions().length > 0) {
            builder.recordExceptions(props.getRecordExceptions());
        }

        if (props.getSlidingWindowSize() != null) {
            builder.slidingWindowSize(props.getSlidingWindowSize());
        }

        if (props.getSlidingWindowType() != null) {
            builder.slidingWindowType(props.getSlidingWindowType());
        }

        if (props.getSlowCallDurationThreshold() != null) {
            builder.slowCallDurationThreshold(props.getSlowCallDurationThreshold());
        }

        if (props.getSlowCallRateThreshold() != null) {
            builder.slowCallRateThreshold(props.getSlowCallRateThreshold());
        }

        if (props.getWaitDurationInOpenState() != null) {
            builder.waitDurationInOpenState(props.getWaitDurationInOpenState());
        }

        CircuitBreakerConfig config = builder.build();
        return registry.circuitBreaker(name, config);
    }
}
