package com.example.http.autoconfiguration.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

class TestRestClientTest {

    private RestClient mockRestClient;
    private TestRestClient client;
    private final String path = "/test";

    @BeforeEach
    void setUp() {
        mockRestClient = mock(RestClient.class);
        client = new TestRestClient(mockRestClient);
    }

    @Test
    void getReturnsExpectedResponse() {
        @SuppressWarnings("rawtypes")
        RequestHeadersUriSpec headersSpec = mock(RequestHeadersUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        doReturn(headersSpec).when(mockRestClient).get();
        when(headersSpec.uri(path)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("value"));

        ResponseEntity<String> response = client.get(path, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("value");
    }

    @Test
    void postSendsPayloadAndReturnsResponse() {
        RestClient.RequestBodyUriSpec bodySpec = mock(RestClient.RequestBodyUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        doReturn(bodySpec).when(mockRestClient).post();
        when(bodySpec.uri(path)).thenReturn(bodySpec);
        when(bodySpec.body("hello")).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("hello"));

        ResponseEntity<String> response = client.post(path, "hello", String.class);

        assertThat(response.getBody()).isEqualTo("hello");
    }

    @Test
    void putSendsRequestBodyAndReceivesResponse() {
        RestClient.RequestBodyUriSpec bodySpec = mock(RestClient.RequestBodyUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        doReturn(bodySpec).when(mockRestClient).method(HttpMethod.PUT);
        when(bodySpec.uri(path)).thenReturn(bodySpec);
        when(bodySpec.body("update")).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("updated"));

        ResponseEntity<String> response = client.put(path, "update", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("updated");
    }

    @Test
    void deleteReturnsNoContent() {
        @SuppressWarnings("rawtypes")
        RequestHeadersUriSpec headersSpec = mock(RequestHeadersUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        doReturn(headersSpec).when(mockRestClient).delete();
        when(headersSpec.uri(path)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.noContent().build());

        ResponseEntity<Void> response = client.delete(path);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void rawRequestAllowsMethodAndHeadersAndBody() {
        RestClient.RequestBodyUriSpec bodySpec = mock(RestClient.RequestBodyUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        doReturn(bodySpec).when(mockRestClient).method(HttpMethod.POST);
        when(bodySpec.uri("/raw")).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body("data")).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        ResponseSpec result = client.rawRequest(HttpMethod.POST, "/raw", "data", Map.of("X-Test", "true"));

        assertThat(result).isSameAs(responseSpec);
    }

    @Test
    void constructorCreatesClientFromBaseUrl() {
        TestRestClient restClient = new TestRestClient("http://localhost:9999");

        assertThat(restClient).isNotNull();
    }

    @Test
    void canUpdateRestClientDynamically() {
        RestClient newClient = mock(RestClient.class);
        client.setRestClient(newClient);

        assertThat(client).isNotNull();
    }
}
