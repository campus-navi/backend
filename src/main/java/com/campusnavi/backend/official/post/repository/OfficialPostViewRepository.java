package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


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
}
