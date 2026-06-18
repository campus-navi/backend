package com.campusnavi.backend.studio.academicplan.api.dto;

import com.campusnavi.backend.studio.academicplan.entity.TargetMajor;

public record TargetMajorResponse(
        Long id,
        String name
) {
    public static TargetMajorResponse from(TargetMajor targetMajor) {
        return new TargetMajorResponse(targetMajor.getId(), targetMajor.getName());
    }
}
