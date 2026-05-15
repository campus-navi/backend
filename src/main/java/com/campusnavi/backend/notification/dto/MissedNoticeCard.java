package com.campusnavi.backend.notification.dto;

import java.time.LocalDate;

public record MissedNoticeCard(
        LocalDate missedDate,
        int count
) {}
