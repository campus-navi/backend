package com.campusnavi.backend.official.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OfficialAiRequest(
        Long postId,
        String structuredText,
        List<String> imageUrls,
        List<String> attachmentUrls
) {
}
