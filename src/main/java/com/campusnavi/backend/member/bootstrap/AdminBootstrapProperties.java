package com.campusnavi.backend.member.bootstrap;

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
