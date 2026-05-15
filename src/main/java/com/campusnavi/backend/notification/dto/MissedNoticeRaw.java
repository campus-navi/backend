package com.campusnavi.backend.notification.dto;

import java.time.LocalDate;

public record MissedNoticeRaw(
        Long postId,
        String title,
        String tagName,
        LocalDate publishedAt,
        LocalDate endDate
) {}
