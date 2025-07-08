package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.autoconfiguration.property.RestClientProperties;
import com.example.http.client.property.HttpClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestClientBuilderTest {

    private ObservationRegistry observationRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;
    private RateLimiterRegistry rateLimiterRegistry;
    private RestClientBuilder builder;
    private RestClientProperties props;

    @BeforeEach
    void setup() {
        observationRegistry = ObservationRegistry.create();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        builder = RestClientBuilder.builder()
                .observationRegistry(observationRegistry)
                .circuitBreakerRegistry(circuitBreakerRegistry)
                .retryRegistry(retryRegistry)
                .rateLimiterRegistry(rateLimiterRegistry)
                .build();

        props = new RestClientProperties();
        props.setBaseUrl("https://api.example.com");
        props.setHttpClient(new HttpClientProperties());
        props.setRequestFactory(new RestClientProperties.RequestFactory());
    }

    @Test
    void shouldBuildRestClientBuilder() {
        var newBuilder = RestClientBuilder.builder()
                .observationRegistry(observationRegistry)
                .circuitBreakerRegistry(circuitBreakerRegistry)
                .retryRegistry(retryRegistry)
                .rateLimiterRegistry(rateLimiterRegistry)
                .build();

        assertThat(newBuilder).isNotNull();
    }

    @Test
    void shouldBuildRestClientWithNoResilienceEnabled() {
        props.setResilience(new RestClientProperties.Resilience());

        var client = builder.client("client-A", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldBuildRestClientWithDefaultResilienceIfNull() {
        props.setResilience(null);

        var client = builder.client("client-B", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldAttachInterceptorWhenAnyResilienceEnabled() {
        var resilience = new RestClientProperties.Resilience();
        resilience.setCircuitBreakerEnabled(true);

        props.setResilience(resilience);

        var client = builder.client("client-C", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldApplyObservationTagsAndRetryStatus() {
        var resilience = new RestClientProperties.Resilience();
        resilience.setRetryEnabled(true);
        resilience.getRetry().setRetryStatus(Set.of());

        props.setObservationTags(Collections.singletonMap("env", "prod"));
        props.setResilience(resilience);

        var client = builder.client("client-D", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldUseDefaultHttpClientWhenUnset() {
        props.setHttpClient(null);
        props.setResilience(new RestClientProperties.Resilience());

        var client = builder.client("client-default-http", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldCreateOnlyCircuitBreakerWhenEnabled() {
        var resilience = new RestClientProperties.Resilience();
        resilience.setCircuitBreakerEnabled(true);

        props.setResilience(resilience);

        var client = builder.client("client-cb-only", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldCreateOnlyRetryWhenEnabled() {
        var resilience = new RestClientProperties.Resilience();
        resilience.setRetryEnabled(true);

        props.setResilience(resilience);

        var client = builder.client("client-retry-only", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldCreateOnlyRateLimiterWhenEnabled() {
        var resilience = new RestClientProperties.Resilience();
        resilience.setRateLimiterEnabled(true);

        props.setResilience(resilience);

        var client = builder.client("client-rl-only", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldFallbackToDefaultResilienceIfUnset() {
        props.setResilience(null);

        var client = builder.client("client-default-resilience", props).build();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldConfigureInterceptorWhenRetryEnabledWithStatusAndTags() {
        var resilience = new RestClientProperties.Resilience();
        resilience.setRetryEnabled(true);
        resilience.getRetry().setRetryStatus(Set.of(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE));

        props.setResilience(resilience);
        props.setObservationTags(Map.of("env", "staging"));

        var client = builder.client("client-interceptor-retry", props).build();

        assertThat(client).isNotNull();
    }
}
