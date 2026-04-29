package com.campusnavi.backend.official.post.dto;

import com.campusnavi.backend.member.dto.MemberScope;

import java.util.List;
import java.util.Objects;

public record OfficialPostScopeCondition(
        Long universityId,
        Long campusId,
        List<Long> collegeIds,
        List<Long> departmentIds
) {
    public static OfficialPostScopeCondition from(Long universityId, List<MemberScope> scopes) {
        if (scopes.isEmpty()) {
            return new OfficialPostScopeCondition(universityId, null, null, null);
        }
        return new OfficialPostScopeCondition(
                universityId,
                scopes.getFirst().campusId(),
                toCollegeIds(scopes),
                toDepartmentIds(scopes));
    }

    private static List<Long> toCollegeIds(List<MemberScope> scopes) {
        return scopes.stream().map(MemberScope::collegeId).filter(Objects::nonNull).toList();
    }

    private static List<Long> toDepartmentIds(List<MemberScope> scopes) {
        return scopes.stream().map(MemberScope::departmentId).filter(Objects::nonNull).toList();
    }
}
