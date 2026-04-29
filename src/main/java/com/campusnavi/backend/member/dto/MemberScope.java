package com.campusnavi.backend.member.dto;

public record MemberScope(
        Long campusId,
        Long collegeId,
        Long departmentId
) {
}
