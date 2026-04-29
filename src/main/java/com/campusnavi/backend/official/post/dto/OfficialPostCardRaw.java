package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record OfficialPostCardRaw(
        Long postId,
        String title,
        String tagName,
        String summary,
        String s3Key,
        LocalDate publishedAt
) {
}
