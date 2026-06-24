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

    @Query("SELECT s FROM FeedRecommendSnapshot s JOIN FETCH s.items WHERE s.memberId = :memberId ORDER BY s.slotAt DESC LIMIT 1")
    Optional<FeedRecommendSnapshot> findLatestByMemberId(@Param("memberId") Long memberId);

    Optional<FeedRecommendSnapshot> findByMemberIdAndSlotAt(Long memberId, LocalDateTime slotAt);

    @Modifying
    @Query(value = "INSERT INTO feed_recommend_snapshot (member_id, slot_at, created_at) " +
                   "VALUES (:memberId, :slotAt, now()) ON CONFLICT (member_id, slot_at) DO NOTHING",
           nativeQuery = true)
    void insertIfAbsent(@Param("memberId") Long memberId, @Param("slotAt") LocalDateTime slotAt);

    @Query(value = """
            SELECT s.member_id, i.post_id
            FROM feed_recommend_snapshot s
            JOIN feed_recommend_snapshot_item i ON i.snapshot_id = s.id
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
