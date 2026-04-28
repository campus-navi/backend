package com.campusnavi.backend.official.crawler.service;

import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.parser.CrawlParser;
import com.campusnavi.backend.official.crawler.parser.CrawlParserFactory;
import com.campusnavi.backend.official.source.entity.OfficialSource;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.source.repository.OfficialSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerOrchestratorService {

    private final OfficialSourceRepository sourceRepository;
    private final OfficialPostRepository postRepository;
    private final CrawlParserFactory parserFactory;
    private final CrawlerPostService crawlerPostService;

    public void runAll() {
        run(false);
    }

    public void runSeed() {
        run(true);
    }

    public void run(boolean isSeed) {
        List<OfficialSource> sources = sourceRepository.findAllByIsActiveTrue();
        log.info("크롤링 대상 소스 수: {}", sources.size());
        for (OfficialSource source : sources) {
            try {
                crawlSource(source, isSeed);
            } catch (Exception e) {
                log.error("소스 크롤링 실패 [{}]: {}", source.getName(), e.getMessage(), e);
            }
        }
    }

    private void crawlSource(OfficialSource source, boolean isSeed) {
        log.debug("소스 크롤링 시작 [{}] lastCrawledAt={}", source.getName(), source.getLastCrawledAt());
        CrawlParser parser = parserFactory.getParser(source.getParserType());
        Set<String> existingIds = postRepository.findOriginalIdsBySourceId(source.getId());
        LocalDate lastCrawledAt = source.getLastCrawledAt();
        int totalSaved = 0;

        for (int page = 1; ; page++) {
            List<PostList> lists = parser.fetchList(source.getListUrl(), page);
            log.debug("  [{}] page={} 조회됨={}", source.getName(), page, lists.size());
            if (lists.isEmpty()) break;

            int nullDate = 0, tooOld = 0, duplicate = 0, saved = 0;
            boolean hasNewPost = false;
            for (PostList post : lists) {
                if (post.publishedAt() == null) {
                    nullDate++;
                    continue;
                }
                if (post.publishedAt().isBefore(lastCrawledAt)) {
                    tooOld++;
                    continue;
                }
                if (existingIds.contains(post.originalId())) {
                    duplicate++;
                    continue;
                }

                hasNewPost = true;
                try {
                    if (isSeed) {
                        crawlerPostService.crawlAndSaveSeed(source, post, parser);
                    } else {
                        crawlerPostService.crawlAndSave(source, post, parser);
                    }
                    existingIds.add(post.originalId());
                    saved++;
                } catch (Exception e) {
                    log.error("게시물 크롤링 실패 [{}]: {}", post.title(), e.getMessage(), e);
                }
            }
            log.debug("  [{}] page={} 결과: 저장={} / 날짜없음={} / 날짜오래됨={} / 중복={}", source.getName(), page, saved, nullDate, tooOld, duplicate);
            totalSaved += saved;

            if (!hasNewPost) break;
        }

        source.updateLastCrawledAt(LocalDate.now());
        sourceRepository.save(source);
        log.debug("소스 크롤링 완료 [{}] 총 저장={}", source.getName(), totalSaved);
    }
}
