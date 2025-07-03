package com.example.http.autoconfiguration.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.micrometer.observation.Observation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResilienceObservationTagContributorTest {

    private Observation observationMock;

    @BeforeEach
    void setUp() {
        observationMock = mock(Observation.class);
        when(observationMock.lowCardinalityKeyValue(anyString(), anyString())).thenReturn(observationMock);
    }

    @Test
    void shouldContributeAllTagsWhenComponentsArePresent() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        when(cb.getState()).thenReturn(State.CLOSED);

        Retry retry = mock(Retry.class);
        RateLimiter rl = mock(RateLimiter.class);
        Map<String, String> customTags = Map.of("env", "test", "tier", "gold");

        ResilienceObservationTagContributor.contribute(observationMock, "ApiClient", cb, retry, rl, customTags);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(observationMock, atLeast(1)).lowCardinalityKeyValue(keyCaptor.capture(), valueCaptor.capture());

        Map<String, String> tags = zipToMap(keyCaptor.getAllValues(), valueCaptor.getAllValues());

        assertThat(tags)
                .containsEntry("client", "ApiClient")
                .containsEntry("cb.state", "CLOSED")
                .containsEntry("retry.enabled", "true")
                .containsEntry("rl.enabled", "true")
                .containsEntry("env", "test")
                .containsEntry("tier", "gold");
    }

    @Test
    void shouldHandleNullComponentsGracefully() {
        ResilienceObservationTagContributor.contribute(observationMock, "SimpleClient", null, null, null, null);

        verify(observationMock).lowCardinalityKeyValue("client", "SimpleClient");
        verify(observationMock).lowCardinalityKeyValue("cb.state", "none");
        verify(observationMock).lowCardinalityKeyValue("retry.enabled", "false");
        verify(observationMock).lowCardinalityKeyValue("rl.enabled", "false");
    }

    private Map<String, String> zipToMap(List<String> keys, List<String> values) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(keys.size(), values.size()); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }
}
