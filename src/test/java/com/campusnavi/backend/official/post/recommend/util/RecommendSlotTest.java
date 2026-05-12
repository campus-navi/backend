package com.campusnavi.backend.official.post.recommend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendSlotTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 12);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Nested
    @DisplayName("current(now)")
    class Current {

        @Test
        @DisplayName("00:00 ~ 08:59 사이는 어제 18:00 슬롯을 반환한다")
        void beforeMorning() {
            assertThat(RecommendSlot.current(TODAY.atTime(0, 0)))
                    .isEqualTo(YESTERDAY.atTime(18, 0));
            assertThat(RecommendSlot.current(TODAY.atTime(8, 59)))
                    .isEqualTo(YESTERDAY.atTime(18, 0));
        }

        @Test
        @DisplayName("09:00 ~ 17:59 사이는 오늘 09:00 슬롯을 반환한다")
        void betweenMorningAndEvening() {
            assertThat(RecommendSlot.current(TODAY.atTime(9, 0)))
                    .isEqualTo(TODAY.atTime(9, 0));
            assertThat(RecommendSlot.current(TODAY.atTime(12, 30)))
                    .isEqualTo(TODAY.atTime(9, 0));
            assertThat(RecommendSlot.current(TODAY.atTime(17, 59)))
                    .isEqualTo(TODAY.atTime(9, 0));
        }

        @Test
        @DisplayName("18:00 ~ 23:59 사이는 오늘 18:00 슬롯을 반환한다")
        void afterEvening() {
            assertThat(RecommendSlot.current(TODAY.atTime(18, 0)))
                    .isEqualTo(TODAY.atTime(18, 0));
            assertThat(RecommendSlot.current(TODAY.atTime(23, 59)))
                    .isEqualTo(TODAY.atTime(18, 0));
        }
    }
}
