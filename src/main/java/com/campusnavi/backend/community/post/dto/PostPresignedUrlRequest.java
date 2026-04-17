package com.campusnavi.backend.community.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PostPresignedUrlRequest(
        @NotBlank String filename,
        @NotBlank String contentType,
        @Positive long size
) {
}
