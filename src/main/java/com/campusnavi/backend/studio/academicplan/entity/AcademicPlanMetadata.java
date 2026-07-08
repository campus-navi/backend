package com.campusnavi.backend.studio.academicplan.entity;

import com.campusnavi.backend.studio.document.entity.DocumentMetadata;

public record AcademicPlanMetadata(
        MajorType majorType,
        String campusName,
        String targetName
) implements DocumentMetadata {
}
