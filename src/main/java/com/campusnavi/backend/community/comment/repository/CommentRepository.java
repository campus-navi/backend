package com.campusnavi.backend.community.comment.repository;

import com.campusnavi.backend.community.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :commentId")
    void incrementCommentCount(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = GREATEST(c.replyCount - 1, 0) WHERE c.id = :commentId")
    void decrementCommentCount(@Param("commentId") Long commentId);

    Optional<Comment> findByIdAndPostIdAndDeletedAtIsNull(Long id, Long postId);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.member
            WHERE c.post.id = :postId
            AND c.parent IS NULL
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findParentComments(@Param("postId") Long postId);

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.member
            WHERE c.parent.id IN :parentIds
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findRepliesByParentId(@Param("parentIds") List<Long> parentIds);
}
