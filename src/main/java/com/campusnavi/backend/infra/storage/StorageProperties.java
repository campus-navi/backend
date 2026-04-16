package com.campusnavi.backend.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record StorageProperties(
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        String endPoint,
        boolean pathStyleAccess
) { }
