package com.mt.agent.workflow.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Data
@Configuration
@ConfigurationProperties(prefix = "dify")
public class DifyConfig {

    private Nl2sql nl2sql;

    @Data
    public static class Nl2sql {
        private String baseUrl;
        private String apiKey;
    }

    @Bean
    public WebClient webClient() {
        // We can pre-configure the WebClient if needed, e.g., setting a base URL
        return WebClient.builder().build();
    }
}
