package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPostScrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficialPostScrapRepository extends JpaRepository<OfficialPostScrap, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    Optional<OfficialPostScrap> findByMemberIdAndPostId(Long memberId, Long postId);
}
