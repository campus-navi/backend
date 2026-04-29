package com.campusnavi.backend.feed.dto;

import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;

import java.util.List;

public record CardListResponse(
        List<OfficialPostCardResponse> newPosts,
        List<OfficialPostCardResponse> recommendedPosts
) {
}
