package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.properties.HttpClientDefaultSettings;
import com.example.http.autoconfiguration.properties.HttpClientProperties;
import com.example.http.autoconfiguration.properties.RestClientDefaultSettings;
import com.example.http.autoconfiguration.properties.RestClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collections;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

public class RestClientBuilder {

    private final ObservationRegistry observationRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    private RestClientBuilder(
            ObservationRegistry observationRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry) {

        this.observationRegistry = observationRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    /**
     * Entry point to supply the shared dependencies.
     */
    public static DependenciesBuilder builder() {
        return new DependenciesBuilder();
    }

    /**
     * After dependencies are set, you get back a RestClientBuilder you can
     * use to build individual RestClient instances.
     */
    public static class DependenciesBuilder {
        private ObservationRegistry observationRegistry;
        private CircuitBreakerRegistry circuitBreakerRegistry;
        private RetryRegistry retryRegistry;
        private RateLimiterRegistry rateLimiterRegistry;

        public DependenciesBuilder observationRegistry(ObservationRegistry r) {
            this.observationRegistry = r;
            return this;
        }

        public DependenciesBuilder circuitBreakerRegistry(CircuitBreakerRegistry r) {
            this.circuitBreakerRegistry = r;
            return this;
        }

        public DependenciesBuilder retryRegistry(RetryRegistry r) {
            this.retryRegistry = r;
            return this;
        }

        public DependenciesBuilder rateLimiterRegistry(RateLimiterRegistry r) {
            this.rateLimiterRegistry = r;
            return this;
        }

        public RestClientBuilder build() {
            return new RestClientBuilder(
                    observationRegistry, circuitBreakerRegistry, retryRegistry, rateLimiterRegistry);
        }
    }

    /**
     * Start configuring a single RestClient by client name + its properties.
     */
    public ClientBuilder client(String name, RestClientProperties props) {
        return new ClientBuilder(name, props);
    }

    public class ClientBuilder {
        private final String name;
        private final RestClientProperties props;

        private ClientBuilder(String name, RestClientProperties props) {
            this.name = name;
            this.props = props;
        }

        public RestClient build() {
            // 1) Resolve HTTP‐client properties
            HttpClientProperties httpProps = props.getHttpClient() != null
                    ? props.getHttpClient()
                    : HttpClientDefaultSettings.defaultHttpClient();

            // 2) Build underlying request‐factory
            var factory = HttpClientConfigurer.configure(httpProps);

            // 3) Obtain resilience instances
            var resilienceConfig = props.getResilience() != null
                    ? props.getResilience()
                    : RestClientDefaultSettings.defaultResilience();
            CircuitBreaker cb =
                    ResilienceInstanceFactory.getCircuitBreaker(name, circuitBreakerRegistry, resilienceConfig);
            Retry retry = ResilienceInstanceFactory.getRetry(name, retryRegistry, resilienceConfig);
            RateLimiter rl = ResilienceInstanceFactory.getRateLimiter(name, rateLimiterRegistry, resilienceConfig);

            // 4) Decide which statuses to retry
            Set<HttpStatus> statuses = resilienceConfig.isRetryEnabled()
                    ? resilienceConfig.getRetry().getRetryStatus()
                    : Collections.emptySet();

            // 5) Build the interceptor
            var interceptor = ResilienceHttpRequestInterceptor.builder(observationRegistry)
                    .clientName(name)
                    .observationTags(props.getObservationTags())
                    .circuitBreaker(cb)
                    .retry(retry)
                    .retryStatus(statuses)
                    .rateLimiter(rl)
                    .build();

            // 6) Final RestClient
            return RestClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .requestFactory(factory)
                    .requestInterceptor(interceptor)
                    .build();
        }
    }
}
