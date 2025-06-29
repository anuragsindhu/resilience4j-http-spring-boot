# Resilient HTTP Client Starter

A Spring Boot starter that provides plug-and-play **resilience features** (Circuit Breaker, Retry, Rate Limiter) and rich **observability** for HTTP clients, powered by [Resilience4j](https://resilience4j.readme.io/) and [Micrometer](https://micrometer.io/).

Built to help developers quickly adopt production-grade, fault-tolerant client-side HTTP logic — with sensible defaults and deep integration into Spring’s `RestClient`.

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

| Config property             | Default value                                                  | Description                                                                                                                                                                                                   |
|-----------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| failAfterMaxAttempts        | FALSE                                                          | A boolean to enable or disable throwing of MaxRetriesExceededException when the Retry has reached the configured maxAttempts, and the result is still not passing the retryOnResultPredicate                  |
| ignoreExceptions            | empty                                                          | Configures a list of Throwable classes that are ignored and thus are not retried. This parameter supports subtyping.                                                                                          |
| ~~intervalBiFunction~~      | ~~(numOfAttempts, Either<throwable, result>) -> waitDuration~~ | ~~A function to modify the waiting interval after a failure based on attempt number and result or exception. When used together with intervalFunction will throw an IllegalStateException.~~                  |
| ~~intervalFunction~~        | ~~numOfAttempts -> waitDuration~~                              | ~~A function to modify the waiting interval after a failure. By default the wait duration remains constant.~~                                                                                                 |
| maxAttempts                 | 3                                                              | The maximum number of attempts (including the initial call as the first attempt)                                                                                                                              |
| ~~retryExceptionPredicate~~ | ~~throwable -> true~~                                          | ~~Configures a Predicate which evaluates if an exception should be retried. The Predicate must return true, if the exception should be retried, otherwise it must return false.~~                             |
| retryExceptions             | empty                                                          | Configures a list of Throwable classes that are recorded as a failure and thus are retried. This parameter supports subtyping.       Note: If you are using Checked Exceptions you must use a CheckedSupplier |
| ~~retryOnResultPredicate~~  | ~~result -> false~~                                            | ~~Configures a Predicate which evaluates if a result should be retried. The Predicate must return true, if the result should be retried, otherwise it must return false.~~                                    |
| waitDuration                | 500 [ms]                                                       | A fixed wait duration between retry attempts                                                                                                                                                                  |

### Client Configuration

```yaml
group:
  http:
    clients:
      my-service:
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
```