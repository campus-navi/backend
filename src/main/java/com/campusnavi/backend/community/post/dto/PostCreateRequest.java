package com.campusnavi.backend.community.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank
        String title,
        @NotBlank
        String content,
        boolean isAnonymous,
        @Size(max = 10)
        List<String> imageKeys
) {
}
