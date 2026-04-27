package com.campusnavi.backend.official.ai.dto;

import java.util.List;

public record OfficialAiBatchResponse(
        List<OfficialAiBatchItemResponse> results
) {
}
