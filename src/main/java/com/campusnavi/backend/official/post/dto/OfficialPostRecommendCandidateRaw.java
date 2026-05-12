package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record OfficialPostRecommendCandidateRaw(
        Long postId,
        String title,
        Long tagId,
        String tagName,
        String summary,
        String s3Key,
        LocalDate publishedAt
) {
}
