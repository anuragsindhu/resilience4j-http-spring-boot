package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.property.RestClientDefaultSettings;
import com.example.http.autoconfiguration.property.RestClientProperties;
import com.example.http.client.builder.HttpClientConfigurer;
import com.example.http.client.property.HttpClientDefaultSettings;
import com.example.http.client.property.HttpClientProperties;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
            var httpClient = HttpClientConfigurer.configure(httpProps);
            var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(props.getRequestFactory().getConnectTimeout());
            factory.setConnectionRequestTimeout(props.getRequestFactory().getConnectionRequestTimeout());
            factory.setReadTimeout(props.getRequestFactory().getReadTimeout());

            // 3) Obtain resilience configuration
            var resilienceConfig = props.getResilience() != null
                    ? props.getResilience()
                    : RestClientDefaultSettings.defaultResilience();

            // 4) Conditionally resolve resilience instances
            CircuitBreaker cb = resilienceConfig.isCircuitBreakerEnabled()
                    ? ResilienceInstanceFactory.getCircuitBreaker(name, circuitBreakerRegistry, resilienceConfig)
                    : null;
            Retry retry = resilienceConfig.isRetryEnabled()
                    ? ResilienceInstanceFactory.getRetry(name, retryRegistry, resilienceConfig)
                    : null;
            RateLimiter rl = resilienceConfig.isRateLimiterEnabled()
                    ? ResilienceInstanceFactory.getRateLimiter(name, rateLimiterRegistry, resilienceConfig)
                    : null;

            // 5) Only attach interceptor if any resilience is enabled
            var restClientBuilder =
                    RestClient.builder().baseUrl(props.getBaseUrl()).requestFactory(factory);

            boolean shouldConfigureInterceptor = cb != null || retry != null || rl != null;
            if (shouldConfigureInterceptor) {
                Set<HttpStatus> statuses =
                        retry != null ? resilienceConfig.getRetry().getRetryStatus() : Collections.emptySet();

                var interceptor = ResilienceHttpRequestInterceptor.builder(observationRegistry)
                        .clientName(name)
                        .observationTags(props.getObservationTags())
                        .circuitBreaker(cb)
                        .retry(retry)
                        .retryStatus(statuses)
                        .rateLimiter(rl)
                        .build();

                restClientBuilder.requestInterceptor(interceptor);
            }

            // 6) Return built RestClient
            return restClientBuilder.build();
        }
    }
}
