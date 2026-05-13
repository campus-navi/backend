package com.campusnavi.backend.official.crawler.failure.service;

import com.campusnavi.backend.official.crawler.failure.repository.CrawlFailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class CrawlFailureRetryHandler {

    private final CrawlFailureRepository repository;

    @Transactional
    public void markSuccess(Long failureId) {
        repository.deleteById(failureId);
    }

    @Transactional
    public void markFailure(Long failureId, Throwable e) {
        repository.findById(failureId).ifPresent(failure -> failure.incrementRetry(e));
    }
}
