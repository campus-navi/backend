package com.campusnavi.backend.member.repository;

import com.campusnavi.backend.member.dto.MemberScope;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.campusnavi.backend.member.entity.QMemberDepartment.memberDepartment;
import static com.campusnavi.backend.university.entity.QDepartment.department;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MemberScope> findScopesByMemberId(Long memberId) {
        return queryFactory
                .select(Projections.constructor(MemberScope.class,
                        department.campus.id,
                        department.college.id,
                        department.id))
                .from(memberDepartment)
                .join(memberDepartment.department,department)
                .where(memberDepartment.member.id.eq(memberId))
                .fetch();
    }
}
