package com.campusnavi.backend.official.post.recommend.scheduler;

import com.campusnavi.backend.official.post.recommend.repository.FeedRecommendSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RecommendSnapshotCleaner {

    private final FeedRecommendSnapshotRepository snapshotRepository;

    @Transactional
    public void cleanupOlderThan(LocalDateTime before) {
        snapshotRepository.deleteOlderThan(before);
    }
}
