package com.example.http.autoconfiguration.logging;

import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ResilienceEventPublisherLoggerTest {

    @Test
    void shouldAttachToCircuitBreaker() {
        // Arrange
        Logger logger = mock(Logger.class);
        CircuitBreaker.EventPublisher cbPublisher = mock(CircuitBreaker.EventPublisher.class, RETURNS_SELF);
        CircuitBreaker cb = mock(CircuitBreaker.class);
        when(cb.getName()).thenReturn("my-cb");
        when(cb.getEventPublisher()).thenReturn(cbPublisher);

        // Act
        ResilienceEventPublisherLogger.attach(null, cb, null, logger);

        // Assert
        verify(cbPublisher).onStateTransition(any());
        verify(cbPublisher).onCallNotPermitted(any());
        verify(cbPublisher).onError(any());
        verify(cbPublisher).onSuccess(any());
    }

    @Test
    void shouldAttachToRetry() {
        // Arrange
        Logger logger = mock(Logger.class);
        Retry.EventPublisher retryPublisher = mock(Retry.EventPublisher.class, RETURNS_SELF);
        Retry retry = mock(Retry.class);
        when(retry.getName()).thenReturn("my-retry");
        when(retry.getEventPublisher()).thenReturn(retryPublisher);

        // Act
        ResilienceEventPublisherLogger.attach(retry, null, null, logger);

        // Assert
        verify(retryPublisher).onRetry(any());
        verify(retryPublisher).onSuccess(any());
        verify(retryPublisher).onError(any());
        verify(retryPublisher).onIgnoredError(any());
    }

    @Test
    void shouldAttachToRateLimiter() {
        // Arrange
        Logger logger = mock(Logger.class);
        RateLimiter.EventPublisher rlPublisher = mock(RateLimiter.EventPublisher.class, RETURNS_SELF);
        RateLimiter rl = mock(RateLimiter.class);
        when(rl.getName()).thenReturn("my-rl");
        when(rl.getEventPublisher()).thenReturn(rlPublisher);

        // Act
        ResilienceEventPublisherLogger.attach(null, null, rl, logger);

        // Assert
        verify(rlPublisher).onSuccess(any());
        verify(rlPublisher).onFailure(any());
        verify(rlPublisher).onEvent(any());
    }

    @Test
    void shouldDoNothingIfAllResilience4jComponentsAreNull() {
        Logger logger = mock(Logger.class);

        // Should not throw or log anything
        ResilienceEventPublisherLogger.attach(null, null, null, logger);

        verifyNoInteractions(logger);
    }
}
