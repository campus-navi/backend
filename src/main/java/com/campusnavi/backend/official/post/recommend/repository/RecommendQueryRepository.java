package com.campusnavi.backend.official.post.recommend.repository;

import com.campusnavi.backend.member.entity.MemberStatus;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostRecommendCandidateRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.dto.OfficialPostStatsRaw;
import com.campusnavi.backend.official.post.entity.QOfficialAttachment;
import com.campusnavi.backend.official.post.entity.QOfficialPostView;
import com.campusnavi.backend.official.post.recommend.config.RecommendProperties;
import com.campusnavi.backend.official.post.repository.OfficialPostScopeConditions;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.campusnavi.backend.member.entity.QMember.member;
import static com.campusnavi.backend.member.entity.QMemberDepartment.memberDepartment;
import static com.campusnavi.backend.official.post.entity.QOfficialPost.officialPost;
import static com.campusnavi.backend.official.post.entity.QOfficialPostAiMeta.officialPostAiMeta;
import static com.campusnavi.backend.tag.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class RecommendQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final RecommendProperties recommendProperties;

    public List<OfficialPostRecommendCandidateRaw> findRecommendCandidates(
            OfficialPostScopeCondition condition, LocalDateTime slotAt, Long memberId) {
        LocalDate slotDate = slotAt.toLocalDate();
        LocalDate freshnessFrom = slotDate.minusDays(recommendProperties.freshnessDays());
        QOfficialAttachment att = new QOfficialAttachment("att");
        QOfficialPostView view = QOfficialPostView.officialPostView;

        return queryFactory
                .select(Projections.constructor(OfficialPostRecommendCandidateRaw.class,
                        officialPost.id,
                        officialPost.title,
                        tag.id,
                        tag.name,
                        officialPostAiMeta.summary,
                        att.s3Key,
                        officialPost.publishedAt))
                .from(officialPost)
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .join(officialPostAiMeta.tag, tag)
                .leftJoin(att).on(att.post.id.eq(officialPost.id)
                        .and(att.isImage.isTrue())
                        .and(att.sortOrder.eq((short) 0)))
                .leftJoin(view).on(view.postId.eq(officialPost.id)
                        .and(view.memberId.eq(memberId)))
                .where(OfficialPostScopeConditions.baseCondition(condition))
                .where(tag.isRecommendable.isTrue())
                .where(officialPost.publishedAt.goe(freshnessFrom))
                .where(officialPost.crawledAt.loe(slotAt))
                .where(officialPostAiMeta.endDate.isNull()
                        .or(officialPostAiMeta.endDate.goe(slotDate)))
                .where(view.id.isNull())
                .orderBy(officialPost.publishedAt.desc(),
                        officialPost.id.desc())
                .limit(recommendProperties.candidateLimit())
                .fetch();
    }

    public List<OfficialPostCardRaw> findCardsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        QOfficialAttachment att = new QOfficialAttachment("att");
        return queryFactory
                .select(Projections.constructor(OfficialPostCardRaw.class,
                        officialPost.id,
                        officialPost.title,
                        tag.name,
                        officialPostAiMeta.summary,
                        att.s3Key,
                        officialPost.publishedAt))
                .from(officialPost)
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .join(officialPostAiMeta.tag, tag)
                .leftJoin(att).on(att.post.id.eq(officialPost.id)
                        .and(att.isImage.isTrue())
                        .and(att.sortOrder.eq((short) 0)))
                .where(officialPost.id.in(ids))
                .fetch();
    }

    public List<OfficialPostStatsRaw> findStatsByPostIds(
            List<Long> postIds, int reqAdmissionYear, int reqGrade,
            List<Long> requesterDeptIds) {
        if (postIds == null || postIds.isEmpty()) return List.of();
        if (requesterDeptIds == null || requesterDeptIds.isEmpty()) return List.of();

        QOfficialPostView view = QOfficialPostView.officialPostView;

        NumberExpression<Integer> capped =
                Expressions.numberTemplate(Integer.class, "LEAST({0}, {1})", view.viewCount, recommendProperties.viewCap());

        NumberExpression<Long> sameAdmissionSum =
                Expressions.numberTemplate(Long.class,
                        "sum(case when {0} = {1} then 1 else 0 end)",
                        member.admissionYear, reqAdmissionYear);

        NumberExpression<Long> sameGradeSum =
                Expressions.numberTemplate(Long.class,
                        "sum(case when {0} = {1} then 1 else 0 end)",
                        member.grade, reqGrade);

        return queryFactory
                .select(Projections.constructor(OfficialPostStatsRaw.class,
                        view.postId,
                        capped.sumLong().coalesce(0L),
                        sameAdmissionSum.coalesce(0L),
                        sameGradeSum.coalesce(0L),
                        view.memberId.countDistinct()))
                .from(view)
                .join(member).on(member.id.eq(view.memberId)
                        .and(member.status.eq(MemberStatus.ACTIVE)))
                .where(view.postId.in(postIds))
                .where(JPAExpressions.selectOne()
                        .from(memberDepartment)
                        .where(memberDepartment.member.id.eq(member.id)
                                .and(memberDepartment.department.id.in(requesterDeptIds)))
                        .exists())
                .groupBy(view.postId)
                .fetch();
    }
}
