package com.campusnavi.backend.notification.repository;

import com.campusnavi.backend.notification.dto.MissedNoticeRaw;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.campusnavi.backend.official.post.entity.QOfficialPost.officialPost;
import static com.campusnavi.backend.official.post.entity.QOfficialPostAiMeta.officialPostAiMeta;
import static com.campusnavi.backend.tag.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MissedNoticeRaw> findMissedNoticesByIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .select(Projections.constructor(MissedNoticeRaw.class,
                        officialPost.id,
                        officialPost.title,
                        tag.name,
                        officialPost.publishedAt,
                        officialPostAiMeta.endDate))
                .from(officialPost)
                .join(officialPostAiMeta).on(officialPostAiMeta.officialPost.id.eq(officialPost.id))
                .join(officialPostAiMeta.tag, tag)
                .where(officialPost.id.in(postIds))
                .where(officialPostAiMeta.endDate.isNull()
                        .or(officialPostAiMeta.endDate.goe(LocalDate.now())))
                .fetch();
    }
}
