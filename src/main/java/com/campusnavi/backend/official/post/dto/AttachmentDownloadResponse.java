package com.campusnavi.backend.official.post.dto;

public record AttachmentDownloadResponse(
        String downloadUrl,
        long expiresInSeconds
) {
}
