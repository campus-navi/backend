package com.campusnavi.backend.member.repository;

import com.campusnavi.backend.member.dto.MemberScope;
import com.campusnavi.backend.member.entity.MemberStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.campusnavi.backend.member.entity.QMember.member;
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

    public long countActiveMembersInDepartments(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) return 0L;

        Long count = queryFactory
                .select(memberDepartment.member.id.countDistinct())
                .from(memberDepartment)
                .join(memberDepartment.member, member)
                .where(memberDepartment.department.id.in(departmentIds),
                        member.status.eq(MemberStatus.ACTIVE))
                .fetchOne();
        return count == null ? 0L : count;
    }
}
