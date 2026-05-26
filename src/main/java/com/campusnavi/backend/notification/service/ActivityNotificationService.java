package com.campusnavi.backend.notification.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.notification.dto.MissedNoticeCard;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.notification.repository.NotificationQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostCardRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import com.campusnavi.backend.official.post.service.OfficialPostCardResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
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
    private final OfficialPostCardResponseMapper cardResponseMapper;

    public List<MissedNoticeCard> getActivityCards(AuthContext context) {
        List<ActivityNotificationSnapshot> snapshots =
                snapshotRepository.findAllByMemberIdOrderByMissedDateDesc(context.memberId());
        if (snapshots.isEmpty()) {
            return List.of();
        }

        Set<Long> allPostIds = snapshots.stream()
                .flatMap(s -> s.getPostIds().stream())
                .collect(Collectors.toSet());
        Set<Long> validIds = new HashSet<>(
                notificationQueryRepository.findValidPostIdsByIds(allPostIds));
        Set<Long> viewedIds = officialPostViewRepository
                .findPostIdsByMemberIdAndPostIdIn(context.memberId(), allPostIds);

        List<MissedNoticeCard> cards = new ArrayList<>();
        for (ActivityNotificationSnapshot snapshot : snapshots) {
            int count = (int) snapshot.getPostIds().stream()
                    .distinct()
                    .filter(validIds::contains)
                    .filter(pid -> !viewedIds.contains(pid))
                    .count();
            if (count > 0) {
                cards.add(new MissedNoticeCard(snapshot.getMissedDate(), count));
            }
        }
        return cards;
    }

    public List<OfficialPostCardResponse> getActivityDetail(AuthContext context, LocalDate missedDate) {
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

        Map<Long, OfficialPostCardRaw> rawMap = notificationQueryRepository.findMissedCardsByIds(missedIds).stream()
                .collect(Collectors.toMap(OfficialPostCardRaw::postId, Function.identity()));

        return missedIds.stream()
                .map(rawMap::get)
                .filter(Objects::nonNull)
                .map(cardResponseMapper::toResponse)
                .toList();
    }
}
