package com.campusnavi.backend.studio.academicplan.api.dto;

import com.campusnavi.backend.university.entity.Department;

public record TargetDepartmentResponse(
        Long id,
        String name
) {
    public static TargetDepartmentResponse from(Department department) {
        return new TargetDepartmentResponse(department.getId(), department.getName());
    }
}
