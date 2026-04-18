package com.campusnavi.backend.community.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank String content,
        boolean isAnonymous
) {
}
