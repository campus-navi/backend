package com.campusnavi.backend.official.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OfficialAiBatchItemResponse(
        Long postId,
        boolean success,
        String reason,
        OfficialAiResponse result
) {
}
