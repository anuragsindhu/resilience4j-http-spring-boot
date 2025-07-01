# Resilient HTTP Client Starter

A Spring Boot starter that provides plug-and-play **resilience features** (Circuit Breaker, Retry, Rate Limiter) and
rich **observability** for HTTP clients, powered by [Resilience4j](https://resilience4j.readme.io/)
and [Micrometer](https://micrometer.io/).

Built to help developers quickly adopt production-grade, fault-tolerant client-side HTTP logic — with sensible defaults
and deep integration into Spring’s `RestClient`.

---

## Features

- Declarative client configuration via `application.yml`
- Resilience4j-based:
    - Circuit Breaker
    - Retry
    - Rate Limiter
- Metrics & tracing via Micrometer `Observation`

---

## 🚀 Getting Started

### 1. Add the starter to your project

```xml

<dependency>
    <groupId>com.example</groupId>
    <artifactId>resilient-http-starter</artifactId>
    <version>X.X.X</version>
</dependency>
```

## Configuration

### Circuit Breaker

| Config  property                             | Default  Value                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|----------------------------------------------|----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| automaticTransitionFromOpenToHalfOpenEnabled | FALSE                                                                      | If set to true it means that the CircuitBreaker will automatically transition from open to half-open state and no call is needed to trigger the transition. A thread is created to monitor all the instances of CircuitBreakers to transition them to HALF_OPEN once waitDurationInOpenState passes. Whereas, if set to false the transition to HALF_OPEN only happens if a call is made, even after waitDurationInOpenState is passed. The advantage here is no thread monitors the state of all CircuitBreakers. |
| failureRateThreshold                         | 50                                                                         | Configures the failure rate threshold in percentage. When the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open and starts short-circuiting calls.                                                                                                                                                                                                                                                                                                                        |
| ~~ignoreExceptionPredicate~~                 | ~~throwable ->  false By default no exception is ignored.~~                | ~~A custom Predicate which evaluates if an exception should be ignored and neither count as a failure nor success. The Predicate must return true if the exception should be ignored.   The Predicate must return false, if the exception should count as a failure.~~                                                                                                                                                                                                                                             |
| ignoreExceptions                             | empty                                                                      | A list of exceptions that are ignored and neither count as a failure nor success.   Any exception matching or inheriting from one of the list will not count as a failure nor success, even if the exceptions is part of recordExceptions.                                                                                                                                                                                                                                                                         |
| maxWaitDurationInHalfOpenState               | 0 [ms]                                                                     | Configures a maximum wait duration which controls the longest amount of time a CircuitBreaker could stay in Half Open state, before it switches to open. Value 0 means Circuit Breaker would wait infinitely in HalfOpen State until all permitted calls have been completed.                                                                                                                                                                                                                                      |
| minimumNumberOfCalls                         | 100                                                                        | Configures the minimum number of calls which are required (per sliding window period) before the CircuitBreaker can calculate the error rate or slow call rate.   For example, if minimumNumberOfCalls is 10, then at least 10 calls must be recorded, before the failure rate can be calculated.   If only 9 calls have been recorded the CircuitBreaker will not transition to open even if all 9 calls have failed.                                                                                             |
| permittedNumberOfCallsInHalfOpenState        | 10                                                                         | Configures the number of permitted calls when the CircuitBreaker is half open.                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| recordExceptions                             | empty                                                                      | A list of exceptions that are recorded as a failure and thus increase the failure rate.   Any exception matching or inheriting from one of the list counts as a failure, unless explicitly ignored via ignoreExceptions.   If you specify a list of exceptions, all other exceptions count as a success, unless they are explicitly ignored by ignoreExceptions.                                                                                                                                                   |
| ~~recordFailurePredicate~~                   | ~~throwable ->  true By default all exceptions are recorded as failures.~~ | ~~A custom Predicate which evaluates if an exception should be recorded as a failure.   The Predicate must return true if the exception should count as a failure. The Predicate must return false, if the  exception should count as a success, unless the exception is explicitly ignored by ignoreExceptions.~~                                                                                                                                                                                                 |
| slidingWindowSize                            | 100                                                                        | Configures the size of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed.                                                                                                                                                                                                                                                                                                                                                                                          |
| slidingWindowType                            | COUNT_BASED                                                                | Configures the type of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed.   Sliding window can either be count-based or time-based.       If the sliding window is COUNT_BASED, the last slidingWindowSize calls are recorded and aggregated.   If the sliding window is TIME_BASED, the calls of the last slidingWindowSize seconds recorded and aggregated.                                                                                                      |
| slowCallDurationThreshold                    | 60000 [ms]                                                                 | Configures the duration threshold above which calls are considered as slow and increase the rate of slow calls.                                                                                                                                                                                                                                                                                                                                                                                                    |
| slowCallRateThreshold                        | 100                                                                        | Configures a threshold in percentage. The CircuitBreaker considers a call as slow when the call duration is greater than  slowCallDurationThreshold When the percentage of slow calls is equal or greater the threshold, the CircuitBreaker transitions to open and starts short-circuiting calls.                                                                                                                                                                                                                 |
| waitDurationInOpenState                      | 60000 [ms]                                                                 | The time that the CircuitBreaker should wait before transitioning from open to half-open.                                                                                                                                                                                                                                                                                                                                                                                                                          |

### Rate Limiter

| Config property    | Default value | Description                                                                                                                   |
|--------------------|---------------|-------------------------------------------------------------------------------------------------------------------------------|
| limitForPeriod     | 50            | The number of permissions available during one limit refresh period                                                           |
| limitRefreshPeriod | 500 [ns]      | The period of a limit refresh. After each period the rate limiter sets its permissions count back to the limitForPeriod value |
| timeoutDuration    | 5 [s]         | The default wait time a thread waits for a permission                                                                         |

### Retry

| Config property              | Default value                                                  | Description                                                                                                                                                                                                   |
|------------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| exponentialBackoffMultiplier | 2.0                                                            | Retry delay doubles with each attempt                                                                                                                                                                         |
| exponentialMaxWaitDuration   | 1 [m]                                                          | Caps individual retry delay at given value                                                                                                                                                                    |
| failAfterMaxAttempts         | FALSE                                                          | A boolean to enable or disable throwing of MaxRetriesExceededException when the Retry has reached the configured maxAttempts, and the result is still not passing the retryOnResultPredicate                  |
| ignoreExceptions             | empty                                                          | Configures a list of Throwable classes that are ignored and thus are not retried. This parameter supports subtyping.                                                                                          |
| ~~intervalBiFunction~~       | ~~(numOfAttempts, Either<throwable, result>) -> waitDuration~~ | ~~A function to modify the waiting interval after a failure based on attempt number and result or exception. When used together with intervalFunction will throw an IllegalStateException.~~                  |
| ~~intervalFunction~~         | ~~numOfAttempts -> waitDuration~~                              | ~~A function to modify the waiting interval after a failure. By default the wait duration remains constant.~~                                                                                                 |
| maxAttempts                  | 3                                                              | The maximum number of attempts (including the initial call as the first attempt)                                                                                                                              |
| randomizedWaitFactor         | 0.25                                                           | Jitter to each retry interval to avoid retry storms                                                                                                                                                           |
| ~~retryExceptionPredicate~~  | ~~throwable -> true~~                                          | ~~Configures a Predicate which evaluates if an exception should be retried. The Predicate must return true, if the exception should be retried, otherwise it must return false.~~                             |
| retryExceptions              | empty                                                          | Configures a list of Throwable classes that are recorded as a failure and thus are retried. This parameter supports subtyping.       Note: If you are using Checked Exceptions you must use a CheckedSupplier |
| ~~retryOnResultPredicate~~   | ~~result -> false~~                                            | ~~Configures a Predicate which evaluates if a result should be retried. The Predicate must return true, if the result should be retried, otherwise it must return false.~~                                    |
| waitDuration                 | 500 [ms]                                                       | A fixed wait duration between retry attempts                                                                                                                                                                  |

### Client Configuration

```yaml
group:
  http:
    clients:
      foo-service:
        base-url: https://api.example.com
        connect-timeout: 2s
        read-timeout: 4s
        resilience:
          retry-enabled: true
          retry:
            max-attempts: 3
            wait-duration: 500ms
          circuit-breaker-enabled: true
          circuit-breaker:
            sliding-window-size: 5
            failure-rate-threshold: 50
          rate-limiter-enabled: true
          rate-limiter:
            limit-for-period: 10
            limit-refresh-period: 1s
      another-service:
        base-url: https://api.another-example.com
        resilience:
          retry-enabled: true
          retry:
            enabled: true
            exponential-backoff-multiplier: 2.0
            exponential-max-wait-duration: 2s
            max-attempts: 5
            randomized-wait-factor: 0.25
            wait-duration: 200ms
```

---

## DEFAULTS

| Configuration Parameter      | Default Value | What It Controls                                             | Why This Value Is Sensible                                                                                          |
|------------------------------|---------------|--------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `connectionRequestTimeout`   | 2 seconds     | Max time to wait to lease a connection from the pool         | Prevents queuing delays in high load; 2s is tight enough for fast-failing systems but tolerant of short contention. |
| `connectTimeout`             | 5 seconds     | Max time to establish a TCP connection                       | Avoids hanging on unreachable hosts or slow networks; 5s gives servers a fair chance without delaying the client.   |
| `evictIdleConnections`       | 1 minute      | Interval to close idle pooled connections                    | Cleans up unused sockets; helps reclaim resources under bursty or low traffic conditions.                           |
| `maxConnPerRoute`            | 20            | Max concurrent connections to a single host                  | Helps avoid host-specific overload while maintaining throughput; aligns with HTTP/1.x best practices.               |
| `maxConnTotal`               | 200           | Max connections across all routes                            | Supports high concurrency and parallelism; ideal for services with multiple endpoints or multitenant APIs.          |
| `rcvBufSize / sndBufSize`    | 8 KB          | Receive/send buffer size at the socket level                 | 8 KB aligns with standard MTU and is large enough for headers and small payloads without over-allocating memory.    |
| `socketTimeout` (a.k.a read) | 10 seconds    | Max time to wait for response data after a request           | Helps detect stalled responses; tuned higher than connection timeout to support slow endpoints or large payloads.   |
| `soLinger`                   | 2 seconds     | Max time to linger in close() before connection force-closes | Allows graceful socket closure; helps flush final ACKs and minimizes half-closed socket issues.                     |
| `tcpNoDelay`                 | `true`        | Disables Nagle’s algorithm — sends data immediately          | Reduces latency for small messages (e.g. JSON over REST); recommended for most client-side HTTP workloads.          |
| `timeToLive`                 | 5 minutes     | Max lifetime of a pooled connection regardless of activity   | Ensures stale connections (e.g. DNS changes, proxy idle) are periodically recycled for fresh connectivity.          |
| `validateAfterInactivity`    | 30 seconds    | Min idle time before validating a connection in the pool     | Prevents using silently closed TCP connections (common issue in long-lived pools or mobile networks).               |


```text
+--------+           +------------------+           +---------+           +------------+
| Client |           | ConnectionManager|           |  Socket |           |   Server   |
+--------+           +------------------+           +---------+           +------------+
    |                        |                          |                      |
    |--- request() --------->|                          |                      |
    |                        |-- leaseConnection() ---->|                      |
    |                        |  [connectionRequestTimeout]                     |
    |                        |                          |                      |
    |                        |                          |-- connect() -------->|
    |                        |                          |  [connectTimeout]    |
    |                        |                          |                      |
    |                        |                          |  TCP Setup           |
    |                        |                          |<---------------------|
    |                        |                          |                      |
    |                        |                          | setSocketOptions()   |
    |                        |                          |  └─ [soTimeout]      |
    |                        |                          |  └─ [rcvBufSize]     |
    |                        |                          |  └─ [sndBufSize]     |
    |                        |                          |  └─ [soLinger]       |
    |                        |                          |  └─ [tcpNoDelay]     |
    |                        |                          |                      |
    |                        |                          |-- send HTTP request  |
    |                        |                          |                      |
    |                        |                          |<- receive response - |
    |                        |                          |  [socketTimeout]     |
    |                        |                          |                      |
    |                        |<- return connection -----|                      |
    |                        |  [validateAfterInactivity]                      |
    |                        |  [timeToLive]                                   |
    |                        |  [evictIdleConnections (background)]            |
    |<-- response ------------|                                                |
    |                        |                                                 |
```

## RESILIENCE4J DEFAULTS

---

### **Retry Configuration Defaults**

| Setting                | Suggested Default         | Why It’s Sensible                                                  |
|------------------------|---------------------------|---------------------------------------------------------------------|
| `maxAttempts`          | `3`                       | Limits retries to reduce load amplification                        |
| `waitDuration`         | `500ms`                   | Reasonable interval before retrying (avoid overwhelming server)     |
| `exponentialBackoff`   | Enabled (`1.5` multiplier) | Prevents thundering herd problems                                  |
| `retryOnResult`        | HTTP 5xx / timeout        | Retries only on recoverable conditions                             |
| `failAfterMaxAttempts` | `true`                    | Surfaces error after N tries, enabling downstream fallback behavior |

In YAML:

```yaml
retry:
  instances:
    default:
      maxAttempts: 3
      waitDuration: 500ms
      exponentialBackoffMultiplier: 1.5
      retryExceptions:
        - org.springframework.web.client.HttpServerErrorException
        - java.io.IOException
```

---

### **Circuit Breaker Configuration Defaults**

| Setting                       | Suggested Default           | Why It Works                                                       |
|-------------------------------|-----------------------------|--------------------------------------------------------------------|
| `slidingWindowType`           | `COUNT_BASED`               | Predictable behavior across low-traffic scenarios                  |
| `slidingWindowSize`           | `20`                        | Enough samples to be meaningful but still responsive               |
| `failureRateThreshold`        | `50`                        | Triggers when half the calls fail                                 |
| `permittedCallsInHalfOpen`    | `3`                         | Small number of test probes after transition                       |
| `minimumNumberOfCalls`        | `10`                        | Avoids flaky behavior with small sample sizes                      |
| `waitDurationInOpenState`     | `10s`                       | Gives dependent service time to recover                            |

In YAML:

```yaml
circuitbreaker:
  instances:
    default:
      slidingWindowSize: 20
      slidingWindowType: COUNT_BASED
      failureRateThreshold: 50
      minimumNumberOfCalls: 10
      permittedNumberOfCallsInHalfOpenState: 3
      waitDurationInOpenState: 10s
```

---

### **Rate Limiter Configuration Defaults**

| Setting                 | Suggested Default     | Why It's Reasonable                                                  |
|-------------------------|-----------------------|-----------------------------------------------------------------------|
| `limitForPeriod`        | `10`                  | Allows bursts of 10 calls                                            |
| `limitRefreshPeriod`    | `1s`                  | Resets quota every second                                            |
| `timeoutDuration`       | `0`                   | Fail fast if no permit available (change if async fallback desired)  |
| `registerHealthIndicator` | `true`              | Exposes state for monitoring tools                                   |

In YAML:

```yaml
ratelimiter:
  instances:
    default:
      limitForPeriod: 10
      limitRefreshPeriod: 1s
      timeoutDuration: 0
```

```java
package com.example.http;

import com.example.http.autoconfiguration.utils.WireMockErrorScenarioBuilder;
import com.example.http.testing.TestRestClient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
        .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    private TestRestClient client;
    private WireMockErrorScenarioBuilder stub;

    @BeforeEach
    void setup() {
        String baseUrl = wiremock.getRuntimeInfo().getHttpBaseUrl();
        client = new TestRestClient(baseUrl);
        stub = WireMockErrorScenarioBuilder.forPort(wiremock.getPort());
    }

    @Test
    void shouldRetryOnServerErrorAndEventuallySucceed() {
        String path = "/resilient";
        stub.withRetryableEndpoint("retry-scenario", path, 2);

        ResponseEntity<String> response = client.get(path, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Recovered");
    }

    @Test
    void shouldTripCircuitBreakerAfterEnoughFailures() {
        String path = "/fail-forever";
        stub.withHttp500(path);

        // Trigger repeated failures
        for (int i = 0; i < 10; i++) {
            try {
                client.get(path, String.class);
            } catch (Exception ignored) {}
        }

        // Next call should be blocked by CircuitBreaker
        assertThatThrownBy(() -> client.get(path, String.class))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("Call is not permitted");
    }

    @Test
    void shouldEnforceRateLimiting() {
        String path = "/limited";
        stub.withStub("GET", path, 200, "ok", java.util.Map.of());

        int allowed = 0;
        int blocked = 0;
        for (int i = 0; i < 20; i++) {
            try {
                client.get(path, String.class);
                allowed++;
            } catch (RestClientException ex) {
                if (ex.getMessage().contains("Request not permitted")) {
                    blocked++;
                }
            }
        }

        assertThat(allowed).isLessThanOrEqualTo(10);
        assertThat(blocked).isGreaterThan(0);
    }

    @Test
    void shouldFailOnNetworkDelayBeyondTimeout() {
        String path = "/timeout";
        stub.withNetworkTimeout(path);

        assertThatThrownBy(() -> client.get(path, String.class))
            .isInstanceOf(RestClientException.class)
            .hasMessageContaining("Read timed out");
    }
}
```

Perfect! With your `WireMockErrorScenarioBuilder` utility now available, we can cleanly and expressively stub out all resilience-related behaviors in tests for `TestRestClient`. Here's the refactored integration test that combines it with the previously discussed sensible defaults for Retry, Circuit Breaker, and Rate Limiter:

---

### `ResilienceIntegrationTest.java`

```java
package com.example.http;

import com.example.http.autoconfiguration.utils.WireMockErrorScenarioBuilder;
import com.example.http.testing.TestRestClient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
        .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    private TestRestClient client;
    private WireMockErrorScenarioBuilder stub;

    @BeforeEach
    void setup() {
        String baseUrl = wiremock.getRuntimeInfo().getHttpBaseUrl();
        client = new TestRestClient(baseUrl);
        stub = WireMockErrorScenarioBuilder.forPort(wiremock.getPort());
    }

    @Test
    void shouldRetryOnServerErrorAndEventuallySucceed() {
        String path = "/resilient";
        stub.withRetryableEndpoint("retry-scenario", path, 2);

        ResponseEntity<String> response = client.get(path, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Recovered");
    }

    @Test
    void shouldTripCircuitBreakerAfterEnoughFailures() {
        String path = "/fail-forever";
        stub.withHttp500(path);

        // Trigger repeated failures
        for (int i = 0; i < 10; i++) {
            try {
                client.get(path, String.class);
            } catch (Exception ignored) {}
        }

        // Next call should be blocked by CircuitBreaker
        assertThatThrownBy(() -> client.get(path, String.class))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("Call is not permitted");
    }

    @Test
    void shouldEnforceRateLimiting() {
        String path = "/limited";
        stub.withStub("GET", path, 200, "ok", java.util.Map.of());

        int allowed = 0;
        int blocked = 0;
        for (int i = 0; i < 20; i++) {
            try {
                client.get(path, String.class);
                allowed++;
            } catch (RestClientException ex) {
                if (ex.getMessage().contains("Request not permitted")) {
                    blocked++;
                }
            }
        }

        assertThat(allowed).isLessThanOrEqualTo(10);
        assertThat(blocked).isGreaterThan(0);
    }

    @Test
    void shouldFailOnNetworkDelayBeyondTimeout() {
        String path = "/timeout";
        stub.withNetworkTimeout(path);

        assertThatThrownBy(() -> client.get(path, String.class))
            .isInstanceOf(RestClientException.class)
            .hasMessageContaining("Read timed out");
    }
}
```

### What This Covers

| Resilience Feature | Builder Method Used                | Test Validates                         |
|--------------------|------------------------------------|----------------------------------------|
| Retry              | `withRetryableEndpoint(...)`       | Success after transient failure        |
| Circuit Breaker    | `withHttp500(...)`                 | Trips after repeated 5xx               |
| Rate Limiter       | `withStub(...)` (rapid loop)       | Enforces `limitForPeriod`              |
| Timeout Handling   | `withNetworkTimeout(...)`          | Fails with socket read timeout         |

---