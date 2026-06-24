package com.campusnavi.backend.notification.scheduler;

import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.notification.entity.ActivityNotificationSnapshot;
import com.campusnavi.backend.notification.repository.ActivityNotificationSnapshotRepository;
import com.campusnavi.backend.official.post.entity.OfficialPostView;
import com.campusnavi.backend.official.post.recommend.service.RecommendCandidateReader;
import com.campusnavi.backend.official.post.repository.OfficialPostViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final RecommendCandidateReader recommendCandidateReader;
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

        Map<Long, Set<Long>> candidateByMember =
                recommendCandidateReader.findCandidatesByMemberIdsAndDate(targetMemberIds, missedDate);
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
