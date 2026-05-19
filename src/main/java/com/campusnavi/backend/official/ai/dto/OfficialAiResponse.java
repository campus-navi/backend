package com.campusnavi.backend.official.ai.dto;


import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OfficialAiResponse(
        String summary,
        Integer targetGradeMin,
        Integer targetGradeMax,
        String tagCode,
        List<String> keywords,
        String contactPhone,
        String contactEmail,
        LocalDate startDate,
        @JsonDeserialize(using = StartTimeDeserializer.class)
        LocalTime startTime,
        LocalDate endDate,
        @JsonDeserialize(using = EndTimeDeserializer.class)
        LocalTime endTime,
        String requiredDocuments,
        String applyMethodType,
        String applyMethodDetail,
        String eligibility,
        boolean isApplicable
) {
}
