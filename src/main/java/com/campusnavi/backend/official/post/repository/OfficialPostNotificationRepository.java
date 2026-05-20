package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPostNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OfficialPostNotificationRepository extends JpaRepository<OfficialPostNotification, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    Optional<OfficialPostNotification> findByMemberIdAndPostId(Long memberId, Long postId);

    @Query("SELECT n.post.id FROM OfficialPostNotification n "
            + "WHERE n.memberId = :memberId AND n.post.id IN :postIds")
    List<Long> findExistingPostIds(@Param("memberId") Long memberId,
                                   @Param("postIds") Collection<Long> postIds);

    @Modifying
    @Query("DELETE FROM OfficialPostNotification n "
            + "WHERE n.memberId = :memberId AND n.post.id IN :postIds")
    int deleteByMemberIdAndPostIdIn(@Param("memberId") Long memberId,
                                    @Param("postIds") Collection<Long> postIds);
}
