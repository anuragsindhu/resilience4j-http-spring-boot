package com.example.http.autoconfiguration.utils;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class WireMockScenarioExtension
        implements AutoCloseable, BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    private final Path mappingsDir;
    private final Consumer<WireMockErrorScenarioBuilder> defaultStubSetup;

    private WireMockExtension wireMock;
    private WireMockErrorScenarioBuilder builder;

    public WireMockScenarioExtension() {
        this(null, null);
    }

    public WireMockScenarioExtension(Path mappingsDir, Consumer<WireMockErrorScenarioBuilder> defaultStubSetup) {
        this.mappingsDir = mappingsDir;
        this.defaultStubSetup = defaultStubSetup;
    }

    @Override
    public void close() {
        try {
            afterAll(null); // you might need to track context if used
        } catch (Exception e) {
            throw new RuntimeException("Failed to close WireMockExtension", e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        wireMock = WireMockExtension.newInstance()
                .options(WireMockConfiguration.options()
                        .dynamicPort()
                        .usingFilesUnderClasspath("wiremock")
                        .extensions(new ResponseTemplateTransformer(false)))
                .build();

        wireMock.beforeAll(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (wireMock == null) {
            throw new IllegalStateException("WireMockExtension not initialized — did you forget to call beforeAll()?");
        }

        wireMock.beforeEach(context);
        builder = WireMockErrorScenarioBuilder.forPort(wireMock.getPort());
        builder.reset();

        if (defaultStubSetup != null) {
            defaultStubSetup.accept(builder);
        }

        if (mappingsDir != null && Files.exists(mappingsDir)) {
            try (Stream<Path> paths = Files.walk(mappingsDir)) {
                WireMock wireMockClient = wireMock.getRuntimeInfo().getWireMock();
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            try {
                                String json = Files.readString(file);
                                StubMapping stub = StubMapping.buildFrom(json);
                                wireMockClient.register(stub);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to load stub from: " + file, e);
                            }
                        });
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        wireMock.afterAll(context);
    }

    public WireMockErrorScenarioBuilder builder() {
        return builder;
    }

    public String baseUrl() {
        return wireMock.getRuntimeInfo().getHttpBaseUrl();
    }

    public WireMockExtension wireMock() {
        return wireMock;
    }

    public void resetAll() {
        wireMock.resetAll();
    }
}
