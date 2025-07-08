package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

class ResilienceHttpRequestInterceptorTest {

    private static final String CLIENT_NAME = "test-client";

    @Test
    void shouldBuildInterceptorWithAllComponents() {
        ObservationRegistry registry = ObservationRegistry.create();
        CircuitBreaker cb = CircuitBreakerRegistry.ofDefaults().circuitBreaker(CLIENT_NAME);
        RateLimiter rl = RateLimiterRegistry.ofDefaults().rateLimiter(CLIENT_NAME);
        Retry retry = RetryRegistry.ofDefaults().retry(CLIENT_NAME);

        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName(CLIENT_NAME)
                .observationTags(Map.of("env", "test"))
                .circuitBreaker(cb)
                .rateLimiter(rl)
                .retry(retry)
                .retryStatus(Set.of(HttpStatus.TOO_MANY_REQUESTS, HttpStatus.BAD_GATEWAY))
                .build();

        assertThat(interceptor).isNotNull();
    }

    @Test
    void shouldThrowHttpClientErrorExceptionForRetryableStatus() throws IOException {
        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .retryStatus(Set.of(HttpStatus.TOO_MANY_REQUESTS))
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(java.net.URI.create("/api"));

        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(429));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("429");
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForRetryableStatus() throws IOException {
        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .retryStatus(Set.of(HttpStatus.BAD_GATEWAY))
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(java.net.URI.create("/server"));

        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(502));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageContaining("502");
    }

    @Test
    void shouldPropagateRetryableIOException() throws IOException {
        Retry retry = RetryRegistry.ofDefaults().retry(CLIENT_NAME);

        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .retry(retry)
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(java.net.URI.create("/timeout"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenThrow(new SocketTimeoutException("timeout"));

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(RestClientException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void shouldThrowCallNotPermittedExceptionWhenCircuitBreakerIsOpen() throws IOException {
        CircuitBreaker cb = CircuitBreakerRegistry.ofDefaults().circuitBreaker(CLIENT_NAME);
        cb.transitionToOpenState();

        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .circuitBreaker(cb)
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(java.net.URI.create("/cb"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void shouldThrowRequestNotPermittedWhenRateLimitExceeded() throws IOException {
        RateLimiter rl = RateLimiter.of(
                "rl-test",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(Duration.ofSeconds(10))
                        .timeoutDuration(Duration.ofMillis(0))
                        .build());

        rl.acquirePermission(); // Consume the one available permit

        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .rateLimiter(rl)
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.PUT);
        when(request.getURI()).thenReturn(java.net.URI.create("/rl"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void shouldAllowNormalResponseWhenNotRetryableStatus() throws IOException {
        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .retryStatus(Set.of(HttpStatus.BAD_GATEWAY))
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(java.net.URI.create("/ok"));

        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldUnwrapUncheckedIOException() throws IOException {
        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.PATCH);
        when(request.getURI()).thenReturn(java.net.URI.create("/unwrapped"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenAnswer(invocation -> {
            throw new UncheckedIOException(new IOException("IO exploded"));
        });

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("IO exploded");
    }

    @Test
    void shouldWrapUnknownThrowableInRestClientException() throws IOException {
        ResilienceHttpRequestInterceptor interceptor = ResilienceHttpRequestInterceptor.builder(
                        ObservationRegistry.NOOP)
                .clientName(CLIENT_NAME)
                .build();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.DELETE);
        when(request.getURI()).thenReturn(java.net.URI.create("/unknown"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenAnswer(invocation -> {
            throw new RuntimeException(new IOException("nested IO"));
        });

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(RestClientException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
