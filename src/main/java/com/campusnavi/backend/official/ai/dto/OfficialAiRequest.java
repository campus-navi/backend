package com.campusnavi.backend.official.ai.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OfficialAiRequest(
        Long postId,
        String structuredText,
        List<String> imageUrls,
        List<String> attachmentUrls
) {
}
