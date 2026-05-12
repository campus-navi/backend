package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.List;

import static com.campusnavi.backend.global.common.ProcessingStatus.DONE;
import static com.campusnavi.backend.official.post.entity.QOfficialPost.officialPost;
import static com.campusnavi.backend.official.post.entity.QOfficialPostAiMeta.officialPostAiMeta;

public final class OfficialPostScopeConditions {

    private OfficialPostScopeConditions() {
    }

    public static BooleanExpression[] baseCondition(OfficialPostScopeCondition condition) {
        return new BooleanExpression[]{
                officialPost.isActive.isTrue(),
                officialPostAiMeta.status.eq(DONE),
                universityScope(condition.universityId()),
                campusScope(condition.campusId()),
                collegeScope(condition.collegeIds()),
                departmentScope(condition.departmentIds())
        };
    }

    private static BooleanExpression universityScope(Long universityId) {
        return officialPost.universityId.isNull().or(officialPost.universityId.eq(universityId));
    }

    private static BooleanExpression campusScope(Long campusId) {
        if (campusId == null) return null;
        return officialPost.campusId.isNull().or(officialPost.campusId.eq(campusId));
    }

    private static BooleanExpression collegeScope(List<Long> collegeIds) {
        if (collegeIds == null) return null;
        if (collegeIds.isEmpty()) return officialPost.collegeId.isNull();
        return officialPost.collegeId.isNull().or(officialPost.collegeId.in(collegeIds));
    }

    private static BooleanExpression departmentScope(List<Long> departmentIds) {
        if (departmentIds == null) return null;
        if (departmentIds.isEmpty()) return officialPost.departmentId.isNull();
        return officialPost.departmentId.isNull().or(officialPost.departmentId.in(departmentIds));
    }
}
