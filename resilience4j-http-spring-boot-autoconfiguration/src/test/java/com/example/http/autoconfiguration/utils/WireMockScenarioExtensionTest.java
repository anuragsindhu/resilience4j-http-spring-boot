package com.example.http.autoconfiguration.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockScenarioExtensionTest {

    @RegisterExtension
    static WireMockScenarioExtension extension = new WireMockScenarioExtension();

    private final ExtensionContext mockContext = mock(ExtensionContext.class);

    @BeforeEach
    void resetWireMock() {
        extension.resetAll();
    }

    @Test
    void shouldInitializeWithDefaultConstructor() {
        assertThat(extension.builder()).isNotNull();
        assertThat(extension.baseUrl()).startsWith("http://localhost");
        assertThat(extension.wireMock()).isNotNull();
    }

    @Test
    void shouldApplyDefaultStubSetupWhenProvided() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        Consumer<WireMockErrorScenarioBuilder> stubSetup = builder -> {
            builder.withHttp500("/api/failure");
            invoked.set(true);
        };

        try (WireMockScenarioExtension custom = new WireMockScenarioExtension(null, stubSetup)) {
            custom.beforeAll(mockContext);
            custom.beforeEach(mockContext);

            assertThat(invoked).isTrue();

            int status = TestHttpClient.get(custom.baseUrl() + "/api/failure");
            assertThat(status).isEqualTo(500);

            custom.afterAll(mockContext);
        }
    }

    @Test
    void shouldLoadJsonStubMappingsFromDirectory() throws Exception {
        Path dir = Files.createTempDirectory("stubs");
        Path file = dir.resolve("stub.json");

        Files.writeString(
                file,
                """
            {
              "request": { "method": "GET", "url": "/api/dynamic" },
              "response": { "status": 202, "body": "Loaded" }
            }
            """);

        try (WireMockScenarioExtension mapped = new WireMockScenarioExtension(dir, null)) {
            mapped.beforeAll(mockContext);
            mapped.beforeEach(mockContext);

            int status = TestHttpClient.get(mapped.baseUrl() + "/api/dynamic");
            assertThat(status).isEqualTo(202);

            mapped.afterAll(mockContext);
        }
    }

    @Test
    void shouldThrowExceptionIfStubJsonIsMalformed() throws Exception {
        Path dir = Files.createTempDirectory("badstubs");
        Files.writeString(dir.resolve("bad.json"), "{ malformed json }");

        try (WireMockScenarioExtension broken = new WireMockScenarioExtension(dir, null)) {
            broken.beforeAll(mockContext);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> broken.beforeEach(mockContext));
            assertThat(ex).hasMessageContaining("Failed to load stub");
            broken.afterAll(mockContext);
        }
    }

    @Test
    void shouldReturnBuilderWireMockAndBaseUrl() {
        assertThat(extension.builder()).isNotNull();
        assertThat(extension.wireMock()).isNotNull();
        assertThat(extension.baseUrl()).startsWith("http://localhost");
    }

    @Test
    void shouldWorkWithNullMappingsDirAndStubSetup() throws Exception {
        try (WireMockScenarioExtension empty = new WireMockScenarioExtension(null, null)) {
            empty.beforeAll(mockContext);
            assertThatCode(() -> empty.beforeEach(mockContext)).doesNotThrowAnyException();
            empty.afterAll(mockContext);
        }
    }
}
