package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.official.ai.dto.OfficialAiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialAiResponseDeserializeTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private OfficialAiResponse read(String startTime, String endTime) {
        String json = """
                {"start_time": "%s", "end_time": "%s", "is_applicable": true}
                """.formatted(startTime, endTime);
        return mapper.readValue(json, OfficialAiResponse.class);
    }

    @Nested
    @DisplayName("24:00:00 변환")
    class Midnight {

        @Test
        @DisplayName("startTime 24:00:00 은 00:00:00 으로 변환된다")
        void startTimeToMidnight() {
            OfficialAiResponse response = read("24:00:00", "12:00:00");

            assertThat(response.startTime()).isEqualTo(LocalTime.MIDNIGHT);
        }

        @Test
        @DisplayName("endTime 24:00:00 은 23:59:59 로 변환된다")
        void endTimeToEndOfDay() {
            OfficialAiResponse response = read("09:00:00", "24:00:00");

            assertThat(response.endTime()).isEqualTo(LocalTime.of(23, 59, 59));
        }

        @Test
        @DisplayName("24:00 도 동일하게 변환된다")
        void shortFormat() {
            OfficialAiResponse response = read("24:00", "24:00");

            assertThat(response.startTime()).isEqualTo(LocalTime.MIDNIGHT);
            assertThat(response.endTime()).isEqualTo(LocalTime.of(23, 59, 59));
        }
    }

    @Nested
    @DisplayName("정상 값")
    class Normal {

        @Test
        @DisplayName("일반 시각은 그대로 파싱된다")
        void parsesAsIs() {
            OfficialAiResponse response = read("09:30:00", "18:00:00");

            assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 30));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(18, 0));
        }

        @Test
        @DisplayName("빈 문자열은 null 로 처리된다")
        void blankToNull() {
            OfficialAiResponse response = read("", "");

            assertThat(response.startTime()).isNull();
            assertThat(response.endTime()).isNull();
        }
    }
}
