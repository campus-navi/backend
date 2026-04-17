package com.campusnavi.backend.community.post.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank
        String title,
        String content,
        boolean isAnonymous,
        List<String> imageKeys
) {
}
