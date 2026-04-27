package com.campusnavi.backend.official.repository;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.official.entity.OfficialPostAiMeta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfficialPostAiMetaRepository extends JpaRepository<OfficialPostAiMeta,Long> {
    Optional<OfficialPostAiMeta> findByOfficialPostId(Long postId);

    @EntityGraph(attributePaths = "officialPost")
    List<OfficialPostAiMeta> findAllByStatusAndRetryCountLessThan(ProcessingStatus status, int retryCountIsLessThan);
}
