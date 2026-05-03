package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.dto.DeadlinePostResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostScopeCondition;
import com.campusnavi.backend.official.post.entity.QOfficialAttachment;
import com.campusnavi.backend.official.post.entity.QOfficialPostNotification;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.campusnavi.backend.global.common.ProcessingStatus.DONE;
import static com.campusnavi.backend.official.post.entity.QOfficialPost.officialPost;
import static com.campusnavi.backend.official.post.entity.QOfficialPostAiMeta.officialPostAiMeta;
import static com.campusnavi.backend.tag.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class OfficialPostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<OfficialPostCardRaw> findRecentPosts(OfficialPostScopeCondition condition) {
        return cardBaseQuery(condition)
                .orderBy(officialPost.publishedAt.desc().nullsLast(), officialPost.crawledAt.desc(), officialPost.id.desc())
                .limit(9)
                .fetch();
    }

    public List<OfficialPostCardRaw> findRecommendPosts(OfficialPostScopeCondition condition, List<Long> tagIds) {
        return cardBaseQuery(condition)
                .where(tag.isRecommendable.isTrue(), tagsIn(tagIds))
                .orderBy(officialPost.publishedAt.desc().nullsLast(), officialPost.crawledAt.desc(), officialPost.id.desc())
                .limit(8)
                .fetch();
    }

    public List<DeadlinePostResponse> findDeadlinePostsForFeed(OfficialPostScopeCondition condition, LocalDate today, Long memberId) {
        return deadlineBaseQuery(condition, today, memberId)
                .limit(8)
                .fetch();
    }

    public List<DeadlinePostResponse> findAllDeadlinePosts(OfficialPostScopeCondition condition, LocalDate today, Long memberId) {
        return deadlineBaseQuery(condition, today, memberId)
                .fetch();
    }

    private JPAQuery<DeadlinePostResponse> deadlineBaseQuery(OfficialPostScopeCondition condition, LocalDate today, Long memberId) {
        QOfficialPostNotification noti = QOfficialPostNotification.officialPostNotification;
        return queryFactory
                .select(Projections.constructor(DeadlinePostResponse.class,
                        officialPost.id,
                        officialPost.title,
                        tag.name,
                        officialPost.publisher,
                        officialPost.publishedAt,
                        officialPostAiMeta.endDate,
                        noti.id.isNotNull()))
                .from(officialPost)
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .join(officialPostAiMeta.tag, tag)
                .leftJoin(noti).on(noti.post.id.eq(officialPost.id).and(noti.memberId.eq(memberId)))
                .where(baseCondition(condition))
                .where(
                        officialPostAiMeta.endDate.isNotNull(),
                        officialPostAiMeta.endDate.goe(today),
                        officialPostAiMeta.endDate.loe(today.plusDays(7))
                )
                .orderBy(officialPostAiMeta.endDate.asc(), officialPost.id.asc());
    }

    private JPAQuery<OfficialPostCardRaw> cardBaseQuery(OfficialPostScopeCondition condition) {
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
                        .and(att.sortOrder.eq((short) 1)))
                .where(baseCondition(condition));
    }

    private BooleanExpression[] baseCondition(OfficialPostScopeCondition condition) {
        return new BooleanExpression[]{
                officialPost.isActive.isTrue(),
                officialPostAiMeta.status.eq(DONE),
                universityScope(condition.universityId()),
                campusScope(condition.campusId()),
                collegeScope(condition.collegeIds()),
                departmentScope(condition.departmentIds())
        };
    }


    private BooleanExpression tagsIn(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }

        return officialPostAiMeta.tag.id.in(tagIds);
    }

    private BooleanExpression universityScope(Long universityId) {
        return officialPost.universityId.isNull().or(officialPost.universityId.eq(universityId));
    }

    private BooleanExpression campusScope(Long campusId) {
        if (campusId == null) return null;
        return officialPost.campusId.isNull().or(officialPost.campusId.eq(campusId));
    }

    private BooleanExpression collegeScope(List<Long> collegeIds) {
        if (collegeIds == null) return null;
        if (collegeIds.isEmpty()) return officialPost.collegeId.isNull();
        return officialPost.collegeId.isNull().or(officialPost.collegeId.in(collegeIds));
    }

    private BooleanExpression departmentScope(List<Long> departmentIds) {
        if (departmentIds == null) return null;
        if (departmentIds.isEmpty()) return officialPost.departmentId.isNull();
        return officialPost.departmentId.isNull().or(officialPost.departmentId.in(departmentIds));
    }
}
