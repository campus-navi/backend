package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record FolderScrapResponse(
        Long scrapId,
        Long postId,
        String title,
        String tagName,
        LocalDate endDate,
        LocalDate publishedAt,
        boolean isActive
) {
}
