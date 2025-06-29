package com.example.http.autoconfiguration.observation;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.micrometer.observation.Observation;
import java.util.Map;

public final class ResilienceObservationTagContributor {

    public static void contribute(
            Observation observation,
            String clientName,
            CircuitBreaker cb,
            Retry retry,
            RateLimiter rl,
            Map<String, String> customTags) {
        observation
                .lowCardinalityKeyValue("client", clientName)
                .lowCardinalityKeyValue("cb.state", cb != null ? cb.getState().name() : "none")
                .lowCardinalityKeyValue("retry.enabled", retry != null ? "true" : "false")
                .lowCardinalityKeyValue("rl.enabled", rl != null ? "true" : "false");

        if (customTags != null) {
            customTags.forEach(observation::lowCardinalityKeyValue);
        }
    }
}
