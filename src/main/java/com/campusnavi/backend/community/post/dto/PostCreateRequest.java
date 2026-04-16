package com.campusnavi.backend.community.post.dto;

import java.util.List;

public record PostCreateRequest(
        String title,
        String content,
        boolean isAnonymous,
        List<String> imageKeys
) {
}
