package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.dto.RecentViewResponse;
import com.campusnavi.backend.official.post.entity.OfficialPostView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface OfficialPostViewRepository extends JpaRepository<OfficialPostView, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO official_post_view (member_id, post_id)
            VALUES (:memberId, :postId)
            ON CONFLICT (member_id, post_id) DO UPDATE
            SET last_viewed_at = now(),
                view_count     = official_post_view.view_count + 1
            """, nativeQuery = true)
    void upsert(@Param("memberId") Long memberId,
                @Param("postId") Long postId);

    List<OfficialPostView> findByMemberIdInAndPostIdIn(Collection<Long> memberIds,
                                                       Collection<Long> postIds);

    @Query("SELECT v.postId FROM OfficialPostView v " +
           "WHERE v.memberId = :memberId AND v.postId IN :postIds")
    Set<Long> findPostIdsByMemberIdAndPostIdIn(@Param("memberId") Long memberId,
                                               @Param("postIds") Collection<Long> postIds);

    void deleteByMemberIdAndPostId(Long memberId, Long postId);

    @Query("""
            SELECT new com.campusnavi.backend.official.post.dto.RecentViewResponse(
                p.id, p.title, t.name, m.endDate, v.lastViewedAt)
            FROM OfficialPostView v
            JOIN OfficialPost p ON p.id = v.postId
            LEFT JOIN OfficialPostAiMeta m ON m.officialPost = p
            LEFT JOIN m.tag t
            WHERE v.memberId = :memberId
              AND (:cursor IS NULL OR v.lastViewedAt < :cursor)
            ORDER BY v.lastViewedAt DESC
            """)
    List<RecentViewResponse> findRecentViews(@Param("memberId") Long memberId,
                                             @Param("cursor") LocalDateTime cursor,
                                             Pageable pageable);
}
