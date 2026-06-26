package com.campusnavi.backend.studio.document.entity;

import com.campusnavi.backend.studio.academicplan.document.entity.AcademicPlanMetadata;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(@JsonSubTypes.Type(AcademicPlanMetadata.class))
public interface DocumentMetadata {
}
