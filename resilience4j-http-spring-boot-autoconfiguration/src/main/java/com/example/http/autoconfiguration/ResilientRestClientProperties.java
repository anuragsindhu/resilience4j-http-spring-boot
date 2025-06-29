package com.example.http.autoconfiguration;

import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.http.HttpStatus;

@ConfigurationProperties(value = "group.http", ignoreUnknownFields = false)
@Getter
@Setter
public class ResilientRestClientProperties {

    private Map<String, Client> clients = new HashMap<>();

    @Getter
    @Setter
    public static class Client {
        private String baseUrl;

        private Duration connectTimeout = Duration.ofSeconds(5);

        private Duration readTimeout = Duration.ofSeconds(5);

        @NestedConfigurationProperty
        private Resilience resilience = new Resilience();
    }

    @Getter
    @Setter
    public static class Resilience {
        private boolean circuitBreakerEnabled = false;

        private boolean retryEnabled = false;

        private boolean rateLimiterEnabled = false;

        @NestedConfigurationProperty
        private CircuitBreakerProperties.InstanceProperties circuitBreaker =
                new CircuitBreakerProperties.InstanceProperties();

        @NestedConfigurationProperty
        private RetryWrapper retry = new RetryWrapper();

        @NestedConfigurationProperty
        private RateLimiterProperties.InstanceProperties rateLimiter = new RateLimiterProperties.InstanceProperties();
    }

    @Getter
    @Setter
    public static class RetryWrapper extends RetryProperties.InstanceProperties {

        private Set<HttpStatus> retryStatus = new HashSet<>();
    }
}
