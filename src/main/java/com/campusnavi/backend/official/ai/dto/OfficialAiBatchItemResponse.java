package com.campusnavi.backend.official.ai.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OfficialAiBatchItemResponse(
        Long postId,
        boolean success,
        String reason,
        OfficialAiResponse result
) {
}
