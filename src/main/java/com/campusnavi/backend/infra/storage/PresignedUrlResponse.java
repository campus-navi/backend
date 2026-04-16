package com.campusnavi.backend.infra.storage;

public record PresignedUrlResponse(
        String url,
        String key
) {
}
