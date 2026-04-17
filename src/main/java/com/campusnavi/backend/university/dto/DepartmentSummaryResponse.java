package com.campusnavi.backend.university.dto;

import com.campusnavi.backend.university.entity.Department;

public record DepartmentSummaryResponse(
        Long id,
        String name
) {
    public static DepartmentSummaryResponse of(Department department) {
        return new DepartmentSummaryResponse(department.getId(), department.getName());
    }
}
