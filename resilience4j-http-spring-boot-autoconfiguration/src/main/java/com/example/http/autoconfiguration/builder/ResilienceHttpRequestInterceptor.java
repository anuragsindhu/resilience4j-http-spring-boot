package com.example.http.autoconfiguration.builder;

import com.example.http.autoconfiguration.logging.ResilienceEventPublisherLogger;
import com.example.http.autoconfiguration.observation.ResilienceObservationTagContributor;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
public class ResilienceHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final ObservationRegistry registry;
    private final Map<String, String> observationTags;
    private final String clientName;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final Set<HttpStatus> retryStatus;

    private ResilienceHttpRequestInterceptor(Builder builder) {
        this.registry = builder.registry;
        this.clientName = builder.clientName;
        this.observationTags = builder.observationTags;
        this.circuitBreaker = builder.circuitBreaker;
        this.retry = builder.retry;
        this.rateLimiter = builder.rateLimiter;
        this.retryStatus = builder.retryStatus;
    }

    public static Builder builder(ObservationRegistry registry) {
        return new Builder(registry != null ? registry : ObservationRegistry.NOOP);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        Observation obs = Observation.createNotStarted("http.client.request.resilient", registry);
        ResilienceObservationTagContributor.contribute(
                obs, clientName, circuitBreaker, retry, rateLimiter, observationTags);

        return Objects.requireNonNull(obs.lowCardinalityKeyValue("client", clientName)
                .lowCardinalityKeyValue("http.method", request.getMethod().name())
                .lowCardinalityKeyValue("http.uri", request.getURI().getPath())
                .observeChecked(() -> {
                    CheckedSupplier<ClientHttpResponse> supplier = () -> {
                        ClientHttpResponse rsp = execution.execute(request, body);
                        HttpStatusCode code = rsp.getStatusCode();
                        HttpStatus hs = HttpStatus.resolve(code.value());

                        boolean shouldRetry = hs != null && retryStatus != null && retryStatus.contains(hs);

                        if (shouldRetry) {
                            if (hs.is4xxClientError()) {
                                throw new HttpClientErrorException(hs, "Client error (retryable): " + hs.value());
                            } else if (hs.is5xxServerError()) {
                                throw new HttpServerErrorException(hs, "Server error (retryable): " + hs.value());
                            } else {
                                throw new RestClientException("Unexpected retryable status: " + hs);
                            }
                        }

                        if (hs != null && hs.is5xxServerError()) {
                            throw new HttpServerErrorException(hs, "Server error: " + hs.value());
                        }

                        return rsp;
                    };

                    // Wrap: Retry → CircuitBreaker → RateLimiter
                    if (retry != null) {
                        supplier = Retry.decorateCheckedSupplier(retry, supplier);
                    }
                    if (circuitBreaker != null) {
                        supplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier);
                    }
                    if (rateLimiter != null) {
                        supplier = RateLimiter.decorateCheckedSupplier(rateLimiter, supplier);
                    }

                    try {
                        return supplier.get();
                    } catch (UncheckedIOException uio) {
                        throw uio.getCause();
                    } catch (CallNotPermittedException
                            | HttpClientErrorException
                            | HttpServerErrorException
                            | RequestNotPermitted ex) {
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

    public static class Builder {
        private final ObservationRegistry registry;
        private String clientName;
        private Map<String, String> observationTags;
        private CircuitBreaker circuitBreaker;
        private Retry retry;
        private RateLimiter rateLimiter;
        private Set<HttpStatus> retryStatus;

        public Builder(ObservationRegistry registry) {
            this.registry = registry;
        }

        public Builder observationTags(Map<String, String> observationTags) {
            this.observationTags = observationTags;
            return this;
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

        public Builder retryStatus(Set<HttpStatus> statuses) {
            this.retryStatus = statuses;
            return this;
        }

        public Builder rateLimiter(RateLimiter rl) {
            this.rateLimiter = rl;
            return this;
        }

        public ResilienceHttpRequestInterceptor build() {
            ResilienceHttpRequestInterceptor interceptor = new ResilienceHttpRequestInterceptor(this);
            ResilienceEventPublisherLogger.attach(retry, circuitBreaker, rateLimiter, log);
            return interceptor;
        }
    }
}
