package com.campusnavi.backend.notification.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.notification.dto.MissedNotice;
import com.campusnavi.backend.notification.dto.MissedNoticeCard;
import com.campusnavi.backend.notification.dto.MissedNoticeRaw;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.notification.repository.NotificationQueryRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityNotificationService {

    private final ActivityNotificationSnapshotRepository snapshotRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final OfficialPostViewRepository officialPostViewRepository;

    public List<MissedNoticeCard> getActivityCards(AuthContext context) {
        List<ActivityNotificationSnapshot> snapshots =
                snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(context.memberId());
        if (snapshots.isEmpty()) {
            return List.of();
        }

        Set<Long> allPostIds = snapshots.stream()
                .flatMap(s -> s.getPostIds().stream())
                .collect(Collectors.toSet());
        Set<Long> validIds = notificationQueryRepository.findMissedNoticesByIds(allPostIds).stream()
                .map(MissedNoticeRaw::postId)
                .collect(Collectors.toSet());
        Set<Long> viewedIds = officialPostViewRepository
                .findPostIdsByMemberIdAndPostIdIn(context.memberId(), allPostIds);

        List<MissedNoticeCard> cards = new ArrayList<>();
        for (ActivityNotificationSnapshot snapshot : snapshots) {
            int count = (int) snapshot.getPostIds().stream()
                    .distinct()
                    .filter(validIds::contains)
                    .filter(pid -> !viewedIds.contains(pid))
                    .count();
            cards.add(new MissedNoticeCard(snapshot.getMissedDate(), count));
        }
        return cards;
    }

    public List<MissedNotice> getActivityDetail(AuthContext context, LocalDate missedDate) {
        ActivityNotificationSnapshot snapshot = snapshotRepository
                .findByMemberIdAndMissedDate(context.memberId(), missedDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOTIFICATION_NOT_FOUND));

        List<Long> postIds = snapshot.getPostIds();
        if (postIds.isEmpty()) {
            return List.of();
        }

        Set<Long> viewedIds = officialPostViewRepository
                .findPostIdsByMemberIdAndPostIdIn(context.memberId(), postIds);

        List<Long> missedIds = postIds.stream()
                .distinct()
                .filter(pid -> !viewedIds.contains(pid))
                .toList();
        if (missedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, MissedNoticeRaw> rawMap = notificationQueryRepository.findMissedNoticesByIds(missedIds).stream()
                .collect(Collectors.toMap(MissedNoticeRaw::postId, Function.identity()));

        return missedIds.stream()
                .map(rawMap::get)
                .filter(Objects::nonNull)
                .map(r -> new MissedNotice(r.postId(), r.title(), r.tagName(),
                        r.publishedAt(), r.endDate()))
                .toList();
    }
}
