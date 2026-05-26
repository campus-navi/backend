package com.campusnavi.backend.scrap.repository;

import com.campusnavi.backend.scrap.entity.ScrapFolder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScrapFolderRepository extends JpaRepository<ScrapFolder, Long> {

    boolean existsByMemberIdAndName(Long memberId, String name);

    long countByMemberId(Long memberId);

    Optional<ScrapFolder> findByIdAndMemberId(Long id, Long memberId);

    List<ScrapFolder> findByMemberId(Long memberId, Sort sort);

    List<ScrapFolder> findAllByIdInAndMemberId(Collection<Long> ids, Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ScrapFolder f SET f.scrapCount = f.scrapCount + 1 WHERE f.id = :folderId")
    void incrementScrapCount(@Param("folderId") Long folderId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ScrapFolder f SET f.scrapCount = GREATEST(f.scrapCount - 1, 0) WHERE f.id = :folderId")
    void decrementScrapCount(@Param("folderId") Long folderId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ScrapFolder f SET f.scrapCount = f.scrapCount + :count WHERE f.id = :folderId")
    void incrementScrapCount(@Param("folderId") Long folderId, @Param("count") long count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ScrapFolder f SET f.scrapCount = GREATEST(f.scrapCount - :count, 0) WHERE f.id = :folderId")
    void decrementScrapCount(@Param("folderId") Long folderId, @Param("count") long count);
}
