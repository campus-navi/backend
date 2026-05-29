package com.campusnavi.backend.notification.repository;

import com.campusnavi.backend.notification.dto.RemindNotice;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.entity.QOfficialAttachment;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.campusnavi.backend.official.post.entity.QOfficialPost.officialPost;
import static com.campusnavi.backend.official.post.entity.QOfficialPostAiMeta.officialPostAiMeta;
import static com.campusnavi.backend.official.post.entity.QOfficialPostNotification.officialPostNotification;
import static com.campusnavi.backend.tag.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Long> findValidPostIdsByIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .select(officialPost.id)
                .from(officialPost)
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .where(officialPost.id.in(postIds))
                .where(officialPostAiMeta.endDate.isNull()
                        .or(officialPostAiMeta.endDate.goe(LocalDate.now())))
                .fetch();
    }

    public List<OfficialPostCardRaw> findMissedCardsByIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
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
                .where(officialPost.id.in(postIds))
                .where(officialPostAiMeta.endDate.isNull()
                        .or(officialPostAiMeta.endDate.goe(LocalDate.now())))
                .fetch();
    }

    public List<RemindNotice> findRemindNoticesByMemberId(Long memberId) {
        return queryFactory
                .select(Projections.constructor(RemindNotice.class,
                        officialPost.id,
                        officialPost.title,
                        tag.name,
                        officialPostAiMeta.endDate))
                .from(officialPostNotification)
                .join(officialPost).on(officialPost.id.eq(officialPostNotification.post.id))
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .join(officialPostAiMeta.tag, tag)
                .where(officialPostNotification.memberId.eq(memberId)
                        .and(officialPostAiMeta.endDate.isNotNull())
                        .and(officialPostAiMeta.endDate.goe(LocalDate.now())))
                .orderBy(officialPostAiMeta.endDate.asc())
                .fetch();
    }

    public long countActiveRemindsByMemberId(Long memberId) {
        Long count = queryFactory
                .select(officialPost.id.count())
                .from(officialPostNotification)
                .join(officialPost).on(officialPost.id.eq(officialPostNotification.post.id))
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .where(officialPostNotification.memberId.eq(memberId)
                        .and(officialPostAiMeta.endDate.isNotNull())
                        .and(officialPostAiMeta.endDate.goe(LocalDate.now())))
                .fetchOne();
        return count == null ? 0L : count;
    }
}
