package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPostNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficialPostNotificationRepository extends JpaRepository<OfficialPostNotification, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    Optional<OfficialPostNotification> findByMemberIdAndPostId(Long memberId, Long postId);
}
