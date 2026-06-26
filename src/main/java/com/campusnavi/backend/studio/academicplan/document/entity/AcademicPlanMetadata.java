package com.campusnavi.backend.studio.academicplan.document.entity;

import com.campusnavi.backend.studio.academicplan.entity.MajorType;

public record AcademicPlanMetadata(
        MajorType majorType,
        String campusName,
        String targetName
) {
}
