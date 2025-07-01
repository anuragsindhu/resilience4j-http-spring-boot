package com.example.http.autoconfiguration.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class TestHttpClient {

    static int get(String url) {
        try {
            return HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                            HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    static int post(String url, String body) {
        return post(url, body, Map.of());
    }

    static int post(String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json");

            headers.forEach(builder::header);

            return HttpClient.newHttpClient()
                    .send(builder.build(), HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    static int put(String url, String body) {
        try {
            return HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                                    .header("Content-Type", "application/json")
                                    .build(),
                            HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (Exception e) {
            return -1;
        }
    }
}
