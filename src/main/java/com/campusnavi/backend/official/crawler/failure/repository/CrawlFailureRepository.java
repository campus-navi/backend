package com.campusnavi.backend.official.crawler.failure.repository;

import com.campusnavi.backend.official.crawler.failure.entity.CrawlFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrawlFailureRepository extends JpaRepository<CrawlFailure, Long> {

    Optional<CrawlFailure> findBySourceIdAndOriginalId(Long sourceId, String originalId);

    List<CrawlFailure> findByRetryCountLessThan(int maxRetry);
}
