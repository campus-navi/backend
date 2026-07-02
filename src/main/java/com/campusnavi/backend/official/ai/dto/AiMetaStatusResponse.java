package com.campusnavi.backend.official.ai.dto;

import java.time.LocalDateTime;

public record AiMetaStatusResponse(
        boolean running,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        long pendingCount,
        long doneCount,
        long failedCount
) {
}
