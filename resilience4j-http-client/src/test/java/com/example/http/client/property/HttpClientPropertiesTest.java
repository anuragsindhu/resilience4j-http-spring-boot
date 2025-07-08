package com.example.http.client.property;

import com.example.http.client.validation.SslStoreGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.Set;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpClientPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateDefaultHttpClientProperties() {
        HttpClientProperties props = HttpClientProperties.defaultConfig();

        Assertions.assertThat(props).isNotNull();
        Assertions.assertThat(props.getPool()).isNotNull();
        Assertions.assertThat(props.getRequestFactory()).isNotNull();
        Assertions.assertThat(props.getSsl()).isNotNull();
    }

    @Test
    void shouldValidateConnectionPoolSettings() {
        HttpClientProperties.Pool.Connection connection = HttpClientProperties.Pool.Connection.builder()
                .connectTimeout(Duration.ofSeconds(2))
                .idleEvictionTimeout(Duration.ofMinutes(1))
                .timeToLive(Duration.ofMinutes(5))
                .validateAfterInactivity(Duration.ofSeconds(30))
                .build();

        Set<ConstraintViolation<HttpClientProperties.Pool.Connection>> violations = validator.validate(connection);
        Assertions.assertThat(violations).isEmpty();
    }

    @Test
    void shouldTriggerValidationErrorsForInvalidConnection() {
        HttpClientProperties.Pool.Connection connection = HttpClientProperties.Pool.Connection.builder()
                .connectTimeout(null)
                .idleEvictionTimeout(null)
                .timeToLive(null)
                .validateAfterInactivity(null)
                .build();

        Set<ConstraintViolation<HttpClientProperties.Pool.Connection>> violations = validator.validate(connection);
        Assertions.assertThat(violations).hasSize(8);
    }

    @Test
    void shouldValidateSocketSettings() {
        HttpClientProperties.Pool.Socket socket = HttpClientProperties.Pool.Socket.builder()
                .rcvBuffSize(32768)
                .sndBuffSize(32768)
                .soLinger(Duration.ofSeconds(-1))
                .soTimeout(Duration.ofSeconds(10))
                .tcpNoDelay(true)
                .build();

        Set<ConstraintViolation<HttpClientProperties.Pool.Socket>> violations = validator.validate(socket);
        Assertions.assertThat(violations).isEmpty();
    }

    @Test
    void shouldValidateRequestFactorySettings() {
        HttpClientProperties.RequestFactory factory = HttpClientProperties.RequestFactory.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .connectionRequestTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(10))
                .build();

        Set<ConstraintViolation<HttpClientProperties.RequestFactory>> violations = validator.validate(factory);
        Assertions.assertThat(violations).isEmpty();
    }

    @Test
    void shouldValidateSslStoreSettings() {
        HttpClientProperties.Store store = HttpClientProperties.Store.builder()
                .location("classpath:truststore.jks")
                .password("changeit")
                .provider("SunJSSE")
                .type("JKS")
                .build();

        Set<ConstraintViolation<HttpClientProperties.Store>> violations =
                validator.validate(store, SslStoreGroup.class);

        Assertions.assertThat(violations).isEmpty();
    }

    @Test
    void shouldTriggerValidationErrorsForInvalidSslStore() {
        HttpClientProperties.Store store = HttpClientProperties.Store.builder()
                .location("")
                .password("")
                .provider("A")
                .type("")
                .build();

        Set<ConstraintViolation<HttpClientProperties.Store>> violations =
                validator.validate(store, SslStoreGroup.class);

        Assertions.assertThat(violations).hasSize(4);
    }

    @Test
    void builderShouldSetAllPropertiesCorrectly() {
        HttpClientProperties props = HttpClientProperties.builder()
                .pool(HttpClientDefaultSettings.defaultPool())
                .requestFactory(HttpClientDefaultSettings.defaultRequestFactory())
                .ssl(HttpClientDefaultSettings.defaultSsl())
                .build();

        Assertions.assertThat(props.getPool().getConcurrencyPolicy()).isEqualTo("LAX");
        Assertions.assertThat(props.getRequestFactory().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        Assertions.assertThat(props.getSsl().getHostnameVerificationPolicy())
                .isEqualTo(HostnameVerificationPolicy.BUILTIN);
    }

    @Test
    void shouldSupportEmptySslConfiguration() {
        HttpClientProperties.Ssl ssl = HttpClientProperties.Ssl.builder().build();

        Assertions.assertThat(ssl.isEnabled()).isFalse();
        Assertions.assertThat(ssl.isTrustAll()).isFalse();
        Assertions.assertThat(ssl.getHostnameVerificationPolicy()).isEqualTo(HostnameVerificationPolicy.BUILTIN);
    }

    @Test
    void storeShouldSupportEmptyProvider() {
        HttpClientProperties.Store store = new HttpClientProperties.Store();

        store.setLocation("classpath:test.jks");
        store.setPassword("secret");
        store.setProvider("SunJSSE");
        store.setType("PKCS12");

        Assertions.assertThat(store.getLocation()).isEqualTo("classpath:test.jks");
        Assertions.assertThat(store.getType()).isEqualTo("PKCS12");
    }
}
