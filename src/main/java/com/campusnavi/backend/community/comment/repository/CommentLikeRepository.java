package com.campusnavi.backend.community.comment.repository;

import com.campusnavi.backend.community.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike,Long> {
    Optional<CommentLike> findByMemberIdAndCommentId(Long memberId, Long commentId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.member.id = :memberId AND cl.comment.id IN :commentIds")
    List<Long> findLikedCommentIds(@Param("memberId") Long memberId, @Param("commentIds") List<Long> commentIds);
}
