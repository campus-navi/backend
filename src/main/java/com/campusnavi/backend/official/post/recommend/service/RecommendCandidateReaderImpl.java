package com.campusnavi.backend.official.post.recommend.service;

import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
public class RecommendCandidateReaderImpl implements RecommendCandidateReader {

    private static final LocalTime WINDOW_START_TIME = LocalTime.of(9, 0);

    private final FeedRecommendSnapshotRepository snapshotRepository;

    @Override
    public Map<Long, Set<Long>> findCandidatesByMemberIdsAndDate(Collection<Long> memberIds, LocalDate date) {
        if (memberIds.isEmpty()) return Map.of();

        LocalDateTime start = date.atTime(WINDOW_START_TIME);
        LocalDateTime end = start.plusDays(1);

        List<Object[]> rows = snapshotRepository.findRawByMemberIdsAndSlotRange(memberIds, start, end);
        return rows.stream()
                .collect(groupingBy(
                        row -> ((Number) row[0]).longValue(),
                        mapping(row -> ((Number) row[1]).longValue(), toSet())));
    }
}