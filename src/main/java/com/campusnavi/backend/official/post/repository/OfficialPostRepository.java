package com.campusnavi.backend.official.post.repository;

import com.campusnavi.backend.official.post.entity.OfficialPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OfficialPostRepository extends JpaRepository<OfficialPost, Long> {

    @Query("SELECT p.originalId FROM OfficialPost p WHERE p.source.id = :sourceId")
    Set<String> findOriginalIdsBySourceId(@Param("sourceId") Long sourceId);

    @Query("""
            SELECT p FROM OfficialPost p
            WHERE p.id = :id
              AND p.isActive = true
              AND (p.universityId IS NULL OR p.universityId = :universityId)
            """)
    Optional<OfficialPost> findActiveByIdAndUniversityScope(
            @Param("id") Long id, @Param("universityId") Long universityId);

    @Query("""
            SELECT (COUNT(p) > 0) FROM OfficialPost p
            WHERE p.id = :id
              AND p.isActive = true
              AND (p.universityId IS NULL OR p.universityId = :universityId)
            """)
    boolean existsActiveByIdAndUniversityScope(
            @Param("id") Long id, @Param("universityId") Long universityId);

    @Query("""
            SELECT p FROM OfficialPost p
            WHERE p.id IN :ids
              AND (p.universityId IS NULL OR p.universityId = :universityId)
            """)
    List<OfficialPost> findByIdInAndUniversityScope(
            @Param("ids") Collection<Long> ids, @Param("universityId") Long universityId);
}
