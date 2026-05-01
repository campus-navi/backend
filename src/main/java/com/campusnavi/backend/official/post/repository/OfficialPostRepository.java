package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPost;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface OfficialPostRepository extends JpaRepository<OfficialPost,Long> {
    @Query("SELECT p.originalId FROM OfficialPost p WHERE p.source.id = :sourceId")
    Set<String> findOriginalIdsBySourceId(@Param("sourceId") Long sourceId);

    Optional<OfficialPost> findByIdAndIsActiveTrue(Long id);
}
