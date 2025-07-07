package com.example.http.client.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.client.property.HttpClientProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class SslValidatorTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldNotThrowIfSslIsNull() {
        SslValidator.validateStores(null, validator);
    }

    @Test
    void shouldNotThrowIfValidatorIsNull() {
        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().enabled(true).build();
        SslValidator.validateStores(ssl, null);
    }

    @Test
    void shouldNotThrowIfStoresAreFullyEmpty() {
        HttpClientProperties.Ssl ssl = HttpClientProperties.Ssl.builder()
                .truststore(HttpClientProperties.Store.builder().build())
                .keystore(HttpClientProperties.Store.builder().build())
                .build();
        SslValidator.validateStores(ssl, validator);
    }

    @Test
    void shouldThrowIfTruststoreIsPartiallyConfigured() {
        HttpClientProperties.Store truststore = HttpClientProperties.Store.builder()
                .location("classpath:missing.jks")
                .type("JKS")
                .build();

        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().truststore(truststore).build();

        assertThatThrownBy(() -> SslValidator.validateStores(ssl, validator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("truststore.password");
    }

    @Test
    void shouldThrowIfKeystoreIsPartiallyConfigured() {
        HttpClientProperties.Store keystore = HttpClientProperties.Store.builder()
                .location("file:/some/path.jks")
                .password("secret")
                .build();

        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().keystore(keystore).build();

        assertThatThrownBy(() -> SslValidator.validateStores(ssl, validator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keystore.type");
    }

    @Test
    void shouldNotThrowIfTruststoreIsFullyConfigured() {
        HttpClientProperties.Store truststore = HttpClientProperties.Store.builder()
                .location("classpath:dummy.jks")
                .password("changeit")
                .type("JKS")
                .provider("SUN")
                .build();

        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().truststore(truststore).build();
        SslValidator.validateStores(ssl, validator);
    }
}
