package com.campusnavi.backend.community.post.repository;

import com.campusnavi.backend.community.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    @Query("""
            SELECT p FROM Post p
            WHERE p.universityId = :universityId
            AND p.deletedAt IS NULL
            AND (:cursorId IS NULL OR p.id < :cursorId)
            ORDER BY p.id DESC
            LIMIT :size
            """)
    List<Post> findPosts(@Param("universityId") Long universityId,
                         @Param("cursorId") Long cursorId,
                         @Param("size") int size);
}
