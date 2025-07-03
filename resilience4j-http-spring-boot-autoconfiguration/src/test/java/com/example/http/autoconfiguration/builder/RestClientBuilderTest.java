package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.properties.HttpClientProperties;
import com.example.http.autoconfiguration.properties.RestClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientBuilderTest {

    private RestClientBuilder builder;

    @BeforeEach
    void setUp() {
        ObservationRegistry obs = ObservationRegistry.create();
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        RateLimiterRegistry rlRegistry = RateLimiterRegistry.ofDefaults();

        builder = RestClientBuilder.builder()
                .observationRegistry(obs)
                .circuitBreakerRegistry(cbRegistry)
                .retryRegistry(retryRegistry)
                .rateLimiterRegistry(rlRegistry)
                .build();
    }

    @Test
    void dependenciesBuilderProducesNonNullBuilder() {
        assertThat(builder).isNotNull();
    }

    @Test
    void clientBuilderFluentApiAndBuildNotThrow() {
        RestClientProperties props = RestClientProperties.builder().build();

        RestClientBuilder.ClientBuilder clientBuilder = builder.client("myClient", props);

        assertThat(clientBuilder).isNotNull();
        assertThatNoException().isThrownBy(clientBuilder::build);
    }

    @Test
    void buildAlwaysReturnsDistinctClients() {
        RestClientProperties props =
                RestClientProperties.builder().baseUrl("http://example.com").build();

        RestClient c1 = builder.client("svc", props).build();
        RestClient c2 = builder.client("svc", props).build();
        RestClient c3 = builder.client("other", props).build();

        assertThat(c1).isNotSameAs(c2);
        assertThat(c1).isNotSameAs(c3);
        assertThat(c2).isNotSameAs(c3);
    }

    @Test
    void buildHandlesNullHttpClientAndResilienceGracefully() {
        RestClientProperties props =
                RestClientProperties.builder().httpClient(null).resilience(null).build();

        assertThatNoException()
                .isThrownBy(() -> builder.client("nullProps", props).build());
    }

    @Test
    void buildThrowsWhenHttpClientDisabled() {
        HttpClientProperties disabledHttp =
                HttpClientProperties.builder().enabled(false).build();
        RestClientProperties props =
                RestClientProperties.builder().httpClient(disabledHttp).build();

        assertThatThrownBy(() -> builder.client("disabled", props).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HttpClient configuration is disabled.");
    }
}
