package com.campusnavi.backend.official.crawler.service;

import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.parser.CrawlParser;
import com.campusnavi.backend.official.crawler.parser.CrawlParserFactory;
import com.campusnavi.backend.official.source.entity.OfficialSource;
import com.campusnavi.backend.official.source.entity.SourceType;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.source.repository.OfficialSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CrawlerOrchestratorServiceTest {

    @Mock
    private OfficialSourceRepository sourceRepository;

    @Mock
    private OfficialPostRepository postRepository;

    @Mock
    private CrawlParserFactory parserFactory;

    @Mock
    private CrawlerPostService crawlerPostService;

    @InjectMocks
    private CrawlerOrchestratorService orchestratorService;

    @Mock
    private CrawlParser parser;

    private OfficialSource source;

    @BeforeEach
    void setUp() {
        source = OfficialSource.create(1L, 1L, null, null, SourceType.CRAWL,
                "KU_SEJONG_BASIC", "고려대 세종 공지", "https://test.com/list");
        source.updateLastCrawledAt(LocalDate.of(2024, 1, 1));
    }

    @Nested
    @DisplayName("전체 소스 크롤링")
    class RunAll {

        @Test
        @DisplayName("활성 소스가 없으면 크롤링을 수행하지 않는다")
        void noActiveSources() {
            // given
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of());

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("소스 크롤링 중 예외가 발생해도 나머지 소스는 계속 처리한다")
        void sourceFailure() {
            // given
            OfficialSource source2 = OfficialSource.create(1L, 1L, null, null, SourceType.CRAWL,
                    "KU_SEOUL_BASIC", "고려대 서울 공지", "https://test2.com/list");
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of(source, source2));
            given(parserFactory.getParser(any())).willThrow(new RuntimeException("파서 오류"));

            // when & then
            assertThatCode(() -> orchestratorService.runAll()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("페이지 순회")
    class PageIteration {

        @BeforeEach
        void setUp() {
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of(source));
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>());
            given(parserFactory.getParser(any())).willReturn(parser);
        }

        @Test
        @DisplayName("첫 페이지가 비어있으면 즉시 순회를 중단한다")
        void emptyFirstPage() {
            // given
            given(parser.fetchList(any(), anyInt())).willReturn(List.of());

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).shouldHaveNoInteractions();
            then(parser).should(times(1)).fetchList(any(), anyInt());
        }

        @Test
        @DisplayName("페이지에 새 게시물이 없으면 다음 페이지로 넘어가지 않는다")
        void noNewPostInPage() {
            // given
            PostList oldPost = new PostList("1", "오래된 게시물", null, "https://test.com/1",
                    LocalDate.of(2023, 12, 31));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(oldPost));

            // when
            orchestratorService.runAll();

            // then
            then(parser).should(times(1)).fetchList(any(), anyInt());
            then(crawlerPostService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("새 게시물이 있으면 다음 페이지도 순회한다")
        void hasNewPost() throws Exception {
            // given
            PostList newPost = new PostList("1", "새 게시물", null, "https://test.com/1",
                    LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(newPost));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of());

            // when
            orchestratorService.runAll();

            // then
            then(parser).should(times(2)).fetchList(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("게시물 필터링")
    class PostFiltering {

        @BeforeEach
        void setUp() {
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of(source));
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>());
            given(parserFactory.getParser(any())).willReturn(parser);
        }

        @Test
        @DisplayName("lastCrawledAt 이전 게시물이면 크롤링하지 않는다")
        void oldPost() throws Exception {
            // given
            PostList oldPost = new PostList("1", "오래된 게시물", null, "https://test.com/1",
                    LocalDate.of(2023, 12, 31));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(oldPost));

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should(never()).crawlAndSave(any(), eq(oldPost), any());
        }

        @Test
        @DisplayName("lastCrawledAt 이후 게시물이면 크롤링한다")
        void newPost() throws Exception {
            // given
            PostList newPost = new PostList("1", "새 게시물", null, "https://test.com/1",
                    LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(newPost));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of());

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should(times(1)).crawlAndSave(eq(source), eq(newPost), eq(parser));
        }

        @Test
        @DisplayName("publishedAt이 null인 게시물이면 스킵한다")
        void nullPublishedAt() throws Exception {
            // given
            PostList nullDatePost = new PostList("1", "날짜 없는 게시물", null, "https://test.com/1", null);
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(nullDatePost));

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should(never()).crawlAndSave(any(), eq(nullDatePost), any());
        }

        @Test
        @DisplayName("이미 수집된 originalId이면 크롤링하지 않는다")
        void duplicateOriginalId() throws Exception {
            // given
            PostList duplicatePost = new PostList("99", "이미 있는 게시물", null, "https://test.com/99",
                    LocalDate.of(2024, 1, 15));
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>(Set.of("99")));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(duplicatePost));

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should(never()).crawlAndSave(any(), eq(duplicatePost), any());
        }

        @Test
        @DisplayName("크롤링 성공한 게시물은 같은 실행 내에서 다시 수집하지 않는다")
        void runtimeDuplicatePrevented() throws Exception {
            // given
            PostList post = new PostList("1", "게시물", null, "https://test.com/1", LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(post));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of(post));

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should(times(1)).crawlAndSave(any(), eq(post), any());
        }

        @Test
        @DisplayName("게시물 크롤링이 실패해도 다음 게시물은 계속 처리한다")
        void postFailure() throws Exception {
            // given
            PostList post1 = new PostList("1", "게시물1", null, "https://test.com/1", LocalDate.of(2024, 1, 15));
            PostList post2 = new PostList("2", "게시물2", null, "https://test.com/2", LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(post1, post2));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of());
            willThrow(new RuntimeException("크롤링 오류")).given(crawlerPostService).crawlAndSave(any(), eq(post1), any());

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should(times(1)).crawlAndSave(any(), eq(post2), any());
        }
    }

    @Nested
    @DisplayName("Seed 크롤링")
    class SeedCrawl {

        @BeforeEach
        void setUp() {
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of(source));
            given(parserFactory.getParser(any())).willReturn(parser);
        }

        @Test
        @DisplayName("Seed 크롤링은 crawlAndSaveSeed를 호출한다")
        void savesSeed() throws Exception {
            // given
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>());
            PostList newPost = new PostList("1", "새 게시물", null, "https://test.com/1",
                    LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(newPost));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of());

            // when
            orchestratorService.runSeed();

            // then
            then(crawlerPostService).should().crawlAndSaveSeed(eq(source), eq(newPost), eq(parser));
            then(crawlerPostService).should(never()).crawlAndSave(any(), any(), any());
        }

        @Test
        @DisplayName("일반 크롤링은 crawlAndSave를 호출한다")
        void savesPost() throws Exception {
            // given
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>());
            PostList newPost = new PostList("1", "새 게시물", null, "https://test.com/1",
                    LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(newPost));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of());

            // when
            orchestratorService.runAll();

            // then
            then(crawlerPostService).should().crawlAndSave(eq(source), eq(newPost), eq(parser));
            then(crawlerPostService).should(never()).crawlAndSaveSeed(any(), any(), any());
        }

        @Test
        @DisplayName("Seed 크롤링 중 예외가 발생해도 나머지 소스는 계속 처리한다")
        void sourceFailure() {
            // given
            given(parserFactory.getParser(any())).willThrow(new RuntimeException("파서 오류"));

            // when & then
            assertThatCode(() -> orchestratorService.runSeed()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("완료 처리")
    class CompletionHandling {

        @Test
        @DisplayName("크롤링 완료 후 lastCrawledAt을 오늘 날짜로 업데이트하고 저장한다")
        void updatesLastCrawledAt() {
            // given
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of(source));
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>());
            given(parserFactory.getParser(any())).willReturn(parser);
            given(parser.fetchList(any(), anyInt())).willReturn(List.of());

            // when
            orchestratorService.runAll();

            // then
            assertThat(source.getLastCrawledAt()).isEqualTo(LocalDate.now());
            then(sourceRepository).should().save(source);
        }
    }
}
