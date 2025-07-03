package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

class ResilienceHttpRequestInterceptorTest {

    private final ObservationRegistry registry = ObservationRegistry.create();
    private final HttpRequest request = mock(HttpRequest.class);
    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final byte[] body = new byte[0];

    @Test
    void shouldSucceedOn200() throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(execution.execute(request, body)).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/api"));

        var interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName("test-client")
                .build();

        var result = interceptor.intercept(request, body, execution);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowOnRetryable5xx() throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(execution.execute(request, body)).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/fail"));

        Retry retry = Retry.ofDefaults("retry-test");

        var interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName("fail-client")
                .retry(retry)
                .build();

        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageContaining("500");
    }

    @Test
    void shouldRetryCustomStatusIfConfigured() throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
        when(execution.execute(request, body)).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/custom"));

        var interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName("custom-client")
                .retryStatus(Set.of(HttpStatus.BAD_GATEWAY))
                .build();

        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageContaining("502");
    }

    @Test
    void shouldWrapIOExceptionAsRestClientException() throws Exception {
        when(execution.execute(request, body)).thenThrow(new IOException("kaboom"));
        when(request.getMethod()).thenReturn(HttpMethod.PUT);
        when(request.getURI()).thenReturn(URI.create("/io"));

        var interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName("io-client")
                .build();

        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("Resilience4j call failed");
    }

    @Test
    void shouldPropagateRequestNotPermitted() throws Exception {
        RateLimiter rl = RateLimiter.of(
                "rl-client",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(Duration.ofSeconds(60))
                        .timeoutDuration(Duration.ZERO)
                        .build());

        // Exhaust the single permit
        rl.acquirePermission();

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/rl"));

        var interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName("rl-client")
                .rateLimiter(rl)
                .build();

        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void shouldPropagateCallNotPermitted() throws Exception {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(1)
                .slidingWindowSize(1)
                .writableStackTraceEnabled(false)
                .build();

        CircuitBreaker cb = CircuitBreaker.of("cb-client", config);

        // Force it open by recording a failure
        cb.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("fail"));
        cb.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("fail"));

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/cb"));

        var interceptor = ResilienceHttpRequestInterceptor.builder(registry)
                .clientName("cb-client")
                .circuitBreaker(cb)
                .build();

        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
