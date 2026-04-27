package com.campusnavi.backend.infra.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai.server")
public record AiClientProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration responseTimeout,
        Duration connectionRequestTimeout,
        int maxTotal,
        int maxPerRoute
) {
}
