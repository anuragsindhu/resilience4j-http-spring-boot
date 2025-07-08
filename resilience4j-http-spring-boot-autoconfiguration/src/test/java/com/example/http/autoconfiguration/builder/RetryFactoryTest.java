package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryProperties;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class RetryFactoryTest {

    @Test
    void shouldReturnNullIfPropertiesAreNull() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        Retry retry = RetryFactory.create("null-config", registry, null);

        assertThat(retry).isNull();
    }

    @Test
    void shouldCreateRetryWithBasicMaxAttemptsAndWait() throws Exception {
        RetryProperties.InstanceProperties props = new RetryProperties.InstanceProperties();
        props.setExponentialBackoffMultiplier(2.0);
        props.setMaxAttempts(3);
        props.setRandomizedWaitFactor(0.5);
        props.setWaitDuration(Duration.ofMillis(200));

        RetryRegistry registry = RetryRegistry.ofDefaults();
        Retry retry = RetryFactory.create("basic-config", registry, props);

        Callable<String> decorated = Retry.decorateCallable(retry, () -> {
            throw new ConnectException("Simulated failure");
        });

        assertThatThrownBy(decorated::call).isInstanceOf(ConnectException.class);

        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(retry.getRetryConfig().getIntervalBiFunction().apply(1, null))
                .isEqualTo(200L);
    }

    @Test
    void shouldApplyExponentialBackoffWithRandomization() {
        RetryProperties.InstanceProperties props = new RetryProperties.InstanceProperties();
        props.setWaitDuration(Duration.ofMillis(100));
        props.setExponentialBackoffMultiplier(2.5);
        props.setRandomizedWaitFactor(0.3);
        props.setExponentialMaxWaitDuration(Duration.ofMillis(1000));

        RetryRegistry registry = RetryRegistry.ofDefaults();
        Retry retry = RetryFactory.create("backoff-config", registry, props);

        var backoffFn = retry.getRetryConfig().getIntervalBiFunction();

        long firstDelay = backoffFn.apply(1, null);
        long secondDelay = backoffFn.apply(2, null);

        assertThat(firstDelay).isGreaterThanOrEqualTo(100L).isLessThanOrEqualTo(1000L);

        assertThat(secondDelay).isGreaterThanOrEqualTo(firstDelay).isLessThanOrEqualTo(1000L);
    }

    @Test
    void shouldRespectRetryAndIgnoreExceptionsBehaviorally() throws Exception {
        RetryProperties.InstanceProperties props = new RetryProperties.InstanceProperties();
        props.setRetryExceptions(new Class[] {ConnectException.class});
        props.setIgnoreExceptions(new Class[] {IllegalArgumentException.class});
        props.setMaxAttempts(2);
        props.setWaitDuration(Duration.ofMillis(100));

        RetryRegistry registry = RetryRegistry.ofDefaults();
        Retry retry = RetryFactory.create("exception-config", registry, props);

        Callable<String> ignoredSupplier = Retry.decorateCallable(retry, () -> {
            throw new IllegalArgumentException("Should not be retried");
        });

        Callable<String> retryableSupplier = Retry.decorateCallable(retry, () -> {
            throw new ConnectException("Should be retried");
        });

        assertThatThrownBy(ignoredSupplier::call).isInstanceOf(IllegalArgumentException.class);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
                .isEqualTo(1);

        assertThatThrownBy(retryableSupplier::call).isInstanceOf(ConnectException.class);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }
}
