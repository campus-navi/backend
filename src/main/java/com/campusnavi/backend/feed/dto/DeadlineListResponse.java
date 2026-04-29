package com.campusnavi.backend.feed.dto;

import com.campusnavi.backend.official.post.dto.DeadlinePostResponse;

import java.util.List;

public record DeadlineListResponse(
        List<DeadlinePostResponse> posts
) {
}
