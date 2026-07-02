package com.campusnavi.backend.official.crawler.dto;

import java.time.LocalDateTime;

public record CrawlStatusResponse(
        boolean running,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
