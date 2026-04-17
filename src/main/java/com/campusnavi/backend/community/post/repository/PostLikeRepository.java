package com.campusnavi.backend.community.post.repository;

import com.campusnavi.backend.community.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByMemberIdAndPostId(Long memberId, Long postId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.member.id = :memberId AND pl.post.id IN :postIds")
    List<Long> findLikedPostIds(@Param("memberId") Long memberId, @Param("postIds") List<Long> postIds);
}
