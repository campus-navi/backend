package com.campusnavi.backend.community.post.repository;

import com.campusnavi.backend.community.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post,Long> {
    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.universityId = :universityId
            AND p.deletedAt IS NULL
            AND (:cursorId IS NULL OR p.id < :cursorId)
            ORDER BY p.id DESC
            LIMIT :size
            """)
    List<Post> findLatestPosts(@Param("universityId") Long universityId,
                               @Param("cursorId") Long cursorId,
                               @Param("size") int size);

    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.universityId = :universityId
            AND p.deletedAt IS NULL
            AND p.likeCount >= 10
            AND p.scrapCount >= 10
            AND (:cursorId IS NULL OR p.id < :cursorId)
            ORDER BY p.id DESC
            LIMIT :size
            """)
    List<Post> findPopularPosts(@Param("universityId") Long universityId,
                                @Param("cursorId") Long cursorId,
                                @Param("size") int size);

    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.universityId = :universityId
            AND p.deletedAt IS NULL
            AND (:cursorId IS NULL
                 OR p.scrapCount < :cursorScrapCount
                 OR (p.scrapCount = :cursorScrapCount AND p.id < :cursorId))
            ORDER BY p.scrapCount DESC, p.id DESC
            LIMIT :size
            """)
    List<Post> findScrapPosts(@Param("universityId") Long universityId,
                              @Param("cursorId") Long cursorId,
                              @Param("cursorScrapCount") Integer cursorScrapCount,
                              @Param("size") int size);

    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.id = :id
            AND p.deletedAt IS NULL
            AND p.universityId = :universityId
            """)
    Optional<Post> findByIdWithMember(Long id, Long universityId);
}
