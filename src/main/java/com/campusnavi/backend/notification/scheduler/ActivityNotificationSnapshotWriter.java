package com.campusnavi.backend.notification.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.official.post.entity.OfficialPostView;
import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
public class ActivityNotificationSnapshotWriter {

    private static final LocalTime WINDOW_START_TIME = LocalTime.of(9, 0);

    private final FeedRecommendSnapshotRepository recommendSnapshotRepository;
    private final OfficialPostViewRepository viewRepository;
    private final ActivityNotificationSnapshotRepository snapshotRepository;

    @Transactional
    public void writeChunk(List<Member> chunk, LocalDate missedDate) {
        List<Long> memberIds = chunk.stream().map(Member::getId).toList();

        Set<Long> alreadyProcessed = snapshotRepository
                .findMemberIdsByMissedDateAndMemberIdIn(missedDate, memberIds);
        List<Long> targetMemberIds = memberIds.stream()
                .filter(id -> !alreadyProcessed.contains(id))
                .toList();
        if (targetMemberIds.isEmpty()) return;

        LocalDateTime windowStart = missedDate.atTime(WINDOW_START_TIME);
        LocalDateTime windowEnd = windowStart.plusDays(1);

        Map<Long, Set<Long>> candidateByMember = recommendSnapshotRepository
                .findRawByMemberIdsAndSlotRange(targetMemberIds, windowStart, windowEnd).stream()
                .collect(groupingBy(row -> ((Number) row[0]).longValue(),
                        mapping(row -> ((Number) row[1]).longValue(), toSet())));
        if (candidateByMember.isEmpty()) return;

        Set<Long> allPostIds = candidateByMember.values().stream()
                .flatMap(Set::stream).collect(toSet());

        Map<Long, Set<Long>> viewedByMember = viewRepository
                .findByMemberIdInAndPostIdIn(targetMemberIds, allPostIds).stream()
                .collect(groupingBy(OfficialPostView::getMemberId,
                        mapping(OfficialPostView::getPostId, toSet())));

        List<ActivityNotificationSnapshot> toCreate = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : candidateByMember.entrySet()) {
            Long memberId = entry.getKey();
            Set<Long> candidates = entry.getValue();
            Set<Long> viewed = viewedByMember.getOrDefault(memberId, Set.of());
            List<Long> missed = candidates.stream()
                    .filter(pid -> !viewed.contains(pid))
                    .toList();
            if (!missed.isEmpty()) {
                toCreate.add(ActivityNotificationSnapshot.of(memberId, missedDate, missed));
            }
        }
        snapshotRepository.saveAll(toCreate);
    }

    @Transactional
    public void cleanupOlderThan(LocalDate before) {
        snapshotRepository.deleteOlderThan(before);
    }
}
