package com.example.http.autoconfiguration;

import com.example.http.autoconfiguration.builder.HttpClientConfigurer;
import com.example.http.autoconfiguration.builder.ResilienceHttpRequestInterceptor;
import com.example.http.autoconfiguration.builder.ResilienceInstanceFactory;
import com.example.http.autoconfiguration.properties.HttpClientDefaultSettings;
import com.example.http.autoconfiguration.properties.HttpClientProperties;
import com.example.http.autoconfiguration.properties.RestClientsProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryProperties;
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
@EnableConfigurationProperties(RestClientsProperties.class)
@Slf4j
public class ResilientRestClientAutoConfiguration {

    private final ObservationRegistry observationRegistry;
    private final RestClientsProperties clientProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ResilientRestClientAutoConfiguration(
            ObservationRegistry observationRegistry,
            RestClientsProperties clientProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryProperties retryProperties,
            RateLimiterProperties rateLimiterProperties) {

        this.observationRegistry = observationRegistry;
        this.clientProperties = clientProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Bean
    @ConditionalOnMissingBean(name = "resilientRestClients")
    public Map<String, RestClient> resilientRestClients() {
        Map<String, RestClient> clients = new LinkedHashMap<>();

        clientProperties.getClients().forEach((name, props) -> {
            HttpClientProperties httpProps = props.getHttpClient() != null
                    ? props.getHttpClient()
                    : HttpClientDefaultSettings.defaultHttpClient();

            var factory = HttpClientConfigurer.configure(httpProps);

            CircuitBreaker circuitBreaker =
                    ResilienceInstanceFactory.getCircuitBreaker(name, circuitBreakerRegistry, props.getResilience());

            Retry retry = ResilienceInstanceFactory.getRetry(name, retryRegistry, props.getResilience());

            RateLimiter rateLimiter =
                    ResilienceInstanceFactory.getRateLimiter(name, rateLimiterRegistry, props.getResilience());

            Set<HttpStatus> retryStatusSet = props.getResilience().isRetryEnabled()
                    ? props.getResilience().getRetry().getRetryStatus()
                    : Collections.emptySet();

            var interceptor = ResilienceHttpRequestInterceptor.builder(observationRegistry)
                    .clientName(name)
                    .observationTags(props.getObservationTags())
                    .circuitBreaker(circuitBreaker)
                    .retry(retry)
                    .retryStatus(retryStatusSet)
                    .rateLimiter(rateLimiter)
                    .build();

            RestClient client = RestClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .requestFactory(factory)
                    .requestInterceptor(interceptor)
                    .build();

            clients.put(name, client);
            log.debug("Configured resilient RestClient for '{}'", name);
        });

        return clients;
    }
}
