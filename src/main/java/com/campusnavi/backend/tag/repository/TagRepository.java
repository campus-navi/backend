package com.campusnavi.backend.tag.repository;

import com.campusnavi.backend.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag,Long> {
    List<Tag> findByIsRecommendableTrueOrderBySortOrderAsc();

    Optional<Tag> findByCode(String code);
}
