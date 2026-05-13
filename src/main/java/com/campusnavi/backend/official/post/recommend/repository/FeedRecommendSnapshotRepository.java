package com.campusnavi.backend.official.post.recommend.repository;

import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedRecommendSnapshotRepository
        extends JpaRepository<FeedRecommendSnapshot, Long> {

    Optional<FeedRecommendSnapshot> findByMemberId(Long memberId);

    @Modifying
    @Query(value = """
            INSERT INTO feed_recommend_snapshot (member_id, post_ids, computed_at)
            VALUES (:memberId, CAST(:postIdsJson AS jsonb), now())
            ON CONFLICT (member_id) DO UPDATE
            SET post_ids    = EXCLUDED.post_ids,
                computed_at = EXCLUDED.computed_at
            """, nativeQuery = true)
    void upsert(@Param("memberId") Long memberId,
                @Param("postIdsJson") String postIdsJson);
}
