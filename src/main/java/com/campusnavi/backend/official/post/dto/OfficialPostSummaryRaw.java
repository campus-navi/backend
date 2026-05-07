package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record OfficialPostSummaryRaw(
        Long postId,
        String title,
        String tagName,
        LocalDate publishedAt,
        LocalDate endDate
) {}
