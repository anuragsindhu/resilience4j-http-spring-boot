package com.example.http.autoconfiguration.properties;

import com.example.http.autoconfiguration.validation.MinDuration;
import com.example.http.autoconfiguration.validation.SslStoreGroup;
import jakarta.validation.constraints.*;
import java.time.Duration;
import javax.net.ssl.HostnameVerifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpClientProperties {

    @Builder.Default
    private boolean enabled = true;

    @NotNull @Builder.Default
    private Pool pool = HttpClientDefaultSettings.defaultPool();

    @NotNull @Builder.Default
    private RequestFactory requestFactory = HttpClientDefaultSettings.defaultRequestFactory();

    @NotNull @Builder.Default
    private Ssl ssl = HttpClientDefaultSettings.defaultSsl();

    public static HttpClientProperties defaultConfig() {
        return HttpClientProperties.builder().build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pool {

        @NotBlank @Builder.Default
        private String concurrencyPolicy = "LAX";

        @Min(1) @Builder.Default
        private int maxConnectionsPerRoute = 20;

        @Min(1) @Builder.Default
        private int maxTotalConnections = 200;

        @NotNull @Builder.Default
        private Connection connection = HttpClientDefaultSettings.defaultConnection();

        @NotNull @Builder.Default
        private Socket socket = HttpClientDefaultSettings.defaultSocket();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Connection {

            @NotNull @MinDuration(value = 1, message = "Connect timeout must be at least {value} ms")
            @Builder.Default
            private Duration connectTimeout = Duration.ofSeconds(2);

            @NotNull @MinDuration(value = 1, message = "Idle eviction timeout must be at least {value} ms")
            @Builder.Default
            private Duration idleEvictionTimeout = Duration.ofMinutes(1);

            @NotNull @MinDuration(value = 1, message = "Time-to-live must be at least {value} ms")
            @Builder.Default
            private Duration timeToLive = Duration.ofMinutes(5);

            @NotNull @MinDuration(value = 1, message = "Validation interval must be at least {value} ms")
            @Builder.Default
            private Duration validateAfterInactivity = Duration.ofSeconds(30);
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Socket {

            @NotNull @MinDuration(value = 1, message = "Linger timeout must be at least {value} ms")
            @Builder.Default
            private Duration soLinger = Duration.ofSeconds(-1);

            @Min(1) @Builder.Default
            private int rcvBuffSize = 32 * 1024;

            @Min(1) @Builder.Default
            private int sndBuffSize = 32 * 1024;

            @NotNull @MinDuration(value = 1, message = "Socket timeout must be at least {value} ms")
            @Builder.Default
            private Duration soTimeout = Duration.ofSeconds(10);

            @Builder.Default
            private boolean tcpNoDelay = true;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestFactory {

        @NotNull @MinDuration(value = 1, message = "Connect timeout must be at least {value} ms")
        @Builder.Default
        private Duration connectTimeout = Duration.ofSeconds(5);

        @NotNull @MinDuration(value = 1, message = "Connection request timeout must be at least {value} ms")
        @Builder.Default
        private Duration connectionRequestTimeout = Duration.ofSeconds(2);

        @NotNull @MinDuration(value = 1, message = "Read timeout must be at least {value} ms")
        @Builder.Default
        private Duration readTimeout = Duration.ofSeconds(10);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ssl {

        @Builder.Default
        private boolean enabled = false;

        @Builder.Default
        private boolean trustAll = false;

        private Store truststore;
        private Store keystore;

        private String hostnameVerifierBeanName;
        private transient HostnameVerifier hostnameVerifier;

        @NotNull @Builder.Default
        private HostnameVerificationPolicy hostnameVerificationPolicy = HostnameVerificationPolicy.BUILTIN;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Store {

        @NotBlank(message = "Location must not be blank", groups = SslStoreGroup.class) private String location;

        @NotBlank(message = "Password must not be blank", groups = SslStoreGroup.class) private String password;

        @NotBlank(message = "Type must not be blank", groups = SslStoreGroup.class) private String type;

        @Size(min = 2, max = 20, message = "Provider must be between 2–20 chars", groups = SslStoreGroup.class) private String provider;
    }
}
