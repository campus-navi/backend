package com.campusnavi.backend.studio.academicplan.dto;

import com.campusnavi.backend.university.entity.Campus;

public record TargetCampusResponse(
        Long id,
        String name
) {
    public static TargetCampusResponse from(Campus campus) {
        return new TargetCampusResponse(campus.getId(), campus.getName());
    }
}
