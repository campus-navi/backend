package com.campusnavi.backend.official.post.dto;

public record OfficialPostStatsRaw(
        Long postId,
        long cappedViewSum,
        long sameAdmissionCount,
        long sameGradeCount,
        long viewerCount
) {
    public static OfficialPostStatsRaw empty(Long postId) {
        return new OfficialPostStatsRaw(postId, 0L, 0L, 0L, 0L);
    }
}
