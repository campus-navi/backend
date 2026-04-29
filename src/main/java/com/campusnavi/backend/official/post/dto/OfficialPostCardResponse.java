package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record OfficialPostCardResponse(
        Long postId,
        String title,
        String tagName,
        String summary,
        String imageUrl,
        LocalDate publishedAt
) {
}
