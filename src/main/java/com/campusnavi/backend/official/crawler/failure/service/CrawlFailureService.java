package com.campusnavi.backend.official.crawler.failure.service;

import com.campusnavi.backend.official.crawler.config.CrawlProperties;
import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.failure.entity.CrawlFailure;
import com.campusnavi.backend.official.crawler.failure.repository.CrawlFailureRepository;
import com.campusnavi.backend.official.source.entity.OfficialSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlFailureService {

    private final CrawlFailureRepository repository;
    private final CrawlFailureRetryHandler retryHandler;
    private final CrawlProperties properties;

    @Transactional
    public void record(OfficialSource source, PostList post, Throwable e) {
        repository.findBySourceIdAndOriginalId(source.getId(), post.originalId())
                .ifPresentOrElse(
                        existing -> existing.touchError(e),
                        () -> repository.save(CrawlFailure.create(source, post, e))
                );
    }

    @Transactional(readOnly = true)
    public List<CrawlFailure> findRetryTargets() {
        return repository.findByRetryCountLessThan(properties.retry().maxCount());
    }


    public void retryOne(CrawlFailure failure, RetryAction action) {
        try {
            action.execute();
            retryHandler.markSuccess(failure.getId());
        } catch (Exception e) {
            log.error("크롤링 재시도 실패 [sourceId={}, originalId={}]: {}",
                    failure.getSourceId(), failure.getOriginalId(), e.getMessage(), e);
            retryHandler.markFailure(failure.getId(), e);
        }
    }
}
