package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.dto.*;
import com.campusnavi.backend.official.post.entity.QOfficialAttachment;
import com.campusnavi.backend.official.post.entity.QOfficialPostNotification;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.campusnavi.backend.official.post.dto.OfficialPostListSort.DEADLINE;
import static com.campusnavi.backend.official.post.entity.QOfficialPost.officialPost;
import static com.campusnavi.backend.official.post.entity.QOfficialPostAiMeta.officialPostAiMeta;
import static com.campusnavi.backend.official.post.repository.OfficialPostScopeConditions.baseCondition;
import static com.campusnavi.backend.tag.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class OfficialPostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<OfficialPostCardRaw> findRecentPosts(OfficialPostScopeCondition condition, LocalDateTime slotAt) {
        return cardBaseQuery(condition)
                .where(officialPost.crawledAt.loe(slotAt))
                .orderBy(officialPost.publishedAt.desc().nullsLast(), officialPost.crawledAt.desc(), officialPost.id.desc())
                .limit(9)
                .fetch();
    }

    public List<DeadlinePostRaw> findDeadlinePostsForFeed(OfficialPostScopeCondition condition, Long memberId) {
        return deadlineBaseQuery(condition, memberId)
                .limit(8)
                .fetch();
    }

    public List<DeadlinePostRaw> findAllDeadlinePosts(OfficialPostScopeCondition condition, Long memberId) {
        return deadlineBaseQuery(condition, memberId)
                .fetch();
    }

    public List<OfficialPostSummaryRaw> findList(
            OfficialPostScopeCondition condition,
            String q, Long tagId,
            OfficialPostListSort sort,
            LocalDate latestCursorPublishedAt, Long latestCursorId,
            LocalDate deadlineCursorDate, Long deadlineCursorId,
            int limit) {

        LocalDate today = LocalDate.now();

        JPAQuery<OfficialPostSummaryRaw> query = queryFactory
                .select(Projections.constructor(OfficialPostSummaryRaw.class,
                        officialPost.id,
                        officialPost.title,
                        tag.name,
                        officialPost.publishedAt,
                        officialPostAiMeta.endDate))
                .from(officialPost)
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .join(officialPostAiMeta.tag, tag)
                .where(baseCondition(condition))
                .where(keywordFilter(q))
                .where(tagFilter(tagId));

        if (sort == DEADLINE) {
            query.where(
                    officialPostAiMeta.endDate.isNotNull(),
                    officialPostAiMeta.endDate.goe(today),
                    deadlineCursorFilter(deadlineCursorDate, deadlineCursorId));
            query.orderBy(officialPostAiMeta.endDate.asc(), officialPost.id.asc());
        } else {
            query.where(latestCursorFilter(latestCursorPublishedAt, latestCursorId));
            query.orderBy(officialPost.publishedAt.desc().nullsLast(), officialPost.id.desc());
        }

        return query.limit(limit).fetch();
    }

    private BooleanExpression keywordFilter(String q) {
        if (q == null || q.isBlank()) return null;
        String kw = q.trim();
        return officialPost.title.containsIgnoreCase(kw)
                .or(officialPostAiMeta.summary.containsIgnoreCase(kw));
    }

    private BooleanExpression tagFilter(Long tagId) {
        if (tagId == null) return null;
        return officialPostAiMeta.tag.id.eq(tagId);
    }

    private BooleanExpression latestCursorFilter(LocalDate cursorPublishedAt, Long cursorId) {
        if (cursorId == null) return null;
        if (cursorPublishedAt == null) {
            return officialPost.publishedAt.isNull().and(officialPost.id.lt(cursorId));
        }
        return officialPost.publishedAt.lt(cursorPublishedAt)
                .or(officialPost.publishedAt.eq(cursorPublishedAt).and(officialPost.id.lt(cursorId)))
                .or(officialPost.publishedAt.isNull());
    }

    private BooleanExpression deadlineCursorFilter(LocalDate cursorDate, Long cursorId) {
        if (cursorDate == null || cursorId == null) return null;
        return officialPostAiMeta.endDate.gt(cursorDate)
                .or(officialPostAiMeta.endDate.eq(cursorDate).and(officialPost.id.gt(cursorId)));
    }

    private JPAQuery<DeadlinePostRaw> deadlineBaseQuery(OfficialPostScopeCondition condition, Long memberId) {
        LocalDate today = LocalDate.now();
        QOfficialPostNotification noti = QOfficialPostNotification.officialPostNotification;
        return queryFactory
                .select(Projections.constructor(DeadlinePostRaw.class,
                        officialPost.id,
                        officialPost.title,
                        tag.name,
                        officialPost.publisher,
                        officialPost.publishedAt,
                        officialPostAiMeta.endDate,
                        officialPostAiMeta.endTime,
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
                        .and(att.sortOrder.eq(0)))
                .where(baseCondition(condition));
    }
}
