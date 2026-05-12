package com.campusnavi.backend.official.post.recommend.repository;

import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FeedRecommendSnapshotRepository
        extends JpaRepository<FeedRecommendSnapshot, Long> {

    Optional<FeedRecommendSnapshot> findByMemberIdAndSlotAt(Long memberId, LocalDateTime slotAt);
}
