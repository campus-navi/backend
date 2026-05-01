package com.campusnavi.backend.official.post.dto;

public record AttachmentResponse(
        Long id,
        String name,
        boolean isDownloaded
) {
}
