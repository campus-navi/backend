package com.campusnavi.backend.infra.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        String from,
        String senderName,
        String awsRegion
) {
}
