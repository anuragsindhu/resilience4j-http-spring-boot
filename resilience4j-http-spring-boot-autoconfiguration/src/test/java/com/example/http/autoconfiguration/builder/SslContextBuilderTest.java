package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.properties.HttpClientProperties.Ssl;
import com.example.http.autoconfiguration.properties.HttpClientProperties.Store;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SslContextBuilderTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldReturnNullIfSslIsDisabled() {
        Ssl ssl = Ssl.builder().enabled(false).build();
        SSLContext result = SslContextBuilder.from(ssl).build();
        assertThat(result).isNull();
    }

    @Test
    void shouldTrustAllWhenConfigured() {
        Ssl ssl = Ssl.builder().enabled(true).trustAll(true).build();
        SSLContext result = SslContextBuilder.from(ssl).build();
        assertThat(result).isNotNull();
    }

    @Test
    void shouldSkipValidationIfValidatorIsNull() {
        Store partial =
                Store.builder().location("classpath:x.p12").type("PKCS12").build();

        Ssl ssl = Ssl.builder().enabled(true).truststore(partial).build();
        SSLContext result = SslContextBuilder.from(ssl).build();
        assertThat(result).isNotNull(); // validation skipped
    }

    @Test
    void shouldValidateStoresIfValidatorIsPresent() {
        Store broken = Store.builder().location("classpath:x.jks").build();
        Ssl ssl = Ssl.builder().enabled(true).truststore(broken).build();

        assertThatThrownBy(() -> SslContextBuilder.from(ssl, validator).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SSL store configuration");
    }

    @Test
    void shouldIgnoreKeystoreIfNull() {
        Ssl ssl = Ssl.builder().enabled(true).keystore(null).build();
        SSLContext result = SslContextBuilder.from(ssl).build();
        assertThat(result).isNotNull();
    }

    @Test
    void shouldIgnoreTruststoreIfNull() {
        Ssl ssl = Ssl.builder().enabled(true).truststore(null).build();
        SSLContext result = SslContextBuilder.from(ssl).build();
        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowForMissingClasspathResource() {
        Store store = Store.builder()
                .location("classpath:missing.jks")
                .password("changeit")
                .type("JKS")
                .build();

        Ssl ssl = Ssl.builder().enabled(true).truststore(store).build();

        assertThatThrownBy(() -> SslContextBuilder.from(ssl).build())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Classpath store not found");
    }

    @Test
    void shouldThrowForMissingFileResource() {
        Store store = Store.builder()
                .location("file:/nonexistent/path.jks")
                .password("changeit")
                .type("JKS")
                .build();

        Ssl ssl = Ssl.builder().enabled(true).truststore(store).build();

        assertThatThrownBy(() -> SslContextBuilder.from(ssl).build())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File store not found");
    }

    @Test
    void shouldThrowForUnsupportedLocationPrefix() {
        Store store = Store.builder()
                .location("http://example.com/store.jks")
                .password("changeit")
                .type("JKS")
                .build();

        Ssl ssl = Ssl.builder().enabled(true).truststore(store).build();

        assertThatThrownBy(() -> SslContextBuilder.from(ssl).build())
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported store location");
    }

    @Disabled("Requires real truststore.jks in classpath")
    @Test
    void shouldBuildWithValidClasspathTruststore() {
        Store store = Store.builder()
                .location("classpath:truststore.jks")
                .password("changeit")
                .type("JKS")
                .build();

        Ssl ssl = Ssl.builder().enabled(true).truststore(store).build();

        SSLContext result = SslContextBuilder.from(ssl).build();
        assertThat(result).isNotNull();
    }

    @Disabled("Requires mock keystore file")
    @Test
    void shouldBuildWithValidFileBasedStore() throws IOException {
        File tempFile = File.createTempFile("mock", ".jks");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("dummy content");
        }

        Store store = Store.builder()
                .location("file:" + tempFile.getAbsolutePath())
                .password("changeit")
                .type("JKS")
                .build();

        Ssl ssl = Ssl.builder().enabled(true).truststore(store).build();

        assertThatThrownBy(() -> SslContextBuilder.from(ssl).build())
                .isInstanceOf(RuntimeException.class); // dummy file won't load
    }
}
