package com.campusnavi.backend.official.ai.dto;

import java.util.List;

public record OfficialAiBatchRequest(
        List<OfficialAiRequest> items
) {
}
