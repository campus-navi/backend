package com.campusnavi.backend.official.crawler.dto;

import java.time.LocalDate;

public record PostList(
        String originalId,
        String title,
        String publisher,
        String detailUrl,
        LocalDate publishedAt) {
}
