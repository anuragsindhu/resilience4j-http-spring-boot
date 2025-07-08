package com.example.http.autoconfiguration.utils;

import com.example.http.autoconfiguration.property.RestClientProperties;
import com.example.http.client.builder.HttpClientConfigurer;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec;

public class TestRestClient {

    private RestClient restClient;

    public TestRestClient(String baseUrl) {
        this.restClient =
                defaultRestClient(baseUrl, RestClientProperties.builder().build());
    }

    public TestRestClient(String baseUrl, RestClientProperties properties) {
        this.restClient = defaultRestClient(baseUrl, properties);
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

    private RestClient defaultRestClient(String baseUrl, RestClientProperties properties) {
        var httpClient = HttpClientConfigurer.configure(properties.getHttpClient());
        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(properties.getRequestFactory().getConnectTimeout());
        factory.setConnectionRequestTimeout(properties.getRequestFactory().getConnectionRequestTimeout());
        factory.setReadTimeout(properties.getRequestFactory().getReadTimeout());
        return RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }
}
