package com.campusnavi.backend.notification.dto;

import java.time.LocalDate;

public record RemindNotice(
        Long postId,
        String title,
        LocalDate endDate
) {}
