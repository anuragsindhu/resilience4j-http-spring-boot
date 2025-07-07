package com.example.http.autoconfiguration.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.properties.HttpClientProperties.Ssl;
import com.example.http.autoconfiguration.properties.HttpClientProperties.Store;
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
        Ssl ssl = Ssl.builder().enabled(true).build();
        SslValidator.validateStores(ssl, null);
    }

    @Test
    void shouldNotThrowIfStoresAreFullyEmpty() {
        Ssl ssl = Ssl.builder()
                .truststore(Store.builder().build())
                .keystore(Store.builder().build())
                .build();
        SslValidator.validateStores(ssl, validator);
    }

    @Test
    void shouldThrowIfTruststoreIsPartiallyConfigured() {
        Store truststore =
                Store.builder().location("classpath:missing.jks").type("JKS").build();

        Ssl ssl = Ssl.builder().truststore(truststore).build();

        assertThatThrownBy(() -> SslValidator.validateStores(ssl, validator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("truststore.password");
    }

    @Test
    void shouldThrowIfKeystoreIsPartiallyConfigured() {
        Store keystore = Store.builder()
                .location("file:/some/path.jks")
                .password("secret")
                .build();

        Ssl ssl = Ssl.builder().keystore(keystore).build();

        assertThatThrownBy(() -> SslValidator.validateStores(ssl, validator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keystore.type");
    }

    @Test
    void shouldNotThrowIfTruststoreIsFullyConfigured() {
        Store truststore = Store.builder()
                .location("classpath:dummy.jks")
                .password("changeit")
                .type("JKS")
                .provider("SUN")
                .build();

        Ssl ssl = Ssl.builder().truststore(truststore).build();
        SslValidator.validateStores(ssl, validator);
    }
}
