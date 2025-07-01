package com.example.http.autoconfiguration.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import org.assertj.core.api.AbstractAssert;

public class WireMockRequestAssert extends AbstractAssert<WireMockRequestAssert, List<LoggedRequest>> {

    private final String path;
    private final String method;

    public WireMockRequestAssert(String method, String path) {
        super(
                WireMock.findAll(
                        RequestPatternBuilder.newRequestPattern(RequestMethod.fromString(method), urlEqualTo(path))),
                WireMockRequestAssert.class);
        this.method = method.toUpperCase();
        this.path = path;
    }

    public WireMockRequestAssert wasCalledTimes(int expected) {
        isNotNull();
        int actualCalls = actual.size();
        if (actualCalls != expected) {
            failWithMessage("Expected [%s %s] to be called %d times but was %d", method, path, expected, actualCalls);
        }
        return this;
    }

    public WireMockRequestAssert hasHeader(String name, String value) {
        boolean match = actual.stream().anyMatch(r -> value.equals(r.getHeader(name)));
        if (!match) {
            failWithMessage("Expected header [%s: %s] in [%s %s] but none found", name, value, method, path);
        }
        return this;
    }

    public WireMockRequestAssert hasBodyContaining(String snippet) {
        boolean match = actual.stream().anyMatch(r -> r.getBodyAsString().contains(snippet));
        if (!match) {
            failWithMessage("Expected body of [%s %s] to contain: '%s'", method, path, snippet);
        }
        return this;
    }
}
