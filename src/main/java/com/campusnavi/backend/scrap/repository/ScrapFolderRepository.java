package com.campusnavi.backend.scrap.repository;

import com.campusnavi.backend.scrap.entity.ScrapFolder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapFolderRepository extends JpaRepository<ScrapFolder, Long> {

    boolean existsByMemberIdAndName(Long memberId, String name);

    Optional<ScrapFolder> findByIdAndMemberId(Long id, Long memberId);

    List<ScrapFolder> findByMemberId(Long memberId, Sort sort);
}
