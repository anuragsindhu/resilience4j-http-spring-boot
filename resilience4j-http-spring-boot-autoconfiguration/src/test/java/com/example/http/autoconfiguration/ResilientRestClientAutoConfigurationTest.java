package com.example.http.autoconfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.autoconfiguration.property.RestClientProperties;
import com.example.http.autoconfiguration.property.RestClientsProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ResilientRestClientAutoConfigurationTest {

    private RestClientsProperties clientsProperties;
    private ObservationRegistry observationRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;
    private RateLimiterRegistry rateLimiterRegistry;

    @BeforeEach
    void setup() {
        observationRegistry = ObservationRegistry.create();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        clientsProperties = new RestClientsProperties();
        clientsProperties.setClients(new LinkedHashMap<>());

        // Define two mock clients with basic baseUrls
        RestClientProperties clientA = new RestClientProperties();
        clientA.setBaseUrl("https://api.clientA.com");
        clientsProperties.getClients().put("clientA", clientA);

        RestClientProperties clientB = new RestClientProperties();
        clientB.setBaseUrl("https://api.clientB.com");
        clientsProperties.getClients().put("clientB", clientB);
    }

    @Test
    void shouldCreateConfiguredRestClients() {
        var config = new ResilientRestClientAutoConfiguration(
                observationRegistry, clientsProperties, circuitBreakerRegistry, retryRegistry, rateLimiterRegistry);

        Map<String, RestClient> clients = config.resilientRestClients();

        assertThat(clients).hasSize(2);
        assertThat(clients).containsKeys("clientA", "clientB");
        assertThat(clients.get("clientA")).isNotNull();
        assertThat(clients.get("clientB")).isNotNull();
    }

    @Test
    void shouldReturnEmptyMapWhenNoClientsProvided() {
        RestClientsProperties emptyProps = new RestClientsProperties();
        emptyProps.setClients(new LinkedHashMap<>()); // explicitly initialize

        var config = new ResilientRestClientAutoConfiguration(
                observationRegistry, emptyProps, circuitBreakerRegistry, retryRegistry, rateLimiterRegistry);

        Map<String, RestClient> clients = config.resilientRestClients();

        assertThat(clients).isEmpty();
    }
}
