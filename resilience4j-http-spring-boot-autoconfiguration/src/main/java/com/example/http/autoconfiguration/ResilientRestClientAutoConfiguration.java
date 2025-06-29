package com.example.http.autoconfiguration;

import com.example.http.autoconfiguration.builder.CircuitBreakerFactory;
import com.example.http.autoconfiguration.builder.HttpClientFactory;
import com.example.http.autoconfiguration.builder.RateLimiterFactory;
import com.example.http.autoconfiguration.builder.ResilienceHttpRequestInterceptor;
import com.example.http.autoconfiguration.builder.RetryFactory;
import com.example.http.autoconfiguration.logging.ResilienceEventPublisherLogger;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(ResilientRestClientProperties.class)
@Slf4j
public class ResilientRestClientAutoConfiguration {

    private final ObservationRegistry observationRegistry;
    private final ResilientRestClientProperties properties;
    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rlRegistry;

    public ResilientRestClientAutoConfiguration(
            ObservationRegistry observationRegistry,
            ResilientRestClientProperties properties,
            CircuitBreakerRegistry cbRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rlRegistry) {
        this.observationRegistry = observationRegistry;
        this.properties = properties;
        this.cbRegistry = cbRegistry;
        this.retryRegistry = retryRegistry;
        this.rlRegistry = rlRegistry;
    }

    @Bean
    @ConditionalOnMissingBean(name = "resilientRestClients")
    public Map<String, RestClient> resilientRestClients() {
        Map<String, RestClient> clients = new LinkedHashMap<>();
        for (Map.Entry<String, ResilientRestClientProperties.Client> entry :
                properties.getClients().entrySet()) {

            String name = entry.getKey();
            ResilientRestClientProperties.Client cfg = entry.getValue();

            // 1) HTTP factory (Apache HttpClient with timeouts)
            var factory = HttpClientFactory.createFactory(cfg);

            // 2) Resilience4j components
            var resilience = cfg.getResilience();
            CircuitBreaker cb = resilience.isCircuitBreakerEnabled()
                    ? CircuitBreakerFactory.create(name, cbRegistry, resilience.getCircuitBreaker())
                    : null;

            Retry retry = null;
            Set<HttpStatus> statusSet = Collections.emptySet();
            if (resilience.isRetryEnabled()) {
                // build the Retry from the nested config
                retry = RetryFactory.create(name, retryRegistry, resilience.getRetry());
                // grab the custom statuses
                statusSet = resilience.getRetry().getRetryStatus();
            }

            RateLimiter rl = resilience.isRateLimiterEnabled()
                    ? RateLimiterFactory.create(name, rlRegistry, resilience.getRateLimiter())
                    : null;

            ResilienceEventPublisherLogger.attach(retry, cb, rl, log);

            // 3) Build the RestClient, registering our ClientHttpRequestInterceptor
            ResilienceHttpRequestInterceptor resilienceInterceptor = ResilienceHttpRequestInterceptor.builder(
                            observationRegistry)
                    .circuitBreaker(cb)
                    .clientName(name)
                    .observationTags(cfg.getObservationTags())
                    .rateLimiter(rl)
                    .retry(retry)
                    .retryStatus(statusSet)
                    .build();

            RestClient client = RestClient.builder()
                    .baseUrl(cfg.getBaseUrl())
                    .requestFactory(factory)
                    .requestInterceptor(resilienceInterceptor)
                    .build();

            clients.put(name, client);
        }
        return clients;
    }
}
