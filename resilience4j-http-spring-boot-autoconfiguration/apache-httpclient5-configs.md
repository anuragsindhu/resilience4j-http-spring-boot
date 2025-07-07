# HTTP Client Configurations

## HttpComponentsClientHttpRequestFactory

`HttpComponentsClientHttpRequestFactory` from Spring Framework acts as a bridge between Spring's `RestClient` and Apache
`HttpClient`

### Timeout Settings in `HttpComponentsClientHttpRequestFactory`

| Method                                  | Purpose (with HttpClient Mapping)                                                                                                                                       | Default Value  | Rationale                                                                          | Usage                                                                     |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| `setConnectTimeout(Duration)`           | Time to establish a TCP connection to the target server. <br> Maps to `ConnectionConfig.setConnectTimeout` in Apache HttpClient 5 when building the connection manager. | `0` (infinite) | Prevents the client from hanging indefinitely when the target host is unreachable. | Set to 1–2 seconds to fail fast on unreachable or slow hosts.             |
| `setConnectionRequestTimeout(Duration)` | Time to wait for a connection from the connection pool. <br> Maps to `RequestConfig.setConnectionRequestTimeout` in Apache HttpClient 5.                                | `0` (infinite) | Avoids thread starvation when all pooled connections are in use.                   | Set to 500ms–1 second in high-concurrency environments to avoid blocking. |
| `setReadTimeout(Duration)`              | Time to wait for the full response after the request is sent. <br> Maps to `RequestConfig.setResponseTimeout` in Apache HttpClient 5.                                   | `0` (infinite) | Ensures the client doesn't hang waiting for a slow or unresponsive server.         | Set to 3–5 seconds depending on expected response time and SLA.           |

## Apache HttpClient5 Configurations

### Request Configuration Parameters

These settings operate at the Request level to fine tune behaviour per request.

| Configuration                     | Purpose                                                                 | Default Value      | Rationale                                                                  | Usage                                                                    |
|-----------------------------------|-------------------------------------------------------------------------|--------------------|----------------------------------------------------------------------------|--------------------------------------------------------------------------|
| **`setConnectionRequestTimeout`** | Time to wait for a connection from the connection pool.                 | 3 minutes          | Prevents threads from waiting indefinitely when the pool is exhausted.     | Important in high-load scenarios where pooled connections may be scarce. |
| **`setResponseTimeout`**          | Maximum time to wait for the entire response after the request is sent. | Not set (infinite) | Ensures the client doesn't hang waiting for a slow or unresponsive server. | Ideal for setting a hard timeout for the full request-response cycle.    |

### Connection Configuration Parameters

These settings operate at the Connection management level, influencing the lifecycle of HTTP connections in the pool.

| Configuration                    | Purpose                                                                           | Default                                                           | Rationale                                                                       | Usage                                                                                    |
|----------------------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------|---------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| **`setConnectTimeout`**          | Sets the maximum time to establish a connection to the target server.             | Not explicitly set; defaults to system-level or infinite.         | Prevents the client from hanging indefinitely when the server is unreachable.   | Useful in high-availability systems to fail fast and retry or fallback.                  |
| **`setSocketTimeout`**           | Sets the maximum time to wait for data after the connection is established.       | Not explicitly set; defaults to system-level or infinite.         | Ensures that the client doesn't hang waiting for a slow or unresponsive server. | Critical for APIs where response time is important.                                      |
| **`setTimeToLive`**              | Sets the maximum lifespan of a persistent connection, regardless of activity.     | Not set by default (connections live indefinitely unless closed). | Prevents stale connections from being reused beyond a safe period.              | Helps avoid issues with long-lived connections in load balancers or proxies.             |
| **`setValidateAfterInactivity`** | Sets the period of inactivity after which a connection is validated before reuse. | Not set by default.                                               | Ensures idle connections are still valid before reuse, avoiding I/O errors.     | Important in environments with firewalls or proxies that silently drop idle connections. |

### Socket Configuration Parameters

These settings operate at the TCP socket level, influencing how data is buffered, transmitted, and received.

| Configuration       | Purpose                                                     | Default       | Rationale                                                                                                    | Usage                                                                                  |
|---------------------|-------------------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| **`setRcvBufSize`** | Sets the size of the socket receive buffer (SO_RCVBUF).     | OS default    | Controls how much data can be buffered when receiving. Larger buffers help with high-throughput connections. | Tune for high-latency or high-bandwidth networks to avoid packet loss or throttling.   |
| **`setSoLinger`**   | Sets the linger time (SO_LINGER) for socket close behavior. | -1 (disabled) | Controls how long the socket will block on `close()` to ensure data is sent.                                 | Use with caution; can block threads. Useful when graceful shutdown of TCP is critical. |
| **`setSoTimeout`**  | Sets the socket read timeout (SO_TIMEOUT).                  | 0 (infinite)  | Prevents indefinite blocking when waiting for data.                                                          | Essential for responsiveness in real-time or user-facing applications.                 |
| **`setSndBufSize`** | Sets the size of the socket send buffer (SO_SNDBUF).        | OS default    | Controls how much data can be buffered when sending. Larger buffers help with bursty traffic.                | Tune for high-throughput uploads or streaming scenarios.                               |
| **`setTcpNoDelay`** | Enables/disables TCP_NODELAY (disables Nagle’s algorithm).  | `true`        | Reduces latency by sending packets immediately without waiting to batch small messages.                      | Ideal for low-latency applications like chat, gaming, or real-time APIs.               |

# Resilience4j Configurations for Http Client

## Circuit Breaker Configurations

| Configuration Key                                     | Default Value | Sensible Default                                                               | Purpose                                                                 | Rationale                                                                            |
|-------------------------------------------------------|---------------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| `automatic-transition-from-open-to-half-open-enabled` | `false`       | `true`                                                                         | Whether to automatically try recovery after open state                  | Enables self-healing without requiring manual intervention                           |
| `failure-rate-threshold`                              | `50`          | `50`                                                                           | Percentage of failed calls to trigger circuit breaker                   | 50% is a balanced threshold to detect instability without being too sensitive        |
| `ignore-exceptions`                                   | `[]`          | `java.lang.IllegalArgumentException`, `jakarta.validation.ValidationException` | Exceptions that should not be counted as failures                       | Avoids triggering circuit breaker on client-side or validation errors                |
| `max-wait-duration-in-half-open-state`                | `null`        | `5s`                                                                           | Max time to wait for test calls in HALF_OPEN before reverting to OPEN   | Prevents the breaker from stalling in HALF_OPEN if test calls are delayed or missing |
| `minimum-number-of-calls`                             | `100`         | `10`                                                                           | Minimum number of calls before evaluating failure rate                  | Prevents premature triggering in low-traffic systems                                 |
| `permitted-number-of-calls-in-half-open-state`        | `10`          | `3`                                                                            | Number of trial calls allowed when transitioning from OPEN to HALF_OPEN | 3 is enough to test recovery without flooding the backend                            |
| `record-exceptions`                                   | `[]`          | `[]`                                                                           | Exceptions that should be considered failures                           | Targets transient failures that are retryable or recoverable                         |
| `sliding-window-size`                                 | `100`         | `10`                                                                           | Number of calls to evaluate in the sliding window                       | 10 gives fast feedback in high-throughput systems                                    |
| `sliding-window-type`                                 | `COUNT_BASED` | `COUNT_BASED`                                                                  | Whether to use count-based or time-based sliding window                 | Count-based is simpler and more predictable                                          |
| `slow-call-duration-threshold`                        | `60s`         | `2s`                                                                           | Duration beyond which a call is considered slow                         | 2s is a good threshold for latency-sensitive services                                |
| `slow-call-rate-threshold`                            | `100`         | `100`                                                                          | Percentage of slow calls to trigger circuit breaker                     | Use only if slow calls are critical; 100 disables this trigger by default            |
| `wait-duration-in-open-state`                         | `60s`         | `10s`                                                                          | Time to wait before transitioning from OPEN to HALF_OPEN                | 10s allows quick recovery while still giving time to stabilize                       |

## Rate Limiter Configurations

| Configuration Key              | Default Value | Sensible Default | Purpose                                                           | Rationale                                                              |
|--------------------------------|---------------|------------------|-------------------------------------------------------------------|------------------------------------------------------------------------|
| `limit-for-period`             | `50`          | `10`             | Number of permissions available per refresh period                | Prevents overloading downstream systems; 10 is a safe starting point   |
| `limit-refresh-period`         | `500ns`       | `1s`             | Time window in which `limit-for-period` permissions are available | 1s window is intuitive and aligns with typical rate-limiting semantics |
| `timeout-duration`             | `5s`          | `500ms`          | Max time to wait for a permission before failing                  | Keeps callers responsive; avoids long blocking under high contention   |
| `writable-stack-trace-enabled` | `true`        | `false`          | Whether to include full stack trace in exception messages         | Reduces overhead in high-throughput systems                            |

## Retry Configurations

| Configuration Key                | Default Value | Sensible Default                                                                     | Purpose                                                                        | Rationale                                                                      |
|----------------------------------|---------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `exponential-backoff-multiplier` | N/A           | `2.0`                                                                                | Multiplies the wait duration exponentially after each retry attempt.           | Reduces retry pressure on downstream systems by spacing out retries.           |
| `exponential-max-wait-duration`  | N/A           | `10s`                                                                                | Caps the maximum wait duration when using exponential backoff.                 | Prevents unbounded delays and keeps retry latency predictable.                 |
| `fail-after-max-attempts`        | `false`       | `true`                                                                               | Whether to throw `MaxRetriesExceededException` after the final failed attempt. | Improves observability and allows fallback logic to trigger explicitly.        |
| `ignore-exceptions`              | `[]`          | - `java.lang.IllegalArgumentException`<br>- `jakarta.validation.ValidationException` | Defines exceptions that should not trigger a retry.                            | Avoids retrying on client-side or logic errors that are not recoverable.       |
| `max-attempts`                   | `3`           | `4`                                                                                  | Total number of attempts (initial + retries).                                  | Balances retry effort with latency and system load.                            |
| `randomized-wait-factor`         | `0.0`         | `0.5`                                                                                | Adds jitter to the wait duration to randomize retry intervals.                 | Prevents retry storms and reduces contention in distributed systems.           |
| `retry-exceptions`               | `[]`          | - `java.io.IOException`<br>- `java.util.concurrent.TimeoutException`                 | Defines exceptions that should trigger a retry.                                | Targets transient failures that are likely to succeed on a subsequent attempt. |
| `wait-duration`                  | `500ms`       | `1s`                                                                                 | Fixed delay between retry attempts (if no backoff is used).                    | Allows brief recovery time between retries without overwhelming the system.    |
