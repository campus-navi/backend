package com.campusnavi.backend.community.comment.repository;

import com.campusnavi.backend.community.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :commentId")
    void incrementCommentCount(Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = GREATEST(c.replyCount - 1, 0) WHERE c.id = :commentId")
    void decrementCommentCount(Long commentId);

    Optional<Comment> findByIdAndPostIdAndDeletedAtIsNull(Long id, Long postId);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
