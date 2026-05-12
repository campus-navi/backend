package com.campusnavi.backend.official.post.recommend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class RecommendSlot {

    public static final LocalTime MORNING = LocalTime.of(9, 0);
    public static final LocalTime EVENING = LocalTime.of(18, 0);

    private RecommendSlot() {
    }

    public static LocalDateTime current(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        if (time.isBefore(MORNING)) {
            return today.minusDays(1).atTime(EVENING);
        }
        if (time.isBefore(EVENING)) {
            return today.atTime(MORNING);
        }
        return today.atTime(EVENING);
    }
}
