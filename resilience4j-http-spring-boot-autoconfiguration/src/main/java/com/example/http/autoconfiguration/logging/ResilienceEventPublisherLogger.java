package com.example.http.autoconfiguration.logging;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;

public final class ResilienceEventPublisherLogger {

    public static void attach(Retry retry, CircuitBreaker cb, RateLimiter rl, Logger log) {
        if (cb != null) {
            cb.getEventPublisher()
                    .onStateTransition(event -> log.info(
                            "Circuit breaker[{}] state transition from {} to {}",
                            cb.getName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()))
                    .onCallNotPermitted(
                            event -> log.warn("Circuit breaker[{}] call not permitted (circuit is OPEN)", cb.getName()))
                    .onError(event -> log.error(
                            "Circuit breaker[{}] error recorded: {}",
                            cb.getName(),
                            event.getThrowable().toString()))
                    .onSuccess(event -> log.debug(
                            "Circuit breaker[{}] call succeeded in {}ms",
                            cb.getName(),
                            event.getElapsedDuration().toMillis()));
        }

        if (retry != null) {
            retry.getEventPublisher()
                    .onRetry(event -> log.info(
                            "Retry[{}] retry attempt #{} due to {}",
                            retry.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().toString()))
                    .onSuccess(event -> log.info(
                            "Retry[{}] call succeeded after {} attempts",
                            retry.getName(),
                            event.getNumberOfRetryAttempts()))
                    .onError(event -> log.warn(
                            "Retry[{}] retries exhausted after {} attempts; last error: {}",
                            retry.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().toString()))
                    .onIgnoredError(event -> log.trace(
                            "Retry[{}] error ignored (not ble): {}",
                            retry.getName(),
                            event.getLastThrowable().toString()));
        }

        if (rl != null) {
            rl.getEventPublisher()
                    .onSuccess(event ->
                            log.debug("Rate limiter[{}] permission granted: {}", rl.getName(), event.getEventType()))
                    .onFailure(
                            event -> log.warn("Rate limiter[{}] call blocked: {}", rl.getName(), event.getEventType()))
                    .onEvent(event -> log.trace("Rate limiter[{}] full event: {}", rl.getName(), event));
        }
    }
}
