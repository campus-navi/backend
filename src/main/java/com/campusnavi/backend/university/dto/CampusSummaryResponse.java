package com.campusnavi.backend.university.dto;

import com.campusnavi.backend.university.entity.Campus;

public record CampusSummaryResponse(
        Long id,
        String name,
        String domain
) {
    public static CampusSummaryResponse of(Campus campus) {
        return new CampusSummaryResponse(campus.getId(), campus.getName(),campus.getDomain());
    }
}
