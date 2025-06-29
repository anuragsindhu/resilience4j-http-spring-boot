package com.example.http.autoconfiguration.builder;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

public class ResilienceHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final ObservationRegistry registry;
    private final String clientName;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final Set<HttpStatus> retryStatus;

    private ResilienceHttpRequestInterceptor(Builder b) {
        this.registry = b.registry;
        this.clientName = b.clientName;
        this.circuitBreaker = b.circuitBreaker;
        this.retry = b.retry;
        this.rateLimiter = b.rateLimiter;
        this.retryStatus = b.retryStatus;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        return Objects.requireNonNull(Observation.createNotStarted("http.client.request.resilient", registry)
                .lowCardinalityKeyValue("client", clientName)
                .lowCardinalityKeyValue("http.method", request.getMethod().name())
                .lowCardinalityKeyValue("http.uri", request.getURI().getPath())
                .observeChecked(() -> {
                    // Build the supplier that may throw to trigger retry
                    CheckedSupplier<ClientHttpResponse> supplier = () -> {
                        ClientHttpResponse rsp = execution.execute(request, body);
                        HttpStatusCode code = rsp.getStatusCode();

                        boolean shouldRetry;
                        if (retryStatus == null || retryStatus.isEmpty()) {
                            shouldRetry = code.is5xxServerError();
                        } else {
                            HttpStatus hs = HttpStatus.resolve(code.value());
                            shouldRetry = hs != null && retryStatus.contains(hs);
                        }

                        if (shouldRetry) {
                            HttpStatus hs = HttpStatus.valueOf(code.value());
                            throw new HttpServerErrorException(hs, "Server error (retryable): " + hs.value());
                        }
                        return rsp;
                    };

                    // Decorate Retry → CircuitBreaker → RateLimiter
                    if (retry != null) {
                        // innermost: retry a few times on transient failures
                        supplier = Retry.decorateCheckedSupplier(retry, supplier);
                    }
                    if (circuitBreaker != null) {
                        // next: one “logical call” per invocation counted toward CB
                        supplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier);
                    }
                    if (rateLimiter != null) {
                        // outermost: one permit consumed per invocation (not per retry)
                        supplier = RateLimiter.decorateCheckedSupplier(rateLimiter, supplier);
                    }

                    // Execute and unwrap
                    try {
                        return supplier.get();
                    } catch (UncheckedIOException uio) {
                        throw uio.getCause();
                    } catch (CallNotPermittedException | HttpStatusCodeException | RequestNotPermitted ex) {
                        throw ex;
                    } catch (Throwable t) {
                        if (t.getCause() instanceof CallNotPermittedException cnpe) {
                            throw cnpe;
                        }
                        if (t.getCause() instanceof RequestNotPermitted rnpe) {
                            throw rnpe;
                        }
                        throw new RestClientException("Resilience4j call failed", t);
                    }
                }));
    }

    public static Builder builder(ObservationRegistry registry) {
        return new Builder(Objects.requireNonNull(registry, "ObservationRegistry must not be null"));
    }

    public static class Builder {
        private final ObservationRegistry registry;
        private String clientName;
        private CircuitBreaker circuitBreaker;
        private Retry retry;
        private RateLimiter rateLimiter;
        private Set<HttpStatus> retryStatus;

        private Builder(ObservationRegistry registry) {
            this.registry = registry;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder circuitBreaker(CircuitBreaker cb) {
            this.circuitBreaker = cb;
            return this;
        }

        public Builder retry(Retry r) {
            this.retry = r;
            return this;
        }

        public Builder rateLimiter(RateLimiter rl) {
            this.rateLimiter = rl;
            return this;
        }

        public Builder retryStatus(Set<HttpStatus> statuses) {
            this.retryStatus = statuses;
            return this;
        }

        public ResilienceHttpRequestInterceptor build() {
            Objects.requireNonNull(clientName, "clientName is required");
            return new ResilienceHttpRequestInterceptor(this);
        }
    }
}
