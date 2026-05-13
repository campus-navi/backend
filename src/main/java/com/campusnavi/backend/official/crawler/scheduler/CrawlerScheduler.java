package com.campusnavi.backend.official.crawler.scheduler;

import com.campusnavi.backend.official.crawler.service.CrawlerOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crawl.enabled", havingValue = "true")
public class CrawlerScheduler {

    private final CrawlerOrchestratorService crawlerOrchestratorService;

    @Scheduled(cron = "0 30 8,17 * * *", zone = "Asia/Seoul")
    public void scheduleCrawl() {
        log.info("크롤링 스케줄 시작");
        crawlerOrchestratorService.runAll();
        log.info("크롤링 스케줄 완료");
    }

    @Scheduled(cron = "0 0 9,18 * * *", zone = "Asia/Seoul")
    public void scheduleRetry() {
        log.info("크롤링 재시도 스케줄 시작");
        crawlerOrchestratorService.retryAll();
        log.info("크롤링 재시도 스케줄 완료");
    }
}
