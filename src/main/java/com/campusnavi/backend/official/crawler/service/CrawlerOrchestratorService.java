package com.campusnavi.backend.official.crawler.service;

import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.parser.CrawlParser;
import com.campusnavi.backend.official.crawler.parser.CrawlParserFactory;
import com.campusnavi.backend.official.entity.OfficialSource;
import com.campusnavi.backend.official.repository.OfficialPostRepository;
import com.campusnavi.backend.official.repository.OfficialSourceRepository;
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
    private final CrawlPostService crawlPostService;

    public void runAll() {
        List<OfficialSource> sources = sourceRepository.findAllByIsActiveTrue();
        log.info("크롤링 대상 소스 수: {}", sources.size());
        for (OfficialSource source : sources) {
            try {
                crawlSource(source);
            } catch (Exception e) {
                log.error("소스 크롤링 실패 [{}]: {}", source.getName(), e.getMessage(), e);
            }
        }
    }

    private void crawlSource(OfficialSource source) {
        log.info("소스 크롤링 시작 [{}]", source.getName());
        CrawlParser parser = parserFactory.getParser(source.getParserType());
        Set<String> existingIds = postRepository.findOriginalIdsBySourceId(source.getId());
        LocalDate lastCrawledAt = source.getLastCrawledAt();

        for (int page = 1; ; page++) {
            List<PostList> lists = parser.fetchList(source.getListUrl(), page);
            if (lists.isEmpty()) break;

            boolean hasNewPost = false;
            for (PostList post : lists) {
                if (post.publishedAt() == null) continue;
                if (post.publishedAt().isBefore(lastCrawledAt)) continue;
                if (existingIds.contains(post.originalId())) continue;

                hasNewPost = true;
                try {
                    crawlPostService.crawlAndSave(source, post, parser);
                    existingIds.add(post.originalId());
                } catch (Exception e) {
                    log.error("게시물 크롤링 실패 [{}]: {}", post.title(), e.getMessage(), e);
                }
            }

            if (!hasNewPost) break;
        }

        source.updateLastCrawledAt(LocalDate.now());
        sourceRepository.save(source);
        log.info("소스 크롤링 완료 [{}]", source.getName());
    }
}
