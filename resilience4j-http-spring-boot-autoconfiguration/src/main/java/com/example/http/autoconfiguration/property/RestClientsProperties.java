package com.example.http.autoconfiguration.property;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "group.http", ignoreUnknownFields = false)
@NoArgsConstructor
@AllArgsConstructor
public class RestClientsProperties {
    private Map<String, RestClientProperties> clients;
}
