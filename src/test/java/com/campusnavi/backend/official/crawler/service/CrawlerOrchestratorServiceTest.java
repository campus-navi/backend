package com.campusnavi.backend.official.crawler.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.failure.entity.CrawlFailure;
import com.campusnavi.backend.official.crawler.failure.service.CrawlFailureService;
import com.campusnavi.backend.official.crawler.failure.service.RetryAction;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
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

    @Mock
    private CrawlFailureService crawlFailureService;

    @Mock
    private Executor adminTaskExecutor;

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

    private void runsInline() {
        willAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).given(adminTaskExecutor).execute(any());
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
            orchestratorService.runAllScheduled();

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
            assertThatCode(() -> orchestratorService.runAllScheduled()).doesNotThrowAnyException();
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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

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
            orchestratorService.runAllScheduled();

            // then
            then(crawlerPostService).should(times(1)).crawlAndSave(any(), eq(post2), any());
            then(crawlFailureService).should().record(eq(source), eq(post1), any(Throwable.class));
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
            runsInline();
            given(postRepository.findOriginalIdsBySourceId(any())).willReturn(new HashSet<>());
            PostList newPost = new PostList("1", "새 게시물", null, "https://test.com/1",
                    LocalDate.of(2024, 1, 15));
            given(parser.fetchList(any(), eq(1))).willReturn(List.of(newPost));
            given(parser.fetchList(any(), eq(2))).willReturn(List.of());

            // when
            orchestratorService.startSeedAsync();

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
            orchestratorService.runAllScheduled();

            // then
            then(crawlerPostService).should().crawlAndSave(eq(source), eq(newPost), eq(parser));
            then(crawlerPostService).should(never()).crawlAndSaveSeed(any(), any(), any());
        }

        @Test
        @DisplayName("Seed 크롤링 중 예외가 발생해도 나머지 소스는 계속 처리한다")
        void sourceFailure() {
            // given
            runsInline();
            given(parserFactory.getParser(any())).willThrow(new RuntimeException("파서 오류"));

            // when & then
            assertThatCode(() -> orchestratorService.startSeedAsync()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("비동기 실행 가드")
    class AsyncGuard {

        @Test
        @DisplayName("실행 중 재요청이면 예외가 발생한다")
        void alreadyRunning() {
            // given
            orchestratorService.startSeedAsync();

            // when & then
            assertThatThrownBy(() -> orchestratorService.startSeedAsync())
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CRAWL_ALREADY_RUNNING));
        }

        @Test
        @DisplayName("작업 완료 후에는 다시 실행할 수 있다")
        void guardReleased() {
            // given
            given(sourceRepository.findAllByIsActiveTrue()).willReturn(List.of());
            orchestratorService.startSeedAsync();
            assertThat(orchestratorService.status().running()).isTrue();

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            then(adminTaskExecutor).should().execute(captor.capture());

            // when
            captor.getValue().run();

            // then
            assertThat(orchestratorService.status().running()).isFalse();
            assertThat(orchestratorService.status().finishedAt()).isNotNull();
            assertThatCode(() -> orchestratorService.startSeedAsync()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("크롤링 본체가 예외를 던져도 가드가 해제된다")
        void guardReleasedOnFailure() {
            // given
            given(sourceRepository.findAllByIsActiveTrue()).willThrow(new RuntimeException("DB 오류"));
            orchestratorService.startSeedAsync();

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            then(adminTaskExecutor).should().execute(captor.capture());

            // when
            assertThatCode(() -> captor.getValue().run()).doesNotThrowAnyException();

            // then
            assertThat(orchestratorService.status().running()).isFalse();
        }

        @Test
        @DisplayName("시드 크롤링 실행 중이면 스케줄 크롤링은 skip 한다")
        void scheduledSkipped() {
            // given
            orchestratorService.startSeedAsync();

            // when
            orchestratorService.runAllScheduled();

            // then
            then(sourceRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("시드 크롤링 실행 중이면 재시도 스케줄은 skip 한다")
        void retrySkipped() {
            // given
            orchestratorService.startSeedAsync();

            // when
            orchestratorService.retryAll();

            // then
            then(crawlFailureService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("재시도 스케줄 완료 후 가드가 해제된다")
        void guardReleasedAfterRetry() {
            // given
            given(crawlFailureService.findRetryTargets()).willReturn(List.of());
            orchestratorService.retryAll();

            // when & then
            assertThatCode(() -> orchestratorService.startSeedAsync()).doesNotThrowAnyException();
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
            orchestratorService.runAllScheduled();

            // then
            assertThat(source.getLastCrawledAt()).isEqualTo(LocalDate.now());
            then(sourceRepository).should().save(source);
        }
    }

    @Nested
    @DisplayName("재시도 (retryAll)")
    class RetryAll {

        private CrawlFailure failure(Long id, Long sourceId, String originalId) {
            PostList post = new PostList(originalId, "제목", "공지팀", "https://test.com/" + id,
                    LocalDate.of(2026, 5, 13));
            OfficialSource src = OfficialSource.create(1L, 1L, null, null, SourceType.CRAWL,
                    "X", "X", "https://x.com");
            ReflectionTestUtils.setField(src, "id", sourceId);
            CrawlFailure f = CrawlFailure.create(src, post, new RuntimeException("이전 오류"));
            ReflectionTestUtils.setField(f, "id", id);
            ReflectionTestUtils.setField(f, "sourceId", sourceId);
            return f;
        }

        @Test
        @DisplayName("재시도 대상이 없으면 source/parser 조회와 retryOne을 호출하지 않는다")
        void noTargets() {
            // given
            given(crawlFailureService.findRetryTargets()).willReturn(List.of());

            // when
            orchestratorService.retryAll();

            // then
            then(sourceRepository).should(never()).findAllById(any());
            then(parserFactory).shouldHaveNoInteractions();
            then(crawlFailureService).should(never()).retryOne(any(), any());
        }

        @Test
        @DisplayName("source가 조회되지 않으면 해당 failure 들은 skip 하고 다음 source로 넘어간다")
        void sourceNotFound() {
            // given
            CrawlFailure f1 = failure(100L, 99L, "orig-1");
            given(crawlFailureService.findRetryTargets()).willReturn(List.of(f1));
            given(sourceRepository.findAllById(Set.of(99L))).willReturn(List.of());

            // when
            orchestratorService.retryAll();

            // then
            then(parserFactory).shouldHaveNoInteractions();
            then(crawlFailureService).should(never()).retryOne(any(), any());
        }

        @Test
        @DisplayName("source 가 비활성이면 해당 source 의 failure 들은 skip 한다")
        void inactiveSourceSkipped() {
            // given
            ReflectionTestUtils.setField(source, "id", 10L);
            ReflectionTestUtils.setField(source, "isActive", false);
            CrawlFailure f1 = failure(100L, 10L, "orig-1");
            given(crawlFailureService.findRetryTargets()).willReturn(List.of(f1));
            given(sourceRepository.findAllById(Set.of(10L))).willReturn(List.of(source));

            // when
            orchestratorService.retryAll();

            // then
            then(parserFactory).shouldHaveNoInteractions();
            then(crawlFailureService).should(never()).retryOne(any(), any());
        }

        @Test
        @DisplayName("같은 source 에 여러 failure 가 있어도 parser 조회는 한 번만 한다")
        void parserLookupOncePerSource() {
            // given
            ReflectionTestUtils.setField(source, "id", 10L);
            CrawlFailure f1 = failure(100L, 10L, "orig-1");
            CrawlFailure f2 = failure(101L, 10L, "orig-2");
            given(crawlFailureService.findRetryTargets()).willReturn(List.of(f1, f2));
            given(sourceRepository.findAllById(Set.of(10L))).willReturn(List.of(source));
            given(parserFactory.getParser(any())).willReturn(parser);

            // when
            orchestratorService.retryAll();

            // then
            then(parserFactory).should(times(1)).getParser(eq(source.getParserType()));
            then(crawlFailureService).should(times(2)).retryOne(any(), any());
        }

        @Test
        @DisplayName("retryOne 에 넘긴 action 을 실행하면 crawlAndSave 가 호출된다")
        void actionInvokesCrawlAndSave() throws Exception {
            // given
            ReflectionTestUtils.setField(source, "id", 10L);
            CrawlFailure f1 = failure(100L, 10L, "orig-1");
            given(crawlFailureService.findRetryTargets()).willReturn(List.of(f1));
            given(sourceRepository.findAllById(Set.of(10L))).willReturn(List.of(source));
            given(parserFactory.getParser(any())).willReturn(parser);

            // when
            orchestratorService.retryAll();

            // then
            ArgumentCaptor<RetryAction> actionCaptor = ArgumentCaptor.forClass(RetryAction.class);
            then(crawlFailureService).should().retryOne(eq(f1), actionCaptor.capture());
            actionCaptor.getValue().execute();
            then(crawlerPostService).should().crawlAndSave(eq(source), any(PostList.class), eq(parser));
        }
    }
}
