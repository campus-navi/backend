package com.campusnavi.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        String username,
        String password,
        String email,
        String nickname,
        Long universityId
) {
}
