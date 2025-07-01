package com.example.http.autoconfiguration.utils;

import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec;

public class TestRestClient {

    private RestClient restClient;

    public TestRestClient(String baseUrl) {
        this.restClient = defaultRestClient(baseUrl);
    }

    public TestRestClient(RestClient preconfiguredClient) {
        this.restClient = preconfiguredClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return restClient.get().uri(path).retrieve().toEntity(responseType);
    }

    public <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        return restClient.post().uri(path).body(body).retrieve().toEntity(responseType);
    }

    public <T> ResponseEntity<T> put(String path, Object body, Class<T> responseType) {
        return restClient.method(HttpMethod.PUT).uri(path).body(body).retrieve().toEntity(responseType);
    }

    public ResponseEntity<Void> delete(String path) {
        return restClient.delete().uri(path).retrieve().toBodilessEntity();
    }

    public ResponseSpec rawRequest(HttpMethod method, String path, Object body, Map<String, String> headers) {
        return restClient
                .method(method)
                .uri(path)
                .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                .body(body)
                .retrieve();
    }

    private RestClient defaultRestClient(String baseUrl) {
        HttpComponentsClientHttpRequestFactory factory = HttpClientConfigurer.configureHttpClient5();
        return RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }
}
