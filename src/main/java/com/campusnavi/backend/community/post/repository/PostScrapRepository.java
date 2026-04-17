package com.campusnavi.backend.community.post.repository;

import com.campusnavi.backend.community.post.entity.PostScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    Optional<PostScrap> findByMemberIdAndPostId(Long memberId, Long postId);

    @Query("SELECT ps.post.id FROM PostScrap ps WHERE ps.member.id = :memberId AND ps.post.id IN :postIds")
    List<Long> findScrapedPostIds(@Param("memberId") Long memberId, @Param("postIds") List<Long> postIds);
}
