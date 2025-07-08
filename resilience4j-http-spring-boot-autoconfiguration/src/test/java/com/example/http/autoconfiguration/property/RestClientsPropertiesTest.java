package com.example.http.autoconfiguration.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.client.property.HttpClientProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RestClientsPropertiesTest {

    @Test
    void shouldCreateEmptyClientsMapByDefault() {
        RestClientsProperties props = new RestClientsProperties();

        assertThat(props.getClients()).isNull();
    }

    @Test
    void shouldPopulateClientsMapUsingConstructor() {
        RestClientProperties userClient = RestClientProperties.builder()
                .clientName("user-service")
                .baseUrl("https://api.example.com/user")
                .build();

        RestClientProperties orderClient = RestClientProperties.builder()
                .clientName("order-service")
                .baseUrl("https://api.example.com/order")
                .build();

        Map<String, RestClientProperties> clients = Map.of(
                "user", userClient,
                "order", orderClient);

        RestClientsProperties props = new RestClientsProperties(clients);

        assertThat(props.getClients()).containsKey("user");
        assertThat(props.getClients()).containsKey("order");
        assertThat(props.getClients().get("user").getBaseUrl()).isEqualTo("https://api.example.com/user");
        assertThat(props.getClients().get("order").getClientName()).isEqualTo("order-service");
    }

    @Test
    void shouldAllowDirectSettersForClientMap() {
        RestClientProperties adminClient = RestClientProperties.builder()
                .baseUrl("https://api.example.com/admin")
                .httpClient(HttpClientProperties.builder()
                        .ssl(HttpClientProperties.Ssl.builder()
                                .enabled(true)
                                .trustAll(true)
                                .build())
                        .build())
                .build();

        RestClientsProperties props = new RestClientsProperties();
        props.setClients(Map.of("admin", adminClient));

        assertThat(props.getClients()).containsKey("admin");
        assertThat(props.getClients().get("admin").getBaseUrl()).isEqualTo("https://api.example.com/admin");
        assertThat(props.getClients().get("admin").getHttpClient().getSsl().isEnabled())
                .isTrue();
    }
}
