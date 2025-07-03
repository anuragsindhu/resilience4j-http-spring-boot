package com.example.http.autoconfiguration;

import com.example.http.autoconfiguration.builder.RestClientBuilder;
import com.example.http.autoconfiguration.properties.RestClientsProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientsProperties.class)
public class ResilientRestClientAutoConfiguration {

    private final RestClientsProperties clientProperties;
    private final RestClientBuilder builder;

    public ResilientRestClientAutoConfiguration(
            ObservationRegistry observationRegistry,
            RestClientsProperties clientProperties,
            CircuitBreakerRegistry cbRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rlRegistry) {

        this.clientProperties = clientProperties;
        this.builder = RestClientBuilder.builder()
                .observationRegistry(observationRegistry)
                .circuitBreakerRegistry(cbRegistry)
                .retryRegistry(retryRegistry)
                .rateLimiterRegistry(rlRegistry)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "resilientRestClients")
    public Map<String, RestClient> resilientRestClients() {
        Map<String, RestClient> clients = new LinkedHashMap<>();
        for (Map.Entry<String, com.example.http.autoconfiguration.properties.RestClientProperties> entry :
                clientProperties.getClients().entrySet()) {

            String name = entry.getKey();
            var props = entry.getValue();
            RestClient client = builder.client(name, props).build();
            clients.put(name, client);
        }
        return clients;
    }
}
