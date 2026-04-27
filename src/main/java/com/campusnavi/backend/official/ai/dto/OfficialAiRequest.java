package com.campusnavi.backend.official.ai.dto;

import java.util.List;

public record OfficialAiRequest(
        Long postId,
        String structuredText,
        List<String> imageUrls,
        List<String> attachmentUrls
) {
}
