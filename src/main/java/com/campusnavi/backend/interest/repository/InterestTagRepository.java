package com.campusnavi.backend.interest.repository;

import com.campusnavi.backend.interest.entity.InterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestTagRepository extends JpaRepository<InterestTag,Long> {
    List<InterestTag> findByIsRecommendableTrueOrderBySortOrderAsc();
}
