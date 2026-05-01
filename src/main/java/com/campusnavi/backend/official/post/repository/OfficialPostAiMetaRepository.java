package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfficialPostAiMetaRepository extends JpaRepository<OfficialPostAiMeta,Long> {
    @EntityGraph(attributePaths = "officialPost")
    Page<OfficialPostAiMeta> findAllByStatus(ProcessingStatus status, Pageable pageable);

    Optional<OfficialPostAiMeta> findByOfficialPostId(Long postId);

    @EntityGraph(attributePaths = "officialPost")
    List<OfficialPostAiMeta> findAllByStatusAndRetryCountLessThan(ProcessingStatus status, int retryCountIsLessThan);

    @Query("SELECT m FROM OfficialPostAiMeta m LEFT JOIN FETCH m.tag WHERE m.officialPost.id = :postId AND m.status = :status")
    Optional<OfficialPostAiMeta> findByOfficialPostIdWithTag(@Param("postId") Long postId, @Param("status") ProcessingStatus status);
}
