package com.example.http.autoconfiguration.properties;

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

    @Builder.Default
    private Pool pool = HttpClientDefaultSettings.defaultPool();

    @Builder.Default
    private RequestFactory requestFactory = HttpClientDefaultSettings.defaultRequestFactory();

    @Builder.Default
    private Ssl ssl = HttpClientDefaultSettings.defaultSsl();

    public static HttpClientProperties defaultConfig() {
        return HttpClientDefaultSettings.defaultHttpClient();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pool {
        @Builder.Default
        private String concurrencyPolicy = "LAX";

        @Builder.Default
        private int maxConnectionsPerRoute = 20;

        @Builder.Default
        private int maxTotalConnections = 200;

        @Builder.Default
        private Connection connection = Connection.builder().build();

        @Builder.Default
        private Socket socket = Socket.builder().build();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Connection {
            @Builder.Default
            private Duration idleEvictionTimeout = Duration.ofMinutes(1);

            @Builder.Default
            private Duration timeToLive = Duration.ofMinutes(5);

            @Builder.Default
            private Duration validateAfterInactivity = Duration.ofSeconds(30);
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Socket {
            @Builder.Default
            private Duration lingerTimeout = Duration.ofSeconds(2);

            @Builder.Default
            private int receiveBufferSize = 8192;

            @Builder.Default
            private int sendBufferSize = 8192;

            @Builder.Default
            private Duration socketTimeout = Duration.ofSeconds(10);

            @Builder.Default
            private boolean tcpNoDelay = true;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestFactory {
        @Builder.Default
        private Duration connectTimeout = Duration.ofSeconds(5);

        @Builder.Default
        private Duration connectionRequestTimeout = Duration.ofSeconds(2);

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

        private String trustStorePath;
        private String trustStorePassword;
        private String keyStorePath;
        private String keyStorePassword;
        private String hostnameVerifierBeanName;
        private transient HostnameVerifier hostnameVerifier;

        @Builder.Default
        private HostnameVerificationPolicy hostnameVerificationPolicy = HostnameVerificationPolicy.CLIENT;
    }
}
