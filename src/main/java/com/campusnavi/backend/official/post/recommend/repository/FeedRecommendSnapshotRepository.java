package com.campusnavi.backend.official.post.recommend.repository;

import com.campusnavi.backend.official.post.recommend.entity.FeedRecommendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedRecommendSnapshotRepository
        extends JpaRepository<FeedRecommendSnapshot, Long> {

    Optional<FeedRecommendSnapshot> findFirstByMemberIdOrderBySlotAtDesc(Long memberId);

    @Modifying
    @Query(value = """
            INSERT INTO feed_recommend_snapshot (member_id, slot_at, post_ids)
            VALUES (:memberId, :slotAt, CAST(:postIdsJson AS jsonb))
            ON CONFLICT (member_id, slot_at) DO UPDATE
            SET post_ids = EXCLUDED.post_ids
            """, nativeQuery = true)
    void upsertSlot(@Param("memberId") Long memberId,
                    @Param("slotAt") LocalDateTime slotAt,
                    @Param("postIdsJson") String postIdsJson);

    @Query(value = """
            SELECT s.member_id, pid::bigint
            FROM feed_recommend_snapshot s,
                 jsonb_array_elements_text(s.post_ids) AS pid
            WHERE s.member_id IN (:memberIds)
              AND s.slot_at >= :start
              AND s.slot_at < :end
            """, nativeQuery = true)
    List<Object[]> findRawByMemberIdsAndSlotRange(@Param("memberIds") Collection<Long> memberIds,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Modifying
    @Query("DELETE FROM FeedRecommendSnapshot s WHERE s.slotAt < :before")
    void deleteOlderThan(@Param("before") LocalDateTime before);
}
