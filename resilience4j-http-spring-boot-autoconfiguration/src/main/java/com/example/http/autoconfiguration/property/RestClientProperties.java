package com.example.http.autoconfiguration.property;

import com.example.http.client.property.HttpClientProperties;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.http.HttpStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestClientProperties {

    @Builder.Default
    private boolean enabled = true;

    private String baseUrl;

    @Builder.Default
    private String clientName = "default";

    @Builder.Default
    @NestedConfigurationProperty
    private HttpClientProperties httpClient = HttpClientProperties.builder().build();

    @Builder.Default
    private Map<String, String> observationTags = new HashMap<>();

    @Builder.Default
    @NestedConfigurationProperty
    private Resilience resilience = RestClientDefaultSettings.defaultResilience();

    @Builder.Default
    private RequestFactory requestFactory = RestClientDefaultSettings.defaultRequestFactory();

    public static RestClientProperties defaultConfig() {
        return RestClientProperties.builder().build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resilience {

        @Builder.Default
        private boolean circuitBreakerEnabled = false;

        @Builder.Default
        private boolean retryEnabled = false;

        @Builder.Default
        private boolean rateLimiterEnabled = false;

        @Builder.Default
        private CircuitBreakerProperties.InstanceProperties circuitBreaker =
                RestClientDefaultSettings.defaultCircuitBreakerProperties();

        @Builder.Default
        private RateLimiterProperties.InstanceProperties rateLimiter =
                RestClientDefaultSettings.defaultRateLimiterProperties();

        @Builder.Default
        private RetryWrapper retry = RestClientDefaultSettings.defaultRetryWrapper();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestFactory {

        @Builder.Default
        private Duration connectTimeout = Duration.ofSeconds(5);

        @Builder.Default
        private Duration connectionRequestTimeout = Duration.ofSeconds(2);

        @Builder.Default
        private Duration readTimeout = Duration.ofSeconds(10);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class RetryWrapper extends CommonRetryConfigurationProperties.InstanceProperties {

        @Builder.Default
        private Set<HttpStatus> retryStatus = Set.of(
                HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.GATEWAY_TIMEOUT);
    }
}
